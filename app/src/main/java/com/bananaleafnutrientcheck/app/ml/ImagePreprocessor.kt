package com.bananaleafnutrientcheck.app.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.bananaleafnutrientcheck.app.data.image.ImageSource
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImagePreprocessor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun preprocess(source: ImageSource): ModelInputTensor = withContext(dispatcher) {
        val orientation = readExifOrientation(source)
        val decoded = decodeBitmap(source)
        val oriented = decoded.applyExifOrientation(orientation)
        val resized = oriented.resizeToModelInput()

        resized.toModelInputTensor()
    }

    private fun readExifOrientation(source: ImageSource): Int =
        source.openStream().use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }

    private fun decodeBitmap(source: ImageSource): Bitmap =
        source.openStream().use { stream ->
            decodeBitmap(stream) ?: throw ImagePreprocessingException("Unable to decode image for preprocessing.")
        }

    private fun decodeBitmap(stream: InputStream): Bitmap? =
        BitmapFactory.decodeStream(
            stream,
            null,
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )

    private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return this
        }

        val swapsAxes = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        val targetWidth = if (swapsAxes) height else width
        val targetHeight = if (swapsAxes) width else height
        val sourcePixels = IntArray(width * height)
        val targetPixels = IntArray(targetWidth * targetHeight)

        getPixels(sourcePixels, 0, width, 0, 0, width, height)
        for (targetY in 0 until targetHeight) {
            for (targetX in 0 until targetWidth) {
                val sourceIndex = sourceIndexForOrientation(
                    orientation = orientation,
                    targetX = targetX,
                    targetY = targetY,
                    sourceWidth = width,
                    sourceHeight = height,
                )
                targetPixels[targetY * targetWidth + targetX] = sourcePixels[sourceIndex]
            }
        }

        return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).apply {
            setPixels(targetPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        }
    }

    private fun Bitmap.resizeToModelInput(): Bitmap =
        if (width == ModelAssetContract.INPUT_WIDTH && height == ModelAssetContract.INPUT_HEIGHT) {
            this
        } else {
            Bitmap.createScaledBitmap(
                this,
                ModelAssetContract.INPUT_WIDTH,
                ModelAssetContract.INPUT_HEIGHT,
                true,
            )
        }

    private fun Bitmap.toModelInputTensor(): ModelInputTensor {
        val pixels = IntArray(ModelAssetContract.INPUT_WIDTH * ModelAssetContract.INPUT_HEIGHT)
        getPixels(
            pixels,
            0,
            ModelAssetContract.INPUT_WIDTH,
            0,
            0,
            ModelAssetContract.INPUT_WIDTH,
            ModelAssetContract.INPUT_HEIGHT,
        )

        val tensor = FloatArray(pixels.size * ModelAssetContract.INPUT_CHANNELS)
        var tensorIndex = 0
        pixels.forEach { pixel ->
            tensor[tensorIndex++] = Color.red(pixel).normalizeMobileNetV2()
            tensor[tensorIndex++] = Color.green(pixel).normalizeMobileNetV2()
            tensor[tensorIndex++] = Color.blue(pixel).normalizeMobileNetV2()
        }

        return ModelInputTensor(tensor)
    }

    private fun Int.normalizeMobileNetV2(): Float =
        (this / ModelAssetContract.NORMALIZATION_SCALE) - ModelAssetContract.NORMALIZATION_OFFSET

    private fun sourceIndexForOrientation(
        orientation: Int,
        targetX: Int,
        targetY: Int,
        sourceWidth: Int,
        sourceHeight: Int,
    ): Int {
        val sourceX: Int
        val sourceY: Int

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                sourceX = sourceWidth - 1 - targetX
                sourceY = targetY
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                sourceX = sourceWidth - 1 - targetX
                sourceY = sourceHeight - 1 - targetY
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                sourceX = targetX
                sourceY = sourceHeight - 1 - targetY
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                sourceX = targetY
                sourceY = targetX
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                sourceX = targetY
                sourceY = sourceHeight - 1 - targetX
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                sourceX = sourceWidth - 1 - targetY
                sourceY = sourceHeight - 1 - targetX
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                sourceX = sourceWidth - 1 - targetY
                sourceY = targetX
            }
            else -> {
                sourceX = targetX
                sourceY = targetY
            }
        }

        return sourceY * sourceWidth + sourceX
    }
}

class ImagePreprocessingException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
