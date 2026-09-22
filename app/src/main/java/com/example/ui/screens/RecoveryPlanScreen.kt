package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import java.util.Locale
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveRecoveryPlan
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.util.FallingBehindDetector
import com.example.util.RecoveryPlan
import com.example.util.StudyPaceStatus
import com.example.util.StudyStatusAssessment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryPlanScreen(
    tasks: List<StudyTask>,
    exams: List<ExamItem>,
    flashcards: List<FlashcardItem>,
    assessment: StudyStatusAssessment,
    activePlan: ActiveRecoveryPlan?,
    onApplyPlan: (RecoveryPlan) -> Unit,
    onCancelActivePlan: () -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onNavigateBack: () -> Unit
) {
    var dailyPaceMinutes by remember { mutableIntStateOf(90) }
    var daysAvailable by remember { mutableIntStateOf(3) }
    var showAdjustPanel by remember { mutableStateOf(false) }

    // Dynamically regenerate preview plan whenever user adjusts pace or days
    val previewPlan = remember(tasks, exams, dailyPaceMinutes, daysAvailable) {
        FallingBehindDetector.generateRecoveryPlan(
            tasks = tasks,
            exams = exams,
            dailyPaceMinutes = dailyPaceMinutes,
            daysAvailable = daysAvailable
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fix My Plan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("recovery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Case 1: An active recovery plan is currently running
            if (activePlan != null && activePlan.isActive) {
                item {
                    ActiveRecoveryProgressSection(
                        activePlan = activePlan,
                        tasks = tasks,
                        onToggleTask = onToggleTask,
                        onCancelActivePlan = onCancelActivePlan,
                        onReplan = { showAdjustPanel = true }
                    )
                }
            }

            // Status & Diagnostic Banner
            item {
                DiagnosticBanner(assessment = assessment)
            }

            // Case 2: Student has no overdue tasks, no pending tasks to recover, and no active plan
            if (previewPlan.allPlannedTasks.isEmpty() && assessment.overdueTasks.isEmpty() && (activePlan == null || !activePlan.isActive)) {
                item {
                    OnTrackNoPlanNeededCard(onBack = onNavigateBack)
                }
            } else {
                // Situation Overview (Overdue, Exams, Study time)
                item {
                    RecoveryOverviewCard(
                        assessment = assessment,
                        previewPlan = previewPlan
                    )
                }

                // Adjust Controls Card (Collapsible or visible)
                item {
                    AdjustPaceCard(
                        dailyPaceMinutes = dailyPaceMinutes,
                        daysAvailable = daysAvailable,
                        isOpen = showAdjustPanel,
                        onToggleOpen = { showAdjustPanel = !showAdjustPanel },
                        onPaceChanged = { dailyPaceMinutes = it },
                        onDaysChanged = { daysAvailable = it }
                    )
                }

                // Plan Preview Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Recovery Plan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${previewPlan.totalTasksCount} tasks balanced over ${previewPlan.daysPlanned} days",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IndigoPrimary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${previewPlan.totalStudyMinutes} min total",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Day-by-Day Recovery Schedule Cards
                items(previewPlan.days) { dayGroup ->
                    DailyScheduleCard(dayGroup = dayGroup)
                }

                // Action Buttons: Apply Plan, Adjust, Cancel
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onApplyPlan(previewPlan)
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_recovery_plan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apply Recovery Plan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAdjustPanel = !showAdjustPanel },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("adjust_recovery_plan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showAdjustPanel) "Hide Options" else "Adjust",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateBack,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cancel_recovery_plan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticBanner(assessment: StudyStatusAssessment) {
    val statusColor = when (assessment.status) {
        StudyPaceStatus.ON_TRACK -> GreenSuccess
        StudyPaceStatus.SLIGHTLY_BEHIND -> AmberStreak
        StudyPaceStatus.FALLING_BEHIND -> Color(0xFFEF4444)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recovery_diagnostic_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (assessment.status == StudyPaceStatus.FALLING_BEHIND)
                            Icons.Filled.WarningAmber
                        else if (assessment.status == StudyPaceStatus.SLIGHTLY_BEHIND)
                            Icons.Filled.Info
                        else
                            Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = assessment.status.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assessment.explanation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun RecoveryOverviewCard(
    assessment: StudyStatusAssessment,
    previewPlan: RecoveryPlan
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recovery Insights",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Overdue tasks
                Column {
                    Text(
                        text = "Overdue Tasks",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${assessment.overdueTasks.size} tasks",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (assessment.overdueTasks.isNotEmpty()) Color(0xFFEF4444) else GreenSuccess
                        )
                    )
                }

                // Approaching Exam
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Upcoming Exam",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = if (assessment.approachingExam != null && assessment.examDaysLeft != null)
                            "${assessment.approachingExam.subject} (${assessment.examDaysLeft}d)"
                        else "None (<14d)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    )
                }

                // Recommended Priority
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Recommended Pace",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${previewPlan.dailyPaceMinutes} min/day",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberStreak
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustPaceCard(
    dailyPaceMinutes: Int,
    daysAvailable: Int,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    onPaceChanged: (Int) -> Unit,
    onDaysChanged: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recovery_adjust_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Adjust Options",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = if (isOpen) "Collapse" else "Customize",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }

            AnimatedVisibility(visible = isOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Study Pace
                    Text(
                        text = "Daily Study Time:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = dailyPaceMinutes == 45,
                            onClick = { onPaceChanged(45) },
                            label = { Text("Less time (45m)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("pace_light_chip")
                        )
                        FilterChip(
                            selected = dailyPaceMinutes == 90,
                            onClick = { onPaceChanged(90) },
                            label = { Text("Balanced (90m)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("pace_balanced_chip")
                        )
                        FilterChip(
                            selected = dailyPaceMinutes == 150,
                            onClick = { onPaceChanged(150) },
                            label = { Text("More time (2.5h)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("pace_intensive_chip")
                        )
                    }

                    // Number of days available
                    Text(
                        text = "Spread Across Days:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 5, 7).forEach { days ->
                            FilterChip(
                                selected = daysAvailable == days,
                                onClick = { onDaysChanged(days) },
                                label = { Text("$days Days") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("days_${days}_chip")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyScheduleCard(dayGroup: com.example.util.DailyRecoveryGroup) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_schedule_card_${dayGroup.dayIndex}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Day Header: e.g. "Today" or "Tomorrow" + Total minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = (if (dayGroup.dayIndex == 0) AmberStreak else IndigoPrimary).copy(alpha = 0.15f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${dayGroup.dayIndex + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayGroup.dayIndex == 0) AmberStreak else IndigoPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = dayGroup.dayLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (dayGroup.dayLabel != dayGroup.dateFormatted) {
                            Text(
                                text = dayGroup.dateFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Total: ${dayGroup.totalMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tasks List for this day
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayGroup.tasks.forEach { plannedTask ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_task_card_${plannedTask.task.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Top row: Subject badge + Priority pill + Study duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Subject
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = plannedTask.task.subject,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Priority
                                    val priorityColor = when (plannedTask.task.priority.lowercase(Locale.ROOT)) {
                                        "high" -> Color(0xFFEF4444)
                                        "medium" -> AmberStreak
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = priorityColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${plannedTask.task.priority} Priority",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = priorityColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Study duration
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${plannedTask.task.estimatedMinutes} min",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            // Task Name (Title)
                            Text(
                                text = plannedTask.task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Date comparison row: Original Due Date -> New Suggested Date
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Original Due Date
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Original: ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Text(
                                            text = plannedTask.task.dueDate,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (FallingBehindDetector.isTaskOverdue(plannedTask.task))
                                                    Color(0xFFEF4444)
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )

                                    // New Suggested Date
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Suggested: ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Text(
                                            text = plannedTask.scheduledDayLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = IndigoPrimary
                                            )
                                        )
                                    }
                                }
                            }

                            // Priority reason
                            if (plannedTask.priorityReason.isNotBlank()) {
                                Text(
                                    text = plannedTask.priorityReason,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (plannedTask.priorityReason.contains("Overdue"))
                                            Color(0xFFEF4444)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
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

@Composable
private fun ActiveRecoveryProgressSection(
    activePlan: ActiveRecoveryPlan,
    tasks: List<StudyTask>,
    onToggleTask: (StudyTask) -> Unit,
    onCancelActivePlan: () -> Unit,
    onReplan: () -> Unit
) {
    val total = if (activePlan.totalTasks > 0) activePlan.totalTasks else activePlan.taskIds.size
    val planTasks = tasks.filter { it.id in activePlan.taskIds }
    val completedCount = planTasks.count { it.isCompleted }
    val isComplete = total > 0 && completedCount >= total
    val progressFloat = if (total > 0) (completedCount.toFloat() / total).coerceIn(0f, 1f) else 1f

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = (if (isComplete) GreenSuccess else IndigoPrimary).copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            1.dp,
            (if (isComplete) GreenSuccess else IndigoPrimary).copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_recovery_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isComplete) Icons.Filled.CheckCircle else Icons.Filled.TrackChanges,
                        contentDescription = null,
                        tint = if (isComplete) GreenSuccess else IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isComplete) "Recovery Goal Achieved!" else "Recovery plan active",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) GreenSuccess else IndigoPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (isComplete) GreenSuccess else IndigoPrimary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$completedCount/$total Done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) GreenSuccess else IndigoPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) GreenSuccess else IndigoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tasks Recovered",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "$completedCount of $total completed",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Days Planned",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${activePlan.daysPlanned} days",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Study Time",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${activePlan.totalStudyMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Recovery Tasks:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Task list with checkboxes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                planTasks.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleTask(task) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
                                    contentDescription = "Toggle Complete",
                                    tint = if (task.isCompleted) GreenSuccess else MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${task.subject}: ${task.title}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Scheduled: ${task.dueDate} • ${task.estimatedMinutes}m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancelActivePlan,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear Recovery Plan", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun OnTrackNoPlanNeededCard(onBack: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = GreenSuccess.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("on_track_no_plan_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = GreenSuccess,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All Caught Up!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GreenSuccess
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "All study tasks are completed and you're fully on track with your study schedule. A recovery plan is not needed right now.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Study Planner", fontWeight = FontWeight.Bold)
            }
        }
    }
}
