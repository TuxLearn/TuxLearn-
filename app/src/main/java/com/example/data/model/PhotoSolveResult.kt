package com.example.data.model

import java.util.UUID

/**
 * Represents an individual calculation or logical step in a photo-solved question.
 */
data class SolveStep(
    val stepNumber: Int,
    val title: String,
    val formulaOrWork: String? = null,
    val explanation: String
)

/**
 * Result model for the "Photo Question Solver" feature.
 * Contains:
 * - Image reference (URI or sample ID)
 * - Readability status & unreadable helpful feedback
 * - Extracted question statement
 * - Automatically identified Subject and Topic
 * - Direct Answer
 * - Step-by-Step Explanation
 * - Easy Explanation
 * - Important Exam Point
 */
data class PhotoSolveResult(
    val id: String = UUID.randomUUID().toString(),
    val imageUriString: String? = null,
    val samplePhotoId: String? = null,
    val samplePhotoTitle: String? = null,
    val isReadable: Boolean = true,
    val unreadableReason: String? = null,
    val extractedQuestion: String,
    val identifiedSubject: String,
    val identifiedTopic: String,
    val directAnswer: String,
    val stepByStepExplanation: List<SolveStep> = emptyList(),
    val easyExplanation: String,
    val examPoint: String,
    val confidenceScore: Int = 96,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sample photo question item for immediate preview and testing in emulator/browser.
 */
data class SampleQuestionPhoto(
    val id: String,
    val title: String,
    val subject: String,
    val topic: String,
    val snippet: String,
    val isBlurryOrUnreadable: Boolean = false
)
