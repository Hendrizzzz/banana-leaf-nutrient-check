package com.bananaleafnutrientcheck.app.ml

data class ModelPrediction(
    val classIndex: Int,
    val label: String,
    val score: Float,
)

data class ModelClassification(
    val predictions: List<ModelPrediction>,
    val outputScores: FloatArray,
    val runtimeTensorDetails: ModelRuntimeTensorDetails,
) {
    init {
        require(predictions.size == TOP_PREDICTION_COUNT) {
            "Expected exactly $TOP_PREDICTION_COUNT predictions, found ${predictions.size}."
        }
        require(outputScores.size == ModelAssetContract.OUTPUT_CLASS_COUNT) {
            "Unexpected output score count: ${outputScores.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelClassification

        if (predictions != other.predictions) return false
        if (!outputScores.contentEquals(other.outputScores)) return false
        if (runtimeTensorDetails != other.runtimeTensorDetails) return false

        return true
    }

    override fun hashCode(): Int {
        var result = predictions.hashCode()
        result = 31 * result + outputScores.contentHashCode()
        result = 31 * result + runtimeTensorDetails.hashCode()
        return result
    }

    companion object {
        const val TOP_PREDICTION_COUNT = 3
    }
}

data class ModelRuntimeTensorDetails(
    val input: ModelTensorDetails,
    val output: ModelTensorDetails,
)

data class ModelTensorDetails(
    val name: String,
    val shape: List<Int>,
    val dataType: String,
)
