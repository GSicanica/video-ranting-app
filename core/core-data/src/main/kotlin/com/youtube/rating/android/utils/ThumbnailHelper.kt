package com.youtube.rating.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import coil.request.ImageRequest
import com.youtube.rating.core.generators.RandomXS128
import com.youtube.rating.core.generators.stableSeedFromString

/**
 * Generates a fallback thumbnail URL for YouTube videos
 * Tries multiple thumbnail quality options
 */
object ThumbnailHelper {
    
    /**
     * Get the best available thumbnail URL with fallbacks
     */
    fun getThumbnailUrl(videoId: String): String {
        // YouTube thumbnail URLs in order of preference
        return "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
    }
    
    /**
     * Get all possible thumbnail URLs for a video (in order of quality)
     */
    fun getAllThumbnailUrls(videoId: String): List<String> {
        return listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",  // 1920x1080
            "https://i.ytimg.com/vi/$videoId/sddefault.jpg",       // 640x480
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",       // 480x360
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",       // 320x180
            "https://i.ytimg.com/vi/$videoId/default.jpg"          // 120x90
        )
    }

    /**
     * Build a de-duplicated list of thumbnail candidates.
     * The preferred URL (if any) is tried first, then standard YouTube sizes.
     */
    fun buildThumbnailCandidates(videoId: String, preferred: String?): List<String> {
        val list = mutableListOf<String>()
        val pref = preferred?.trim().orEmpty()
        if (pref.isNotBlank()) list.add(pref)
        list.addAll(getAllThumbnailUrls(videoId = videoId))
        return list.distinct()
    }
    
    /**
     * Creates an ImageRequest with fallback support
     */
    fun createImageRequest(
        context: Context,
        videoId: String,
        thumbnailUrl: String?
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(thumbnailUrl ?: getThumbnailUrl(videoId = videoId))
            .crossfade(false)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .error(android.R.drawable.ic_menu_gallery) // System gallery icon as fallback
            .placeholder(android.R.drawable.ic_menu_gallery)
            .fallback(android.R.drawable.ic_menu_gallery)
            .build()
    }
    
    /**
     * Creates a placeholder bitmap for missing thumbnails
     */
    fun createPlaceholderBitmap(width: Int = 640, height: Int = 360, videoId: String = ""): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient (stable per videoId to avoid flicker).
        val rng = RandomXS128(stableSeedFromString(input = videoId.ifBlank { "default" }))
        val c1 = Color.argb(255, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
        val c2 = Color.argb(255, rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = android.graphics.LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                c1,
                c2,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // YouTube play icon
        val iconPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 200
        }
        
        val centerX = width / 2f
        val centerY = height / 2f
        val iconSize = width / 8f
        
        // Draw rounded rectangle for YouTube button
        val rect = RectF(
            centerX - iconSize,
            centerY - iconSize * 0.7f,
            centerX + iconSize,
            centerY + iconSize * 0.7f
        )
        canvas.drawRoundRect(rect, iconSize / 4, iconSize / 4, iconPaint)
        
        // Draw play triangle
        val trianglePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#667eea")
            style = Paint.Style.FILL
        }
        
        val path = android.graphics.Path().apply {
            moveTo(centerX - iconSize / 3, centerY - iconSize / 2)
            lineTo(centerX + iconSize / 2, centerY)
            lineTo(centerX - iconSize / 3, centerY + iconSize / 2)
            close()
        }
        canvas.drawPath(path, trianglePaint)
        
        // Text "YouTube Video"
        if (videoId.isNotEmpty()) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = width / 20f
                textAlign = Paint.Align.CENTER
                alpha = 150
            }
            canvas.drawText("Video ID: $videoId", centerX, centerY + iconSize * 1.5f, textPaint)
        }
        
        return bitmap
    }
}
