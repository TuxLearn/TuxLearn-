package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.model.StudyNote
import com.example.ui.screens.NotesScreen
import com.example.ui.theme.StudoraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotesFeatureRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNotesScreenDisplaysNotesAndPDFs() {
        val sampleNotes = listOf(
            StudyNote(
                id = 1,
                title = "Thermodynamics Laws",
                subject = "Physics",
                chapter = "Unit 1",
                content = "First law: dQ = dU + dW. Conservation of energy.",
                tags = "Formulas, Physics",
                isFavorite = true
            ),
            StudyNote(
                id = 2,
                title = "Organic Chemistry Reactions",
                subject = "Chemistry",
                chapter = "Unit 4",
                content = "Aldol condensation, Grignard reagent mechanisms.",
                tags = "PDF, Textbook",
                isFavorite = false
            )
        )

        composeTestRule.setContent {
            StudoraTheme {
                NotesScreen(
                    notes = sampleNotes,
                    onAddNote = { _, _, _, _, _, _ -> },
                    onUpdateNote = {},
                    onDeleteNote = {},
                    onToggleFavorite = {},
                    onMarkTopicStudied = { _, _, _ -> }
                )
            }
        }

        // Verify Title and Metrics
        composeTestRule.onNodeWithText("Notes & PDF Organizer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thermodynamics Laws").assertIsDisplayed()

        // Verify Tabs
        composeTestRule.onNodeWithTag("notes_tab_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notes_tab_imported_docs").assertIsDisplayed()

        // Switch to Imported PDFs tab
        composeTestRule.onNodeWithTag("notes_tab_imported_docs").performClick()
        composeTestRule.onNodeWithText("Organic Chemistry Reactions").assertIsDisplayed()
    }

    @Test
    fun testNotesCreationFlow() {
        var createdTitle = ""
        var createdSubject = ""
        var createdContent = ""

        composeTestRule.setContent {
            StudoraTheme {
                NotesScreen(
                    notes = emptyList(),
                    onAddNote = { title, sub, _, content, _, _ ->
                        createdTitle = title
                        createdSubject = sub
                        createdContent = content
                    },
                    onUpdateNote = {},
                    onDeleteNote = {},
                    onToggleFavorite = {},
                    onMarkTopicStudied = { _, _, _ -> }
                )
            }
        }

        // Click on FAB to open note editor
        composeTestRule.onNodeWithTag("notes_fab_add").performClick()

        // Enter Title and Content
        composeTestRule.onNodeWithTag("notes_input_title").performTextInput("Newton's Laws")
        composeTestRule.onNodeWithTag("notes_input_content").performTextInput("F = ma, action and reaction are equal and opposite")

        // Save
        composeTestRule.onNodeWithTag("notes_btn_save").performClick()

        assertEquals("Newton's Laws", createdTitle)
        assertTrue(createdContent.contains("F = ma"))
    }
}
