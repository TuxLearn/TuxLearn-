package com.example.data.model

import java.util.UUID

/**
 * Data model representing an Ask & Explain concept explanation response.
 * Contains:
 * 1. Direct Answer
 * 2. Easy Explanation in simple student-friendly language
 * 3. A simple Example
 * 4. Important Exam Point
 * 5. One Quick Recall Question & Answer
 */
data class StudyExplanation(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val subject: String,
    val directAnswer: String,
    val easyExplanation: String,
    val example: String,
    val examPoint: String,
    val quickRecallQuestion: String,
    val quickRecallAnswer: String,
    val timestamp: Long = System.currentTimeMillis()
)
