package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.util.StudyPaceStatus
import com.example.util.StudyStatusAssessment

@Composable
fun StudyStatusCard(
    assessment: StudyStatusAssessment,
    onOpenRecoveryPlan: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val activePlan = assessment.activeRecoveryPlan

    // If recovery plan is active, show the "Recovery plan active" state
    if (activePlan != null && activePlan.isActive) {
        ActiveRecoveryPlanCard(
            assessment = assessment,
            onOpenRecoveryPlan = onOpenRecoveryPlan,
            modifier = modifier
        )
        return
    }

    // Standard Study Status (On Track, Slightly Behind, Falling Behind)
    val statusColor = when (assessment.status) {
        StudyPaceStatus.ON_TRACK -> GreenSuccess
        StudyPaceStatus.SLIGHTLY_BEHIND -> AmberStreak
        StudyPaceStatus.FALLING_BEHIND -> Color(0xFFEF4444)
    }

    val statusIcon = when (assessment.status) {
        StudyPaceStatus.ON_TRACK -> Icons.Filled.CheckCircle
        StudyPaceStatus.SLIGHTLY_BEHIND -> Icons.Filled.Info
        StudyPaceStatus.FALLING_BEHIND -> Icons.Filled.WarningAmber
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_status_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Badge & Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study Status",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.18f),
                    modifier = Modifier.testTag("study_status_badge")
                ) {
                    Text(
                        text = assessment.status.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation Text
            Text(
                text = assessment.explanation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("study_status_explanation")
            )

            // Overdue and Exam Quick Indicators (if behind)
            if (assessment.status != StudyPaceStatus.ON_TRACK) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (assessment.overdueTasks.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${assessment.overdueTasks.size} overdue",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            )
                        }
                    }

                    if (assessment.approachingExam != null && assessment.examDaysLeft != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${assessment.approachingExam.subject} in ${assessment.examDaysLeft}d",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = IndigoPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fix My Plan Button
                Button(
                    onClick = onOpenRecoveryPlan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (assessment.status == StudyPaceStatus.FALLING_BEHIND) Color(0xFFEF4444) else AmberStreak,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fix_my_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fix My Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRecoveryPlanCard(
    assessment: StudyStatusAssessment,
    onOpenRecoveryPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlan = assessment.activeRecoveryPlan ?: return
    val totalTasks = if (activePlan.totalTasks > 0) activePlan.totalTasks else activePlan.taskIds.size
    val completedCount = assessment.recoveredTasksCount
    val isComplete = totalTasks > 0 && completedCount >= totalTasks
    val progressFloat = if (totalTasks > 0) (completedCount.toFloat() / totalTasks).coerceIn(0f, 1f) else 1f

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) GreenSuccess.copy(alpha = 0.12f) else IndigoPrimary.copy(alpha = 0.10f)
        ),
        border = BorderStroke(
            1.dp,
            if (isComplete) GreenSuccess.copy(alpha = 0.35f) else IndigoPrimary.copy(alpha = 0.30f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenRecoveryPlan() }
            .testTag("active_recovery_plan_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isComplete) GreenSuccess else IndigoPrimary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isComplete) Icons.Filled.CheckCircle else Icons.Filled.TrackChanges,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isComplete) "Recovery Complete! 🎉" else "Recovery plan active",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) GreenSuccess else IndigoPrimary
                        ),
                        modifier = Modifier.testTag("recovery_plan_active_title")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (isComplete) GreenSuccess else IndigoPrimary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$completedCount/$totalTasks Done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) GreenSuccess else IndigoPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("recovery_progress_bar"),
                color = if (isComplete) GreenSuccess else IndigoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Row: Tasks recovered, Days planned, Total study time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tasks Recovered",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "$completedCount of $totalTasks",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Days Planned",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${activePlan.daysPlanned} days",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Study Time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${activePlan.totalStudyMinutes} mins",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Tap Link to view / adjust plan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View details",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = IndigoPrimary
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
