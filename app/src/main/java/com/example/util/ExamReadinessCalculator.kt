package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import com.example.data.model.WeakAreaTopic
import kotlin.math.roundToInt

enum class ReadinessLevel(
    val label: String,
    val minScore: Int,
    val maxScore: Int,
    val primaryColor: Color,
    val containerColor: Color,
    val textColor: Color
) {
    NEEDS_ATTENTION("Needs Attention", 0, 39, Color(0xFFDC2626), Color(0xFFFEE2E2), Color(0xFF991B1B)),
    GETTING_STARTED("Getting Started", 40, 59, Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF92400E)),
    ALMOST_READY("Almost Ready", 60, 79, Color(0xFF2563EB), Color(0xFFDBEAFE), Color(0xFF1E40AF)),
    EXAM_READY("Exam Ready", 80, 100, Color(0xFF059669), Color(0xFFD1FAE5), Color(0xFF065F46));

    companion object {
        fun fromScore(score: Int): ReadinessLevel = when {
            score >= 80 -> EXAM_READY
            score >= 60 -> ALMOST_READY
            score >= 40 -> GETTING_STARTED
            else -> NEEDS_ATTENTION
        }
    }
}

data class SubjectReadinessData(
    val subject: String,
    val readinessPercentage: Int,
    val level: ReadinessLevel,
    val topicsCompleted: Int,
    val topicsRemaining: Int,
    val totalTopics: Int,
    val strongTopics: List<String>,
    val weakTopics: List<String>,
    val topicsNeedingRevision: List<String>,
    val lastStudiedDate: String,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val masteredCardsCount: Int,
    val totalCardsCount: Int,
    val needsPracticeCardsCount: Int,
    val testPracticeAccuracy: Int,
    val suggestedNextTask: String,
    val daysUntilExam: Int?,
    val examName: String?,
    val examConfidence: Int
)

object ExamReadinessCalculator {

    // Standard syllabus topics for high school & college subjects to complement user-entered items
    private val subjectSyllabusMap = mapOf(
        "Biology" to listOf(
            "Cellular Respiration & ATP",
            "DNA Replication & Protein Synthesis",
            "Mendelian Genetics & Heredity",
            "Photosynthesis & Chloroplasts",
            "Cell Division & Mitosis",
            "Ecology & Ecosystem Dynamics",
            "Human Circulatory & Nervous Systems",
            "Evolution & Natural Selection",
            "Biomolecules & Enzymes",
            "Immune Response & Antibodies"
        ),
        "Chemistry" to listOf(
            "Periodic Trends & Atomic Structure",
            "Chemical Bonding & Molecular Shapes",
            "Stoichiometry & Mole Concept",
            "Chemical Equilibrium & Le Chatelier",
            "Acids, Bases & pH Calculations",
            "Thermodynamics & Enthalpy",
            "Reaction Kinetics & Rate Laws",
            "Organic Chemistry & Functional Groups"
        ),
        "Physics" to listOf(
            "Kinematics & Projectile Motion",
            "Newton's Laws & Dynamics",
            "Work, Energy & Conservation of Momentum",
            "Rotational Motion & Torque",
            "Gravitation & Orbital Mechanics",
            "Simple Harmonic Motion & Waves",
            "Electrostatics & Coulomb's Law",
            "Electromagnetism & Faraday's Law",
            "DC Circuits & Kirchhoff's Rules",
            "Geometric & Wave Optics"
        ),
        "Mathematics" to listOf(
            "Differential Calculus & Chain Rule",
            "Integral Calculus & Substitution",
            "Integration by Parts & Partial Fractions",
            "Differential Equations",
            "Vectors in 2D & 3D Space",
            "Matrices & Systems of Linear Equations",
            "Probability & Combinatorics",
            "Sequences, Series & Taylor Polynomials",
            "Coordinate Geometry & Conic Sections",
            "Trigonometric Identities & Equations",
            "Complex Numbers & De Moivre's Theorem",
            "Statistical Distributions & Hypothesis Testing"
        ),
        "Computer Science" to listOf(
            "Object-Oriented Design & Polymorphism",
            "Arrays, Linked Lists & Stacks",
            "Binary Search Trees & Heaps",
            "Sorting & Searching Algorithms",
            "Graph Traversal: BFS & DFS",
            "Shortest Path Algorithms: Dijkstra",
            "Recursion & Dynamic Programming",
            "Big-O Time & Space Complexity",
            "Relational Databases & SQL Queries",
            "Computer Architecture & Bitwise Operations"
        ),
        "Literature" to listOf(
            "Shakespearean Tragedy & Dramatic Irony",
            "Romantic & Victorian Poetry Analysis",
            "Narrative Perspectives & Character Arcs",
            "Modernist Prose & Stream of Consciousness",
            "Literary Devices & Rhetorical Figures",
            "Comparative Thematic Essay Writing"
        )
    )

    fun calculate(
        subject: String,
        exams: List<ExamItem>,
        tasks: List<StudyTask>,
        flashcards: List<FlashcardItem>,
        weakAreas: List<WeakAreaTopic>
    ): SubjectReadinessData {
        val subjectExams = exams.filter { it.subject.equals(subject, ignoreCase = true) }
        val subjectTasks = tasks.filter { it.subject.equals(subject, ignoreCase = true) }
        val subjectCards = flashcards.filter { it.subject.equals(subject, ignoreCase = true) }
        val subjectWeakAreas = weakAreas.filter { it.subject.equals(subject, ignoreCase = true) }

        // 1. Syllabus Topics
        val syllabusTopicsList = subjectSyllabusMap[subject] ?: listOf(
            "Core Concepts & Definitions",
            "Foundational Theory",
            "Key Formulas & Principles",
            "Problem Solving Techniques",
            "Unit Review & Case Studies",
            "Past Paper Exam Questions"
        )

        val totalTopics = if (subjectExams.isNotEmpty()) {
            subjectExams.sumOf { it.syllabusTotalTopics }.coerceAtLeast(1)
        } else {
            syllabusTopicsList.size
        }

        val coveredTopics = if (subjectExams.isNotEmpty()) {
            subjectExams.sumOf { it.syllabusCoveredTopics }.coerceIn(0, totalTopics)
        } else {
            // Estimate based on completed tasks and mastered flashcards
            val autoCovered = (subjectTasks.count { it.isCompleted } * 2 + subjectCards.count { it.isMastered } * 2)
            autoCovered.coerceIn(0, totalTopics)
        }

        val remainingTopics = (totalTopics - coveredTopics).coerceAtLeast(0)
        val syllabusScore = (coveredTopics.toFloat() / totalTopics) * 100f

        // 2. Study Tasks Component
        val totalTasks = subjectTasks.size
        val completedTasks = subjectTasks.count { it.isCompleted }
        val tasksScore = if (totalTasks > 0) {
            val compRatio = completedTasks.toFloat() / totalTasks
            (compRatio * 50f) + (syllabusScore * 0.5f)
        } else {
            syllabusScore.coerceIn(40f, 85f)
        }

        // 3. Flashcards / Revision Component
        val totalCards = subjectCards.size
        val masteredCards = subjectCards.count { it.isMastered }
        val needsPracticeCards = totalCards - masteredCards
        val cardsScore = if (totalCards > 0) {
            val masteryRatio = (masteredCards.toFloat() / totalCards) * 70f
            val reviewsCount = subjectCards.sumOf { it.reviewCount }
            val reviewBonus = (reviewsCount.toFloat() * 2f).coerceAtMost(12f)
            masteryRatio + 18f + reviewBonus
        } else {
            (syllabusScore * 0.8f).coerceIn(35f, 80f)
        }

        // 4. Weak Areas Component
        val totalWeak = subjectWeakAreas.size
        val resolvedWeak = subjectWeakAreas.count { it.isResolved }
        val unresolvedWeak = totalWeak - resolvedWeak
        val weakAreaScore = if (totalWeak > 0) {
            ((resolvedWeak.toFloat() / totalWeak) * 60f + 40f)
        } else {
            80f
        }

        // 5. Practice / Test Performance & Confidence
        val examConfidence = subjectExams.maxOfOrNull { it.confidenceLevel } ?: 3
        val confidenceScore = (examConfidence.toFloat() / 5f) * 100f

        val testPracticeAccuracy = if (totalCards > 0) {
            val accuracy = ((masteredCards.toFloat() / totalCards) * 100f).roundToInt()
            accuracy.coerceIn(0, 100)
        } else {
            (examConfidence * 20).coerceIn(20, 100)
        }

        // 6. Recent Study Activity bonus (0 to 5 points)
        var recentBonus = 0f
        val hasCompletedRecentTask = subjectTasks.any {
            it.isCompleted && (it.dueDate.equals("Today", ignoreCase = true) ||
                    (it.completedAt != null && System.currentTimeMillis() - it.completedAt <= 48 * 3600 * 1000L))
        }
        val hasRecentFlashcardActivity = subjectCards.any { it.reviewCount > 0 }
        if (hasCompletedRecentTask) recentBonus += 2.0f
        if (hasRecentFlashcardActivity) recentBonus += 2.0f

        // Weighted calculation:
        // Syllabus: 35%, Cards: 25%, Tasks: 15%, Weak Areas: 15%, Confidence: 10%
        val rawScore = (syllabusScore * 0.35f) +
                (cardsScore * 0.25f) +
                (tasksScore * 0.15f) +
                (weakAreaScore * 0.15f) +
                (confidenceScore * 0.10f) +
                recentBonus

        val finalScore = rawScore.roundToInt().coerceIn(0, 100)
        val readinessLevel = ReadinessLevel.fromScore(finalScore)

        // Determine Strong Topics:
        val strongTopics = mutableListOf<String>()
        // Resolved weak areas are mastered topics
        strongTopics.addAll(subjectWeakAreas.filter { it.isResolved }.map { it.topicName })
        // Mastered flashcards
        strongTopics.addAll(subjectCards.filter { it.isMastered }.map { card ->
            card.question.takeWhile { it != '?' }.take(35)
        })
        // Completed study tasks
        strongTopics.addAll(subjectTasks.filter { it.isCompleted }.map { it.title.substringAfter(": ").take(35) })
        // Syllabus covered topics
        val coveredSyllabus = syllabusTopicsList.take(coveredTopics)
        for (topic in coveredSyllabus) {
            if (strongTopics.size < 4 && !strongTopics.contains(topic)) {
                strongTopics.add(topic)
            }
        }
        if (strongTopics.isEmpty() && coveredTopics > 0) {
            strongTopics.addAll(syllabusTopicsList.take(2))
        }

        // Determine Weak Topics:
        val weakTopics = mutableListOf<String>()
        // Unresolved weak areas
        weakTopics.addAll(subjectWeakAreas.filter { !it.isResolved }.map { it.topicName })
        // Unmastered flashcards
        weakTopics.addAll(subjectCards.filter { !it.isMastered && it.reviewCount > 0 }.map { card ->
            card.question.takeWhile { it != '?' }.take(35)
        })
        // Incomplete high priority tasks
        weakTopics.addAll(subjectTasks.filter { !it.isCompleted && it.priority == "High" }.map { it.title.substringAfter(": ").take(35) })
        // Remaining syllabus topics
        val remainingSyllabus = syllabusTopicsList.drop(coveredTopics)
        for (topic in remainingSyllabus) {
            if (weakTopics.size < 4 && !weakTopics.contains(topic)) {
                weakTopics.add(topic)
            }
        }
        if (weakTopics.isEmpty() && remainingTopics > 0) {
            weakTopics.addAll(remainingSyllabus.take(2))
        }

        // Topics Needing Revision:
        val topicsNeedingRevision = mutableListOf<String>()
        topicsNeedingRevision.addAll(subjectWeakAreas.filter { !it.isResolved }.map { it.topicName })
        topicsNeedingRevision.addAll(subjectCards.filter { !it.isMastered }.map { it.question.takeWhile { c -> c != '?' }.take(35) })
        topicsNeedingRevision.addAll(subjectTasks.filter { !it.isCompleted }.map { it.title.substringAfter(": ").take(35) })
        if (topicsNeedingRevision.isEmpty()) {
            topicsNeedingRevision.addAll(remainingSyllabus.take(3))
        }

        // Determine Last Studied Date:
        val lastStudiedDate = when {
            subjectTasks.any { it.isCompleted && (it.dueDate.equals("Today", ignoreCase = true) || (it.completedAt != null && System.currentTimeMillis() - it.completedAt <= 24 * 3600 * 1000L)) } -> "Today"
            subjectCards.any { it.reviewCount > 0 && it.isMastered } -> "Today"
            subjectTasks.any { it.isCompleted && it.completedAt != null && System.currentTimeMillis() - it.completedAt <= 48 * 3600 * 1000L } -> "Yesterday"
            subjectTasks.any { it.isCompleted } -> "2 days ago"
            subjectCards.isNotEmpty() -> "3 days ago"
            else -> "4 days ago"
        }

        // Suggested Next Study Task:
        val uncompletedTask = subjectTasks.firstOrNull { !it.isCompleted }
        val suggestedNextTask = when {
            uncompletedTask != null -> uncompletedTask.title
            weakTopics.isNotEmpty() -> "Revise weak topic: ${weakTopics.first()}"
            needsPracticeCards > 0 -> "Practice ${subject} flashcards (${needsPracticeCards} need review)"
            remainingTopics > 0 -> "Cover upcoming syllabus: ${remainingSyllabus.firstOrNull() ?: "Next chapter"}"
            else -> "Take a full practice test to maintain 100% mastery"
        }

        val nearestExam = subjectExams.minByOrNull { it.daysLeft }

        return SubjectReadinessData(
            subject = subject,
            readinessPercentage = finalScore,
            level = readinessLevel,
            topicsCompleted = coveredTopics,
            topicsRemaining = remainingTopics,
            totalTopics = totalTopics,
            strongTopics = strongTopics.distinct().take(4),
            weakTopics = weakTopics.distinct().take(4),
            topicsNeedingRevision = topicsNeedingRevision.distinct().take(5),
            lastStudiedDate = lastStudiedDate,
            completedTasksCount = completedTasks,
            totalTasksCount = totalTasks,
            masteredCardsCount = masteredCards,
            totalCardsCount = totalCards,
            needsPracticeCardsCount = needsPracticeCards,
            testPracticeAccuracy = testPracticeAccuracy,
            suggestedNextTask = suggestedNextTask,
            daysUntilExam = nearestExam?.daysLeft,
            examName = nearestExam?.examName,
            examConfidence = examConfidence
        )
    }

    fun calculateAllSubjects(
        exams: List<ExamItem>,
        tasks: List<StudyTask>,
        flashcards: List<FlashcardItem>,
        weakAreas: List<WeakAreaTopic>
    ): List<SubjectReadinessData> {
        val coreSubjects = listOf("Biology", "Chemistry", "Physics", "Mathematics", "Computer Science", "Literature")
        val allDistinctSubjects = (coreSubjects +
                exams.map { it.subject } +
                tasks.map { it.subject } +
                flashcards.map { it.subject } +
                weakAreas.map { it.subject })
            .distinctBy { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        return allDistinctSubjects.map { subject ->
            calculate(subject, exams, tasks, flashcards, weakAreas)
        }
    }

    fun calculateOverallReadiness(subjects: List<SubjectReadinessData>): Int {
        if (subjects.isEmpty()) return 70
        return (subjects.sumOf { it.readinessPercentage }.toFloat() / subjects.size).roundToInt().coerceIn(0, 100)
    }
}
