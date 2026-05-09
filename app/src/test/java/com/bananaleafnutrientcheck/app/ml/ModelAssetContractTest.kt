package com.bananaleafnutrientcheck.app.ml

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelAssetContractTest {
    @Test
    fun labelsFileMatchesExpectedOrder() {
        val labels = appFile("src/main/assets/${ModelAssetContract.LABELS_ASSET_FILE}")
            .readLines()
            .map(String::trim)
            .filter(String::isNotEmpty)

        assertEquals(11, labels.size)
        assertEquals(ModelAssetContract.EXPECTED_LABELS, labels)
    }

    @Test
    fun metadataRecordsModelContract() {
        val metadata = appFile("src/main/assets/${ModelAssetContract.METADATA_ASSET_FILE}")
            .readText()

        assertTrue(metadata.contains("\"model_name\""))
        assertTrue(metadata.contains("\"input_shape\""))
        assertTrue(metadata.contains("\"output_shape\""))
        assertTrue(metadata.contains("\"training_function\""))
        assertTrue(metadata.contains("tf.keras.applications.mobilenet_v2.preprocess_input"))
        ModelAssetContract.EXPECTED_LABELS.forEach { label ->
            assertTrue("Missing label in metadata: $label", metadata.contains("\"$label\""))
        }
    }

    @Test
    fun tfliteModelDeclaresExpectedInputAndOutputShapes() {
        val model = TfliteModel(appFile("src/main/assets/${ModelAssetContract.MODEL_ASSET_FILE}").readBytes())

        assertEquals(ModelAssetContract.EXPECTED_INPUT_SHAPE, model.firstInputShape())
        assertEquals(ModelAssetContract.EXPECTED_OUTPUT_SHAPE, model.firstOutputShape())
    }

    private fun appFile(pathFromAppModule: String): File {
        val candidates = listOf(
            File(pathFromAppModule),
            File("app", pathFromAppModule),
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Missing app file $pathFromAppModule; checked ${candidates.joinToString { it.path }}")
    }
}

private class TfliteModel(bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val fileIdentifier: String

    init {
        require(bytes.size >= 8) { "TFLite model file is too small." }
        fileIdentifier = String(bytes, 4, 4, StandardCharsets.US_ASCII)
        require(fileIdentifier == TFLITE_FILE_IDENTIFIER) { "Unexpected TFLite identifier: $fileIdentifier" }
    }

    fun firstInputShape(): List<Int> {
        val subgraph = firstSubgraph()
        val inputs = requireVectorField(subgraph, SUBGRAPH_INPUTS_FIELD, "subgraph inputs")
        val tensors = requireVectorField(subgraph, SUBGRAPH_TENSORS_FIELD, "subgraph tensors")
        val inputTensorIndex = intFromVector(inputs, 0)
        return tensorShape(tensors, inputTensorIndex)
    }

    fun firstOutputShape(): List<Int> {
        val subgraph = firstSubgraph()
        val outputs = requireVectorField(subgraph, SUBGRAPH_OUTPUTS_FIELD, "subgraph outputs")
        val tensors = requireVectorField(subgraph, SUBGRAPH_TENSORS_FIELD, "subgraph tensors")
        val outputTensorIndex = intFromVector(outputs, 0)
        return tensorShape(tensors, outputTensorIndex)
    }

    private fun firstSubgraph(): Int {
        val subgraphs = requireVectorField(rootTable(), MODEL_SUBGRAPHS_FIELD, "model subgraphs")
        require(vectorLength(subgraphs) > 0) { "TFLite model does not declare any subgraphs." }
        return tableFromVector(subgraphs, 0)
    }

    private fun tensorShape(tensorsVector: Int, tensorIndex: Int): List<Int> {
        require(tensorIndex in 0 until vectorLength(tensorsVector)) {
            "Tensor index $tensorIndex is outside tensor vector length ${vectorLength(tensorsVector)}."
        }
        val tensor = tableFromVector(tensorsVector, tensorIndex)
        val shapeVector = requireVectorField(tensor, TENSOR_SHAPE_FIELD, "tensor shape")
        return intVector(shapeVector)
    }

    private fun rootTable(): Int = buffer.getInt(0)

    private fun requireVectorField(table: Int, fieldIndex: Int, name: String): Int =
        vectorField(table, fieldIndex) ?: error("Missing TFLite $name field.")

    private fun vectorField(table: Int, fieldIndex: Int): Int? {
        val offset = fieldOffset(table, fieldIndex)
        return if (offset == 0) null else table + offset + buffer.getInt(table + offset)
    }

    private fun fieldOffset(table: Int, fieldIndex: Int): Int {
        val vtable = table - buffer.getInt(table)
        val vtableLength = unsignedShort(vtable)
        val fieldEntry = vtable + VT_LENGTH_BYTES + fieldIndex * SHORT_BYTES
        return if (fieldEntry < vtable + vtableLength) unsignedShort(fieldEntry) else 0
    }

    private fun tableFromVector(vector: Int, index: Int): Int {
        require(index in 0 until vectorLength(vector)) {
            "Vector index $index is outside vector length ${vectorLength(vector)}."
        }
        val element = vector + VECTOR_HEADER_BYTES + index * INT_BYTES
        return element + buffer.getInt(element)
    }

    private fun intVector(vector: Int): List<Int> =
        (0 until vectorLength(vector)).map { index -> intFromVector(vector, index) }

    private fun intFromVector(vector: Int, index: Int): Int {
        require(index in 0 until vectorLength(vector)) {
            "Vector index $index is outside vector length ${vectorLength(vector)}."
        }
        return buffer.getInt(vector + VECTOR_HEADER_BYTES + index * INT_BYTES)
    }

    private fun vectorLength(vector: Int): Int = buffer.getInt(vector)

    private fun unsignedShort(offset: Int): Int = buffer.getShort(offset).toInt() and U_SHORT_MASK

    private companion object {
        const val TFLITE_FILE_IDENTIFIER = "TFL3"

        const val MODEL_SUBGRAPHS_FIELD = 2

        const val SUBGRAPH_TENSORS_FIELD = 0
        const val SUBGRAPH_INPUTS_FIELD = 1
        const val SUBGRAPH_OUTPUTS_FIELD = 2

        const val TENSOR_SHAPE_FIELD = 0

        const val VT_LENGTH_BYTES = 4
        const val VECTOR_HEADER_BYTES = 4
        const val INT_BYTES = 4
        const val SHORT_BYTES = 2
        const val U_SHORT_MASK = 0xFFFF
    }
}
