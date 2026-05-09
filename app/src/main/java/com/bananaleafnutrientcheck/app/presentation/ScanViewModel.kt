package com.bananaleafnutrientcheck.app.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScanUiState(
    val selectedImageUri: String? = null,
    val isAnalyzing: Boolean = false,
    val result: ScanResultUiModel? = null,
    val analysisError: ScanAnalysisError? = null,
) {
    val hasSelectedImage: Boolean = !selectedImageUri.isNullOrBlank()
    val canAnalyze: Boolean = hasSelectedImage && !isAnalyzing
}

enum class ScanAnalysisError {
    UnableToAnalyzeImage,
}

class ScanViewModel(
    private val analyzer: ScanImageAnalyzer,
    private val resultFormatter: ScanResultFormatter = ScanResultFormatter(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()
    private var analyzeJob: Job? = null

    fun onPhotoPickerResult(imageUri: String?) {
        selectImage(imageUri)
    }

    fun onCameraCaptureResult(imageUri: String?) {
        selectImage(imageUri)
    }

    private fun selectImage(imageUri: String?) {
        if (imageUri.isNullOrBlank()) {
            return
        }

        analyzeJob?.cancel()
        _uiState.update { currentState ->
            currentState.copy(
                selectedImageUri = imageUri,
                isAnalyzing = false,
                result = null,
                analysisError = null,
            )
        }
    }

    fun clearSelectedImage() {
        analyzeJob?.cancel()
        _uiState.update { currentState ->
            currentState.copy(
                selectedImageUri = null,
                isAnalyzing = false,
                result = null,
                analysisError = null,
            )
        }
    }

    fun analyzeSelectedImage() {
        val imageUri = uiState.value.selectedImageUri
        if (imageUri.isNullOrBlank() || uiState.value.isAnalyzing) {
            return
        }

        analyzeJob = viewModelScope.launch {
            _uiState.update { currentState ->
                if (currentState.selectedImageUri == imageUri) {
                    currentState.copy(
                        isAnalyzing = true,
                        result = null,
                        analysisError = null,
                    )
                } else {
                    currentState
                }
            }

            try {
                val classification = analyzer.analyze(imageUri)
                val formattedResult = resultFormatter.format(classification)

                _uiState.update { currentState ->
                    if (currentState.selectedImageUri == imageUri) {
                        currentState.copy(
                            isAnalyzing = false,
                            result = formattedResult,
                            analysisError = null,
                        )
                    } else {
                        currentState
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    if (currentState.selectedImageUri == imageUri) {
                        currentState.copy(
                            isAnalyzing = false,
                            result = null,
                            analysisError = ScanAnalysisError.UnableToAnalyzeImage,
                        )
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    override fun onCleared() {
        analyzeJob?.cancel()
        runCatching {
            analyzer.close()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                        return ScanViewModel(
                            analyzer = OnDeviceScanImageAnalyzer(context.applicationContext),
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
