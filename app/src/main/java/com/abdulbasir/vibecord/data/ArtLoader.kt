package com.abdulbasir.vibecord.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads album art for a song, with a small in-memory cache. */
object ArtLoader {

    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(context: Context, uri: Uri, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri#$size"
        cache.get(key)?.let { return@withContext it }

        val bitmap = fromThumbnail(context, uri, size) ?: fromEmbedded(context, uri, size)
        if (bitmap != null) cache.put(key, bitmap)
        bitmap
    }

    private fun fromThumbnail(context: Context, uri: Uri, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            context.contentResolver.loadThumbnail(uri, Size(size, size), null)
        } catch (e: Exception) {
            null
        }
    }

    private fun fromEmbedded(context: Context, uri: Uri, target: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            // Covers can be several thousand pixels wide; decode them near the size we show.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
