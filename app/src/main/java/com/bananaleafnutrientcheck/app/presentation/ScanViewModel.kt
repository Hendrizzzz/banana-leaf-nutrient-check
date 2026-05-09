package com.bananaleafnutrientcheck.app.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScanUiState(
    val selectedImageUri: String? = null,
) {
    val hasSelectedImage: Boolean = !selectedImageUri.isNullOrBlank()
}

class ScanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onPhotoPickerResult(imageUri: String?) {
        if (imageUri.isNullOrBlank()) {
            return
        }

        _uiState.update { currentState ->
            currentState.copy(selectedImageUri = imageUri)
        }
    }

    fun clearSelectedImage() {
        _uiState.update { currentState ->
            currentState.copy(selectedImageUri = null)
        }
    }
}
