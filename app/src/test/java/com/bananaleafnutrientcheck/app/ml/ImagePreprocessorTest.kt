package com.bananaleafnutrientcheck.app.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.bananaleafnutrientcheck.app.data.image.ImageSource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImagePreprocessorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val preprocessor = ImagePreprocessor(Dispatchers.Unconfined)

    @Test
    fun preprocessProducesExpectedModelInputShapeAndSize() = runBlocking {
        val tensor = preprocessor.preprocess(sourceFor(solidBitmap(Color.rgb(12, 34, 56))))

        assertEquals(ModelAssetContract.EXPECTED_INPUT_SHAPE, tensor.shape)
        assertEquals(
            ModelAssetContract.INPUT_WIDTH * ModelAssetContract.INPUT_HEIGHT * ModelAssetContract.INPUT_CHANNELS,
            tensor.data.size,
        )
    }

    @Test
    fun preprocessPreservesRgbChannelOrder() = runBlocking {
        val tensor = preprocessor.preprocess(sourceFor(solidBitmap(Color.rgb(255, 0, 128))))

        assertEquals(1.0f, tensor.data[0], FLOAT_TOLERANCE)
        assertEquals(-1.0f, tensor.data[1], FLOAT_TOLERANCE)
        assertEquals(0.003921627f, tensor.data[2], FLOAT_TOLERANCE)
    }

    @Test
    fun preprocessUsesMobileNetV2NormalizationRangeAndKnownValues() = runBlocking {
        val bitmap = Bitmap.createBitmap(ModelAssetContract.INPUT_WIDTH, ModelAssetContract.INPUT_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.rgb(0, 127, 255))

        val tensor = preprocessor.preprocess(sourceFor(bitmap))

        assertEquals(-1.0f, tensor.data[0], FLOAT_TOLERANCE)
        assertEquals(-0.0039215684f, tensor.data[1], FLOAT_TOLERANCE)
        assertEquals(1.0f, tensor.data[2], FLOAT_TOLERANCE)
        assertTrue(tensor.data.all { value -> value in -1.0f..1.0f })
    }

    @Test
    fun preprocessResizesRectangularImageWithoutCropping() = runBlocking {
        val bitmap = Bitmap.createBitmap(448, 224, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = when {
                    x < 112 -> Color.RED
                    x >= 336 -> Color.BLUE
                    else -> Color.GREEN
                }
                bitmap.setPixel(x, y, color)
            }
        }

        val tensor = preprocessor.preprocess(sourceFor(bitmap))

        assertPixelNear(tensor, x = 0, y = 112, red = 255, green = 0, blue = 0)
        assertPixelNear(tensor, x = 112, y = 112, red = 0, green = 255, blue = 0)
        assertPixelNear(tensor, x = 223, y = 112, red = 0, green = 0, blue = 255)
    }

    @Test
    fun preprocessAppliesExifRotationBeforeResize() = runBlocking {
        val rotatedFile = imageFileWithExifOrientation(
            width = 224,
            height = 448,
            orientation = ExifInterface.ORIENTATION_ROTATE_90,
        ) { _, y ->
            if (y < 224) AWT_RED else AWT_BLUE
        }

        val tensor = preprocessor.preprocess(FileImageSource(rotatedFile))

        assertPixelNear(tensor, x = 8, y = 112, red = 0, green = 0, blue = 255)
        assertPixelNear(tensor, x = 215, y = 112, red = 255, green = 0, blue = 0)
    }

    private fun solidBitmap(color: Int): Bitmap =
        Bitmap.createBitmap(ModelAssetContract.INPUT_WIDTH, ModelAssetContract.INPUT_HEIGHT, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }

    private fun sourceFor(bitmap: Bitmap): ImageSource = ByteArrayImageSource(bitmap.pngBytes())

    private fun Bitmap.pngBytes(): ByteArray =
        ByteArrayOutputStream().use { output ->
            compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }

    private fun imageFileWithExifOrientation(
        width: Int,
        height: Int,
        orientation: Int,
        colorAt: (x: Int, y: Int) -> Int,
    ): File {
        val file = temporaryFolder.newFile("exif-rotated.jpg")
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, colorAt(x, y))
            }
        }
        ImageIO.write(image, "jpg", file)
        ExifInterface(file.absolutePath).run {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file
    }

    private fun assertPixelNear(
        tensor: ModelInputTensor,
        x: Int,
        y: Int,
        red: Int,
        green: Int,
        blue: Int,
    ) {
        val index = (y * ModelAssetContract.INPUT_WIDTH + x) * ModelAssetContract.INPUT_CHANNELS
        assertEquals(red.normalizeExpected(), tensor.data[index], COLOR_TOLERANCE)
        assertEquals(green.normalizeExpected(), tensor.data[index + 1], COLOR_TOLERANCE)
        assertEquals(blue.normalizeExpected(), tensor.data[index + 2], COLOR_TOLERANCE)
    }

    private fun Int.normalizeExpected(): Float =
        (this / ModelAssetContract.NORMALIZATION_SCALE) - ModelAssetContract.NORMALIZATION_OFFSET

    private class ByteArrayImageSource(private val bytes: ByteArray) : ImageSource {
        override fun openStream(): InputStream = ByteArrayInputStream(bytes)
    }

    private class FileImageSource(private val file: File) : ImageSource {
        override fun openStream(): InputStream = file.inputStream()
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.000001f
        const val COLOR_TOLERANCE = 0.04f
        const val AWT_RED = 0xFF0000
        const val AWT_BLUE = 0x0000FF
    }
}
