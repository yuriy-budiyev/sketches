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
import com.github.yuriybudiyev.sketches.core.data.dao.HiddenBucketsDao
import com.github.yuriybudiyev.sketches.core.data.entity.BookmarkEntity
import com.github.yuriybudiyev.sketches.core.data.entity.HiddenBucketEntity
import com.github.yuriybudiyev.sketches.core.data.model.Bookmark
import com.github.yuriybudiyev.sketches.core.data.model.MediaBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaStoreBatchSize
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.MediaAccess
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.checkMediaAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
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
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    @Dispatcher(Dispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val bookmarksDao: BookmarksDao,
    private val hiddenBucketsDao: HiddenBucketsDao,
): MediaRepository {

    override fun getFiles(): Flow<List<MediaFile>> =
        mediaFilesFlow

    override fun getFilesExcludingHiddenBuckets(): Flow<List<MediaFile>> =
        mediaFilesExcludingHiddenBucketsFlow

    override fun getBookmarks(): Flow<List<MediaFile>> =
        bookmarksFlow

    override fun getBuckets(): Flow<List<MediaBucket>> =
        bucketsFlow

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

    override suspend fun deleteMedia(uris: Collection<Uri>) {
        withContext(ioDispatcher) {
            val chunkSize = uris.size.coerceAtMost(MediaStoreBatchSize)
            val ops = ArrayList<ContentProviderOperation>(chunkSize)
            val contentResolver = appContext.contentResolver
            for (uri in uris) {
                ops.add(ContentProviderOperation.newDelete(uri).build())
                if (ops.size == chunkSize) {
                    contentProviderMutex.withLock {
                        contentResolver.applyBatch(
                            MediaStore.AUTHORITY,
                            ops,
                        )
                    }
                    ops.clear()
                }
            }
            if (ops.isNotEmpty()) {
                contentProviderMutex.withLock {
                    contentResolver.applyBatch(
                        MediaStore.AUTHORITY,
                        ops,
                    )
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

    override fun getHiddenBuckets(): Flow<Set<Long>> =
        hiddenBucketsFlow

    override suspend fun showBucket(bucketId: Long) {
        withContext(ioDispatcher) {
            hiddenBucketsDao.delete(listOf(bucketId))
        }
    }

    override suspend fun hideBucket(bucketId: Long) {
        withContext(ioDispatcher) {
            hiddenBucketsDao.upsert(HiddenBucketEntity(bucketId))
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
                        val imageEntities: List<MediaEntity>
                        val videoEntities: List<MediaEntity>
                        withContext(ioDispatcher) {
                            contentProviderMutex.withLock {
                                imageEntities = queryMediaStoreEntities(MediaType.Image)
                                videoEntities = queryMediaStoreEntities(MediaType.Video)
                            }
                        }
                        val allEntities =
                            ArrayList<MediaEntity>(imageEntities.size + videoEntities.size)
                        allEntities.addAll(imageEntities)
                        allEntities.addAll(videoEntities)
                        allEntities.sortByDescending { entity -> entity.dateAdded }
                        entitiesFlow.emit(allEntities)
                    }
                }
            }
        }
    }

    private fun queryMediaStoreEntities(mediaType: MediaType): List<MediaEntity> {
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
            val files = ArrayList<MediaEntity>(cursor.count)
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
                    MediaEntity(
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

    private fun startMediaFilesFlow() {
        defaultCoroutineScope.launch {
            combineTransform(
                entitiesFlow,
                bookmarksByMediaIdsFlow,
            ) { entities, bookmarks ->
                emit(entities to bookmarks)
            }.collectLatest { (entities, bookmarks) ->
                mediaFilesFlow.emit(
                    entities.mapTo(ArrayList(entities.size)) { entity ->
                        MediaFile(
                            id = entity.id,
                            bucketId = entity.bucketId,
                            name = entity.name,
                            dateAdded = entity.dateAdded,
                            mediaType = entity.mediaType,
                            mimeType = entity.mimeType,
                            uri = entity.uri,
                            bookmark = bookmarks[entity.id],
                        )
                    },
                )
            }
        }
    }

    private fun startMediaFilesExcludingHiddenBucketsFlow() {
        defaultCoroutineScope.launch {
            combineTransform(
                mediaFilesFlow,
                hiddenBucketsFlow,
            ) { files, hiddenBuckets ->
                emit(files to hiddenBuckets)
            }.collectLatest { (files, hiddenBuckets) ->
                mediaFilesExcludingHiddenBucketsFlow.emit(
                    if (hiddenBuckets.isNotEmpty()) {
                        files
                            .filterTo(ArrayList(files.size)) { file -> !hiddenBuckets.contains(file.bucketId) }
                            .apply { trimToSize() }
                    } else {
                        files
                    },
                )
            }
        }
    }

    private fun startBucketsFlow() {
        defaultCoroutineScope.launch {
            combineTransform(
                entitiesFlow,
                hiddenBucketsFlow,
            ) { entities, hiddenBuckets ->
                emit(entities to hiddenBuckets)
            }.collectLatest { (entities, hiddenBuckets) ->
                val bucketsInfo = newLinkedHashMap<Long, MediaBucketInfo>(entities.size)
                for (entity in entities) {
                    val bucketId = entity.bucketId
                    val coverUri = entity.uri
                    val coverDateAdded = entity.dateAdded
                    val bucketInfo = bucketsInfo.getOrPut(bucketId) {
                        MediaBucketInfo(
                            id = bucketId,
                            name = entity.bucketName,
                            coverUri = coverUri,
                            coverDateAdded = coverDateAdded,
                            size = 0,
                        )
                    }
                    bucketInfo.size++
                }
                val buckets = ArrayList<MediaBucket>(bucketsInfo.size)
                for ((_, bucketInfo) in bucketsInfo) {
                    val bucketId = bucketInfo.id
                    buckets.add(
                        MediaBucket(
                            id = bucketId,
                            name = bucketInfo.name,
                            size = bucketInfo.size,
                            coverUri = bucketInfo.coverUri,
                            coverDateAdded = bucketInfo.coverDateAdded,
                            isHidden = hiddenBuckets.contains(bucketId),
                        ),
                    )
                }
                bucketsFlow.emit(buckets)
            }
        }
    }

    private fun startBookmarksFlow() {
        defaultCoroutineScope.launch {
            mediaFilesFlow.collectLatest { files ->
                bookmarksFlow.emit(
                    files
                        .filterTo(ArrayList(files.size)) { file -> file.bookmark != null }
                        .apply { sortByDescending { file -> file.bookmark!!.dateAdded } }
                        .apply { trimToSize() },
                )
            }
        }
    }

    private fun startBookmarksByMediaIdsFlow() {
        defaultCoroutineScope.launch {
            bookmarksDao.getAll().collectLatest { entities ->
                val bookmarks = newLinkedHashMap<Long, Bookmark>(entities.size)
                for (entity in entities) {
                    val mediaId = entity.mediaId
                    bookmarks[mediaId] = Bookmark(
                        mediaId = mediaId,
                        dateAdded = entity.dateAdded,
                    )
                }
                bookmarksByMediaIdsFlow.emit(bookmarks)
            }
        }
    }

    private fun startBookmarksGarbageCollection() {
        defaultCoroutineScope.launch {
            entitiesFlow.collectLatest { entities ->
                val entitiesIds =
                    entities.mapTo(newLinkedHashSet(entities.size)) { entity -> entity.id }
                val bookmarksByMediaIds = bookmarksByMediaIdsFlow.first()
                val bookmarksToDelete = bookmarksByMediaIds.asSequence()
                    .filter { (mediaId, _) -> !entitiesIds.contains(mediaId) }
                    .map { (mediaId, _) -> mediaId }
                    .toCollection(ArrayList(bookmarksByMediaIds.size))
                    .apply { trimToSize() }
                if (bookmarksToDelete.isNotEmpty()) {
                    withContext(ioDispatcher) {
                        bookmarksDao.delete(bookmarksToDelete)
                    }
                }
            }
        }
    }

    private fun startHiddenBucketsFlow() {
        defaultCoroutineScope.launch {
            hiddenBucketsDao.getAll().collectLatest { entities ->
                hiddenBucketsFlow.emit(
                    entities.mapTo(newLinkedHashSet(entities.size)) { entity -> entity.bucketId },
                )
            }
        }
    }

    private fun startHiddenBucketsGarbageCollection() {
        defaultCoroutineScope.launch {
            entitiesFlow.collectLatest { entities ->
                val bucketsIds =
                    entities.mapTo(newLinkedHashSet(entities.size)) { entity -> entity.bucketId }
                val hiddenBuckets = hiddenBucketsFlow.first()
                val hiddenBucketsToDelete = hiddenBuckets
                    .filterTo(ArrayList(hiddenBuckets.size)) { bucketId ->
                        !bucketsIds.contains(bucketId)
                    }
                    .apply { trimToSize() }
                if (hiddenBucketsToDelete.isNotEmpty()) {
                    withContext(ioDispatcher) {
                        hiddenBucketsDao.delete(hiddenBucketsToDelete)
                    }
                }
            }
        }
    }

    private val defaultCoroutineScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob())

    private val entitiesFlow: MutableSharedFlow<List<MediaEntity>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val mediaFilesFlow: MutableSharedFlow<List<MediaFile>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val mediaFilesExcludingHiddenBucketsFlow: MutableSharedFlow<List<MediaFile>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val bucketsFlow: MutableSharedFlow<List<MediaBucket>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val bookmarksFlow: MutableSharedFlow<List<MediaFile>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val bookmarksByMediaIdsFlow: MutableSharedFlow<Map<Long, Bookmark>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val hiddenBucketsFlow: MutableSharedFlow<Set<Long>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    @Volatile
    private var updateAllFilesJob: Job? = null

    private val scheduleAllFilesUpdateMutex: Mutex = Mutex()

    @Volatile
    private var currentMediaAccess: MediaAccess = appContext.checkMediaAccess()

    private val updateMediaAccessMutex: Mutex = Mutex()

    private val contentProviderMutex: Mutex = Mutex()

    init {
        startMediaFilesFlow()
        startMediaFilesExcludingHiddenBucketsFlow()
        startBucketsFlow()
        startBookmarksFlow()
        startBookmarksByMediaIdsFlow()
        startBookmarksGarbageCollection()
        startHiddenBucketsFlow()
        startHiddenBucketsGarbageCollection()
        updateMediaStoreEntities()
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

    private data class MediaEntity(
        val id: Long,
        val bucketId: Long,
        val name: String,
        val bucketName: String,
        val dateAdded: LocalDateTime,
        val mediaType: MediaType,
        val mimeType: String,
        val uri: Uri,
    )

    private data class MediaBucketInfo(
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
