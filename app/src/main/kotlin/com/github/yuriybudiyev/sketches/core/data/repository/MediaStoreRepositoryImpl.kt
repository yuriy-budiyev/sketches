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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.collection.MutableLongObjectMap
import androidx.core.database.getStringOrNull
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.dao.BookmarksDao
import com.github.yuriybudiyev.sketches.core.data.entity.BookmarkEntity
import com.github.yuriybudiyev.sketches.core.data.model.Bookmark
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

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

    fun getAllFiles(): Flow<List<MediaStoreFile>> =
        allFilesFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllBuckets(): Flow<List<MediaStoreBucket>> =
        allFilesFlow.transformLatest { files ->
            val bucketsInfo = LinkedHashMap<Long, MediaStoreBucketInfo>()
            for (file in files) {
                val bucketId = file.bucketId
                val coverUri = file.uri
                val coverDateAdded = file.dateAdded
                val bucketInfo = bucketsInfo.getOrPut(bucketId) {
                    MediaStoreBucketInfo(
                        id = bucketId,
                        name = file.bucketName,
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
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBucketFiles(bucketId: Long): Flow<List<MediaStoreFile>> =
        allFilesFlow.mapLatest { files -> files.filter { file -> file.bucketId == bucketId } }

    suspend fun getBucketsContent(bucketIds: Set<Long>): List<MediaStoreFile> =
        allFilesFlow.first().filter { file -> bucketIds.contains(file.id) }

    private fun collectAllFiles(mediaType: MediaType): List<MediaStoreFile> {
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
            val files = ArrayList<MediaStoreFile>(cursor.count)
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
                        mimeType = cursor.getStringOrNull(mimeTypeColumn) ?: mediaType.mimeType,
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

    private fun updateAllFiles(delayMillis: Long = 0L) {
        updateAllFilesJob?.cancel()
        updateAllFilesJob = defaultCoroutineScope.launch {
            delay(delayMillis)
            val imageFiles = withContext(ioDispatcher) { collectAllFiles(MediaType.Image) }
            val videoFiles = withContext(ioDispatcher) { collectAllFiles(MediaType.Video) }
            val allFiles = ArrayList<MediaStoreFile>(imageFiles.size + videoFiles.size)
            allFiles.addAll(imageFiles)
            allFiles.addAll(videoFiles)
            allFiles.sortByDescending { file -> file.dateAdded }
            allFilesFlow.emit(allFiles)
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

    private var updateAllFilesJob: Job? = null

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

        override fun onChange(
            selfChange: Boolean,
            uri: Uri?,
        ) {
            val currentCallTime = SystemClock.uptimeMillis()
            if (currentCallTime - lastCallTime > 1000L) {
                lastCallTime = currentCallTime
                updateAllFiles()
            } else {
                updateAllFiles(delayMillis = 1000L)
            }
        }

        private var lastCallTime: Long = SystemClock.uptimeMillis()
    }

    private data class MediaStoreBucketInfo(
        val id: Long,
        val name: String,
        val coverUri: Uri,
        val coverDateAdded: LocalDateTime,
        var size: Int,
    )

    //--- Old code ---

    private fun collectFiles(
        mediaType: MediaType,
        bucketId: Long?,
    ): List<MediaStoreFile> {
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
            if (bucketId != null) {
                "${MediaStore.MediaColumns.BUCKET_ID}=?"
            } else {
                null
            },
            if (bucketId != null) {
                arrayOf(bucketId.toString())
            } else {
                null
            },
            null,
        ) ?: return emptyList()
        cursor.use { cursor ->
            val files = ArrayList<MediaStoreFile>(cursor.count)
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
                        mimeType = cursor.getStringOrNull(mimeTypeColumn) ?: mediaType.mimeType,
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

    override suspend fun deleteContent(uris: Collection<Uri>) {
        val contentResolver = appContext.contentResolver
        for (uri in uris) {
            contentResolver.delete(
                uri,
                null,
                null,
            )
        }
    }

    override suspend fun getFiles(bucketId: Long?): List<MediaStoreFile> {
        val imageFiles = collectFiles(
            MediaType.Image,
            bucketId,
        )
        val videoFiles = collectFiles(
            MediaType.Video,
            bucketId,
        )
        val filesCount = imageFiles.size + videoFiles.size
        if (filesCount == 0) {
            return emptyList()
        }
        val files = ArrayList<MediaStoreFile>(filesCount)
        files.addAll(imageFiles)
        files.addAll(videoFiles)
        files.sortByDescending { file -> file.dateAdded }
        return files
    }

    private fun collectBucketsInfo(
        mediaType: MediaType,
        destination: MutableLongObjectMap<BucketInfo>,
    ) {
        val contentUri = mediaType.contentUri
        val cursor = appContext.contentResolver.query(
            contentUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
            ),
            null,
            null,
            null,
        ) ?: return
        cursor.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn =
                c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val bucketId = c.getLong(bucketIdColumn)
                val dateAdded = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(c.getLong(dateAddedColumn)),
                    ZoneId.systemDefault(),
                )
                val bucketInfo = destination.getOrPut(bucketId) {
                    BucketInfo(
                        id = bucketId,
                        name = c.getStringOrNull(bucketNameColumn) ?: id.toString(),
                        coverUri = ContentUris.withAppendedId(
                            contentUri,
                            id,
                        ),
                        coverDateAdded = dateAdded,
                        size = 0,
                    )
                }
                bucketInfo.size++
                if (bucketInfo.coverDateAdded < dateAdded) {
                    bucketInfo.coverDateAdded = dateAdded
                    bucketInfo.coverUri = ContentUris.withAppendedId(
                        contentUri,
                        id,
                    )
                }
            }
        }
    }

    override suspend fun getBuckets(): List<MediaStoreBucket> {
        val bucketsInfo = MutableLongObjectMap<BucketInfo>()
        collectBucketsInfo(
            mediaType = MediaType.Image,
            destination = bucketsInfo,
        )
        collectBucketsInfo(
            mediaType = MediaType.Video,
            destination = bucketsInfo,
        )
        val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
        bucketsInfo.forEachValue { info ->
            buckets.add(
                MediaStoreBucket(
                    id = info.id,
                    name = info.name,
                    size = info.size,
                    coverUri = info.coverUri,
                    coverDateAdded = info.coverDateAdded,
                ),
            )
        }
        buckets.sortByDescending { bucket -> bucket.coverDateAdded }
        return buckets
    }

    override suspend fun createBookmark(mediaId: Long) {
        bookmarksDao.upsert(
            BookmarkEntity(
                mediaId = mediaId,
                dateAdded = LocalDateTime.now(),
            ),
        )
    }

    override suspend fun deleteBookmark(mediaId: Long) {
        bookmarksDao.delete(mediaId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getBookmarks(): Flow<Map<Long, Bookmark>> =
        bookmarksDao.getAll().mapLatest { entities ->
            val size = entities.size
            if (size == 0) {
                return@mapLatest emptyMap()
            }
            val bookmarks: MutableMap<Long, Bookmark> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    LinkedHashMap.newLinkedHashMap(size)
                } else {
                    LinkedHashMap()
                }
            for (entity in entities) {
                bookmarks[entity.mediaId] = Bookmark(
                    mediaId = entity.mediaId,
                    dateAdded = entity.dateAdded,
                )
            }
            return@mapLatest bookmarks
        }

    private data class BucketInfo(
        val id: Long,
        val name: String,
        var size: Int,
        var coverUri: Uri,
        var coverDateAdded: LocalDateTime,
    )
}
