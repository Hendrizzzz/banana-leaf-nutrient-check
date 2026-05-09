package com.bananaleafnutrientcheck.app.ml

import android.content.Context
import android.content.res.AssetManager
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class LiteRtBananaLeafClassifier(
    private val assetManager: AssetManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val postprocessor: ModelPredictionPostprocessor = ModelPredictionPostprocessor(),
) : Closeable {
    constructor(
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        postprocessor: ModelPredictionPostprocessor = ModelPredictionPostprocessor(),
    ) : this(context.applicationContext.assets, dispatcher, postprocessor)

    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var runtimeTensorDetails: ModelRuntimeTensorDetails? = null
    private var closed = false

    suspend fun classify(input: ModelInputTensor): ModelClassification = withContext(dispatcher) {
        synchronized(lock) {
            check(!closed) { "Classifier is already closed." }

            val activeInterpreter = interpreter ?: createInterpreter().also { createdInterpreter ->
                runtimeTensorDetails = createdInterpreter.verifyTensorContract()
                interpreter = createdInterpreter
            }
            val outputScores = activeInterpreter.runInference(input)
            val predictions = postprocessor.process(outputScores)

            ModelClassification(
                predictions = predictions,
                outputScores = outputScores.copyOf(),
                runtimeTensorDetails = requireNotNull(runtimeTensorDetails),
            )
        }
    }

    override fun close() {
        synchronized(lock) {
            interpreter?.close()
            interpreter = null
            runtimeTensorDetails = null
            closed = true
        }
    }

    private fun createInterpreter(): Interpreter =
        Interpreter(
            loadModelBuffer(),
            Interpreter.Options().apply {
                setNumThreads(CPU_THREAD_COUNT)
            },
        )

    private fun loadModelBuffer(): MappedByteBuffer =
        assetManager.openFd(ModelAssetContract.MODEL_ASSET_FILE).use { assetFileDescriptor ->
            FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
                inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.declaredLength,
                )
            }
        }

    private fun Interpreter.verifyTensorContract(): ModelRuntimeTensorDetails {
        require(inputTensorCount == EXPECTED_INPUT_TENSOR_COUNT) {
            "Expected one model input tensor, found $inputTensorCount."
        }
        require(outputTensorCount == EXPECTED_OUTPUT_TENSOR_COUNT) {
            "Expected one model output tensor, found $outputTensorCount."
        }

        val inputTensor = getInputTensor(0)
        val outputTensor = getOutputTensor(0)
        val inputShape = inputTensor.shape().toList()
        val outputShape = outputTensor.shape().toList()

        require(inputShape == ModelAssetContract.EXPECTED_INPUT_SHAPE) {
            "Unexpected runtime input tensor shape: $inputShape"
        }
        require(inputTensor.dataType() == DataType.FLOAT32) {
            "Unexpected runtime input tensor type: ${inputTensor.dataType()}"
        }
        require(outputShape == ModelAssetContract.EXPECTED_OUTPUT_SHAPE) {
            "Unexpected runtime output tensor shape: $outputShape"
        }
        require(outputTensor.dataType() == DataType.FLOAT32) {
            "Unexpected runtime output tensor type: ${outputTensor.dataType()}"
        }

        return ModelRuntimeTensorDetails(
            input = ModelTensorDetails(
                name = inputTensor.name(),
                shape = inputShape,
                dataType = inputTensor.dataType().name,
            ),
            output = ModelTensorDetails(
                name = outputTensor.name(),
                shape = outputShape,
                dataType = outputTensor.dataType().name,
            ),
        )
    }

    private fun Interpreter.runInference(input: ModelInputTensor): FloatArray {
        val inputBuffer = ByteBuffer
            .allocateDirect(input.data.size * FLOAT_BYTE_COUNT)
            .order(ByteOrder.nativeOrder())
        inputBuffer.asFloatBuffer().put(input.data)
        inputBuffer.rewind()

        val output = Array(ModelAssetContract.INPUT_BATCH_SIZE) {
            FloatArray(ModelAssetContract.OUTPUT_CLASS_COUNT)
        }
        run(inputBuffer, output)
        return output.first()
    }

    private companion object {
        const val CPU_THREAD_COUNT = 1
        const val FLOAT_BYTE_COUNT = 4
        const val EXPECTED_INPUT_TENSOR_COUNT = 1
        const val EXPECTED_OUTPUT_TENSOR_COUNT = 1
    }
}
