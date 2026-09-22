package com.example

import com.example.data.model.PhotoSolveResult
import com.example.util.PhotoSolveEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSolveFeatureTest {

    @Test
    fun testSamplePhotosAvailable() {
        val samples = PhotoSolveEngine.SAMPLE_PHOTOS
        assertTrue("At least 5 sample photos should be provided", samples.size >= 5)

        val sampleIds = samples.map { it.id }
        assertTrue(sampleIds.contains("sample_calc"))
        assertTrue(sampleIds.contains("sample_phys"))
        assertTrue(sampleIds.contains("sample_chem"))
        assertTrue(sampleIds.contains("sample_bio"))
        assertTrue(sampleIds.contains("sample_cs"))
        assertTrue(sampleIds.contains("sample_unclear"))
    }

    @Test
    fun testMathCalculusSampleContainsAllFiveRequiredSections() {
        val result = PhotoSolveEngine.solveSamplePhoto("sample_calc")

        assertTrue(result.isReadable)
        assertEquals("Mathematics", result.identifiedSubject)
        assertTrue(result.identifiedTopic.contains("Calculus", ignoreCase = true))

        // 1. Question
        assertTrue("Question must not be blank", result.extractedQuestion.isNotBlank())
        assertTrue(result.extractedQuestion.contains("integral", ignoreCase = true))

        // 2. Direct Answer
        assertTrue("Direct Answer must not be blank", result.directAnswer.isNotBlank())
        assertEquals("10", result.directAnswer.trim())

        // 3. Step-by-Step Explanation
        assertTrue("Must contain step-by-step breakdown", result.stepByStepExplanation.isNotEmpty())
        assertEquals(4, result.stepByStepExplanation.size)
        result.stepByStepExplanation.forEach { step ->
            assertTrue(step.stepNumber > 0)
            assertTrue(step.title.isNotBlank())
            assertTrue(step.explanation.isNotBlank())
        }

        // 4. Easy Explanation
        assertTrue("Easy explanation must not be blank", result.easyExplanation.isNotBlank())
        assertTrue(result.easyExplanation.contains("area", ignoreCase = true))

        // 5. Important Exam Point
        assertTrue("Exam point must not be blank", result.examPoint.isNotBlank())
        assertTrue(result.examPoint.contains("+ C", ignoreCase = true) || result.examPoint.contains("antiderivative", ignoreCase = true))
    }

    @Test
    fun testPhysicsKinematicsSampleContainsRequiredSections() {
        val result = PhotoSolveEngine.solveSamplePhoto("sample_phys")

        assertTrue(result.isReadable)
        assertEquals("Physics", result.identifiedSubject)
        assertTrue(result.identifiedTopic.contains("Kinematics", ignoreCase = true))

        // Direct Answer
        assertTrue(result.directAnswer.contains("20.41", ignoreCase = true))

        // Steps
        assertTrue(result.stepByStepExplanation.size >= 3)
        assertTrue(result.stepByStepExplanation.any { it.formulaOrWork?.contains("v² = u²") == true || it.title.contains("Kinematic") })

        // Easy Explanation
        assertTrue(result.easyExplanation.contains("gravity", ignoreCase = true))

        // Exam Point
        assertTrue(result.examPoint.contains("unit", ignoreCase = true) || result.examPoint.contains("sign", ignoreCase = true))
    }

    @Test
    fun testChemistryStoichiometrySample() {
        val result = PhotoSolveEngine.solveSamplePhoto("sample_chem")

        assertTrue(result.isReadable)
        assertEquals("Chemistry", result.identifiedSubject)
        assertTrue(result.identifiedTopic.contains("Stoichiometry", ignoreCase = true))
        assertTrue(result.directAnswer.contains("22 g", ignoreCase = true))
        assertTrue(result.stepByStepExplanation.isNotEmpty())
    }

    @Test
    fun testBiologyRespirationSample() {
        val result = PhotoSolveEngine.solveSamplePhoto("sample_bio")

        assertTrue(result.isReadable)
        assertEquals("Biology", result.identifiedSubject)
        assertTrue(result.directAnswer.contains("30 to 32 ATP", ignoreCase = true) || result.directAnswer.contains("ATP"))
        assertTrue(result.stepByStepExplanation.size >= 4)
    }

    @Test
    fun testComputerScienceQuickSortSample() {
        val result = PhotoSolveEngine.solveSamplePhoto("sample_cs")

        assertTrue(result.isReadable)
        assertEquals("Computer Science", result.identifiedSubject)
        assertTrue(result.directAnswer.contains("O(n log n)") && result.directAnswer.contains("O(n²)"))
        assertTrue(result.stepByStepExplanation.isNotEmpty())
    }

    @Test
    fun testUnclearOrBlurryPhotoHandledGracefully() {
        // 1. Predefined unclear sample
        val unclearSample = PhotoSolveEngine.solveSamplePhoto("sample_unclear")
        assertFalse("Unclear photo must have isReadable = false", unclearSample.isReadable)
        assertNotNull("Must provide unreadableReason", unclearSample.unreadableReason)
        assertTrue(unclearSample.unreadableReason!!.contains("unclear", ignoreCase = true) ||
                unclearSample.unreadableReason!!.contains("unreadable", ignoreCase = true))

        // 2. Gibberish or blank text from bad OCR
        val blankResult = PhotoSolveEngine.solveExtractedQuestion("   ")
        assertFalse("Blank capture must be marked unreadable", blankResult.isReadable)
        assertNotNull(blankResult.unreadableReason)

        val garbledResult = PhotoSolveEngine.solveExtractedQuestion("!@#$$%^^&&**")
        assertFalse("Garbled OCR noise must be marked unreadable", garbledResult.isReadable)
    }

    @Test
    fun testAutomaticSubjectAndTopicClassification() {
        val (mathSub, mathTopic) = PhotoSolveEngine.identifySubjectAndTopic(
            "Find the derivative of f(x) = 4x^3 - 5x + 2 with respect to x."
        )
        assertEquals("Mathematics", mathSub)
        assertTrue(mathTopic.contains("Calculus", ignoreCase = true))

        val (physSub, physTopic) = PhotoSolveEngine.identifySubjectAndTopic(
            "A circuit has a 12V battery and a 4 ohm resistor. What is the current?"
        )
        assertEquals("Physics", physSub)
        assertTrue(physTopic.contains("Electricity", ignoreCase = true))

        val (chemSub, chemTopic) = PhotoSolveEngine.identifySubjectAndTopic(
            "What is the pH of a 0.01 M hydrochloric acid solution?"
        )
        assertEquals("Chemistry", chemSub)
        assertTrue(chemTopic.contains("Acid", ignoreCase = true))

        val (bioSub, bioTopic) = PhotoSolveEngine.identifySubjectAndTopic(
            "Describe the stages of mitosis during human cell division."
        )
        assertEquals("Biology", bioSub)
        assertTrue(bioTopic.contains("Cell", ignoreCase = true))
    }

    @Test
    fun testDynamicSolverForNewExtractedQuestion() {
        val customQuestion = "Solve for x in the quadratic equation 2x^2 + 5x - 3 = 0."
        val result = PhotoSolveEngine.solveExtractedQuestion(customQuestion, imageUriString = "content://media/photo/123")

        assertTrue(result.isReadable)
        assertEquals("Mathematics", result.identifiedSubject)
        assertEquals(customQuestion, result.extractedQuestion)
        assertEquals("content://media/photo/123", result.imageUriString)
        assertTrue(result.directAnswer.isNotBlank())
        assertTrue(result.stepByStepExplanation.isNotEmpty())
        assertTrue(result.easyExplanation.isNotBlank())
        assertTrue(result.examPoint.isNotBlank())
    }
}
