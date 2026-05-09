package com.bananaleafnutrientcheck.app.presentation

import com.bananaleafnutrientcheck.app.ml.ModelAssetContract
import com.bananaleafnutrientcheck.app.ml.ModelClassification
import com.bananaleafnutrientcheck.app.ml.ModelPrediction
import com.bananaleafnutrientcheck.app.ml.ModelRuntimeTensorDetails
import com.bananaleafnutrientcheck.app.ml.ModelTensorDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultFormatterTest {
    private val formatter = ScanResultFormatter()

    @Test
    fun healthyTopResultUsesApprovedCopy() {
        val result = formatter.format(
            classification(
                prediction(2, "healthy", 0.78f),
                prediction(8, "potassium", 0.12f),
                prediction(10, "zinc", 0.10f),
            ),
        )

        assertEquals(ScanResultFormatter.HEALTHY_RESULT_COPY, result.possibleResultText)
        assertEquals("78%", result.topPrediction.scoreText)
    }

    @Test
    fun phosphorousDisplayLabelDoesNotChangeInternalLabel() {
        val result = formatter.format(
            classification(
                prediction(7, "phosphorous", 0.70f),
                prediction(8, "potassium", 0.20f),
                prediction(2, "healthy", 0.10f),
            ),
        )

        assertEquals("phosphorous", result.topPrediction.internalLabel)
        assertEquals("Phosphorus", result.topPrediction.displayLabel)
        assertEquals("Phosphorus deficiency", result.possibleResultText)
    }

    @Test
    fun datasetCautionAppearsWhenNitrogenOrPhosphorousIsInDisplayedTop3() {
        val nitrogenFirst = formatter.format(
            classification(
                prediction(6, "nitrogen", 0.70f),
                prediction(8, "potassium", 0.20f),
                prediction(2, "healthy", 0.10f),
            ),
        )
        val phosphorousSecond = formatter.format(
            classification(
                prediction(8, "potassium", 0.70f),
                prediction(7, "phosphorous", 0.20f),
                prediction(2, "healthy", 0.10f),
            ),
        )
        val nitrogenThird = formatter.format(
            classification(
                prediction(8, "potassium", 0.70f),
                prediction(2, "healthy", 0.20f),
                prediction(6, "nitrogen", 0.10f),
            ),
        )

        assertTrue(nitrogenFirst.showDatasetCaution)
        assertTrue(phosphorousSecond.showDatasetCaution)
        assertTrue(nitrogenThird.showDatasetCaution)
    }

    @Test
    fun datasetCautionIsHiddenWhenSupplementalClassesAreNotDisplayed() {
        val result = formatter.format(
            classification(
                prediction(8, "potassium", 0.70f),
                prediction(2, "healthy", 0.20f),
                prediction(10, "zinc", 0.10f),
            ),
        )

        assertFalse(result.showDatasetCaution)
    }

    @Test
    fun confidenceWordingFollowsTicketPolicy() {
        assertEquals(
            ScanResultFormatter.AMBIGUOUS_RESULT,
            confidenceFor(topScore = 0.80f, secondScore = 0.71f),
        )
        assertEquals(
            ScanResultFormatter.HIGHER_CONFIDENCE,
            confidenceFor(topScore = 0.75f, secondScore = 0.55f),
        )
        assertEquals(
            ScanResultFormatter.MODERATE_CONFIDENCE,
            confidenceFor(topScore = 0.50f, secondScore = 0.40f),
        )
        assertEquals(
            ScanResultFormatter.LOW_CONFIDENCE,
            confidenceFor(topScore = 0.49f, secondScore = 0.39f),
        )
        assertEquals(
            ScanResultFormatter.MODERATE_CONFIDENCE,
            confidenceFor(topScore = 0.75f, secondScore = 0.56f),
        )
        assertEquals(
            ScanResultFormatter.AMBIGUOUS_RESULT,
            confidenceFor(topScore = 0.95f, secondScore = 0.86f),
        )
    }

    @Test
    fun otherPossibleClassesContainSecondAndThirdScores() {
        val result = formatter.format(
            classification(
                prediction(8, "potassium", 0.8639277f),
                prediction(6, "nitrogen", 0.09507778f),
                prediction(2, "healthy", 0.032105356f),
            ),
        )

        assertEquals(2, result.otherPossibleClasses.size)
        assertEquals("Nitrogen deficiency", result.otherPossibleClasses[0].resultText)
        assertEquals("10%", result.otherPossibleClasses[0].scoreText)
        assertEquals(ScanResultFormatter.HEALTHY_RESULT_COPY, result.otherPossibleClasses[1].resultText)
        assertEquals("3%", result.otherPossibleClasses[1].scoreText)
    }

    private fun confidenceFor(topScore: Float, secondScore: Float): String =
        formatter.format(
            classification(
                prediction(8, "potassium", topScore),
                prediction(10, "zinc", secondScore),
                prediction(2, "healthy", 0.0f),
            ),
        ).confidenceText

    private fun classification(
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

    private fun prediction(classIndex: Int, label: String, score: Float): ModelPrediction =
        ModelPrediction(
            classIndex = classIndex,
            label = label,
            score = score,
        )
}
