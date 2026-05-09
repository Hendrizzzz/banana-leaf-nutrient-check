package com.bananaleafnutrientcheck.app.ml

class ModelInputTensor(
    val data: FloatArray,
    val shape: List<Int> = ModelAssetContract.EXPECTED_INPUT_SHAPE,
) {
    init {
        require(shape == ModelAssetContract.EXPECTED_INPUT_SHAPE) {
            "Unexpected input tensor shape: $shape"
        }
        require(data.size == ModelAssetContract.INPUT_WIDTH * ModelAssetContract.INPUT_HEIGHT * ModelAssetContract.INPUT_CHANNELS) {
            "Unexpected input tensor size: ${data.size}"
        }
    }
}
