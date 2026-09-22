package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import com.example.data.model.WeakAreaTopic
import com.example.ui.navigation.AppScreen
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryDark
import com.example.util.DeviceTimeService
import com.example.util.ExamReadinessCalculator
import com.example.util.ReadinessLevel
import com.example.util.SubjectReadinessData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamReadinessScreen(
    exams: List<ExamItem>,
    weakAreas: List<WeakAreaTopic>,
    tasks: List<StudyTask> = emptyList(),
    flashcards: List<FlashcardItem> = emptyList(),
    onAddExam: (String, String, String, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateExamProgress: (ExamItem, Int, Int) -> Unit = { _, _, _ -> },
    onDeleteExam: (ExamItem) -> Unit = {},
    onAddWeakArea: (String, String, String) -> Unit = { _, _, _ -> },
    onToggleWeakArea: (WeakAreaTopic) -> Unit = {},
    onDeleteWeakArea: (WeakAreaTopic) -> Unit = {},
    onToggleTask: (StudyTask) -> Unit = {},
    onAddTask: (String, String, String, Int, String) -> Unit = { _, _, _, _, _ -> },
    onMarkFlashcard: (FlashcardItem, Boolean) -> Unit = { _, _ -> },
    onAddFlashcard: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onNavigateToScreen: (AppScreen) -> Unit = {}
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var improveSubjectData by remember { mutableStateOf<SubjectReadinessData?>(null) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showAddWeakAreaDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dynamic, activity-driven calculation for all subjects
    val subjectReadinessList = remember(exams, tasks, flashcards, weakAreas) {
        ExamReadinessCalculator.calculateAllSubjects(exams, tasks, flashcards, weakAreas)
    }

    val overallScore = remember(subjectReadinessList) {
        ExamReadinessCalculator.calculateOverallReadiness(subjectReadinessList)
    }

    val overallLevel = remember(overallScore) {
        ReadinessLevel.fromScore(overallScore)
    }

    // Filtered subject list for the dashboard
    val filteredSubjects = remember(subjectReadinessList, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "Needs Attention" -> subjectReadinessList.filter { it.level == ReadinessLevel.NEEDS_ATTENTION }
            "Getting Started" -> subjectReadinessList.filter { it.level == ReadinessLevel.GETTING_STARTED }
            "Almost Ready" -> subjectReadinessList.filter { it.level == ReadinessLevel.ALMOST_READY }
            "Exam Ready" -> subjectReadinessList.filter { it.level == ReadinessLevel.EXAM_READY }
            else -> subjectReadinessList
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedSubject == null) {
                FloatingActionButton(
                    onClick = { showAddExamDialog = true },
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("readiness_fab_add")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Exam")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedSubject != null) {
                val currentData = subjectReadinessList.firstOrNull { it.subject == selectedSubject }
                    ?: ExamReadinessCalculator.calculate(selectedSubject!!, exams, tasks, flashcards, weakAreas)

                SubjectDetailView(
                    data = currentData,
                    exams = exams.filter { it.subject.equals(selectedSubject, true) },
                    tasks = tasks.filter { it.subject.equals(selectedSubject, true) },
                    flashcards = flashcards.filter { it.subject.equals(selectedSubject, true) },
                    weakAreas = weakAreas.filter { it.subject.equals(selectedSubject, true) },
                    onBack = { selectedSubject = null },
                    onImprove = { improveSubjectData = currentData },
                    onToggleTask = onToggleTask,
                    onMarkFlashcard = onMarkFlashcard,
                    onToggleWeakArea = onToggleWeakArea,
                    onDeleteWeakArea = onDeleteWeakArea,
                    onEditExam = { editingExam = it },
                    onDeleteExam = onDeleteExam,
                    onAddWeakAreaClick = { showAddWeakAreaDialog = true },
                    onNavigateToScreen = onNavigateToScreen
                )
            } else {
                DashboardView(
                    overallScore = overallScore,
                    overallLevel = overallLevel,
                    subjects = filteredSubjects,
                    allSubjectCount = subjectReadinessList.size,
                    exams = exams,
                    weakAreas = weakAreas,
                    selectedFilter = selectedCategoryFilter,
                    onSelectFilter = { selectedCategoryFilter = it },
                    onSelectSubject = { selectedSubject = it },
                    onImproveSubject = { subjectName ->
                        val data = subjectReadinessList.firstOrNull { it.subject == subjectName }
                            ?: ExamReadinessCalculator.calculate(subjectName, exams, tasks, flashcards, weakAreas)
                        improveSubjectData = data
                    },
                    onAddExam = { showAddExamDialog = true },
                    onEditExam = { editingExam = it },
                    onDeleteExam = onDeleteExam,
                    onAddWeakArea = { showAddWeakAreaDialog = true },
                    onToggleWeakArea = onToggleWeakArea,
                    onDeleteWeakArea = onDeleteWeakArea
                )
            }
        }
    }

    // Modal Sheet: Improve Readiness Action Hub
    improveSubjectData?.let { data ->
        ImproveReadinessBottomSheet(
            data = data,
            tasks = tasks.filter { it.subject.equals(data.subject, true) },
            flashcards = flashcards.filter { it.subject.equals(data.subject, true) },
            weakAreas = weakAreas.filter { it.subject.equals(data.subject, true) },
            exams = exams.filter { it.subject.equals(data.subject, true) },
            onDismiss = { improveSubjectData = null },
            onToggleTask = onToggleTask,
            onMarkFlashcard = onMarkFlashcard,
            onToggleWeakArea = onToggleWeakArea,
            onUpdateExamProgress = onUpdateExamProgress,
            onAddWeakArea = onAddWeakArea,
            onAddTask = onAddTask,
            onAddFlashcard = onAddFlashcard,
            onNavigateToScreen = { screen ->
                improveSubjectData = null
                onNavigateToScreen(screen)
            }
        )
    }

    // Dialog: Schedule New Exam
    if (showAddExamDialog) {
        AddExamDialog(
            onDismiss = { showAddExamDialog = false },
            onConfirm = { sub, name, date, days, total, cov, conf, target ->
                onAddExam(sub, name, date, days, total, cov, conf, target)
                showAddExamDialog = false
            }
        )
    }

    // Dialog: Update Syllabus Progress
    editingExam?.let { exam ->
        EditExamProgressDialog(
            exam = exam,
            onDismiss = { editingExam = null },
            onSave = { cov, conf ->
                onUpdateExamProgress(exam, cov, conf)
                editingExam = null
            }
        )
    }

    // Dialog: Add Focus Topic
    if (showAddWeakAreaDialog) {
        AddWeakAreaDialog(
            initialSubject = selectedSubject ?: "Mathematics",
            onDismiss = { showAddWeakAreaDialog = false },
            onConfirm = { sub, topic, priority ->
                onAddWeakArea(sub, topic, priority)
                showAddWeakAreaDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 1. DASHBOARD VIEW
// -----------------------------------------------------------------------------

@Composable
private fun DashboardView(
    overallScore: Int,
    overallLevel: ReadinessLevel,
    subjects: List<SubjectReadinessData>,
    allSubjectCount: Int,
    exams: List<ExamItem>,
    weakAreas: List<WeakAreaTopic>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onSelectSubject: (String) -> Unit,
    onImproveSubject: (String) -> Unit,
    onAddExam: () -> Unit,
    onEditExam: (ExamItem) -> Unit,
    onDeleteExam: (ExamItem) -> Unit,
    onAddWeakArea: () -> Unit,
    onToggleWeakArea: (WeakAreaTopic) -> Unit,
    onDeleteWeakArea: (WeakAreaTopic) -> Unit
) {
    val readyCount = subjects.count { it.level == ReadinessLevel.EXAM_READY || it.level == ReadinessLevel.ALMOST_READY }
    val attentionCount = subjects.count { it.level == ReadinessLevel.NEEDS_ATTENTION }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("exam_readiness_dashboard"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Overall Readiness Card
        item {
            OverallReadinessHeroCard(
                score = overallScore,
                level = overallLevel,
                readyCount = readyCount,
                totalSubjects = allSubjectCount,
                attentionCount = attentionCount,
                onAddExam = onAddExam
            )
        }

        // Category Filter Chips
        item {
            Column {
                Text(
                    text = "Subject Readiness",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Calculated live from tasks, revision cards & syllabus completion",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Exam Ready", "Almost Ready", "Getting Started", "Needs Attention")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(filter) },
                            label = {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_$filter")
                        )
                    }
                }
            }
        }

        // Subject Readiness Cards
        if (subjects.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No subjects match this filter",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        } else {
            items(subjects, key = { it.subject }) { subjectData ->
                SubjectReadinessCard(
                    data = subjectData,
                    onTap = { onSelectSubject(subjectData.subject) },
                    onImprove = { onImproveSubject(subjectData.subject) }
                )
            }
        }

        // Scheduled Exams Breakdown Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scheduled Exams & Syllabus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Exam countdowns & syllabus coverage sliders",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                IconButton(onClick = onAddExam) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Exam", tint = IndigoPrimary)
                }
            }
        }

        items(exams, key = { it.id }) { exam ->
            val examReadinessPct = if (exam.syllabusTotalTopics == 0) 0
            else ((exam.syllabusCoveredTopics.toFloat() / exam.syllabusTotalTopics) * 100).toInt()

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_card_${exam.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = getSubjectColor(exam.subject).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = exam.subject,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = getSubjectColor(exam.subject),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exam.examName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberStreak.copy(alpha = 0.15f)
                            ) {
                                val dynamicDays = DeviceTimeService.calculateDaysUntil(exam.examDate) ?: exam.daysLeft
                                Text(
                                    text = "${dynamicDays}d left",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberStreak
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(
                                onClick = { onEditExam(exam) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Progress",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteExam(exam) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Syllabus Covered: ${exam.syllabusCoveredTopics}/${exam.syllabusTotalTopics} topics",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "$examReadinessPct%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (examReadinessPct >= 75) GreenSuccess else AmberStreak
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { examReadinessPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (examReadinessPct >= 75) GreenSuccess else AmberStreak,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Confidence:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            for (i in 1..5) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (i <= exam.confidenceLevel) AmberStreak else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "Target: ${exam.targetMarks}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        )
                    }
                }
            }
        }

        // Global Weak Areas Focus Checklist
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weak Areas Focus Checklist",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Check off topics once thoroughly revised (+20 XP)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(
                    onClick = onAddWeakArea,
                    modifier = Modifier.testTag("add_weak_area_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Focus Area", tint = IndigoPrimary)
                }
            }
        }

        items(weakAreas, key = { it.id }) { area ->
            WeakAreaItemCard(
                area = area,
                onToggle = { onToggleWeakArea(area) },
                onDelete = { onDeleteWeakArea(area) }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 2. HERO OVERALL READINESS CARD
// -----------------------------------------------------------------------------

@Composable
private fun OverallReadinessHeroCard(
    score: Int,
    level: ReadinessLevel,
    readyCount: Int,
    totalSubjects: Int,
    attentionCount: Int,
    onAddExam: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Circular Progress Ring
                Box(contentAlignment = Alignment.Center) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = score / 100f,
                        animationSpec = tween(durationMillis = 800),
                        label = "readiness_hero_ring"
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(86.dp),
                        color = level.primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 9.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = level.containerColor
                    ) {
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = level.primaryColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exam Readiness Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (score >= 80) "All systems go! Keep maintaining your streak 🎉"
                        else if (score >= 60) "$readyCount of $totalSubjects subjects on track for target grades 📈"
                        else "$attentionCount subject(s) need practice to hit targets ⚠️",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick stats strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$readyCount/$totalSubjects Subjects Ready",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = "+ Schedule Exam",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onAddExam() }
                        .padding(4.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. SUBJECT READINESS CARD (DASHBOARD LIST ITEM)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectReadinessCard(
    data: SubjectReadinessData,
    onTap: () -> Unit,
    onImprove: () -> Unit
) {
    val subjectColor = getSubjectColor(data.subject)
    val animatedProgress by animateFloatAsState(
        targetValue = data.readinessPercentage / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "subject_card_progress"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .testTag("subject_card_${data.subject.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Subject, Level Badge, and Circular Progress Ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = subjectColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = subjectColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = data.subject,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = data.level.containerColor
                            ) {
                                Text(
                                    text = data.level.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = data.level.primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (data.daysUntilExam != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${data.daysUntilExam}d to exam",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberStreak,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Visual Progress Ring & Percentage
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(54.dp),
                        color = data.level.primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${data.readinessPercentage}%",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = data.level.primaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = data.level.primaryColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Topics Completed vs Remaining & Last Studied Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 ${data.topicsCompleted} completed • ${data.topicsRemaining} remaining",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "⏱ Studied: ${data.lastStudiedDate}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Strong Topics Preview
            if (data.strongTopics.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GreenSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Strong: ${data.strongTopics.take(2).joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GreenSuccess,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Weak Topics Preview
            if (data.weakTopics.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (data.level == ReadinessLevel.NEEDS_ATTENTION) Color(0xFFDC2626) else AmberStreak,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Weak: ${data.weakTopics.take(2).joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (data.level == ReadinessLevel.NEEDS_ATTENTION) Color(0xFFDC2626) else AmberStreak,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onImprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("improve_button_${data.subject.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Improve Readiness",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTap() }
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. SUBJECT DETAIL VIEW (FULL PAGE FOR TAPPED SUBJECT)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectDetailView(
    data: SubjectReadinessData,
    exams: List<ExamItem>,
    tasks: List<StudyTask>,
    flashcards: List<FlashcardItem>,
    weakAreas: List<WeakAreaTopic>,
    onBack: () -> Unit,
    onImprove: () -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onMarkFlashcard: (FlashcardItem, Boolean) -> Unit,
    onToggleWeakArea: (WeakAreaTopic) -> Unit,
    onDeleteWeakArea: (WeakAreaTopic) -> Unit,
    onEditExam: (ExamItem) -> Unit,
    onDeleteExam: (ExamItem) -> Unit,
    onAddWeakAreaClick: () -> Unit,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val subjectColor = getSubjectColor(data.subject)
    val animatedProgress by animateFloatAsState(
        targetValue = data.readinessPercentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "detail_ring_progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("subject_detail_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_to_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Readiness Dashboard"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${data.subject} Readiness",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = data.level.containerColor
                ) {
                    Text(
                        text = data.level.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = data.level.primaryColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Big Hero Readiness Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(110.dp),
                            color = data.level.primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 10.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${data.readinessPercentage}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = data.level.primaryColor
                                )
                            )
                            Text(
                                text = "READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (data.level) {
                            ReadinessLevel.EXAM_READY -> "High mastery! You are fully prepared to excel 🌟"
                            ReadinessLevel.ALMOST_READY -> "Strong foundation. Address remaining weak spots to secure an A 📈"
                            ReadinessLevel.GETTING_STARTED -> "Good momentum. Complete more revision tasks to boost your score 🚀"
                            ReadinessLevel.NEEDS_ATTENTION -> "Needs focused revision. Review weak areas and practice cards ⚠️"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onImprove,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_improve_readiness_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Improve ${data.subject} Readiness",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 4 Key Activity Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Tasks Done",
                    value = "${data.completedTasksCount}/${data.totalTasksCount}",
                    icon = Icons.Filled.CheckCircle,
                    tint = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Cards Mastered",
                    value = "${data.masteredCardsCount}/${data.totalCardsCount}",
                    icon = Icons.Filled.Psychology,
                    tint = CyanSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Practice Accuracy",
                    value = "${data.testPracticeAccuracy}%",
                    icon = Icons.Filled.TrendingUp,
                    tint = AmberStreak,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Syllabus Covered",
                    value = "${data.topicsCompleted}/${data.totalTopics}",
                    icon = Icons.Filled.MenuBook,
                    tint = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Suggested Next Study Task Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IndigoPrimary.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = IndigoPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suggested Next Study Action",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.suggestedNextTask,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Topics That Need Revision
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Topics That Need Revision",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = AmberStreak.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${data.topicsNeedingRevision.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberStreak
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (data.topicsNeedingRevision.isEmpty()) {
                        Text(
                            text = "No pending revision topics! All caught up 🎉",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.topicsNeedingRevision.forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "• $topic",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Strong Areas (Mastered Topics)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = GreenSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strong Areas (Mastered)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = GreenSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${data.strongTopics.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (data.strongTopics.isEmpty()) {
                        Text(
                            text = "Complete study tasks and master flashcards to build strong areas.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.strongTopics.forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GreenSuccess.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = GreenSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = topic,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = GreenSuccess,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Focus Areas / Weak Areas Checklist for this Subject
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weak Areas Checklist",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onAddWeakAreaClick) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Weak Topic", tint = IndigoPrimary)
                }
            }
        }

        if (weakAreas.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No weak areas flagged for ${data.subject}. Tap + to add topics you struggle with.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(weakAreas, key = { it.id }) { area ->
                WeakAreaItemCard(
                    area = area,
                    onToggle = { onToggleWeakArea(area) },
                    onDelete = { onDeleteWeakArea(area) }
                )
            }
        }

        // Active Tasks for this Subject
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Tasks (${tasks.count { it.isCompleted }}/${tasks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Open Planner →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToScreen(AppScreen.PLANNER) }
                )
            }
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No study tasks scheduled for ${data.subject}. Tap 'Improve Readiness' to add one!",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTask(task) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (task.isCompleted) GreenSuccess else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Due: ${task.dueDate} • ${task.estimatedMinutes}m • ${task.priority} Priority",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IndigoPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "+${task.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = IndigoPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Revision Flashcards for this Subject
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Revision Flashcards (${flashcards.count { it.isMastered }}/${flashcards.size} Mastered)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Open Revision →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToScreen(AppScreen.REVISION) }
                )
            }
        }

        if (flashcards.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No flashcards created for ${data.subject}. Use 'Improve Readiness' to add practice cards!",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(flashcards, key = { it.id }) { card ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (card.isMastered) GreenSuccess.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (card.isMastered) "Mastered ✓" else "Needs Practice",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (card.isMastered) GreenSuccess else AmberStreak,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reviewed: ${card.reviewCount} times",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                            Button(
                                onClick = { onMarkFlashcard(card, !card.isMastered) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (card.isMastered) MaterialTheme.colorScheme.surfaceVariant else IndigoPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (card.isMastered) "Mark Needs Practice" else "Mark Mastered (+15 XP)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (card.isMastered) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. METRIC TILE COMPONENT
// -----------------------------------------------------------------------------

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 6. WEAK AREA ITEM CARD
// -----------------------------------------------------------------------------

@Composable
private fun WeakAreaItemCard(
    area: WeakAreaTopic,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (area.isResolved)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("weak_area_${area.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (area.isResolved) GreenSuccess else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (area.isResolved) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = area.topicName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (area.isResolved)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = area.subject,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = getSubjectColor(area.subject),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${area.priority} Priority",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (area.priority == "High") Color(0xFFDC2626) else AmberStreak,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (area.isResolved) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GreenSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Conquered! +20 XP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 7. IMPROVE READINESS BOTTOM SHEET (ACTION HUB)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImproveReadinessBottomSheet(
    data: SubjectReadinessData,
    tasks: List<StudyTask>,
    flashcards: List<FlashcardItem>,
    weakAreas: List<WeakAreaTopic>,
    exams: List<ExamItem>,
    onDismiss: () -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onMarkFlashcard: (FlashcardItem, Boolean) -> Unit,
    onToggleWeakArea: (WeakAreaTopic) -> Unit,
    onUpdateExamProgress: (ExamItem, Int, Int) -> Unit,
    onAddWeakArea: (String, String, String) -> Unit,
    onAddTask: (String, String, String, Int, String) -> Unit,
    onAddFlashcard: (String, String, String, String) -> Unit,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pendingTasks = tasks.filter { !it.isCompleted }
    val unmasteredCards = flashcards.filter { !it.isMastered }
    val unresolvedWeakAreas = weakAreas.filter { !it.isResolved }
    val currentExam = exams.firstOrNull()

    var showQuickTaskInput by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var showQuickWeakInput by remember { mutableStateOf(false) }
    var newWeakTopicName by remember { mutableStateOf("") }
    var showQuickCardInput by remember { mutableStateOf(false) }
    var newCardQuestion by remember { mutableStateOf("") }
    var newCardAnswer by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("improve_readiness_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Boost ${data.subject} Readiness",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Current: ${data.readinessPercentage}% • ${data.level.label}",
                            style = MaterialTheme.typography.bodySmall.copy(color = data.level.primaryColor, fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Complete any of these actions to instantly boost your readiness score and earn XP:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            // Action 1: Revise Weak Topics
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null, tint = AmberStreak, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1. Revise Weak Topics (+20 XP)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "${unresolvedWeakAreas.size} need work",
                                style = MaterialTheme.typography.labelSmall.copy(color = AmberStreak, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (unresolvedWeakAreas.isEmpty()) {
                            Text(
                                text = "🎉 All recorded weak topics are conquered!",
                                style = MaterialTheme.typography.bodySmall.copy(color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            unresolvedWeakAreas.take(3).forEach { area ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${area.topicName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onToggleWeakArea(area) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Conquer (+20 XP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (showQuickWeakInput) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newWeakTopicName,
                                    onValueChange = { newWeakTopicName = it },
                                    placeholder = { Text("Topic name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newWeakTopicName.isNotBlank()) {
                                            onAddWeakArea(data.subject, newWeakTopicName.trim(), "High")
                                            newWeakTopicName = ""
                                            showQuickWeakInput = false
                                        }
                                    },
                                    enabled = newWeakTopicName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                ) {
                                    Text("Add")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+ Add a topic you struggle with",
                                style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .clickable { showQuickWeakInput = true }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action 2: Complete Unfinished Study Tasks
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. Complete Unfinished Tasks",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "${pendingTasks.size} pending",
                                style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (pendingTasks.isEmpty()) {
                            Text(
                                text = "No pending tasks! Schedule a new task to boost readiness.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else {
                            pendingTasks.take(3).forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${task.title}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onToggleTask(task) },
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Mark Done (+${task.xpReward} XP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (showQuickTaskInput) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTaskTitle,
                                    onValueChange = { newTaskTitle = it },
                                    placeholder = { Text("Task title (e.g. Practice Chapter 4)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newTaskTitle.isNotBlank()) {
                                            onAddTask(newTaskTitle.trim(), data.subject, "Today", 30, "High")
                                            newTaskTitle = ""
                                            showQuickTaskInput = false
                                        }
                                    },
                                    enabled = newTaskTitle.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                ) {
                                    Text("Add")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+ Schedule a new study task for ${data.subject}",
                                style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .clickable { showQuickTaskInput = true }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action 3: Practice & Master Flashcards
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Psychology, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3. Practice Revision Cards (+15 XP)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "${unmasteredCards.size} need practice",
                                style = MaterialTheme.typography.labelSmall.copy(color = AmberStreak, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (unmasteredCards.isEmpty()) {
                            Text(
                                text = "All flashcards for ${data.subject} are mastered! ✨",
                                style = MaterialTheme.typography.bodySmall.copy(color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            unmasteredCards.take(2).forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Q: ${card.question}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Button(
                                        onClick = { onMarkFlashcard(card, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Master (+15 XP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (showQuickCardInput) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = newCardQuestion,
                                    onValueChange = { newCardQuestion = it },
                                    placeholder = { Text("Question") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCardAnswer,
                                    onValueChange = { newCardAnswer = it },
                                    placeholder = { Text("Answer") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            if (newCardQuestion.isNotBlank() && newCardAnswer.isNotBlank()) {
                                                onAddFlashcard(data.subject, newCardQuestion.trim(), newCardAnswer.trim(), "")
                                                newCardQuestion = ""
                                                newCardAnswer = ""
                                                showQuickCardInput = false
                                            }
                                        },
                                        enabled = newCardQuestion.isNotBlank() && newCardAnswer.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                    ) {
                                        Text("Save Card")
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Add new flashcard",
                                    style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .clickable { showQuickCardInput = true }
                                        .padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "Practice in Revision Hub →",
                                    style = MaterialTheme.typography.labelSmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .clickable { onNavigateToScreen(AppScreen.REVISION) }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action 4: Update Syllabus Progress
            if (currentExam != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "4. Syllabus Progress",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "${currentExam.syllabusCoveredTopics}/${currentExam.syllabusTotalTopics} topics",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val newCovered = (currentExam.syllabusCoveredTopics + 1).coerceAtMost(currentExam.syllabusTotalTopics)
                                        onUpdateExamProgress(currentExam, newCovered, currentExam.confidenceLevel)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+1 Topic Completed", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val newCovered = (currentExam.syllabusCoveredTopics - 1).coerceAtLeast(0)
                                        onUpdateExamProgress(currentExam, newCovered, currentExam.confidenceLevel)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("-1 Topic", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 8. EXISTING DIALOGS PRESERVED
// -----------------------------------------------------------------------------

@Composable
fun EditExamProgressDialog(
    exam: ExamItem,
    onDismiss: () -> Unit,
    onSave: (coveredTopics: Int, confidence: Int) -> Unit
) {
    var covered by remember { mutableFloatStateOf(exam.syllabusCoveredTopics.toFloat()) }
    var confidence by remember { mutableIntStateOf(exam.confidenceLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update ${exam.subject} Progress",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Topics Covered: ${covered.toInt()} / ${exam.syllabusTotalTopics}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = covered,
                    onValueChange = { covered = it },
                    valueRange = 0f..exam.syllabusTotalTopics.toFloat(),
                    steps = (exam.syllabusTotalTopics - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoPrimary,
                        activeTrackColor = IndigoPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Confidence Level: $confidence / 5",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { confidence = i }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$i stars",
                                tint = if (i <= confidence) AmberStreak else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(covered.toInt(), confidence) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Save Progress")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddExamDialog(
    onDismiss: () -> Unit,
    onConfirm: (subject: String, name: String, date: String, daysLeft: Int, totalTopics: Int, coveredTopics: Int, confidence: Int, target: Int) -> Unit
) {
    var subject by remember { mutableStateOf("Physics") }
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("Oct 15, 2026") }
    var daysLeft by remember { mutableIntStateOf(30) }
    var totalTopics by remember { mutableIntStateOf(10) }
    var coveredTopics by remember { mutableIntStateOf(5) }
    var confidence by remember { mutableIntStateOf(4) }
    var target by remember { mutableIntStateOf(90) }

    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "Literature", "History")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule New Exam",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Subject", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { item ->
                        FilterChip(
                            selected = subject == item,
                            onClick = { subject = item },
                            label = { Text(item) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exam Name (e.g. Midterm, Final)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = daysLeft.toString(),
                        onValueChange = { daysLeft = it.toIntOrNull() ?: daysLeft },
                        label = { Text("Days Left") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalTopics.toString(),
                        onValueChange = { totalTopics = it.toIntOrNull() ?: totalTopics },
                        label = { Text("Total Topics") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coveredTopics.toString(),
                        onValueChange = { coveredTopics = it.toIntOrNull() ?: coveredTopics },
                        label = { Text("Covered") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(subject, name.trim(), date.trim(), daysLeft, totalTopics, coveredTopics, confidence, target)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddWeakAreaDialog(
    initialSubject: String = "Mathematics",
    onDismiss: () -> Unit,
    onConfirm: (subject: String, topic: String, priority: String) -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject) }
    var topic by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("High") }

    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "Literature")
    val priorities = listOf("High", "Medium", "Low")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Focus Area",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text("Subject", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { item ->
                        FilterChip(
                            selected = subject == item,
                            onClick = { subject = item },
                            label = { Text(item) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Chapter you struggle with") },
                    placeholder = { Text("e.g. Integration by parts, Organic mechanisms") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Priority", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { item ->
                        FilterChip(
                            selected = priority == item,
                            onClick = { priority = item },
                            label = { Text(item) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topic.isNotBlank()) {
                        onConfirm(subject, topic.trim(), priority)
                    }
                },
                enabled = topic.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Add Topic")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
