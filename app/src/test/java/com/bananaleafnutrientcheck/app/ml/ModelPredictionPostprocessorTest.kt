package com.bananaleafnutrientcheck.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPredictionPostprocessorTest {
    private val postprocessor = ModelPredictionPostprocessor()

    @Test
    fun processReturnsTopThreePredictionsInGoldenReferenceOrder() {
        val predictions = postprocessor.process(GOLDEN_REFERENCE_OUTPUT.copyOf())

        assertEquals(3, predictions.size)
        assertEquals(ModelPrediction(8, "potassium", 0.8639251589775085f), predictions[0])
        assertEquals(ModelPrediction(6, "nitrogen", 0.09507831931114197f), predictions[1])
        assertEquals(ModelPrediction(2, "healthy", 0.032106947153806686f), predictions[2])
    }

    @Test
    fun processDoesNotApplySecondSoftmaxToGoldenReferenceScores() {
        val predictions = postprocessor.process(GOLDEN_REFERENCE_OUTPUT.copyOf())

        assertEquals(GOLDEN_REFERENCE_OUTPUT[8], predictions[0].score, FLOAT_TOLERANCE)
        assertEquals(GOLDEN_REFERENCE_OUTPUT[6], predictions[1].score, FLOAT_TOLERANCE)
        assertEquals(GOLDEN_REFERENCE_OUTPUT[2], predictions[2].score, FLOAT_TOLERANCE)
    }

    @Test
    fun processBreaksScoreTiesByClassIndexAscending() {
        val tiedScores = floatArrayOf(
            0.25f,
            0.0f,
            0.25f,
            0.0f,
            0.25f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            0.25f,
        )

        val predictions = postprocessor.process(tiedScores)

        assertEquals(listOf(0, 2, 4), predictions.map(ModelPrediction::classIndex))
        assertEquals(listOf("boron", "healthy", "magnesium"), predictions.map(ModelPrediction::label))
    }

    @Test
    fun processRejectsWrongOutputLength() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            postprocessor.process(FloatArray(10) { 0.1f })
        }

        assertEquals("Expected 11 output scores, found 10.", error.message)
    }

    @Test
    fun processRejectsNonFiniteScores() {
        val scores = GOLDEN_REFERENCE_OUTPUT.copyOf()
        scores[3] = Float.NaN

        val error = assertThrows(IllegalArgumentException::class.java) {
            postprocessor.process(scores)
        }

        assertEquals("Output score at index 3 is not finite.", error.message)
    }

    @Test
    fun processRejectsScoresOutsideSoftmaxRange() {
        val scores = GOLDEN_REFERENCE_OUTPUT.copyOf()
        scores[0] = 1.1f

        val error = assertThrows(IllegalArgumentException::class.java) {
            postprocessor.process(scores)
        }

        assertEquals("Output score at index 0 is outside [0, 1]: 1.1", error.message)
    }

    @Test
    fun processRejectsScoresThatDoNotSumApproximatelyToOne() {
        val scores = GOLDEN_REFERENCE_OUTPUT.copyOf()
        scores[8] = 0.5f

        val error = assertThrows(IllegalArgumentException::class.java) {
            postprocessor.process(scores)
        }

        assertTrue(
            error.message.orEmpty().startsWith("Output scores do not sum approximately to 1.0:"),
        )
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.0f

        val GOLDEN_REFERENCE_OUTPUT = floatArrayOf(
            0.008831710554659367f,
            6.886718842760708e-13f,
            0.032106947153806686f,
            1.1143614031539073e-08f,
            3.507793522317115e-08f,
            1.4476853493761155e-08f,
            0.09507831931114197f,
            5.735694503528066e-05f,
            0.8639251589775085f,
            1.8853484311937008e-12f,
            4.143959984048706e-07f,
        )
    }
}
