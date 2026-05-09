package com.bananaleafnutrientcheck.app.ml

object ModelAssetContract {
    const val MODEL_ASSET_FILE = "banana_mobilenetv2_11class.tflite"
    const val LABELS_ASSET_FILE = "labels.txt"
    const val METADATA_ASSET_FILE = "banana_mobilenetv2_11class_metadata.json"

    const val INPUT_BATCH_SIZE = 1
    const val INPUT_WIDTH = 224
    const val INPUT_HEIGHT = 224
    const val INPUT_CHANNELS = 3
    const val OUTPUT_CLASS_COUNT = 11
    const val NORMALIZATION_SCALE = 127.5f
    const val NORMALIZATION_OFFSET = 1.0f

    val EXPECTED_INPUT_SHAPE = listOf(1, 224, 224, 3)
    val EXPECTED_OUTPUT_SHAPE = listOf(1, 11)

    val EXPECTED_LABELS = listOf(
        "boron",
        "calcium",
        "healthy",
        "iron",
        "magnesium",
        "manganese",
        "nitrogen",
        "phosphorous",
        "potassium",
        "sulphur",
        "zinc",
    )
}
