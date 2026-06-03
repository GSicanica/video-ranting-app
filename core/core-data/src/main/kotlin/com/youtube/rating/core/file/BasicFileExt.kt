package com.youtube.rating.core.file

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun File.deleteSafely(): Boolean =
    if (exists()) delete() else false

fun String.toUri(): Uri = Uri.parse(this)

fun String.toFile(): File = File(this)

fun Uri.resolveDisplayName(context: Context): String? {
    val uriString = toString()
    if (uriString.startsWith("content://", true)) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(this, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex > -1) return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
    } else if (uriString.startsWith("file://", true)) {
        return File(uriString).name
    }
    return lastPathSegment
}

fun ContentResolver.fileSize(uri: Uri): Long? =
    openFileDescriptor(uri, "r")?.use { it.statSize }
