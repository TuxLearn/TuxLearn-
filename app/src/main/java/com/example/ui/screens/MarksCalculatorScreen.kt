package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InitialData
import com.example.data.model.SubjectMark
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary

/**
 * Grade helper according to standard grading requirements:
 * 90–100 = A+
 * 80–89 = A
 * 70–79 = B+
 * 60–69 = B
 * 50–59 = C
 * 40–49 = D
 * Below 40 = F
 */
fun getGradeForPercentage(percentage: Double): String {
    return when {
        percentage >= 90.0 -> "A+"
        percentage >= 80.0 -> "A"
        percentage >= 70.0 -> "B+"
        percentage >= 60.0 -> "B"
        percentage >= 50.0 -> "C"
        percentage >= 40.0 -> "D"
        else -> "F"
    }
}

fun getGradeColor(grade: String): Color {
    return when (grade) {
        "A+" -> Color(0xFF10B981) // Emerald
        "A" -> Color(0xFF06B6D4) // Cyan
        "B+" -> Color(0xFF3B82F6) // Blue
        "B" -> Color(0xFF6366F1) // Indigo
        "C" -> Color(0xFFF59E0B) // Amber
        "D" -> Color(0xFFF97316) // Orange
        else -> Color(0xFFEF4444) // Red
    }
}

fun formatMarksNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}

data class CourseGrade(
    var name: String,
    var credits: String,
    var score: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksCalculatorScreen(
    subjectMarks: List<SubjectMark> = emptyList(),
    onAddSubject: (name: String, max: Double, obtained: Double) -> Unit = { _, _, _ -> },
    onUpdateSubject: (SubjectMark) -> Unit = {},
    onDeleteSubject: (SubjectMark) -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Marks & Percentage, 1: Target Exam Score, 2: GPA Calc

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marks_calculator_screen")
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("tab_marks_calc"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Marks & %")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("tab_target_score"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Target Goal")
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                modifier = Modifier.testTag("tab_gpa_calc"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Grade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPA / CGPA")
                    }
                }
            )
        }

        when (selectedTab) {
            0 -> MarksPercentageCalculatorTab(
                subjectMarks = subjectMarks,
                onAddSubject = onAddSubject,
                onUpdateSubject = onUpdateSubject,
                onDeleteSubject = onDeleteSubject,
                onClearAll = onClearAll
            )
            1 -> TargetExamScoreCalculator()
            2 -> GpaCalculator()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarksPercentageCalculatorTab(
    subjectMarks: List<SubjectMark>,
    onAddSubject: (name: String, max: Double, obtained: Double) -> Unit,
    onUpdateSubject: (SubjectMark) -> Unit,
    onDeleteSubject: (SubjectMark) -> Unit,
    onClearAll: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectMark?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Computations
    val totalMax = subjectMarks.sumOf { it.maxMarks }
    val totalObtained = subjectMarks.sumOf { it.obtainedMarks }
    val overallPercentage = if (totalMax > 0.0) (totalObtained / totalMax) * 100.0 else 0.0
    val overallGrade = if (subjectMarks.isNotEmpty()) getGradeForPercentage(overallPercentage) else "N/A"
    val isPass = overallPercentage >= 40.0 && subjectMarks.isNotEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Calculator Dashboard Hero Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_dashboard_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Row: Title + Pass/Fail Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Calculate,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALCULATOR DASHBOARD",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        if (subjectMarks.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPass) GreenSuccess.copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isPass) GreenSuccess.copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("result_pass_fail_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPass) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (isPass) GreenSuccess else Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isPass) "PASS" else "FAIL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPass) GreenSuccess else Color(0xFFEF4444)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (subjectMarks.isNotEmpty()) {
                        // Overall Percentage Display
                        Text(
                            text = "${"%.2f".format(overallPercentage)}%",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPass) IndigoPrimary else Color(0xFFEF4444)
                            ),
                            modifier = Modifier.testTag("overall_percentage_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Grade Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = getGradeColor(overallGrade).copy(alpha = 0.15f),
                            modifier = Modifier.testTag("overall_grade_pill")
                        ) {
                            Text(
                                text = "Grade $overallGrade",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = getGradeColor(overallGrade)
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (overallPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isPass) IndigoPrimary else Color(0xFFEF4444),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3 Quick Metrics: Marks Obtained, Total Marks, Result
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.testTag("stat_marks_obtained")
                            ) {
                                Text(
                                    text = formatMarksNumber(totalObtained),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Marks Obtained",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.testTag("stat_total_marks")
                            ) {
                                Text(
                                    text = formatMarksNumber(totalMax),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Total Marks",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.testTag("stat_result")
                            ) {
                                Text(
                                    text = if (isPass) "Pass" else "Fail",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPass) GreenSuccess else Color(0xFFEF4444)
                                    )
                                )
                                Text(
                                    text = "Status",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        // Empty Dashboard state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Marks Calculated",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add subjects below to calculate total marks, overall percentage, grade, and pass/fail result.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Grading Scale Reference Pill Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Formula: (Obtained / Max) × 100",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Pass: ≥40% • Fail: <40%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IndigoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // 3. Subject-wise Marks List Header with Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject Performance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${subjectMarks.size} subjects entered",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subjectMarks.isNotEmpty()) {
                        IconButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_reset_calculator")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset Calculator",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_add_subject")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Subject", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 4. Subjects List or Empty State
        if (subjectMarks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("empty_marks_view")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Subjects Added Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add your school or college subjects with maximum marks and obtained marks to calculate performance.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_create_first_subject")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Your First Subject")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                InitialData.sampleMarks.forEach { sample ->
                                    onAddSubject(sample.subjectName, sample.maxMarks, sample.obtainedMarks)
                                }
                            },
                            modifier = Modifier.testTag("btn_load_sample_subjects")
                        ) {
                            Text("Load Sample Subjects (Physics, Chem, Bio, Math)")
                        }
                    }
                }
            }
        } else {
            items(subjectMarks, key = { it.id }) { subject ->
                val subPercentage = if (subject.maxMarks > 0.0) {
                    (subject.obtainedMarks / subject.maxMarks) * 100.0
                } else 0.0
                val subGrade = getGradeForPercentage(subPercentage)
                val subPass = subPercentage >= 40.0

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_card_${subject.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject.subjectName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Marks: ${formatMarksNumber(subject.obtainedMarks)} / ${formatMarksNumber(subject.maxMarks)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Subject Percentage & Grade Badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${"%.1f".format(subPercentage)}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (subPass) IndigoPrimary else Color(0xFFEF4444)
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Grade $subGrade",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = getGradeColor(subGrade)
                                            )
                                        )
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Text(
                                            text = if (subPass) "Pass" else "Fail",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (subPass) GreenSuccess else Color(0xFFEF4444)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Edit Subject
                                IconButton(
                                    onClick = { editingSubject = subject },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_subject_${subject.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Subject",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Subject
                                IconButton(
                                    onClick = { onDeleteSubject(subject) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_subject_${subject.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete Subject",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Indicator
                        LinearProgressIndicator(
                            progress = { (subPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                subPercentage >= 80.0 -> GreenSuccess
                                subPercentage >= 60.0 -> IndigoPrimary
                                subPercentage >= 40.0 -> AmberStreak
                                else -> Color(0xFFEF4444)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Add Subject Dialog
    if (showAddDialog) {
        SubjectMarkFormDialog(
            title = "Add Subject Marks",
            confirmButtonLabel = "Add Subject",
            initialName = "",
            initialMax = "100",
            initialObtained = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, max, obtained ->
                onAddSubject(name, max, obtained)
                showAddDialog = false
            }
        )
    }

    // Edit Subject Dialog
    editingSubject?.let { subject ->
        SubjectMarkFormDialog(
            title = "Edit Subject Marks",
            confirmButtonLabel = "Save Changes",
            initialName = subject.subjectName,
            initialMax = formatMarksNumber(subject.maxMarks),
            initialObtained = formatMarksNumber(subject.obtainedMarks),
            onDismiss = { editingSubject = null },
            onConfirm = { name, max, obtained ->
                onUpdateSubject(
                    subject.copy(
                        subjectName = name,
                        maxMarks = max,
                        obtainedMarks = obtained
                    )
                )
                editingSubject = null
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reset Calculator?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all entered subject marks? All calculations and saved subjects will be removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable dialog for adding or editing a subject with validation:
 * - Marks obtained cannot be greater than maximum marks.
 * - Marks cannot be negative.
 * - Maximum marks must be greater than 0.
 * - Subject name cannot be empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectMarkFormDialog(
    title: String,
    confirmButtonLabel: String,
    initialName: String,
    initialMax: String,
    initialObtained: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, max: Double, obtained: Double) -> Unit
) {
    var subjectName by remember { mutableStateOf(initialName) }
    var maxMarksStr by remember { mutableStateOf(initialMax) }
    var obtainedMarksStr by remember { mutableStateOf(initialObtained) }

    val popularSubjects = listOf("Physics", "Chemistry", "Biology", "Mathematics", "Computer Science", "English", "Other")
    val maxPresets = listOf("100", "50", "75", "20")

    val maxMarks = maxMarksStr.toDoubleOrNull()
    val obtainedMarks = obtainedMarksStr.toDoubleOrNull()

    // Validation checks
    val isNameEmpty = subjectName.trim().isEmpty()
    val isMaxInvalid = maxMarks == null || maxMarks <= 0.0
    val isObtainedNegative = obtainedMarks != null && obtainedMarks < 0.0
    val isObtainedMissing = obtainedMarks == null
    val isObtainedExceedsMax = maxMarks != null && obtainedMarks != null && obtainedMarks > maxMarks

    val isValid = !isNameEmpty && !isMaxInvalid && !isObtainedMissing && !isObtainedNegative && !isObtainedExceedsMax

    // Preview percentage and grade
    val previewPercentage = if (maxMarks != null && obtainedMarks != null && maxMarks > 0.0 && obtainedMarks >= 0.0 && obtainedMarks <= maxMarks) {
        (obtainedMarks / maxMarks) * 100.0
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject Name Field
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g. Physics, Mathematics") },
                    singleLine = true,
                    isError = isNameEmpty && subjectName.isNotEmpty(),
                    supportingText = {
                        if (isNameEmpty && subjectName.isNotEmpty()) {
                            Text("Subject name cannot be empty", color = Color(0xFFEF4444))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_subject_name")
                )

                // Quick Subject Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    popularSubjects.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (subjectName.equals(preset, ignoreCase = true))
                                IndigoPrimary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { subjectName = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (subjectName.equals(preset, ignoreCase = true)) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Maximum Marks Field
                OutlinedTextField(
                    value = maxMarksStr,
                    onValueChange = { maxMarksStr = it },
                    label = { Text("Maximum Marks") },
                    placeholder = { Text("e.g. 100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isMaxInvalid && maxMarksStr.isNotEmpty(),
                    supportingText = {
                        if (isMaxInvalid && maxMarksStr.isNotEmpty()) {
                            Text("Maximum marks must be greater than 0", color = Color(0xFFEF4444))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_max_marks")
                )

                // Quick Max Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preset Max:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    maxPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (maxMarksStr == preset) IndigoPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { maxMarksStr = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (maxMarksStr == preset) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Marks Obtained Field
                OutlinedTextField(
                    value = obtainedMarksStr,
                    onValueChange = { obtainedMarksStr = it },
                    label = { Text("Marks Obtained") },
                    placeholder = { Text("e.g. 72") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = (isObtainedNegative || isObtainedExceedsMax) && obtainedMarksStr.isNotEmpty(),
                    supportingText = {
                        when {
                            isObtainedNegative && obtainedMarksStr.isNotEmpty() -> {
                                Text("Marks cannot be negative", color = Color(0xFFEF4444))
                            }
                            isObtainedExceedsMax -> {
                                Text("Marks obtained cannot be greater than maximum marks", color = Color(0xFFEF4444))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_obtained_marks")
                )

                // Live Preview Result inside Dialog
                if (previewPercentage != null) {
                    val previewGrade = getGradeForPercentage(previewPercentage)
                    val previewPass = previewPercentage >= 40.0

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_preview_result")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Preview Result",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "${"%.1f".format(previewPercentage)}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (previewPass) IndigoPrimary else Color(0xFFEF4444)
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = getGradeColor(previewGrade).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Grade $previewGrade",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = getGradeColor(previewGrade)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (previewPass) GreenSuccess.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (previewPass) "Pass" else "Fail",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (previewPass) GreenSuccess else Color(0xFFEF4444)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && maxMarks != null && obtainedMarks != null) {
                        onConfirm(subjectName.trim(), maxMarks, obtainedMarks)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_subject_button")
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("cancel_subject_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TargetExamScoreCalculator() {
    var currentMarks by remember { mutableStateOf("82") }
    var currentWeight by remember { mutableStateOf("40") }
    var targetGoal by remember { mutableStateOf("90") }

    val currentScoreVal = currentMarks.toDoubleOrNull() ?: 0.0
    val currentWeightVal = currentWeight.toDoubleOrNull() ?: 40.0
    val targetGoalVal = targetGoal.toDoubleOrNull() ?: 90.0

    val finalWeightVal = (100.0 - currentWeightVal).coerceAtLeast(1.0)
    val requiredFinalScore = ((targetGoalVal - (currentScoreVal * (currentWeightVal / 100.0))) / (finalWeightVal / 100.0))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (requiredFinalScore <= 100.0)
                        Color(0xFFEEF2FF)
                    else
                        Color(0xFFFEF2F2)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Required Final Exam Score",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${"%.1f".format(requiredFinalScore)}%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                requiredFinalScore <= 75.0 -> GreenSuccess
                                requiredFinalScore <= 90.0 -> IndigoPrimary
                                requiredFinalScore <= 100.0 -> AmberStreak
                                else -> Color(0xFFDC2626)
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            requiredFinalScore <= 60.0 -> "Very achievable! Keep up steady practice 👍"
                            requiredFinalScore <= 85.0 -> "Well within reach! Focus on core topics 🎯"
                            requiredFinalScore <= 100.0 -> "High effort required! Revise weak areas ⚡"
                            else -> "Target exceeds 100% — check if extra credit is available ⚠️"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Score Parameters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = currentMarks,
                        onValueChange = { currentMarks = it },
                        label = { Text("Current Internal / Coursework Score (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_current_score")
                    )

                    OutlinedTextField(
                        value = currentWeight,
                        onValueChange = { currentWeight = it },
                        label = { Text("Coursework Weightage (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_coursework_weight")
                    )

                    OutlinedTextField(
                        value = targetGoal,
                        onValueChange = { targetGoal = it },
                        label = { Text("Desired Target Grade (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_target_goal")
                    )

                    Text(
                        text = "Final Exam represents ${100 - currentWeightVal.toInt()}% of your total course grade.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun GpaCalculator() {
    val courses = remember {
        mutableStateListOf(
            CourseGrade("Calculus", "4", "92"),
            CourseGrade("Physics", "4", "88"),
            CourseGrade("Computer Science", "3", "95"),
            CourseGrade("Literature", "3", "84")
        )
    }

    val totalCredits = courses.sumOf { it.credits.toDoubleOrNull() ?: 0.0 }
    val weightedMarks = courses.sumOf {
        val cr = it.credits.toDoubleOrNull() ?: 0.0
        val sc = it.score.toDoubleOrNull() ?: 0.0
        cr * sc
    }
    val avgPercentage = if (totalCredits == 0.0) 0.0 else weightedMarks / totalCredits
    val gpaScale = if (totalCredits == 0.0) 0.0 else ((avgPercentage / 20.0) - 1.0).coerceIn(0.0, 4.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.2f".format(gpaScale),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = IndigoPrimary
                            )
                        )
                        Text(
                            text = "Estimated GPA (4.0)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f%%".format(avgPercentage),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenSuccess
                            )
                        )
                        Text(
                            text = "Weighted Average",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Courses & Grades",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { courses.add(CourseGrade("New Course", "3", "85")) },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Course")
                }
            }
        }

        itemsIndexed(courses) { index, course ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = course.name,
                        onValueChange = { courses[index] = course.copy(name = it) },
                        label = { Text("Course") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = course.credits,
                        onValueChange = { courses[index] = course.copy(credits = it) },
                        label = { Text("Credits") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = course.score,
                        onValueChange = { courses[index] = course.copy(score = it) },
                        label = { Text("Score %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    )

                    IconButton(
                        onClick = {
                            if (courses.size > 1) courses.removeAt(index)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
