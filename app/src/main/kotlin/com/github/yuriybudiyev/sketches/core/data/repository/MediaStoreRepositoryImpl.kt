/*
 * MIT License
 *
 * Copyright (c) 2024 Yuriy Budiyev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.yuriybudiyev.sketches.core.data.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.github.yuriybudiyev.sketches.core.collections.newLinkedHashMap
import com.github.yuriybudiyev.sketches.core.collections.newLinkedHashSet
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.dao.BookmarksDao
import com.github.yuriybudiyev.sketches.core.data.entity.BookmarkEntity
import com.github.yuriybudiyev.sketches.core.data.model.Bookmark
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.MediaAccess
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.checkMediaAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@Singleton
class MediaStoreRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    @Dispatcher(Dispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val bookmarksDao: BookmarksDao,
): MediaStoreRepository {

    override fun getFiles(): Flow<List<MediaStoreFile>> =
        allFilesFlow

    override fun getBookmarks(): Flow<List<MediaStoreFile>> =
        allFilesInBookmarksFlow

    override fun getBuckets(): Flow<List<MediaStoreBucket>> =
        allBucketsFlow

    override fun updateMediaAccess() {
        defaultCoroutineScope.launch {
            updateMediaAccessMutex.withLock {
                val current = currentMediaAccess
                val updated = appContext.checkMediaAccess()
                currentMediaAccess = updated
                if (current != updated || updated == MediaAccess.UserSelected) {
                    updateMediaStoreEntities()
                }
            }
        }
    }

    override suspend fun createBookmark(mediaId: Long) {
        bookmarksDao.upsert(
            BookmarkEntity(
                mediaId = mediaId,
                dateAdded = LocalDateTime.now(),
            ),
        )
    }

    override suspend fun deleteBookmarks(mediaIds: Collection<Long>) {
        withContext(ioDispatcher) {
            bookmarksDao.delete(mediaIds)
        }
    }

    override suspend fun deleteMedia(uris: Collection<Uri>) {
        withContext(ioDispatcher) {
            val ops = ArrayList<ContentProviderOperation>(uris.size)
            for (uri in uris) {
                ops.add(ContentProviderOperation.newDelete(uri).build())
            }
            appContext.contentResolver.applyBatch(
                MediaStore.AUTHORITY,
                ops,
            )
        }
    }

    private fun updateMediaStoreEntities(
        delayMillis: Long = 0L,
        nonCancellable: Boolean = false,
    ) {
        defaultCoroutineScope.launch {
            scheduleAllFilesUpdateMutex.withLock {
                updateAllFilesJob?.cancel()
                updateAllFilesJob = defaultCoroutineScope.launch {
                    withContext(if (nonCancellable) NonCancellable else EmptyCoroutineContext) {
                        delay(delayMillis)
                        val imageEntities =
                            withContext(ioDispatcher) { queryEntities(MediaType.Image) }
                        val videoEntities =
                            withContext(ioDispatcher) { queryEntities(MediaType.Video) }
                        val allEntitiesSize = imageEntities.size + videoEntities.size
                        val allEntities = ArrayList<MediaStoreEntity>(allEntitiesSize)
                        allEntities.addAll(imageEntities)
                        allEntities.addAll(videoEntities)
                        allEntities.sortByDescending { entity -> entity.dateAdded }
                        val allEntitiesIds = newLinkedHashSet<Long>(allEntitiesSize)
                        allEntities.mapTo(allEntitiesIds) { file -> file.id }
                        val allBookmarks = bookmarksDao.getAll().first()
                        val deletedBookmarksIds = allBookmarks.asSequence()
                            .filter { entity -> !allEntitiesIds.contains(entity.mediaId) }
                            .map { entity -> entity.mediaId }
                            .toList()
                        publishAllFilesMutex.withLock {
                            withContext(NonCancellable) {
                                allEntitiesFlow.emit(allEntities)
                                if (deletedBookmarksIds.isNotEmpty()) {
                                    bookmarksDao.delete(deletedBookmarksIds)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun queryEntities(mediaType: MediaType): List<MediaStoreEntity> {
        val contentUri = mediaType.contentUri
        val cursor = appContext.contentResolver.query(
            contentUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE,
            ),
            null,
            null,
            null,
        ) ?: return emptyList()
        cursor.use { cursor ->
            val files = ArrayList<MediaStoreEntity>(cursor.count)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketDisplayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                files.add(
                    MediaStoreEntity(
                        id = id,
                        bucketId = bucketId,
                        name = cursor.getStringOrNull(displayNameColumn) ?: id.toString(),
                        bucketName = cursor.getStringOrNull(bucketDisplayNameColumn)
                            ?: bucketId.toString(),
                        dateAdded = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(cursor.getLong(dateAddedColumn)),
                            ZoneId.systemDefault(),
                        ),
                        mediaType = mediaType,
                        mimeType = cursor.getStringOrNull(mimeTypeColumn)
                            ?: mediaType.mimeType,
                        uri = ContentUris.withAppendedId(
                            contentUri,
                            id,
                        ),
                    ),
                )
            }
            return files
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startFilesCollection() {
        defaultCoroutineScope.launch {
            combineTransform(
                allEntitiesFlow,
                allBookmarksFlow,
            ) { entities, bookmarks ->
                emit(entities to bookmarks)
            }.transformLatest { (entities, bookmarks) ->
                if (entities.isEmpty()) {
                    emit(emptyList())
                } else {
                    val files = ArrayList<MediaStoreFile>(entities.size)
                    for (entity in entities) {
                        files.add(
                            MediaStoreFile(
                                id = entity.id,
                                bucketId = entity.bucketId,
                                name = entity.name,
                                dateAdded = entity.dateAdded,
                                mediaType = entity.mediaType,
                                mimeType = entity.mimeType,
                                uri = entity.uri,
                                bookmark = bookmarks[entity.id],
                            ),
                        )
                    }
                    emit(files)
                }
            }.collect { files ->
                allFilesFlow.emit(files)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startBucketsCollection() {
        defaultCoroutineScope.launch {
            allEntitiesFlow.transformLatest { entities ->
                val bucketsInfo = LinkedHashMap<Long, MediaStoreBucketInfo>()
                for (entity in entities) {
                    val bucketId = entity.bucketId
                    val coverUri = entity.uri
                    val coverDateAdded = entity.dateAdded
                    val bucketInfo = bucketsInfo.getOrPut(bucketId) {
                        MediaStoreBucketInfo(
                            id = bucketId,
                            name = entity.bucketName,
                            coverUri = coverUri,
                            coverDateAdded = coverDateAdded,
                            size = 0,
                        )
                    }
                    bucketInfo.size++
                }
                val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
                for ((_, bucketInfo) in bucketsInfo) {
                    buckets.add(
                        MediaStoreBucket(
                            id = bucketInfo.id,
                            name = bucketInfo.name,
                            size = bucketInfo.size,
                            coverUri = bucketInfo.coverUri,
                            coverDateAdded = bucketInfo.coverDateAdded,
                        ),
                    )
                }
                emit(buckets)
            }.collect { buckets ->
                allBucketsFlow.emit(buckets)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startBookmarksCollection() {
        defaultCoroutineScope.launch {
            bookmarksDao.getAll().transformLatest { entities ->
                val bookmarks = newLinkedHashMap<Long, Bookmark>(entities.size)
                for (entity in entities) {
                    val mediaId = entity.mediaId
                    bookmarks[mediaId] = Bookmark(
                        mediaId = mediaId,
                        dateAdded = entity.dateAdded,
                    )
                }
                emit(bookmarks)
            }.collect { bookmarks ->
                allBookmarksFlow.emit(bookmarks)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startFilesInBookmarksCollection() {
        defaultCoroutineScope.launch {
            allFilesFlow.transformLatest { files ->
                val bookmarks = files.filterTo(ArrayList()) { file -> file.bookmark != null }
                bookmarks.sortByDescending { file -> file.bookmark!!.dateAdded }
                bookmarks.trimToSize()
                emit(bookmarks)
            }.collect { files ->
                allFilesInBookmarksFlow.emit(files)
            }
        }
    }

    private val defaultCoroutineScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob())

    private val allEntitiesFlow: MutableSharedFlow<List<MediaStoreEntity>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val allFilesFlow: MutableSharedFlow<List<MediaStoreFile>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val allBucketsFlow: MutableSharedFlow<List<MediaStoreBucket>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val allBookmarksFlow: MutableSharedFlow<Map<Long, Bookmark>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val allFilesInBookmarksFlow: MutableSharedFlow<List<MediaStoreFile>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    @Volatile
    private var updateAllFilesJob: Job? = null

    private val scheduleAllFilesUpdateMutex: Mutex = Mutex()

    private val publishAllFilesMutex: Mutex = Mutex()

    @Volatile
    private var currentMediaAccess: MediaAccess = appContext.checkMediaAccess()

    private val updateMediaAccessMutex: Mutex = Mutex()

    init {
        updateMediaStoreEntities()
        startFilesCollection()
        startBucketsCollection()
        startBookmarksCollection()
        startFilesInBookmarksCollection()
        val mediaObserver = MediaObserver()
        with(appContext.contentResolver) {
            registerContentObserver(
                MediaType.Image.contentUri,
                true,
                mediaObserver,
            )
            registerContentObserver(
                MediaType.Video.contentUri,
                true,
                mediaObserver,
            )
        }
    }

    private data class MediaStoreEntity(
        val id: Long,
        val bucketId: Long,
        val name: String,
        val bucketName: String,
        val dateAdded: LocalDateTime,
        val mediaType: MediaType,
        val mimeType: String,
        val uri: Uri,
    )

    private data class MediaStoreBucketInfo(
        val id: Long,
        val name: String,
        val coverUri: Uri,
        val coverDateAdded: LocalDateTime,
        var size: Int,
    )

    private inner class MediaObserver: ContentObserver(Handler(Looper.getMainLooper())) {

        override fun onChange(selfChange: Boolean) {
            val currentCallTime = SystemClock.uptimeMillis()
            if (currentCallTime - lastCallTime > 1000L) {
                lastCallTime = currentCallTime
                updateMediaStoreEntities(nonCancellable = true)
            } else {
                updateMediaStoreEntities(delayMillis = 1000L)
            }
        }

        private var lastCallTime: Long = SystemClock.uptimeMillis()
    }
}
