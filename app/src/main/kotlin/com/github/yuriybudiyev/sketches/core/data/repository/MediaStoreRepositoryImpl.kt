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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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

    override fun updateMediaAccess() {
        defaultCoroutineScope.launch {
            updateMediaAccessMutex.withLock {
                val current = currentMediaAccess
                val updated = appContext.checkMediaAccess()
                currentMediaAccess = updated
                if (current != updated || updated == MediaAccess.UserSelected) {
                    updateAllFiles()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getBookmarks(): Flow<Map<Long, Bookmark>> =
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
        }.flowOn(defaultDispatcher)

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

    override suspend fun deleteContent(uris: Collection<Uri>) {
        withContext(ioDispatcher) {
            contentResolverMutex.withLock {
                val contentResolver = appContext.contentResolver
                for (uri in uris) {
                    contentResolver.delete(
                        uri,
                        null,
                        null,
                    )
                }
            }
        }
    }

    private fun updateAllFiles(
        delayMillis: Long = 0L,
        nonCancellable: Boolean = false,
    ) {
        defaultCoroutineScope.launch {
            scheduleAllFilesUpdateMutex.withLock {
                updateAllFilesJob?.cancel()
                updateAllFilesJob = defaultCoroutineScope.launch {
                    withContext(if (nonCancellable) NonCancellable else EmptyCoroutineContext) {
                        delay(delayMillis)
                        val imageFiles = getAllFiles(MediaType.Image)
                        val videoFiles = getAllFiles(MediaType.Video)
                        val allFilesSize = imageFiles.size + videoFiles.size
                        val allFiles = ArrayList<MediaStoreFile>(allFilesSize)
                        allFiles.addAll(imageFiles)
                        allFiles.addAll(videoFiles)
                        allFiles.sortByDescending { file -> file.dateAdded }
                        val allFilesIds = newLinkedHashSet<Long>(allFilesSize)
                        allFiles.mapTo(allFilesIds) { file -> file.id }
                        val allBookmarks = bookmarksDao.getAll().first()
                        val deletedBookmarksIds = allBookmarks.asSequence()
                            .filter { entity -> !allFilesIds.contains(entity.mediaId) }
                            .map { entity -> entity.mediaId }
                            .toList()
                        publishAllFilesMutex.withLock {
                            withContext(NonCancellable) {
                                allFilesFlow.emit(allFiles)
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

    private suspend fun getAllFiles(mediaType: MediaType): List<MediaStoreFile> =
        withContext(ioDispatcher) {
            contentResolverMutex.withLock {
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
                ) ?: return@withLock emptyList()
                cursor.use { cursor ->
                    val files = ArrayList<MediaStoreFile>(cursor.count)
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val bucketIdColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                    val displayNameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val bucketDisplayNameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    val dateAddedColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val mimeTypeColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val bucketId = cursor.getLong(bucketIdColumn)
                        files.add(
                            MediaStoreFile(
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
                    return@withLock files
                }
            }
        }

    private val defaultCoroutineScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob())

    private val allFilesFlow: MutableSharedFlow<List<MediaStoreFile>> =
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

    private val contentResolverMutex: Mutex = Mutex()

    init {
        updateAllFiles()
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

    private inner class MediaObserver: ContentObserver(Handler(Looper.getMainLooper())) {

        override fun onChange(selfChange: Boolean) {
            val currentCallTime = SystemClock.uptimeMillis()
            if (currentCallTime - lastCallTime > 1000L) {
                lastCallTime = currentCallTime
                updateAllFiles(nonCancellable = true)
            } else {
                updateAllFiles(delayMillis = 1000L)
            }
        }

        private var lastCallTime: Long = SystemClock.uptimeMillis()
    }
}
