package com.bananaleafnutrientcheck.app.data.image

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.io.InputStream

fun interface ImageSource {
    /** Return a fresh stream each time; preprocessing reads EXIF metadata and pixels separately. */
    @Throws(IOException::class)
    fun openStream(): InputStream
}

class ContentUriImageSource(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) : ImageSource {
    override fun openStream(): InputStream =
        contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open image content for preprocessing.")
}
