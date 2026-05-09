package com.bananaleafnutrientcheck.app.presentation

import android.os.Looper
import com.bananaleafnutrientcheck.app.ml.ModelAssetContract
import com.bananaleafnutrientcheck.app.ml.ModelClassification
import com.bananaleafnutrientcheck.app.ml.ModelPrediction
import com.bananaleafnutrientcheck.app.ml.ModelRuntimeTensorDetails
import com.bananaleafnutrientcheck.app.ml.ModelTensorDetails
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ScanViewModelTest {
    @Test
    fun initialStateHasNoSelectedImage() {
        val viewModel = ScanViewModel(FakeScanImageAnalyzer())

        assertFalse(viewModel.uiState.value.hasSelectedImage)
        assertFalse(viewModel.uiState.value.canAnalyze)
        assertEquals(null, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun photoPickerResultStoresContentUriReference() {
        val viewModel = ScanViewModel(FakeScanImageAnalyzer())
        val imageUri = "content://media/picker/banana-leaf"

        viewModel.onPhotoPickerResult(imageUri)

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertTrue(viewModel.uiState.value.canAnalyze)
        assertEquals(imageUri, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun photoPickerCancellationKeepsExistingStateWithoutError() {
        val viewModel = ScanViewModel(FakeScanImageAnalyzer())
        val imageUri = "content://media/picker/original-leaf"
        viewModel.onPhotoPickerResult(imageUri)

        viewModel.onPhotoPickerResult(null)
        viewModel.onPhotoPickerResult("")

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertEquals(imageUri, viewModel.uiState.value.selectedImageUri)
        assertNull(viewModel.uiState.value.analysisError)
    }

    @Test
    fun clearSelectedImageRemovesStoredReferenceAndResultState() {
        val viewModel = ScanViewModel(FakeScanImageAnalyzer())
        viewModel.onPhotoPickerResult("content://media/picker/banana-leaf")
        viewModel.analyzeSelectedImage()
        drainMain()

        assertTrue(viewModel.uiState.value.result != null)

        viewModel.clearSelectedImage()

        assertFalse(viewModel.uiState.value.hasSelectedImage)
        assertFalse(viewModel.uiState.value.canAnalyze)
        assertEquals(null, viewModel.uiState.value.selectedImageUri)
        assertNull(viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.analysisError)
    }

    @Test
    fun laterSelectionReplacesPreviousImageReferenceAndClearsResult() {
        val viewModel = ScanViewModel(FakeScanImageAnalyzer())
        val replacementUri = "content://media/picker/replacement-leaf"
        viewModel.onPhotoPickerResult("content://media/picker/original-leaf")
        viewModel.analyzeSelectedImage()
        drainMain()

        assertTrue(viewModel.uiState.value.result != null)

        viewModel.onPhotoPickerResult(replacementUri)

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertTrue(viewModel.uiState.value.canAnalyze)
        assertEquals(replacementUri, viewModel.uiState.value.selectedImageUri)
        assertNull(viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.analysisError)
    }

    @Test
    fun analyzeWithoutSelectedImageDoesNotRunAnalyzer() {
        val analyzer = FakeScanImageAnalyzer()
        val viewModel = ScanViewModel(analyzer)

        viewModel.analyzeSelectedImage()
        drainMain()

        assertEquals(0, analyzer.analyzeCalls)
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertNull(viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.analysisError)
    }

    @Test
    fun analyzeSelectedImageShowsLoadingThenFormattedResult() {
        val deferredResult = CompletableDeferred<ModelClassification>()
        val analyzer = FakeScanImageAnalyzer(deferredResult = deferredResult)
        val viewModel = ScanViewModel(analyzer)
        val imageUri = "content://media/picker/banana-leaf"

        viewModel.onPhotoPickerResult(imageUri)
        viewModel.analyzeSelectedImage()
        drainMain()

        assertEquals(1, analyzer.analyzeCalls)
        assertEquals(listOf(imageUri), analyzer.analyzedUris)
        assertTrue(viewModel.uiState.value.isAnalyzing)
        assertFalse(viewModel.uiState.value.canAnalyze)

        deferredResult.complete(defaultClassification())
        drainMain()

        val state = viewModel.uiState.value
        assertFalse(state.isAnalyzing)
        assertTrue(state.canAnalyze)
        assertNull(state.analysisError)
        assertEquals("Potassium deficiency", state.result?.possibleResultText)
        assertEquals("86%", state.result?.topPrediction?.scoreText)
        assertTrue(state.result?.showDatasetCaution == true)
    }

    @Test
    fun analyzeSelectedImagePreventsDuplicateSubmissionsWhileRunning() {
        val analyzer = FakeScanImageAnalyzer(
            deferredResult = CompletableDeferred(),
        )
        val viewModel = ScanViewModel(analyzer)

        viewModel.onPhotoPickerResult("content://media/picker/banana-leaf")
        viewModel.analyzeSelectedImage()
        drainMain()
        viewModel.analyzeSelectedImage()
        drainMain()

        assertEquals(1, analyzer.analyzeCalls)
        assertTrue(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun analyzeErrorShowsSafeErrorState() {
        val analyzer = FakeScanImageAnalyzer(error = IOException("decode failed"))
        val viewModel = ScanViewModel(analyzer)

        viewModel.onPhotoPickerResult("content://media/picker/banana-leaf")
        viewModel.analyzeSelectedImage()
        drainMain()

        val state = viewModel.uiState.value
        assertFalse(state.isAnalyzing)
        assertTrue(state.canAnalyze)
        assertNull(state.result)
        assertEquals(ScanAnalysisError.UnableToAnalyzeImage, state.analysisError)
    }

    @Test
    fun changingImageWhileAnalysisRunsDiscardsStaleResult() {
        val deferredResult = CompletableDeferred<ModelClassification>()
        val analyzer = FakeScanImageAnalyzer(deferredResult = deferredResult)
        val viewModel = ScanViewModel(analyzer)
        val firstImageUri = "content://media/picker/first-leaf"
        val secondImageUri = "content://media/picker/second-leaf"

        viewModel.onPhotoPickerResult(firstImageUri)
        viewModel.analyzeSelectedImage()
        drainMain()
        viewModel.onPhotoPickerResult(secondImageUri)
        drainMain()

        deferredResult.complete(defaultClassification())
        drainMain()

        val state = viewModel.uiState.value
        assertEquals(secondImageUri, state.selectedImageUri)
        assertFalse(state.isAnalyzing)
        assertTrue(state.canAnalyze)
        assertNull(state.result)
        assertNull(state.analysisError)
    }

    private fun drainMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class FakeScanImageAnalyzer(
        private val deferredResult: CompletableDeferred<ModelClassification>? = null,
        private val error: Throwable? = null,
        private val immediateResult: ModelClassification = defaultClassification(),
    ) : ScanImageAnalyzer {
        var analyzeCalls = 0
            private set
        val analyzedUris = mutableListOf<String>()

        override suspend fun analyze(imageUri: String): ModelClassification {
            analyzeCalls += 1
            analyzedUris += imageUri
            error?.let { throw it }

            return deferredResult?.let { deferred ->
                withContext(NonCancellable) {
                    deferred.await()
                }
            } ?: immediateResult
        }
    }

    private companion object {
        fun defaultClassification(): ModelClassification =
            classification(
                prediction(8, "potassium", 0.8639277f),
                prediction(6, "nitrogen", 0.09507778f),
                prediction(2, "healthy", 0.032105356f),
            )

        fun classification(
            first: ModelPrediction,
            second: ModelPrediction,
            third: ModelPrediction,
        ): ModelClassification =
            ModelClassification(
                predictions = listOf(first, second, third),
                outputScores = FloatArray(ModelAssetContract.OUTPUT_CLASS_COUNT).also { scores ->
                    scores[first.classIndex] = first.score
                    scores[second.classIndex] = second.score
                    scores[third.classIndex] = third.score
                },
                runtimeTensorDetails = ModelRuntimeTensorDetails(
                    input = ModelTensorDetails(
                        name = "input",
                        shape = ModelAssetContract.EXPECTED_INPUT_SHAPE,
                        dataType = "FLOAT32",
                    ),
                    output = ModelTensorDetails(
                        name = "output",
                        shape = ModelAssetContract.EXPECTED_OUTPUT_SHAPE,
                        dataType = "FLOAT32",
                    ),
                ),
            )

        fun prediction(classIndex: Int, label: String, score: Float): ModelPrediction =
            ModelPrediction(
                classIndex = classIndex,
                label = label,
                score = score,
            )
    }
}
