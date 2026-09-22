package com.example.data

import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.StudiedTopic
import com.example.data.model.SubjectMark
import com.example.data.model.TimetableSlot
import com.example.data.model.WeakAreaTopic
import com.example.util.DeviceTimeService
import java.time.format.DateTimeFormatter
import java.util.Locale

object InitialData {
    private fun getRelativeDate(daysAhead: Int): String {
        val date = DeviceTimeService.todayLocalDate().plusDays(daysAhead.toLong())
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }
    val sampleProfile = StudentProfile(
        id = 1,
        name = "Alex Rivera",
        grade = "High School / College Prep",
        streakDays = 7,
        xpPoints = 850,
        level = 3,
        targetGpa = "3.9 / 95%",
        totalStudyHours = 42.5,
        tasksCompletedCount = 18
    )

    val sampleTasks = listOf(
        StudyTask(
            id = 1,
            title = "Calculus: Practice Integration by Parts",
            subject = "Mathematics",
            dueDate = "Yesterday",
            estimatedMinutes = 45,
            isCompleted = false,
            priority = "High",
            xpReward = 35
        ),
        StudyTask(
            id = 2,
            title = "Physics: Revise Electromagnetism formulas",
            subject = "Physics",
            dueDate = "Today",
            estimatedMinutes = 30,
            isCompleted = true,
            completedAt = System.currentTimeMillis() - 3600000,
            priority = "High",
            xpReward = 25
        ),
        StudyTask(
            id = 3,
            title = "Chemistry: Complete Periodic Trends worksheet",
            subject = "Chemistry",
            dueDate = "Yesterday",
            estimatedMinutes = 25,
            isCompleted = false,
            priority = "Medium",
            xpReward = 20
        ),
        StudyTask(
            id = 4,
            title = "Physics: Revise Laws of Motion & Momentum",
            subject = "Physics",
            dueDate = "Yesterday",
            estimatedMinutes = 30,
            isCompleted = false,
            priority = "High",
            xpReward = 30
        ),
        StudyTask(
            id = 5,
            title = "Computer Science: Review Binary Trees & Graphs",
            subject = "Computer Science",
            dueDate = "Tomorrow",
            estimatedMinutes = 40,
            isCompleted = false,
            priority = "Medium",
            xpReward = 30
        ),
        StudyTask(
            id = 6,
            title = "Biology: Review Cellular Respiration & ATP Cycle",
            subject = "Biology",
            dueDate = "Today",
            estimatedMinutes = 35,
            isCompleted = false,
            completedAt = null,
            priority = "High",
            xpReward = 30
        ),
        StudyTask(
            id = 7,
            title = "Literature: Outline Shakespeare Analysis Essay",
            subject = "Literature",
            dueDate = getRelativeDate(2),
            estimatedMinutes = 30,
            isCompleted = false,
            priority = "Low",
            xpReward = 20
        ),
        StudyTask(
            id = 8,
            title = "History: Review World War II Timeline",
            subject = "History",
            dueDate = "Yesterday",
            estimatedMinutes = 20,
            isCompleted = false,
            priority = "Medium",
            xpReward = 20
        )
    )

    val sampleExams = listOf(
        ExamItem(
            id = 1,
            subject = "Physics",
            examName = "Final Exam",
            examDate = getRelativeDate(4),
            daysLeft = 4,
            syllabusTotalTopics = 10,
            syllabusCoveredTopics = 5,
            confidenceLevel = 2,
            targetMarks = 92
        ),
        ExamItem(
            id = 2,
            subject = "Mathematics",
            examName = "Calculus Midterm",
            examDate = getRelativeDate(19),
            daysLeft = 19,
            syllabusTotalTopics = 12,
            syllabusCoveredTopics = 9,
            confidenceLevel = 4,
            targetMarks = 88
        ),
        ExamItem(
            id = 3,
            subject = "Chemistry",
            examName = "Organic Chemistry Unit Test",
            examDate = getRelativeDate(25),
            daysLeft = 25,
            syllabusTotalTopics = 8,
            syllabusCoveredTopics = 5,
            confidenceLevel = 3,
            targetMarks = 90
        ),
        ExamItem(
            id = 4,
            subject = "Computer Science",
            examName = "Algorithms Term Exam",
            examDate = getRelativeDate(32),
            daysLeft = 32,
            syllabusTotalTopics = 10,
            syllabusCoveredTopics = 9,
            confidenceLevel = 5,
            targetMarks = 95
        ),
        ExamItem(
            id = 5,
            subject = "Biology",
            examName = "AP Biology Exam",
            examDate = getRelativeDate(21),
            daysLeft = 21,
            syllabusTotalTopics = 10,
            syllabusCoveredTopics = 9,
            confidenceLevel = 5,
            targetMarks = 92
        )
    )

    val sampleFlashcards = listOf(
        FlashcardItem(
            id = 1,
            subject = "Physics",
            question = "What is Faraday's Law of Electromagnetic Induction?",
            answer = "The induced electromotive force (EMF) in a closed loop is equal to the negative rate of change of magnetic flux through the loop: EMF = -dΦ/dt.",
            hint = "Think about rate of change of magnetic flux and Lenz's negative sign.",
            isMastered = false,
            reviewCount = 2
        ),
        FlashcardItem(
            id = 2,
            subject = "Mathematics",
            question = "What is the standard Integration by Parts formula?",
            answer = "∫ u dv = u·v - ∫ v du (derived from the product rule of differentiation).",
            hint = "LIATE rule helps pick 'u'.",
            isMastered = true,
            reviewCount = 4
        ),
        FlashcardItem(
            id = 3,
            subject = "Chemistry",
            question = "State Le Chatelier's Principle.",
            answer = "If an external stress (temperature, pressure, or concentration) is applied to a system at equilibrium, the system shifts to counteract that stress.",
            hint = "Equilibrium counteracting external disturbance.",
            isMastered = true,
            reviewCount = 3
        ),
        FlashcardItem(
            id = 4,
            subject = "Computer Science",
            question = "What is the average time complexity of QuickSort?",
            answer = "Average: O(n log n). Worst case is O(n²) when the pivot chosen is consistently the smallest or largest element.",
            hint = "Divide and conquer, logarithm base 2.",
            isMastered = true,
            reviewCount = 5
        ),
        FlashcardItem(
            id = 5,
            subject = "Biology",
            question = "What is the primary role of ATP synthase in cellular respiration?",
            answer = "ATP synthase uses the proton gradient across the inner mitochondrial membrane (chemiosmosis) to synthesize ATP from ADP and inorganic phosphate.",
            hint = "H+ ion electrochemical gradient driving molecular rotor.",
            isMastered = false,
            reviewCount = 1
        ),
        FlashcardItem(
            id = 6,
            subject = "Biology",
            question = "What are the stages of Mitosis in order?",
            answer = "Prophase, Metaphase, Anaphase, Telophase (PMAT), followed by Cytokinesis.",
            hint = "Think PMAT.",
            isMastered = true,
            reviewCount = 4
        )
    )

    val sampleNotes = listOf(
        StudyNote(
            id = 1,
            title = "Calculus Essential Derivatives & Integrals",
            subject = "Mathematics",
            chapter = "Chapter 4: Integral Calculus",
            content = "1. Derivatives:\n• d/dx (sin x) = cos x\n• d/dx (cos x) = -sin x\n• d/dx (e^x) = e^x\n• d/dx (ln x) = 1/x\n\n2. Common Integrals:\n• ∫ 1/x dx = ln|x| + C\n• ∫ sec²(x) dx = tan(x) + C\n• ∫ e^(kx) dx = (1/k)e^(kx) + C\n\nTip: Always check for u-substitution before attempting integration by parts!",
            tags = "Calculus, Formulas, Derivatives",
            isFavorite = true,
            createdAt = System.currentTimeMillis() - 86400000L * 4,
            updatedAt = System.currentTimeMillis() - 3600000L * 3
        ),
        StudyNote(
            id = 2,
            title = "Laws of Motion & Conservation Principles",
            subject = "Physics",
            chapter = "Chapter 2: Mechanics & Dynamics",
            content = "• First Law: Inertia — an object maintains constant velocity unless acted upon by a net external force.\n• Second Law: F_net = dp/dt = m·a (for constant mass systems).\n• Third Law: Action-Reaction pairs: F_AB = -F_BA acting on different bodies.\n• Work-Energy Theorem: W_net = ΔK = (1/2)mv² - (1/2)mv₀².\n• Total Mechanical Energy is conserved when conservative forces act (E = K + U).",
            tags = "Mechanics, Newton, Energy",
            isFavorite = true,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            updatedAt = System.currentTimeMillis() - 3600000L * 2
        ),
        StudyNote(
            id = 3,
            title = "Chemical Equilibrium & Le Chatelier's Law",
            subject = "Chemistry",
            chapter = "Chapter 7: Equilibrium & Thermodynamics",
            content = "• Equilibrium Constant: Kc = [C]^c [D]^d / ([A]^a [B]^b) for aA + bB ⇌ cC + dD.\n• Reaction Quotient Q vs K: If Q < K, forward direction favored. If Q > K, reverse favored.\n• Le Chatelier's Principle:\n  - Increasing Reactant concentration shifts equilibrium right.\n  - Increasing Pressure shifts toward side with fewer gas moles.\n  - Exothermic (ΔH < 0): Increasing Temperature shifts equilibrium left.",
            tags = "Equilibrium, Kinetics, Thermodynamics",
            isFavorite = false,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            updatedAt = System.currentTimeMillis() - 3600000L * 5
        ),
        StudyNote(
            id = 4,
            title = "Cellular Respiration & Krebs Cycle",
            subject = "Biology",
            chapter = "Unit 3: Cellular Energetics",
            content = "Stages of Aerobic Respiration:\n1. Glycolysis (Cytosol): Glucose (6C) → 2 Pyruvate (3C). Yields net 2 ATP, 2 NADH.\n2. Pyruvate Oxidation: Mitochondrial matrix. Yields 2 Acetyl-CoA, 2 CO₂, 2 NADH.\n3. Citric Acid Cycle (Krebs): Per turn produces 3 NADH, 1 FADH₂, 1 ATP/GTP, 2 CO₂.\n4. Oxidative Phosphorylation (Chemiosmosis): Electron transport chain generates proton gradient; ATP synthase yields ~28-32 ATP.",
            tags = "Biology, ATP, Respiration, Mitochondria",
            isFavorite = true,
            createdAt = System.currentTimeMillis() - 86400000L,
            updatedAt = System.currentTimeMillis() - 3600000L
        ),
        StudyNote(
            id = 5,
            title = "Data Structures & Time Complexities",
            subject = "Computer Science",
            chapter = "Module 2: Core Data Structures",
            content = "• Dynamic Arrays: O(1) random index access, O(n) insert/delete at arbitrary positions.\n• Hash Maps / Hash Tables: O(1) expected lookup, insertion, and deletion.\n• Binary Search Trees (BST): O(log n) balanced search, insert, delete; O(n) worst case skewed.\n• Graph Traversals: BFS uses Queue (shortest unweighted paths), DFS uses Stack or recursion.\n• Heap / Priority Queue: O(1) peek min/max, O(log n) push/pop.",
            tags = "Algorithms, DataStructures, CS, BigO",
            isFavorite = false,
            createdAt = System.currentTimeMillis() - 86400000L * 5,
            updatedAt = System.currentTimeMillis() - 86400000L * 2
        ),
        StudyNote(
            id = 6,
            title = "Feynman Technique for Deep Learning",
            subject = "Other",
            chapter = "Study Methodology & Active Recall",
            content = "4-Step Feynman Technique:\n1. Choose a concept you want to understand deeply.\n2. Teach it to an imaginary middle-school student using plain language and zero jargon.\n3. Identify gaps in your explanation whenever you get stuck or resort to buzzwords.\n4. Return to source notes and refine until the mental model is crystal clear and intuitive.",
            tags = "ActiveRecall, Productivity, MetaLearning",
            isFavorite = false,
            createdAt = System.currentTimeMillis() - 86400000L * 6,
            updatedAt = System.currentTimeMillis() - 86400000L * 3
        )
    )

    val sampleTimetable = listOf(
        TimetableSlot(id = 1, dayOfWeek = "Mon", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Mathematics", roomOrTeacher = "Room 302 • Prof. Evans", isStudySession = false),
        TimetableSlot(id = 2, dayOfWeek = "Mon", startTime = "10:30 AM", endTime = "11:45 AM", subject = "Physics", roomOrTeacher = "Science Lab B • Dr. Patel", isStudySession = false),
        TimetableSlot(id = 3, dayOfWeek = "Mon", startTime = "02:00 PM", endTime = "03:15 PM", subject = "Computer Science", roomOrTeacher = "Lab 4 • Mrs. Zhang", isStudySession = false),
        TimetableSlot(id = 4, dayOfWeek = "Mon", startTime = "04:30 PM", endTime = "05:30 PM", subject = "Self-Study & Revision", roomOrTeacher = "Library Quiet Zone", isStudySession = true),

        TimetableSlot(id = 5, dayOfWeek = "Tue", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Chemistry", roomOrTeacher = "Chem Hall 101", isStudySession = false),
        TimetableSlot(id = 6, dayOfWeek = "Tue", startTime = "11:00 AM", endTime = "12:15 PM", subject = "Literature", roomOrTeacher = "Humanities Room 12", isStudySession = false),
        TimetableSlot(id = 7, dayOfWeek = "Tue", startTime = "04:00 PM", endTime = "05:30 PM", subject = "Physics Problem Solving", roomOrTeacher = "Study Lounge", isStudySession = true),

        TimetableSlot(id = 8, dayOfWeek = "Wed", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Mathematics", roomOrTeacher = "Room 302", isStudySession = false),
        TimetableSlot(id = 9, dayOfWeek = "Wed", startTime = "10:30 AM", endTime = "12:00 PM", subject = "Physics Lab", roomOrTeacher = "Science Lab B", isStudySession = false),
        TimetableSlot(id = 10, dayOfWeek = "Wed", startTime = "03:00 PM", endTime = "04:30 PM", subject = "Math Past Papers Session", roomOrTeacher = "Desk at Home", isStudySession = true),

        TimetableSlot(id = 11, dayOfWeek = "Thu", startTime = "09:30 AM", endTime = "11:00 AM", subject = "Computer Science", roomOrTeacher = "Lab 4", isStudySession = false),
        TimetableSlot(id = 12, dayOfWeek = "Thu", startTime = "01:30 PM", endTime = "02:45 PM", subject = "Chemistry Lab", roomOrTeacher = "Chem Hall 101", isStudySession = false),
        TimetableSlot(id = 13, dayOfWeek = "Thu", startTime = "05:00 PM", endTime = "06:00 PM", subject = "Active Flashcard Recall", roomOrTeacher = "TuxLearn App Session", isStudySession = true),

        TimetableSlot(id = 14, dayOfWeek = "Fri", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Mathematics", roomOrTeacher = "Room 302", isStudySession = false),
        TimetableSlot(id = 15, dayOfWeek = "Fri", startTime = "10:45 AM", endTime = "12:00 PM", subject = "Literature", roomOrTeacher = "Room 12", isStudySession = false),
        TimetableSlot(id = 16, dayOfWeek = "Fri", startTime = "02:00 PM", endTime = "03:30 PM", subject = "Weekly Exam Readiness Review", roomOrTeacher = "Study Desk", isStudySession = true)
    )

    val sampleWeakAreas = listOf(
        WeakAreaTopic(id = 1, subject = "Mathematics", topicName = "Integration by Parts with Trig substitutions", isResolved = false, priority = "High"),
        WeakAreaTopic(id = 2, subject = "Physics", topicName = "Lenz's Law induced current directions", isResolved = false, priority = "High"),
        WeakAreaTopic(id = 3, subject = "Chemistry", topicName = "Buffer solution Henderson-Hasselbalch equation", isResolved = false, priority = "Medium"),
        WeakAreaTopic(id = 4, subject = "Computer Science", topicName = "Dijkstra's shortest path algorithm implementation", isResolved = true, priority = "Medium"),
        WeakAreaTopic(id = 5, subject = "Biology", topicName = "Mendelian Genetics Punnett squares", isResolved = true, priority = "Medium")
    )

    val sampleMarks = listOf(
        SubjectMark(id = 1, subjectName = "Physics", maxMarks = 100.0, obtainedMarks = 72.0),
        SubjectMark(id = 2, subjectName = "Chemistry", maxMarks = 100.0, obtainedMarks = 81.0),
        SubjectMark(id = 3, subjectName = "Biology", maxMarks = 100.0, obtainedMarks = 88.0),
        SubjectMark(id = 4, subjectName = "Mathematics", maxMarks = 100.0, obtainedMarks = 76.0)
    )

    val sampleStudiedTopics = listOf(
        StudiedTopic(
            id = 1,
            subject = "Biology",
            topic = "Cell and Mitochondria",
            studiedAtEpochMillis = System.currentTimeMillis() - 4 * 3600 * 1000L,
            studiedDateFormatted = "Today, 10:15 AM",
            studyDurationMinutes = 35,
            memoryStrength = "Needs Revision",
            recallQuestion = "Without looking at your notes, what is the main function of mitochondria?",
            correctAnswer = "Generate ATP through cellular respiration.",
            explanation = "Mitochondria are the powerhouses of the cell. They synthesize ATP (adenosine triphosphate) through oxidative phosphorylation and the citric acid cycle (Krebs cycle) to power vital cellular activities.",
            scheduledTestEpochMillis = System.currentTimeMillis() - 1800 * 1000L, // Ready to test now
            lastTestedEpochMillis = null,
            testCount = 0,
            forgotCount = 1,
            rememberedCount = 0,
            priorityScore = 30
        ),
        StudiedTopic(
            id = 2,
            subject = "Mathematics",
            topic = "Integration by Parts",
            studiedAtEpochMillis = System.currentTimeMillis() - 24 * 3600 * 1000L,
            studiedDateFormatted = "Yesterday, 03:30 PM",
            studyDurationMinutes = 45,
            memoryStrength = "Strong",
            recallQuestion = "Without looking at your notes, state the Integration by Parts formula and the LIATE rule for choosing 'u'.",
            correctAnswer = "∫ u dv = u·v - ∫ v du, using LIATE priority for 'u'.",
            explanation = "Integration by parts formula: ∫ u dv = u·v - ∫ v du. LIATE stands for Logarithmic, Inverse trig, Algebraic, Trigonometric, Exponential to prioritize which function to differentiate.",
            scheduledTestEpochMillis = System.currentTimeMillis() + 48 * 3600 * 1000L,
            lastTestedEpochMillis = System.currentTimeMillis() - 12 * 3600 * 1000L,
            testCount = 2,
            forgotCount = 0,
            rememberedCount = 2,
            priorityScore = 5
        ),
        StudiedTopic(
            id = 3,
            subject = "Physics",
            topic = "Faraday's Law of Induction",
            studiedAtEpochMillis = System.currentTimeMillis() - 48 * 3600 * 1000L,
            studiedDateFormatted = "2 days ago",
            studyDurationMinutes = 30,
            memoryStrength = "Needs Revision",
            recallQuestion = "Without looking at your notes, what does Faraday's Law state and what is the physical meaning of the minus sign?",
            correctAnswer = "EMF = -dΦ/dt. The negative sign represents Lenz's Law.",
            explanation = "Induced EMF equals the negative rate of change of magnetic flux. The negative sign (Lenz's Law) means the induced current produces a magnetic field opposing the change in magnetic flux.",
            scheduledTestEpochMillis = System.currentTimeMillis() - 600 * 1000L, // Ready to test now
            lastTestedEpochMillis = System.currentTimeMillis() - 20 * 3600 * 1000L,
            testCount = 1,
            forgotCount = 1,
            rememberedCount = 0,
            priorityScore = 35 // high priority because marked "I Forgot" previously
        ),
        StudiedTopic(
            id = 4,
            subject = "Computer Science",
            topic = "Binary Search Trees & Graphs",
            studiedAtEpochMillis = System.currentTimeMillis() - 18 * 3600 * 1000L,
            studiedDateFormatted = "Yesterday, 07:00 PM",
            studyDurationMinutes = 40,
            memoryStrength = "Medium",
            recallQuestion = "Without looking at your notes, what is the key invariant property of a Binary Search Tree (BST)?",
            correctAnswer = "Left subtree < Node < Right subtree.",
            explanation = "For every node N in a BST, all nodes in its left subtree have keys strictly less than N, and all nodes in its right subtree have keys strictly greater than N.",
            scheduledTestEpochMillis = System.currentTimeMillis() + 8 * 3600 * 1000L,
            lastTestedEpochMillis = System.currentTimeMillis() - 8 * 3600 * 1000L,
            testCount = 1,
            forgotCount = 0,
            rememberedCount = 1,
            priorityScore = 15
        ),
        StudiedTopic(
            id = 5,
            subject = "Chemistry",
            topic = "Chemical Equilibrium & Le Chatelier",
            studiedAtEpochMillis = System.currentTimeMillis() - 72 * 3600 * 1000L,
            studiedDateFormatted = "3 days ago",
            studyDurationMinutes = 25,
            memoryStrength = "Strong",
            recallQuestion = "Without looking at your notes, state Le Chatelier's Principle and what happens when pressure increases on a gas equilibrium.",
            correctAnswer = "Equilibrium shifts to counteract disturbance; higher pressure shifts to fewer gas moles.",
            explanation = "When a system at equilibrium is disturbed, it shifts to counteract the disturbance. Increasing pressure shifts equilibrium toward the side with fewer moles of gas.",
            scheduledTestEpochMillis = System.currentTimeMillis() + 72 * 3600 * 1000L,
            lastTestedEpochMillis = System.currentTimeMillis() - 24 * 3600 * 1000L,
            testCount = 3,
            forgotCount = 0,
            rememberedCount = 3,
            priorityScore = 4
        )
    )
}

