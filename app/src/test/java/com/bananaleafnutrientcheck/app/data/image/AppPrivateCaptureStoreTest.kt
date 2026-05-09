package com.bananaleafnutrientcheck.app.data.image

import android.net.Uri
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppPrivateCaptureStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun createsCaptureFilesUnderAppPrivateCache() {
        val context = RuntimeEnvironment.getApplication()
        val store = AppPrivateCaptureStore(context)

        val captureFile = store.createCaptureFile()

        val expectedDirectory = File(
            context.cacheDir,
            AppPrivateCaptureStore.CAPTURE_DIRECTORY_NAME,
        )
        assertTrue(captureFile.exists())
        assertTrue(captureFile.absolutePath.startsWith(expectedDirectory.absolutePath))
        assertEquals("jpg", captureFile.extension)
        assertEquals("file", store.uriFor(captureFile).scheme)
    }

    @Test
    fun fileUriImageSourceOpensFreshStreamsForCapturedImages() {
        val context = RuntimeEnvironment.getApplication()
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)
        val captureFile = temporaryFolder.newFile("capture.jpg").apply {
            writeBytes(imageBytes)
        }
        val source = ContentUriImageSource(
            contentResolver = context.contentResolver,
            uri = Uri.fromFile(captureFile),
        )

        assertArrayEquals(imageBytes, source.openStream().use { it.readBytes() })
        assertArrayEquals(imageBytes, source.openStream().use { it.readBytes() })
    }
}
