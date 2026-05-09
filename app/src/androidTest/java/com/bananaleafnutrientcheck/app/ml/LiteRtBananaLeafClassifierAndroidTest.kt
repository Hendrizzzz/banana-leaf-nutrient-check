package com.bananaleafnutrientcheck.app.ml

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiteRtBananaLeafClassifierAndroidTest {
    @Test
    fun androidRuntimeMatchesTicket007AGoldenReference() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext

        assertEquals(EXPECTED_MODEL_SHA, context.modelSha256())

        val classifier = LiteRtBananaLeafClassifier(context, Dispatchers.IO)
        try {
            val result = classifier.classify(goldenReferenceInput())

            assertEquals(
                ModelAssetContract.EXPECTED_INPUT_SHAPE,
                result.runtimeTensorDetails.input.shape,
            )
            assertEquals("FLOAT32", result.runtimeTensorDetails.input.dataType)
            assertEquals(
                ModelAssetContract.EXPECTED_OUTPUT_SHAPE,
                result.runtimeTensorDetails.output.shape,
            )
            assertEquals("FLOAT32", result.runtimeTensorDetails.output.dataType)

            EXPECTED_OUTPUT.forEachIndexed { index, expected ->
                assertAllClose(index, expected, result.outputScores[index])
            }
            assertEquals(EXPECTED_TOP_3, result.predictions.map(ModelPrediction::classIndex))
            assertEquals(
                listOf("potassium", "nitrogen", "healthy"),
                result.predictions.map(ModelPrediction::label),
            )

            context.writeParityArtifact(result)
        } finally {
            classifier.close()
        }
    }

    private fun goldenReferenceInput(): ModelInputTensor {
        val tensor = FloatArray(
            ModelAssetContract.INPUT_WIDTH *
                ModelAssetContract.INPUT_HEIGHT *
                ModelAssetContract.INPUT_CHANNELS,
        )
        var tensorIndex = 0

        for (y in 0 until ModelAssetContract.INPUT_HEIGHT) {
            for (x in 0 until ModelAssetContract.INPUT_WIDTH) {
                val red = (x * 3 + y * 5) % 256
                val green = (x * 7 + y * 11 + 13) % 256
                val blue = (x * 13 + y * 17 + 29) % 256

                tensor[tensorIndex++] = red.normalizeMobileNetV2()
                tensor[tensorIndex++] = green.normalizeMobileNetV2()
                tensor[tensorIndex++] = blue.normalizeMobileNetV2()
            }
        }

        return ModelInputTensor(tensor)
    }

    private fun Int.normalizeMobileNetV2(): Float =
        (this / ModelAssetContract.NORMALIZATION_SCALE) - ModelAssetContract.NORMALIZATION_OFFSET

    private fun assertAllClose(index: Int, expected: Float, actual: Float) {
        val difference = kotlin.math.abs(expected - actual)
        val tolerance = ABSOLUTE_TOLERANCE + RELATIVE_TOLERANCE * kotlin.math.abs(expected)

        assertTrue(
            "Output score $index differs. expected=$expected actual=$actual difference=$difference tolerance=$tolerance",
            difference <= tolerance,
        )
    }

    private fun Context.modelSha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        assets.open(ModelAssetContract.MODEL_ASSET_FILE).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }

        return digest.digest().joinToString("") { byte -> "%02X".format(byte) }
    }

    private fun Context.writeParityArtifact(result: ModelClassification) {
        val artifact = JSONObject()
            .put("ticket_id", "007")
            .put("artifact_kind", "android-runtime-parity")
            .put("device", deviceJson())
            .put("dependency", dependencyJson())
            .put("model_sha256", EXPECTED_MODEL_SHA)
            .put("runtime_tensors", runtimeTensorJson(result.runtimeTensorDetails))
            .put("expected_vector_source", "artifacts/ml/golden-reference/007A/golden_reference.json")
            .put("expected_output_vector", JSONArray(EXPECTED_OUTPUT.toList()))
            .put("actual_android_runtime_output_vector", JSONArray(result.outputScores.toList()))
            .put("max_absolute_difference", maxAbsoluteDifference(result.outputScores))
            .put("allclose_atol_1e_5_rtol_1e_5", allClose(result.outputScores))
            .put("expected_top3", top3Json(EXPECTED_TOP_3, EXPECTED_OUTPUT))
            .put("actual_top3", predictionsJson(result.predictions))

        openFileOutput(PARITY_ARTIFACT_FILE, Context.MODE_PRIVATE).use { output ->
            output.write(artifact.toString(2).toByteArray(Charsets.UTF_8))
        }
    }

    private fun deviceJson(): JSONObject =
        JSONObject()
            .put("serial", "redacted-on-host")
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("android_api", Build.VERSION.SDK_INT)
            .put("android_release", Build.VERSION.RELEASE)

    private fun dependencyJson(): JSONObject =
        JSONObject()
            .put("coordinate", "com.google.ai.edge.litert:litert")
            .put("version", "1.4.2")

    private fun runtimeTensorJson(details: ModelRuntimeTensorDetails): JSONObject =
        JSONObject()
            .put("input", tensorJson(details.input))
            .put("output", tensorJson(details.output))

    private fun tensorJson(details: ModelTensorDetails): JSONObject =
        JSONObject()
            .put("name", details.name)
            .put("shape", JSONArray(details.shape))
            .put("dtype", details.dataType)

    private fun top3Json(indices: List<Int>, scores: FloatArray): JSONArray =
        JSONArray(
            indices.mapIndexed { rankIndex, classIndex ->
                JSONObject()
                    .put("rank", rankIndex + 1)
                    .put("index", classIndex)
                    .put("label", ModelAssetContract.EXPECTED_LABELS[classIndex])
                    .put("score", scores[classIndex])
            },
        )

    private fun predictionsJson(predictions: List<ModelPrediction>): JSONArray =
        JSONArray(
            predictions.mapIndexed { rankIndex, prediction ->
                JSONObject()
                    .put("rank", rankIndex + 1)
                    .put("index", prediction.classIndex)
                    .put("label", prediction.label)
                    .put("score", prediction.score)
            },
        )

    private fun maxAbsoluteDifference(actual: FloatArray): Double =
        EXPECTED_OUTPUT.indices.maxOf { index ->
            kotlin.math.abs(EXPECTED_OUTPUT[index] - actual[index]).toDouble()
        }

    private fun allClose(actual: FloatArray): Boolean =
        EXPECTED_OUTPUT.indices.all { index ->
            val expected = EXPECTED_OUTPUT[index]
            val difference = kotlin.math.abs(expected - actual[index])
            val tolerance = ABSOLUTE_TOLERANCE + RELATIVE_TOLERANCE * kotlin.math.abs(expected)
            difference <= tolerance
        }

    private companion object {
        const val EXPECTED_MODEL_SHA = "48CB547D209B2A47609318EDB008F55AFDAE0BE3B00E75FD8AD2B8B68310F697"
        const val ABSOLUTE_TOLERANCE = 1e-5f
        const val RELATIVE_TOLERANCE = 1e-5f
        const val PARITY_ARTIFACT_FILE = "android-runtime-parity-007.json"

        val EXPECTED_TOP_3 = listOf(8, 6, 2)

        val EXPECTED_OUTPUT = floatArrayOf(
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
