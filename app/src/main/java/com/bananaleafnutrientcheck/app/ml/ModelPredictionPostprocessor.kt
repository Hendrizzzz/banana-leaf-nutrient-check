package com.bananaleafnutrientcheck.app.ml

class ModelPredictionPostprocessor(
    private val labels: List<String> = ModelAssetContract.EXPECTED_LABELS,
) {
    fun process(outputScores: FloatArray): List<ModelPrediction> {
        validateLabels()
        validateScores(outputScores)

        return outputScores
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<Float>> { it.value }
                    .thenBy { it.index },
            )
            .take(ModelClassification.TOP_PREDICTION_COUNT)
            .map { indexedScore ->
                ModelPrediction(
                    classIndex = indexedScore.index,
                    label = labels[indexedScore.index],
                    score = indexedScore.value,
                )
            }
    }

    private fun validateLabels() {
        require(labels == ModelAssetContract.EXPECTED_LABELS) {
            "Model label order does not match the expected asset contract."
        }
    }

    private fun validateScores(outputScores: FloatArray) {
        require(outputScores.size == ModelAssetContract.OUTPUT_CLASS_COUNT) {
            "Expected ${ModelAssetContract.OUTPUT_CLASS_COUNT} output scores, found ${outputScores.size}."
        }

        outputScores.forEachIndexed { index, score ->
            require(score.isFinite()) { "Output score at index $index is not finite." }
            require(score in MIN_SCORE..MAX_SCORE) {
                "Output score at index $index is outside [0, 1]: $score"
            }
        }

        val sum = outputScores.fold(0.0) { total, score -> total + score.toDouble() }
        require(kotlin.math.abs(sum - EXPECTED_SOFTMAX_SUM) <= SOFTMAX_SUM_TOLERANCE) {
            "Output scores do not sum approximately to 1.0: $sum"
        }
    }

    private companion object {
        const val MIN_SCORE = 0.0f
        const val MAX_SCORE = 1.0f
        const val EXPECTED_SOFTMAX_SUM = 1.0
        const val SOFTMAX_SUM_TOLERANCE = 0.0001
    }
}
