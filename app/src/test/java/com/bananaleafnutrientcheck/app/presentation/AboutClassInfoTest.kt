package com.bananaleafnutrientcheck.app.presentation

import com.bananaleafnutrientcheck.app.R
import com.bananaleafnutrientcheck.app.ml.ModelAssetContract
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutClassInfoTest {
    @Test
    fun classInformationUsesExactModelLabelOrder() {
        assertEquals(
            ModelAssetContract.EXPECTED_LABELS,
            AboutClassInfo.items.map { item -> item.internalLabel },
        )
        assertEquals(
            (1..ModelAssetContract.OUTPUT_CLASS_COUNT).toList(),
            AboutClassInfo.items.map { item -> item.modelOrder },
        )
    }

    @Test
    fun phosphorousKeepsInternalLabelAndUsesReadableDisplayLabel() {
        val phosphorous = AboutClassInfo.items.single { item ->
            item.internalLabel == "phosphorous"
        }

        assertEquals(8, phosphorous.modelOrder)
        assertEquals("phosphorous", phosphorous.internalLabel)
        assertEquals("Phosphorus", phosphorous.displayLabel)
    }

    @Test
    fun healthyClassUsesApprovedNonConfirmingCopy() {
        val healthy = AboutClassInfo.items.single { item ->
            item.internalLabel == "healthy"
        }
        val healthyNote = stringResourceValue("about_class_note_healthy")

        assertEquals(R.string.about_class_note_healthy, healthy.noteResId)
        assertEquals(
            "No visible deficiency pattern detected by this model.",
            healthyNote,
        )
        assertFalse(healthyNote.contains("confirmed", ignoreCase = true))
    }

    @Test
    fun supplementalDisclosureIsLimitedToNitrogenAndPhosphorous() {
        val supplementalLabels = AboutClassInfo.items
            .filter { item -> item.isSupplemental }
            .map { item -> item.internalLabel }

        assertEquals(listOf("nitrogen", "phosphorous"), supplementalLabels)
    }

    @Test
    fun supplementalCopyDisclosesMaizeDerivedAndNotBananaValidated() {
        val supplementalNotes = AboutClassInfo.items
            .filter { item -> item.isSupplemental }
            .map { item -> stringResourceValue("about_class_note_maize_supplemental") }

        assertTrue(
            supplementalNotes.all { note ->
                note.contains("Maize-derived") &&
                    note.contains("not banana-validated")
            },
        )
        assertEquals(
            R.string.about_class_note_maize_supplemental,
            AboutClassInfo.items.single { item -> item.internalLabel == "nitrogen" }.noteResId,
        )
        assertEquals(
            R.string.about_class_note_maize_supplemental,
            AboutClassInfo.items.single { item -> item.internalLabel == "phosphorous" }.noteResId,
        )
    }

    private fun stringResourceValue(name: String): String {
        val stringsFile = listOf(
            File("app/src/main/res/values/strings.xml"),
            File("src/main/res/values/strings.xml"),
        ).first { candidate -> candidate.isFile }
        val document = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(stringsFile)
        val strings = document.getElementsByTagName("string")

        for (index in 0 until strings.length) {
            val node = strings.item(index)
            if (node.attributes.getNamedItem("name").nodeValue == name) {
                return node.textContent
            }
        }

        error("Missing string resource: $name")
    }
}
