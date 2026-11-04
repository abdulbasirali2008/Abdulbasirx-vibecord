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
import java.net.HttpURLConnection
import java.net.URL

/** Loads album art for a song or an online cover, with a small in-memory cache. */
object ArtLoader {

    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(context: Context, uri: Uri, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri#$size"
        cache.get(key)?.let { return@withContext it }

        val isRemote = uri.scheme == "http" || uri.scheme == "https"
        val bitmap = if (isRemote) {
            fromRemote(uri, size)
        } else {
            fromThumbnail(context, uri, size) ?: fromEmbedded(context, uri, size)
        }
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
            decodeSampled(bytes, target)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    /** Cover pictures for online songs are plain image links. */
    private fun fromRemote(uri: Uri, target: Int): Bitmap? {
        return try {
            val connection = URL(uri.toString()).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8000
                connection.readTimeout = 10000
                if (connection.responseCode !in 200..299) return null
                val bytes = connection.inputStream.use { it.readBytes() }
                decodeSampled(bytes, target)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Covers can be several thousand pixels wide; decode them near the size we show. */
    private fun decodeSampled(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
