package com.bananaleafnutrientcheck.app.presentation

import com.bananaleafnutrientcheck.app.ml.ModelClassification
import com.bananaleafnutrientcheck.app.ml.ModelPrediction
import kotlin.math.roundToInt

data class ScanResultUiModel(
    val possibleResultText: String,
    val topPrediction: ScanPredictionUiModel,
    val otherPossibleClasses: List<ScanPredictionUiModel>,
    val confidenceText: String,
    val showDatasetCaution: Boolean,
) {
    val topPredictions: List<ScanPredictionUiModel> =
        listOf(topPrediction) + otherPossibleClasses
}

data class ScanPredictionUiModel(
    val classIndex: Int,
    val internalLabel: String,
    val displayLabel: String,
    val resultText: String,
    val scoreText: String,
)

class ScanResultFormatter {
    fun format(classification: ModelClassification): ScanResultUiModel {
        val predictions = classification.predictions.map { prediction ->
            prediction.toUiModel()
        }
        val topPrediction = predictions.first()

        return ScanResultUiModel(
            possibleResultText = topPrediction.resultText,
            topPrediction = topPrediction,
            otherPossibleClasses = predictions.drop(1),
            confidenceText = confidenceTextFor(classification.predictions),
            showDatasetCaution = classification.predictions.any { prediction ->
                prediction.label == NITROGEN_LABEL || prediction.label == PHOSPHOROUS_LABEL
            },
        )
    }

    private fun ModelPrediction.toUiModel(): ScanPredictionUiModel {
        val displayLabel = displayLabelFor(label)

        return ScanPredictionUiModel(
            classIndex = classIndex,
            internalLabel = label,
            displayLabel = displayLabel,
            resultText = if (label == HEALTHY_LABEL) {
                HEALTHY_RESULT_COPY
            } else {
                "$displayLabel deficiency"
            },
            scoreText = score.toModelScoreText(),
        )
    }

    private fun confidenceTextFor(predictions: List<ModelPrediction>): String {
        val topScore = predictions[0].score
        val margin = topScore - predictions[1].score

        return when {
            margin < AMBIGUOUS_MARGIN_THRESHOLD - COMPARISON_EPSILON -> AMBIGUOUS_RESULT
            topScore.isAtLeast(HIGHER_CONFIDENCE_SCORE_THRESHOLD) &&
                margin.isAtLeast(HIGHER_CONFIDENCE_MARGIN_THRESHOLD) -> HIGHER_CONFIDENCE
            topScore.isAtLeast(MODERATE_CONFIDENCE_SCORE_THRESHOLD) &&
                margin.isAtLeast(MODERATE_CONFIDENCE_MARGIN_THRESHOLD) -> MODERATE_CONFIDENCE
            else -> LOW_CONFIDENCE
        }
    }

    private fun displayLabelFor(label: String): String =
        when (label) {
            "boron" -> "Boron"
            "calcium" -> "Calcium"
            HEALTHY_LABEL -> "Healthy"
            "iron" -> "Iron"
            "magnesium" -> "Magnesium"
            "manganese" -> "Manganese"
            NITROGEN_LABEL -> "Nitrogen"
            PHOSPHOROUS_LABEL -> "Phosphorus"
            "potassium" -> "Potassium"
            "sulphur" -> "Sulphur"
            "zinc" -> "Zinc"
            else -> label.replaceFirstChar { char -> char.uppercase() }
        }

    private fun Float.toModelScoreText(): String =
        "${(this * SCORE_PERCENT_MULTIPLIER).roundToInt()}%"

    private fun Float.isAtLeast(threshold: Float): Boolean =
        this + COMPARISON_EPSILON >= threshold

    companion object {
        const val HEALTHY_RESULT_COPY = "No visible deficiency pattern detected by this model."
        const val HIGHER_CONFIDENCE = "Higher confidence"
        const val MODERATE_CONFIDENCE = "Moderate confidence"
        const val LOW_CONFIDENCE = "Low confidence"
        const val AMBIGUOUS_RESULT = "Ambiguous result"

        private const val HEALTHY_LABEL = "healthy"
        private const val NITROGEN_LABEL = "nitrogen"
        private const val PHOSPHOROUS_LABEL = "phosphorous"
        private const val SCORE_PERCENT_MULTIPLIER = 100
        private const val AMBIGUOUS_MARGIN_THRESHOLD = 0.10f
        private const val HIGHER_CONFIDENCE_SCORE_THRESHOLD = 0.75f
        private const val HIGHER_CONFIDENCE_MARGIN_THRESHOLD = 0.20f
        private const val MODERATE_CONFIDENCE_SCORE_THRESHOLD = 0.50f
        private const val MODERATE_CONFIDENCE_MARGIN_THRESHOLD = 0.10f
        private const val COMPARISON_EPSILON = 0.000001f
    }
}
