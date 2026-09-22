package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentProfile
import com.example.data.model.StudyTask
import com.example.ui.screens.dialogs.AddTaskDialog
import com.example.ui.screens.dialogs.MarkStudiedTopicDialog
import com.example.ui.screens.dialogs.VoiceAddTaskDialog
import com.example.ui.components.StudyStatusCard
import com.example.util.StudyStatusAssessment
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryDark
import com.example.util.DeviceTimeService

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlannerScreen(
    tasks: List<StudyTask>,
    profile: StudentProfile? = null,
    onToggleTask: (StudyTask) -> Unit,
    onAddTask: (
        title: String,
        subject: String,
        dueDate: String,
        minutes: Int,
        priority: String,
        reminderEnabled: Boolean,
        reminderEpochMillis: Long?,
        reminderDateFormatted: String,
        reminderTimeFormatted: String,
        reminderRepeat: String,
        xpReward: Int
    ) -> Unit,
    onEditTask: (StudyTask) -> Unit = {},
    onDeleteTask: (StudyTask) -> Unit,
    onNavigateToReminders: () -> Unit = {},
    studyStatusAssessment: StudyStatusAssessment? = null,
    onNavigateToRecoveryPlan: () -> Unit = {},
    onMarkTopicStudied: (subject: String, topic: String, durationMinutes: Int) -> Unit = { _, _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }
    var topicToMarkStudied by remember { mutableStateOf<StudyTask?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Today, 1: Upcoming, 2: All Tasks, 3: Completed
    var selectedSubject by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("Today", "Upcoming", "All Tasks", "Completed")

    val subjects = remember(tasks) {
        listOf("All") + tasks.map { it.subject }.distinct()
    }

    val filteredTasks = remember(tasks, selectedTab, selectedSubject, searchQuery) {
        tasks.filter { task ->
            val isDueToday = task.dueDate.equals("Today", ignoreCase = true) ||
                (task.reminderEpochMillis != null && DeviceTimeService.isTodayEpoch(task.reminderEpochMillis))
            val matchesTab = when (selectedTab) {
                0 -> isDueToday && !task.isCompleted
                1 -> !isDueToday && !task.isCompleted
                2 -> true // All Tasks: displays all tasks (both active and completed)
                3 -> task.isCompleted
                else -> true
            }
            val matchesSubject = selectedSubject == "All" || task.subject.equals(selectedSubject, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.subject.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesSubject && matchesSearch
        }
    }

    val totalTasksCount = remember(tasks) { tasks.size }
    val completedCount = remember(tasks) { tasks.count { it.isCompleted } }
    val totalTimePlanned = remember(tasks) {
        tasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }
    }
    val activeRemindersCount = remember(tasks) { tasks.count { it.reminderEnabled && !it.isCompleted } }

    val todayTasks = remember(tasks) {
        tasks.filter { task ->
            task.dueDate.equals("Today", ignoreCase = true) ||
            (task.reminderEpochMillis != null && DeviceTimeService.isTodayEpoch(task.reminderEpochMillis))
        }
    }
    val todayCompletedCount = remember(todayTasks) { todayTasks.count { it.isCompleted } }
    val todayProgressPercent = remember(todayTasks, todayCompletedCount, totalTasksCount, completedCount) {
        if (todayTasks.isNotEmpty()) {
            (todayCompletedCount * 100) / todayTasks.size
        } else if (totalTasksCount > 0) {
            (completedCount * 100) / totalTasksCount
        } else {
            0
        }
    }
    val totalUserXp = remember(profile, tasks) {
        profile?.xpPoints ?: tasks.filter { it.isCompleted }.sumOf { it.xpReward }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showVoiceAddDialog = true },
                    containerColor = AmberStreak,
                    contentColor = Color.White,
                    shape = CircleShape,
                    icon = {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voice Add Task")
                    },
                    text = {
                        Text(
                            text = "Voice Add Task",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("planner_voice_add_fab")
                )
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("planner_fab_add")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats summary card (Total Tasks, Completed Tasks, Today's Progress, Time Needed, XP)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("planner_stat_total_tasks")
                        ) {
                            Text(
                                text = "$totalTasksCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            )
                            Text(
                                text = "Total Tasks",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("planner_stat_completed_tasks")
                        ) {
                            Text(
                                text = "$completedCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                )
                            )
                            Text(
                                text = "Completed Tasks",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("planner_stat_time_needed")
                        ) {
                            Text(
                                text = "${totalTimePlanned}m",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberStreak
                                )
                            )
                            Text(
                                text = "Time Needed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Today's Progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("planner_stat_today_progress")
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Today's Progress",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "$todayProgressPercent%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (todayProgressPercent == 100) GreenSuccess else IndigoPrimary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { todayProgressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (todayProgressPercent == 100) GreenSuccess else IndigoPrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // XP Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberStreak.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AmberStreak.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("planner_stat_xp")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "XP",
                                    tint = AmberStreak,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalUserXp XP",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberStreak
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Study Status / Recovery Plan Card
            if (studyStatusAssessment != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    StudyStatusCard(
                        assessment = studyStatusAssessment,
                        onOpenRecoveryPlan = onNavigateToRecoveryPlan
                    )
                }
            }

            // Smart Reminders Navigation Banner
            Card(
                onClick = onNavigateToReminders,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IndigoPrimary.copy(alpha = 0.10f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("planner_to_reminders_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Reminders",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            )
                            Text(
                                text = if (activeRemindersCount > 0)
                                    "$activeRemindersCount active reminder${if (activeRemindersCount > 1) "s" else ""} scheduled"
                                else
                                    "Schedule alerts so you never miss study sessions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Smart Reminders",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Voice Add Task Quick Action Card
            Card(
                onClick = { showVoiceAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IndigoPrimary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("planner_voice_add_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AmberStreak,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice Add Task",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Voice Add Task",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            )
                            Text(
                                text = "Say: \"Tomorrow at 7 AM revise Physics...\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showVoiceAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("planner_voice_banner_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search task or subject...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showVoiceAddDialog = true },
                        modifier = Modifier.testTag("planner_search_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Add Task",
                            tint = AmberStreak
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("planner_search_input")
            )

            // Tabs: Today, Upcoming, All, Completed
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            // Subject Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjects) { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        label = { Text(subject) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Tasks List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tasks found in this view",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' to add a new study goal or session",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        PlannerTaskCard(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { onDeleteTask(task) },
                            onStudied = { topicToMarkStudied = task }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (topicToMarkStudied != null) {
        val currentTask = topicToMarkStudied!!
        MarkStudiedTopicDialog(
            initialSubject = currentTask.subject,
            initialTopic = currentTask.title,
            initialDuration = currentTask.estimatedMinutes,
            onDismiss = { topicToMarkStudied = null },
            onConfirmStudied = { subject, topic, duration ->
                onMarkTopicStudied(subject, topic, duration)
                topicToMarkStudied = null
            }
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            taskToEdit = null,
            initialDueDate = when (selectedTab) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> "Today"
            },
            onDismiss = { showAddDialog = false },
            onConfirmTask = { title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward ->
                onAddTask(title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward)
                showAddDialog = false
            }
        )
    }

    if (showVoiceAddDialog) {
        VoiceAddTaskDialog(
            onDismiss = { showVoiceAddDialog = false },
            onSaveTask = { title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward ->
                onAddTask(title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward)
                showVoiceAddDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        val currentEdit = taskToEdit!!
        AddTaskDialog(
            taskToEdit = currentEdit,
            onDismiss = { taskToEdit = null },
            onConfirmTask = { title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward ->
                onEditTask(
                    currentEdit.copy(
                        title = title,
                        subject = subject,
                        dueDate = dueDate,
                        estimatedMinutes = mins,
                        priority = priority,
                        xpReward = xpReward,
                        reminderEnabled = remEnabled,
                        reminderEpochMillis = if (remEnabled) remMillis else null,
                        reminderDateFormatted = if (remEnabled) remDateStr else "",
                        reminderTimeFormatted = remTimeStr,
                        reminderRepeat = remRepeat
                    )
                )
                taskToEdit = null
            }
        )
    }
}

// Backward-compatible overload with 10 parameters
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlannerScreen(
    tasks: List<StudyTask>,
    onToggleTask: (StudyTask) -> Unit,
    onAddTask: (
        title: String,
        subject: String,
        dueDate: String,
        minutes: Int,
        priority: String,
        reminderEnabled: Boolean,
        reminderEpochMillis: Long?,
        reminderDateFormatted: String,
        reminderTimeFormatted: String,
        reminderRepeat: String
    ) -> Unit,
    onEditTask: (StudyTask) -> Unit = {},
    onDeleteTask: (StudyTask) -> Unit,
    onNavigateToReminders: () -> Unit = {}
) {
    PlannerScreen(
        tasks = tasks,
        profile = null,
        onToggleTask = onToggleTask,
        onAddTask = { title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, _ ->
            onAddTask(title, subject, dueDate, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat)
        },
        onEditTask = onEditTask,
        onDeleteTask = onDeleteTask,
        onNavigateToReminders = onNavigateToReminders
    )
}

// Overload for backward compatibility
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlannerScreen(
    tasks: List<StudyTask>,
    onToggleTask: (StudyTask) -> Unit,
    onAddTask: (String, String, String, Int, String) -> Unit,
    onDeleteTask: (StudyTask) -> Unit
) {
    PlannerScreen(
        tasks = tasks,
        profile = null,
        onToggleTask = onToggleTask,
        onAddTask = { title, subject, dueDate, mins, priority, _, _, _, _, _, _ ->
            onAddTask(title, subject, dueDate, mins, priority)
        },
        onEditTask = {},
        onDeleteTask = onDeleteTask
    )
}

@Composable
fun PlannerTaskCard(
    task: StudyTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onStudied: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("planner_task_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (tap to complete or mark incomplete again)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) GreenSuccess else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggle() }
                    .testTag("planner_task_toggle_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getSubjectColor(task.subject).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = getSubjectColor(task.subject),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = task.dueDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.priority) {
                            "High" -> Color(0xFFFEE2E2)
                            "Medium" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFF1F5F9)
                        }
                    ) {
                        Text(
                            text = task.priority,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (task.priority) {
                                    "High" -> Color(0xFFB91C1C)
                                    "Medium" -> Color(0xFFB45309)
                                    else -> Color(0xFF475569)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${task.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )

                    // Start Time / Scheduled Reminder Badge
                    if (task.reminderTimeFormatted.isNotBlank()) {
                        val isReminderActive = task.reminderEnabled && !task.isCompleted
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isReminderActive) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReminderActive) Icons.Filled.Alarm else Icons.Filled.Schedule,
                                    contentDescription = if (isReminderActive) "Reminder Scheduled" else "Start Time",
                                    tint = if (isReminderActive) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = task.reminderTimeFormatted,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isReminderActive) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IndigoPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clickable { onStudied() }
                        .testTag("planner_task_studied_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "I Studied This",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberStreak.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "+${task.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberStreak,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("planner_edit_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("planner_delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
