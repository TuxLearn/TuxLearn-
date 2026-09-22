package com.example

import com.example.data.model.StudyExplanation
import com.example.util.ExplanationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskExplainFeatureTest {

    @Test
    fun testAllRequiredSubjectsSupported() {
        val expectedSubjects = listOf(
            "Physics",
            "Chemistry",
            "Biology",
            "Mathematics",
            "Computer Science",
            "English",
            "Other"
        )
        for (subject in expectedSubjects) {
            assertTrue(
                "Subject $subject must be supported",
                ExplanationEngine.SUPPORTED_SUBJECTS.contains(subject)
            )
        }
    }

    @Test
    fun testPhysicsQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "What is Newton's Third Law?",
            selectedSubject = "Physics"
        )

        // 1. Direct Answer
        assertTrue(explanation.directAnswer.isNotBlank())
        assertTrue(explanation.directAnswer.contains("equal and opposite", ignoreCase = true))

        // 2. Easy Explanation
        assertTrue(explanation.easyExplanation.isNotBlank())
        assertTrue(explanation.easyExplanation.contains("push", ignoreCase = true))

        // 3. A simple Example
        assertTrue(explanation.example.isNotBlank())
        assertTrue(explanation.example.contains("skateboard", ignoreCase = true))

        // 4. Important Exam Point
        assertTrue(explanation.examPoint.isNotBlank())
        assertTrue(explanation.examPoint.contains("same body", ignoreCase = true) || explanation.examPoint.contains("trap", ignoreCase = true))

        // 5. One Quick Recall Question
        assertTrue(explanation.quickRecallQuestion.isNotBlank())
        assertTrue(explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testChemistryQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "What is the difference between covalent and ionic bonds?",
            selectedSubject = "Chemistry"
        )

        assertTrue("Direct answer missing", explanation.directAnswer.isNotBlank())
        assertTrue("Easy explanation missing", explanation.easyExplanation.isNotBlank())
        assertTrue("Simple example missing", explanation.example.isNotBlank())
        assertTrue("Exam point missing", explanation.examPoint.isNotBlank())
        assertTrue("Quick recall question missing", explanation.quickRecallQuestion.isNotBlank())
        assertTrue("Quick recall answer missing", explanation.quickRecallAnswer.isNotBlank())
        assertEquals("Chemistry", explanation.subject)
    }

    @Test
    fun testBiologyQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "How does photosynthesis work?",
            selectedSubject = "Biology"
        )

        assertTrue(explanation.directAnswer.contains("glucose", ignoreCase = true))
        assertTrue(explanation.easyExplanation.isNotBlank())
        assertTrue(explanation.example.isNotBlank())
        assertTrue(explanation.examPoint.contains("6CO₂", ignoreCase = true) || explanation.examPoint.contains("Chloroplast", ignoreCase = true))
        assertTrue(explanation.quickRecallQuestion.isNotBlank())
        assertTrue(explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testMathematicsQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "What is the Pythagorean theorem?",
            selectedSubject = "Mathematics"
        )

        assertTrue(explanation.directAnswer.contains("a² + b² = c²", ignoreCase = true) || explanation.directAnswer.contains("hypotenuse", ignoreCase = true))
        assertTrue(explanation.easyExplanation.isNotBlank())
        assertTrue(explanation.example.contains("3-4-5", ignoreCase = true) || explanation.example.contains("triangle", ignoreCase = true))
        assertTrue(explanation.examPoint.contains("90°", ignoreCase = true) || explanation.examPoint.contains("right-angled", ignoreCase = true))
        assertTrue(explanation.quickRecallQuestion.isNotBlank())
        assertTrue(explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testComputerScienceQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "What is Binary Search?",
            selectedSubject = "Computer Science"
        )

        assertTrue(explanation.directAnswer.contains("O(log n)", ignoreCase = true) || explanation.directAnswer.contains("sorted", ignoreCase = true))
        assertTrue(explanation.easyExplanation.contains("dictionary", ignoreCase = true) || explanation.easyExplanation.contains("half", ignoreCase = true))
        assertTrue(explanation.example.isNotBlank())
        assertTrue(explanation.examPoint.contains("sorted", ignoreCase = true))
        assertTrue(explanation.quickRecallQuestion.isNotBlank())
        assertTrue(explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testEnglishQuestionContainsAllFiveRequiredSections() {
        val explanation = ExplanationEngine.explainQuestion(
            question = "What is the difference between a Metaphor and a Simile?",
            selectedSubject = "English"
        )

        assertTrue(explanation.directAnswer.contains("simile", ignoreCase = true))
        assertTrue(explanation.easyExplanation.isNotBlank())
        assertTrue(explanation.example.contains("lion", ignoreCase = true) || explanation.example.contains("sun", ignoreCase = true))
        assertTrue(explanation.examPoint.isNotBlank())
        assertTrue(explanation.quickRecallQuestion.isNotBlank())
        assertTrue(explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testDynamicArbitraryQuestionSynthesis() {
        // A unique topic not in curated list
        val customQuestion = "Explain quantum entanglement in quantum physics"
        val explanation = ExplanationEngine.explainQuestion(
            question = customQuestion,
            selectedSubject = "Physics"
        )

        assertNotNull(explanation)
        assertEquals(customQuestion, explanation.question)
        assertEquals("Physics", explanation.subject)
        assertTrue("Direct answer must not be empty", explanation.directAnswer.isNotBlank())
        assertTrue("Easy explanation must not be empty", explanation.easyExplanation.isNotBlank())
        assertTrue("Example must not be empty", explanation.example.isNotBlank())
        assertTrue("Exam point must not be empty", explanation.examPoint.isNotBlank())
        assertTrue("Quick recall question must not be empty", explanation.quickRecallQuestion.isNotBlank())
        assertTrue("Quick recall answer must not be empty", explanation.quickRecallAnswer.isNotBlank())
    }

    @Test
    fun testSamplePromptsAvailability() {
        assertTrue(ExplanationEngine.SAMPLE_PROMPTS.isNotEmpty())
        val physicsPrompts = ExplanationEngine.SAMPLE_PROMPTS.filter { it.subject == "Physics" }
        val chemPrompts = ExplanationEngine.SAMPLE_PROMPTS.filter { it.subject == "Chemistry" }
        val mathPrompts = ExplanationEngine.SAMPLE_PROMPTS.filter { it.subject == "Mathematics" }

        assertTrue(physicsPrompts.isNotEmpty())
        assertTrue(chemPrompts.isNotEmpty())
        assertTrue(mathPrompts.isNotEmpty())
    }
}
