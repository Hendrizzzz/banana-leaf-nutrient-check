package com.bananaleafnutrientcheck.app.data.image

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

class AppPrivateCaptureStore(
    context: Context,
) {
    private val captureDirectory = File(
        context.applicationContext.cacheDir,
        CAPTURE_DIRECTORY_NAME,
    )

    @Throws(IOException::class)
    fun createCaptureFile(): File {
        if (!captureDirectory.exists() && !captureDirectory.mkdirs()) {
            throw IOException("Unable to prepare private capture directory.")
        }

        return File.createTempFile(CAPTURE_FILE_PREFIX, CAPTURE_FILE_SUFFIX, captureDirectory)
    }

    fun uriFor(file: File): Uri = Uri.fromFile(file)

    companion object {
        const val CAPTURE_DIRECTORY_NAME = "captured-leaf-images"
        private const val CAPTURE_FILE_PREFIX = "banana-leaf-"
        private const val CAPTURE_FILE_SUFFIX = ".jpg"
    }
}
