package com.bananaleafnutrientcheck.app.presentation

import android.content.Context
import android.net.Uri
import com.bananaleafnutrientcheck.app.data.image.ContentUriImageSource
import com.bananaleafnutrientcheck.app.ml.ImagePreprocessor
import com.bananaleafnutrientcheck.app.ml.LiteRtBananaLeafClassifier
import com.bananaleafnutrientcheck.app.ml.ModelClassification
import java.io.Closeable

interface ScanImageAnalyzer : Closeable {
    suspend fun analyze(imageUri: String): ModelClassification

    override fun close() = Unit
}

class OnDeviceScanImageAnalyzer(
    context: Context,
    private val preprocessor: ImagePreprocessor = ImagePreprocessor(),
    private val classifier: LiteRtBananaLeafClassifier = LiteRtBananaLeafClassifier(context),
) : ScanImageAnalyzer {
    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun analyze(imageUri: String): ModelClassification {
        val source = ContentUriImageSource(
            contentResolver = contentResolver,
            uri = Uri.parse(imageUri),
        )
        val tensor = preprocessor.preprocess(source)

        return classifier.classify(tensor)
    }

    override fun close() {
        classifier.close()
    }
}
