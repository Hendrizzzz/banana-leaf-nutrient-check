package com.bananaleafnutrientcheck.app.ml

object ModelAssetContract {
    const val MODEL_ASSET_FILE = "banana_mobilenetv2_11class.tflite"
    const val LABELS_ASSET_FILE = "labels.txt"
    const val METADATA_ASSET_FILE = "banana_mobilenetv2_11class_metadata.json"

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
