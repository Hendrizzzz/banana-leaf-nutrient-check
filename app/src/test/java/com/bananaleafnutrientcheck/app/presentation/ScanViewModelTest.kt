package com.bananaleafnutrientcheck.app.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanViewModelTest {
    @Test
    fun initialStateHasNoSelectedImage() {
        val viewModel = ScanViewModel()

        assertFalse(viewModel.uiState.value.hasSelectedImage)
        assertEquals(null, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun photoPickerResultStoresContentUriReference() {
        val viewModel = ScanViewModel()
        val imageUri = "content://media/picker/banana-leaf"

        viewModel.onPhotoPickerResult(imageUri)

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertEquals(imageUri, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun photoPickerCancellationKeepsExistingStateWithoutError() {
        val viewModel = ScanViewModel()
        val imageUri = "content://media/picker/original-leaf"
        viewModel.onPhotoPickerResult(imageUri)

        viewModel.onPhotoPickerResult(null)
        viewModel.onPhotoPickerResult("")

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertEquals(imageUri, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun clearSelectedImageRemovesStoredReference() {
        val viewModel = ScanViewModel()
        viewModel.onPhotoPickerResult("content://media/picker/banana-leaf")

        viewModel.clearSelectedImage()

        assertFalse(viewModel.uiState.value.hasSelectedImage)
        assertEquals(null, viewModel.uiState.value.selectedImageUri)
    }

    @Test
    fun laterSelectionReplacesPreviousImageReference() {
        val viewModel = ScanViewModel()
        val replacementUri = "content://media/picker/replacement-leaf"

        viewModel.onPhotoPickerResult("content://media/picker/original-leaf")
        viewModel.onPhotoPickerResult(replacementUri)

        assertTrue(viewModel.uiState.value.hasSelectedImage)
        assertEquals(replacementUri, viewModel.uiState.value.selectedImageUri)
    }
}
