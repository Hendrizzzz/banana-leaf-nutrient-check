package com.bananaleafnutrientcheck.app.presentation

import androidx.annotation.StringRes
import com.bananaleafnutrientcheck.app.R
import com.bananaleafnutrientcheck.app.ml.ModelAssetContract

data class AboutClassInfoItem(
    val modelOrder: Int,
    val internalLabel: String,
    val displayLabel: String,
    @param:StringRes val noteResId: Int,
    val isSupplemental: Boolean,
)

object AboutClassInfo {
    private val displayInfoByLabel = mapOf(
        "boron" to ClassDisplayInfo(
            displayLabel = "Boron",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "calcium" to ClassDisplayInfo(
            displayLabel = "Calcium",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "healthy" to ClassDisplayInfo(
            displayLabel = "Healthy",
            noteResId = R.string.about_class_note_healthy,
        ),
        "iron" to ClassDisplayInfo(
            displayLabel = "Iron",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "magnesium" to ClassDisplayInfo(
            displayLabel = "Magnesium",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "manganese" to ClassDisplayInfo(
            displayLabel = "Manganese",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "nitrogen" to ClassDisplayInfo(
            displayLabel = "Nitrogen",
            noteResId = R.string.about_class_note_maize_supplemental,
            isSupplemental = true,
        ),
        "phosphorous" to ClassDisplayInfo(
            displayLabel = "Phosphorus",
            noteResId = R.string.about_class_note_maize_supplemental,
            isSupplemental = true,
        ),
        "potassium" to ClassDisplayInfo(
            displayLabel = "Potassium",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "sulphur" to ClassDisplayInfo(
            displayLabel = "Sulphur",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
        "zinc" to ClassDisplayInfo(
            displayLabel = "Zinc",
            noteResId = R.string.about_class_note_banana_deficiency,
        ),
    )

    val items: List<AboutClassInfoItem> =
        ModelAssetContract.EXPECTED_LABELS.mapIndexed { index, label ->
            val displayInfo = displayInfoByLabel.getValue(label)
            AboutClassInfoItem(
                modelOrder = index + 1,
                internalLabel = label,
                displayLabel = displayInfo.displayLabel,
                noteResId = displayInfo.noteResId,
                isSupplemental = displayInfo.isSupplemental,
            )
        }

    private data class ClassDisplayInfo(
        val displayLabel: String,
        @param:StringRes val noteResId: Int,
        val isSupplemental: Boolean = false,
    )
}
