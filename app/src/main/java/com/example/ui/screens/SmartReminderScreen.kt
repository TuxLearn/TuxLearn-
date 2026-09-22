package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.ExamItem
import com.example.data.model.StudyTask
import com.example.notification.StudyReminderScheduler
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.util.DeviceTimeService
import com.example.util.LocalDateTimeParts
import com.example.util.SmartReminderTimeHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    MISSED,
    OFF
}

fun getReminderStatus(task: StudyTask): ReminderStatus {
    if (task.isCompleted) return ReminderStatus.COMPLETED
    if (!task.reminderEnabled) return ReminderStatus.OFF
    val epoch = task.reminderEpochMillis ?: return ReminderStatus.ACTIVE
    return if (epoch < System.currentTimeMillis()) {
        ReminderStatus.MISSED
    } else {
        ReminderStatus.ACTIVE
    }
}

val REMINDER_TYPES = listOf(
    "Study session",
    "Homework",
    "Revision",
    "Exam preparation"
)

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "homework" -> Icons.Filled.Book
        "revision" -> Icons.Filled.Psychology
        "exam preparation" -> Icons.Filled.School
        else -> Icons.Filled.HistoryEdu
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "homework" -> Color(0xFFE65100) // Deep Orange
        "revision" -> Color(0xFF7B1FA2) // Purple
        "exam preparation" -> Color(0xFFC2185B) // Pink / Red accent
        else -> IndigoPrimary
    }
}

fun formatTime12Hour(hour: Int, minute: Int): String {
    return SmartReminderTimeHelper.format12Hour(hour, minute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReminderScreen(
    tasks: List<StudyTask>,
    exams: List<ExamItem> = emptyList(),
    masterReminderEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onToggleReminder: (StudyTask) -> Unit,
    onSnoozeReminder: (StudyTask, Int) -> Unit,
    onRescheduleReminder: (StudyTask, Long, String, String, String) -> Unit,
    onUpdateReminder: (
        task: StudyTask,
        title: String,
        subject: String,
        dateStr: String,
        timeStr: String,
        epochMillis: Long,
        repeat: String,
        category: String,
        enabled: Boolean
    ) -> Unit,
    onMarkDone: (StudyTask) -> Unit,
    onDeleteReminder: (StudyTask) -> Unit,
    onAddReminder: (
        title: String,
        subject: String,
        date: String,
        time: String,
        epochMillis: Long,
        repeat: String,
        category: String
    ) -> Unit,
    onSendTestNotification: (StudyTask?) -> Unit,
    onScheduleTestNotificationIn10Seconds: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }
    var taskToReschedule by remember { mutableStateOf<StudyTask?>(null) }
    var initialPresetTitle by remember { mutableStateOf("") }
    var initialPresetSubject by remember { mutableStateOf("Mathematics") }
    var initialPresetCategory by remember { mutableStateOf("Study session") }

    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Active, 2: Missed, 3: Completed
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Check notification permission state for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var pendingTestAlertRequested by remember { mutableStateOf(false) }
    var pendingTestAlertTask by remember { mutableStateOf<StudyTask?>(null) }
    var pendingTest10sRequested by remember { mutableStateOf(false) }
    var test10sActiveUntilMillis by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
            if (granted) {
                if (pendingTest10sRequested) {
                    pendingTest10sRequested = false
                    test10sActiveUntilMillis = System.currentTimeMillis() + 10_000L
                    onScheduleTestNotificationIn10Seconds()
                } else if (pendingTestAlertRequested) {
                    pendingTestAlertRequested = false
                    val target = pendingTestAlertTask ?: tasks.firstOrNull { !it.isCompleted } ?: tasks.firstOrNull()
                    onSendTestNotification(target)
                }
            } else {
                pendingTest10sRequested = false
                pendingTestAlertRequested = false
                pendingTestAlertTask = null
                android.widget.Toast.makeText(context, "Notification permission is required to display alerts. Please grant permission.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    val triggerTestAlert: (StudyTask?) -> Unit = { targetTask ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingTestAlertRequested = true
            pendingTestAlertTask = targetTask
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val target = targetTask ?: tasks.firstOrNull { !it.isCompleted } ?: tasks.firstOrNull()
            onSendTestNotification(target)
        }
    }

    val triggerTestIn10Seconds: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingTest10sRequested = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            test10sActiveUntilMillis = System.currentTimeMillis() + 10_000L
            onScheduleTestNotificationIn10Seconds()
        }
    }

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager?.canScheduleExactAlarms() ?: true
            } else {
                true
            }
        )
    }

    // Recheck permissions on resume (e.g. when returning from system Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    hasExactAlarmPermission = alarmManager?.canScheduleExactAlarms() ?: true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keep device time live and ticking every second for diagnostic accuracy
    var currentClockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentClockTick = System.currentTimeMillis()
        }
    }

    // Filter tasks that have reminders configured or are scheduled
    val reminderTasks = remember(tasks) {
        tasks.filter { it.reminderEnabled || it.reminderEpochMillis != null }
    }

    val activeCount = remember(tasks) {
        tasks.count { getReminderStatus(it) == ReminderStatus.ACTIVE }
    }
    val missedCount = remember(tasks) {
        tasks.count { getReminderStatus(it) == ReminderStatus.MISSED }
    }
    val completedCount = remember(tasks) {
        tasks.count { it.isCompleted && (it.reminderEnabled || it.reminderEpochMillis != null) }
    }

    // Smart behavior 1: Incomplete tasks without an active reminder
    val incompleteTaskSuggestions = remember(tasks) {
        tasks.filter { !it.isCompleted && (!it.reminderEnabled || it.reminderEpochMillis == null) }
            .take(3)
    }

    // Smart behavior 2: Approaching exams (daysLeft <= 14) without duplicate reminder
    val approachingExamSuggestions = remember(exams, tasks) {
        exams.filter { exam ->
            exam.daysLeft in 1..14 && reminderTasks.none {
                it.subject.equals(exam.subject, ignoreCase = true) &&
                        it.title.contains(exam.examName, ignoreCase = true) &&
                        !it.isCompleted
            }
        }.take(3)
    }

    val filterTabs = listOf(
        "All (${reminderTasks.size})",
        "Active ($activeCount)",
        "Missed ($missedCount)",
        "Completed ($completedCount)"
    )

    val filteredTasks = remember(reminderTasks, selectedFilterIndex, selectedCategoryFilter) {
        reminderTasks.filter { task ->
            val status = getReminderStatus(task)
            val matchesFilter = when (selectedFilterIndex) {
                1 -> status == ReminderStatus.ACTIVE
                2 -> status == ReminderStatus.MISSED
                3 -> status == ReminderStatus.COMPLETED
                else -> true
            }
            val matchesCategory = selectedCategoryFilter == "All" ||
                    task.reminderCategory.equals(selectedCategoryFilter, ignoreCase = true)
            matchesFilter && matchesCategory
        }.sortedWith(
            compareBy(
                { if (getReminderStatus(it) == ReminderStatus.MISSED) 0 else if (getReminderStatus(it) == ReminderStatus.ACTIVE) 1 else 2 },
                { it.reminderEpochMillis ?: Long.MAX_VALUE }
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    initialPresetTitle = ""
                    initialPresetSubject = "Mathematics"
                    initialPresetCategory = "Study session"
                    showAddDialog = true
                },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("reminder_fab_add")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create Reminder")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "New Reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("smart_reminder_screen_list")
        ) {
            // 1. Top Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reminder_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Reminders",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Study sessions, homework & exam alerts",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Quick Test Alert Button - Always visible to test real Android notifications
                    OutlinedButton(
                        onClick = { triggerTestAlert(null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reminder_quick_test_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Test Alert", fontSize = 12.sp)
                    }
                }
            }

            // Diagnostic: Android Device System Clock (strictly device local, never browser or demo time)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("device_system_clock_diagnostic")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Device System Clock Diagnostic",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date: ${SmartReminderTimeHelper.getDeviceLocalDateString(currentClockTick)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.testTag("diagnostic_device_date")
                            )
                            Text(
                                text = "Time: ${SmartReminderTimeHelper.getDeviceLocalTimeString(currentClockTick)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("diagnostic_device_time")
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Timezone: ${SmartReminderTimeHelper.getDeviceTimeZoneId()} (${SmartReminderTimeHelper.getDeviceTimeZoneDisplayName()})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("diagnostic_device_timezone")
                            )
                            Text(
                                text = "Offset: ${SmartReminderTimeHelper.getDeviceUtcOffsetString(currentClockTick)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("diagnostic_device_offset")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test Notification in 10 Seconds Button
                        Button(
                            onClick = triggerTestIn10Seconds,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (test10sActiveUntilMillis > currentClockTick) GreenSuccess else IndigoPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_notification_in_10_seconds_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (test10sActiveUntilMillis > currentClockTick) {
                                    val secsLeft = ((test10sActiveUntilMillis - currentClockTick + 999) / 1000).coerceAtLeast(1)
                                    "Testing in ${secsLeft}s... (Watch Notification Shade)"
                                } else {
                                    "Test Notification in 10 Seconds"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // While testing: show live countdown and device clock confirmation
                        if (test10sActiveUntilMillis > currentClockTick) {
                            val secsLeft = ((test10sActiveUntilMillis - currentClockTick + 999) / 1000).coerceAtLeast(1)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GreenSuccess.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_10s_status_card")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = GreenSuccess
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Testing 10s Alarm ($secsLeft s remaining)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GreenSuccess
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Notification: \"Study Reminder Test\" • \"Your Smart Reminder notification is working.\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "Device Clock: ${SmartReminderTimeHelper.getDeviceLocalDateString(currentClockTick)} at ${SmartReminderTimeHelper.getDeviceLocalTimeString(currentClockTick)} (${SmartReminderTimeHelper.getDeviceTimeZoneString()})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = IndigoPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Prominent "Create Reminder" Button at Top
            item {
                Button(
                    onClick = {
                        initialPresetTitle = ""
                        initialPresetSubject = "Mathematics"
                        initialPresetCategory = "Study session"
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("btn_create_reminder_top")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Reminder",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            // Notification Permission Banner (Android 13+)
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberStreak.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("reminder_permission_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Notifications",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Allow notifications so your study alerts arrive on time.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                modifier = Modifier.testTag("reminder_allow_permission_btn")
                            ) {
                                Text("Allow", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Exact Alarm Permission Banner (Android 12+)
            if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("reminder_exact_alarm_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Exact Alarms",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Allow exact alarms so your study reminders trigger at the exact minute even when your device is asleep.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    StudyReminderScheduler.openExactAlarmSettings(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                modifier = Modifier.testTag("reminder_allow_exact_alarm_btn")
                            ) {
                                Text("Enable", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. Smart Suggestions (Smart Behavior: Incomplete tasks & approaching exams)
            if (incompleteTaskSuggestions.isNotEmpty() || approachingExamSuggestions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Suggestions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberStreak.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI Study Assistant",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberStreak,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(incompleteTaskSuggestions, key = { "task_${it.id}" }) { task ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = IndigoPrimary.copy(alpha = 0.06f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        IndigoPrimary.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .width(260.dp)
                                        .testTag("suggestion_task_${task.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = IndigoPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = task.subject,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IndigoPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Due ${task.dueDate}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Incomplete task without reminder",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                initialPresetTitle = task.title
                                                initialPresetSubject = task.subject
                                                initialPresetCategory = if (task.title.contains("homework", ignoreCase = true) || task.title.contains("worksheet", ignoreCase = true)) "Homework" else "Study session"
                                                showAddDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(34.dp)
                                                .testTag("btn_suggest_task_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Alarm,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Set Reminder", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            items(approachingExamSuggestions, key = { "exam_${it.id}" }) { exam ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFC2185B).copy(alpha = 0.06f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFC2185B).copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .width(260.dp)
                                        .testTag("suggestion_exam_${exam.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFC2185B).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = exam.subject,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC2185B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AmberStreak.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "${exam.daysLeft}d left",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberStreak,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = exam.examName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Exam approaching! Plan revision",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                initialPresetTitle = "Revise for ${exam.examName}"
                                                initialPresetSubject = exam.subject
                                                initialPresetCategory = "Exam preparation"
                                                showAddDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(34.dp)
                                                .testTag("btn_suggest_exam_${exam.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.School,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Schedule Revision", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Master Switch & Stats Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (masterReminderEnabled)
                            IndigoPrimary.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("reminder_master_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (masterReminderEnabled) IndigoPrimary else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (masterReminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Smart Reminders Service",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (masterReminderEnabled) "Active • Local notifications enabled" else "Paused • Alerts are silenced",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (masterReminderEnabled) IndigoPrimary else Color.Gray
                                        )
                                    )
                                }
                            }

                            Switch(
                                checked = masterReminderEnabled,
                                onCheckedChange = onToggleMaster,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("reminder_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReminderStatChip(
                                count = activeCount,
                                label = "Active",
                                dotColor = GreenSuccess,
                                testTag = "stat_chip_active"
                            )
                            ReminderStatChip(
                                count = missedCount,
                                label = "Missed",
                                dotColor = AmberStreak,
                                testTag = "stat_chip_missed"
                            )
                            ReminderStatChip(
                                count = completedCount,
                                label = "Done",
                                dotColor = CyanSecondary,
                                testTag = "stat_chip_completed"
                            )
                        }
                    }
                }
            }

            // 5. Filter Tabs
            item {
                SecondaryTabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    filterTabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedFilterIndex == idx,
                            onClick = { selectedFilterIndex = idx },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedFilterIndex == idx) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            modifier = Modifier.testTag("reminder_tab_$idx")
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                val categories = listOf("All") + REMINDER_TYPES
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = IndigoPrimary
                            )
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Upcoming Reminders",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${filteredTasks.size} scheduled",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when (selectedFilterIndex) {
                                1 -> "No active reminders"
                                2 -> "No missed sessions"
                                3 -> "No completed reminders yet"
                                else -> "No reminders found"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set a smart reminder at any time (e.g. 12:01 AM, 3:27 AM, 4:16 PM) for study sessions, homework, or exams.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                initialPresetTitle = ""
                                initialPresetSubject = "Mathematics"
                                initialPresetCategory = "Study session"
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("reminder_empty_add_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Reminder")
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    ReminderItemCard(
                        task = task,
                        onToggle = {
                            if (!task.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            onToggleReminder(task)
                        },
                        onEdit = { taskToEdit = task },
                        onSnooze = { mins -> onSnoozeReminder(task, mins) },
                        onReschedule = { taskToReschedule = task },
                        onMarkDone = { onMarkDone(task) },
                        onTestAlert = {
                            triggerTestAlert(task)
                        },
                        onDelete = { onDeleteReminder(task) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Reminder Dialog
    if (showAddDialog) {
        CreateOrEditReminderDialog(
            taskToEdit = null,
            presetTitle = initialPresetTitle,
            presetSubject = initialPresetSubject,
            presetCategory = initialPresetCategory,
            existingTasks = tasks.filter { !it.isCompleted },
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subject, date, time, epoch, repeat, category, enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onAddReminder(title, subject, date, time, epoch, repeat, category)
                showAddDialog = false
            }
        )
    }

    // Edit Reminder Dialog
    if (taskToEdit != null) {
        val editingTask = taskToEdit!!
        CreateOrEditReminderDialog(
            taskToEdit = editingTask,
            presetTitle = editingTask.title,
            presetSubject = editingTask.subject,
            presetCategory = editingTask.reminderCategory.ifBlank { "Study session" },
            existingTasks = emptyList(),
            onDismiss = { taskToEdit = null },
            onConfirm = { title, subject, date, time, epoch, repeat, category, enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onUpdateReminder(
                    editingTask,
                    title,
                    subject,
                    date,
                    time,
                    epoch,
                    repeat,
                    category,
                    enabled
                )
                taskToEdit = null
            }
        )
    }

    // Reschedule Dialog
    if (taskToReschedule != null) {
        val currentTask = taskToReschedule!!
        RescheduleReminderDialog(
            task = currentTask,
            onDismiss = { taskToReschedule = null },
            onConfirm = { epoch, dateStr, timeStr, repeat ->
                onRescheduleReminder(currentTask, epoch, dateStr, timeStr, repeat)
                taskToReschedule = null
            }
        )
    }
}

@Composable
fun ReminderStatChip(
    count: Int,
    label: String,
    dotColor: Color,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    task: StudyTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: (Int) -> Unit,
    onReschedule: () -> Unit,
    onMarkDone: () -> Unit,
    onTestAlert: () -> Unit,
    onDelete: () -> Unit
) {
    val status = getReminderStatus(task)
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(task.reminderCategory)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("reminder_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Status badge, Category Badge, Repeat badge, ON/OFF Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (status) {
                        ReminderStatus.ACTIVE -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GreenSuccess.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(GreenSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GreenSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        ReminderStatus.MISSED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberStreak.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = AmberStreak,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Missed",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberStreak,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        ReminderStatus.COMPLETED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanSecondary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        ReminderStatus.OFF -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Off",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Category Badge
                    val category = task.reminderCategory.ifBlank { "Study session" }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = categoryColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Repeat Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IndigoPrimary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = task.reminderRepeat.ifBlank { "Once" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // ON/OFF Switch
                Switch(
                    checked = task.reminderEnabled && !task.isCompleted,
                    onCheckedChange = { onToggle() },
                    enabled = !task.isCompleted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoPrimary
                    ),
                    modifier = Modifier.testTag("reminder_toggle_${task.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder Title & Subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = task.subject,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reminder_edit_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Reminder",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reminder_delete_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date & Exact Time indicators
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (task.reminderEnabled) IndigoPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_scheduled_datetime_${task.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (task.reminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val reminderDisplayDate = if (task.reminderEpochMillis != null && task.reminderEpochMillis > 0L) {
                            SmartReminderTimeHelper.getDisplayDateLabelFromEpoch(task.reminderEpochMillis)
                        } else {
                            task.reminderDateFormatted.ifBlank { task.dueDate }
                        }
                        Text(
                            text = reminderDisplayDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (task.reminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.reminderTimeFormatted.ifBlank { "Not set" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (task.reminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Missed alert banner & Reschedule
            if (status == ReminderStatus.MISSED) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberStreak.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You missed this study session",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberStreak
                                )
                            )
                        }

                        Button(
                            onClick = onReschedule,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.testTag("reminder_reschedule_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reschedule", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // Active actions: Remind later, Test alert, Mark as Done
            if (status == ReminderStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { showSnoozeMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("reminder_snooze_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind later", fontSize = 11.sp)
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showSnoozeMenu,
                            onDismissRequest = { showSnoozeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("+15 minutes") },
                                onClick = {
                                    onSnooze(15)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("+30 minutes") },
                                onClick = {
                                    onSnooze(30)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("+1 hour") },
                                onClick = {
                                    onSnooze(60)
                                    showSnoozeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pick manual time...") },
                                onClick = {
                                    showSnoozeMenu = false
                                    onReschedule()
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onTestAlert,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reminder_test_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Test Notification",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onMarkDone,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        modifier = Modifier.testTag("reminder_done_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark as Done", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Unified Dialog for Creating or Editing a Reminder with FULL MANUAL AND FLEXIBLE TIME SELECTION.
 * - Native Android TimePickerDialog allowing ANY hour (12:00 AM - 11:59 PM) and ANY minute (e.g. 12:01 AM, 3:27 AM, 4:16 PM).
 * - Exact minute steppers for ultra-precise manual adjustments.
 * - Handles already-passed times for Today accurately without silently mutating the student's chosen time.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditReminderDialog(
    taskToEdit: StudyTask? = null,
    presetTitle: String = "",
    presetSubject: String = "Mathematics",
    presetCategory: String = "Study session",
    existingTasks: List<StudyTask> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: String,
        date: String,
        time: String,
        epochMillis: Long,
        repeat: String,
        category: String,
        enabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = taskToEdit != null

    var taskTitle by remember {
        mutableStateOf(taskToEdit?.title ?: presetTitle)
    }
    var subject by remember {
        mutableStateOf(taskToEdit?.subject ?: presetSubject)
    }

    // Rebuilt date and time logic using device local time as single source of truth
    val initialTimeParts = remember(taskToEdit) {
        SmartReminderTimeHelper.getInitialFutureReminderTime(taskToEdit?.reminderEpochMillis)
    }

    var reminderYear by remember { mutableIntStateOf(initialTimeParts.year) }
    var reminderMonth by remember { mutableIntStateOf(initialTimeParts.month0) }
    var reminderDay by remember { mutableIntStateOf(initialTimeParts.day) }

    var selectedHour by remember { mutableIntStateOf(initialTimeParts.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTimeParts.minute) }
    var pastTimeValidationMessage by remember { mutableStateOf<String?>(null) }

    var repeatOption by remember {
        mutableStateOf(
            when (taskToEdit?.reminderRepeat) {
                "Daily" -> "Daily"
                "Weekly" -> "Weekly"
                else -> "Once"
            }
        )
    }

    var selectedType by remember {
        mutableStateOf(
            if (isEditMode) {
                taskToEdit!!.reminderCategory.ifBlank { "Study session" }
            } else {
                presetCategory.ifBlank { "Study session" }
            }
        )
    }

    var reminderToggleEnabled by remember {
        mutableStateOf(taskToEdit?.reminderEnabled ?: true)
    }

    var expandedSubject by remember { mutableStateOf(false) }
    var expandedExistingTasks by remember { mutableStateOf(false) }

    val subjectList = listOf(
        "Mathematics", "Physics", "Chemistry", "Biology",
        "Computer Science", "English Literature", "History", "Economics", "General Study"
    )

    val isToday = SmartReminderTimeHelper.isToday(reminderYear, reminderMonth, reminderDay)
    val isTomorrow = SmartReminderTimeHelper.isTomorrow(reminderYear, reminderMonth, reminderDay)

    val isTimePassedForToday = SmartReminderTimeHelper.isSelectedTimePassed(
        year = reminderYear,
        month0 = reminderMonth,
        day = reminderDay,
        selectedHour = selectedHour,
        selectedMinute = selectedMinute
    )

    val targetEpochMillis = SmartReminderTimeHelper.toLocalEpochMillis(
        year = reminderYear,
        month0 = reminderMonth,
        day = reminderDay,
        hour = selectedHour,
        minute = selectedMinute
    )

    val formattedDateStr = SmartReminderTimeHelper.formatShortDate(reminderYear, reminderMonth, reminderDay)
    val formattedTimeStr = SmartReminderTimeHelper.format12Hour(selectedHour, selectedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Edit else Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEditMode) "Edit Reminder" else "Create Reminder",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Link to pending task if available
                if (!isEditMode && existingTasks.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = expandedExistingTasks,
                            onExpandedChange = { expandedExistingTasks = !expandedExistingTasks }
                        ) {
                            OutlinedTextField(
                                value = if (taskTitle.isBlank()) "Choose pending task (optional)" else "Selected: $taskTitle",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Link to Pending Task", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExistingTasks) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedExistingTasks,
                                onDismissRequest = { expandedExistingTasks = false }
                            ) {
                                existingTasks.forEach { task ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(task.title, fontWeight = FontWeight.SemiBold)
                                                Text(task.subject, style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        onClick = {
                                            taskTitle = task.title
                                            subject = task.subject
                                            expandedExistingTasks = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 1. Reminder Title
                item {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Reminder Title") },
                        placeholder = { Text("e.g. Calculus Chapter 4 Review") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_title_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Select Subject
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedSubject,
                        onExpandedChange = { expandedSubject = !expandedSubject }
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Select Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubject) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .testTag("reminder_subject_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubject,
                            onDismissRequest = { expandedSubject = false }
                        ) {
                            subjectList.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        subject = item
                                        expandedSubject = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. Select Date
                item {
                    Text(
                        text = "Select Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isToday,
                            onClick = {
                                val (y, m, d) = SmartReminderTimeHelper.getTodayParts()
                                reminderYear = y
                                reminderMonth = m
                                reminderDay = d
                            },
                            label = { Text("Today", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("date_chip_today")
                        )

                        FilterChip(
                            selected = isTomorrow,
                            onClick = {
                                val (y, m, d) = SmartReminderTimeHelper.getTomorrowParts()
                                reminderYear = y
                                reminderMonth = m
                                reminderDay = d
                            },
                            label = { Text("Tomorrow", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("date_chip_tomorrow")
                        )

                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        reminderYear = y
                                        reminderMonth = m
                                        reminderDay = d
                                    },
                                    reminderYear,
                                    reminderMonth,
                                    reminderDay
                                ).apply {
                                    datePicker.minDate = SmartReminderTimeHelper.nowCalendar().timeInMillis
                                }.show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_pick_calendar_date")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!isToday && !isTomorrow) formattedDateStr else "Pick Date",
                                fontSize = 11.sp
                            )
                        }
                    }
                    val selectedDateDisplay = if (isToday) "Today ($formattedDateStr)" else if (isTomorrow) "Tomorrow ($formattedDateStr)" else formattedDateStr
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selected Date: $selectedDateDisplay",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = IndigoPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4. MANUAL & FLEXIBLE TIME PICKER (Any hour, Any minute, AM/PM)
                item {
                    Text(
                        text = "Reminder Time (Manual Time Picker)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.07f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        selectedHour = hourOfDay
                                        selectedMinute = minute
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    false // 12-hour view with AM/PM
                                ).show()
                            }
                            .testTag("time_picker_trigger_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(IndigoPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = "Clock",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = formattedTimeStr,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary
                                        )
                                    )
                                    Text(
                                        text = "Tap to open clock • Any hour & minute (12:00 AM - 11:59 PM)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            selectedHour = hourOfDay
                                            selectedMinute = minute
                                        },
                                        selectedHour,
                                        selectedMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                modifier = Modifier.testTag("btn_pick_time")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditCalendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Time", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Precise Minute Adjuster Steppers (-10m, -1m, +1m, +10m, and AM/PM toggle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val newMin = (selectedMinute - 10 + 60) % 60
                                    val hourDelta = if (selectedMinute < 10) -1 else 0
                                    selectedHour = (selectedHour + hourDelta + 24) % 24
                                    selectedMinute = newMin
                                }
                        ) {
                            Text(
                                text = "-10m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val newMin = (selectedMinute - 1 + 60) % 60
                                    val hourDelta = if (selectedMinute == 0) -1 else 0
                                    selectedHour = (selectedHour + hourDelta + 24) % 24
                                    selectedMinute = newMin
                                }
                        ) {
                            Text(
                                text = "-1m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val newMin = (selectedMinute + 1) % 60
                                    val hourDelta = if (selectedMinute == 59) 1 else 0
                                    selectedHour = (selectedHour + hourDelta) % 24
                                    selectedMinute = newMin
                                }
                        ) {
                            Text(
                                text = "+1m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val newMin = (selectedMinute + 10) % 60
                                    val hourDelta = if (selectedMinute >= 50) 1 else 0
                                    selectedHour = (selectedHour + hourDelta) % 24
                                    selectedMinute = newMin
                                }
                        ) {
                            Text(
                                text = "+10m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        // AM/PM quick toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable {
                                    selectedHour = (selectedHour + 12) % 24
                                }
                        ) {
                            Text(
                                text = if (selectedHour < 12) "Switch PM" else "Switch AM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    // Requirement 10: Handle passed time for today without silently changing the time
                    if (isTimePassedForToday) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberStreak.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberStreak.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = AmberStreak,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "This time has already passed for today.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberStreak
                                        ),
                                        modifier = Modifier.testTag("time_passed_warning_text")
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (repeatOption == "Daily")
                                        "Because Daily repeat is enabled, your first alarm will trigger tomorrow at $formattedTimeStr."
                                    else
                                        "Today reminders must be in the future. You can switch to Tomorrow or pick a later time today.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        val (y, m, d) = SmartReminderTimeHelper.getTomorrowParts()
                                        reminderYear = y
                                        reminderMonth = m
                                        reminderDay = d
                                        pastTimeValidationMessage = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Schedule for Tomorrow at $formattedTimeStr", fontSize = 11.sp, color = Color.White)
                                }
                                if (pastTimeValidationMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚠️ $pastTimeValidationMessage",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.testTag("past_time_error_text")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 5. Repeat Options: Once, Daily, Weekly
                item {
                    Text(
                        text = "Repeat Options",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Once", "Daily", "Weekly").forEach { opt ->
                            FilterChip(
                                selected = repeatOption == opt,
                                onClick = { repeatOption = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("repeat_chip_${opt.lowercase()}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 6. Reminder Type: Study session, Homework, Revision, Exam preparation
                item {
                    Text(
                        text = "Reminder Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        REMINDER_TYPES.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text(type, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getCategoryColor(type).copy(alpha = 0.18f),
                                    selectedLabelColor = getCategoryColor(type),
                                    selectedLeadingIconColor = getCategoryColor(type)
                                ),
                                modifier = Modifier.testTag("type_chip_${type.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 7. Toggle ON/OFF Switch
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (reminderToggleEnabled) "Reminder Status: ON" else "Reminder Status: OFF",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (reminderToggleEnabled) IndigoPrimary else Color.Gray
                            )
                        )
                        Switch(
                            checked = reminderToggleEnabled,
                            onCheckedChange = { reminderToggleEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoPrimary
                            ),
                            modifier = Modifier.testTag("dialog_reminder_toggle")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Summary Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IndigoPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val displayDateLabel = SmartReminderTimeHelper.getDisplayDateLabel(reminderYear, reminderMonth, reminderDay)
                            Text(
                                text = "Alarm: $displayDateLabel at $formattedTimeStr • $repeatOption ($selectedType)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = IndigoPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTimePassedForToday) {
                        pastTimeValidationMessage = "This time has already passed for today."
                        return@Button
                    }
                    val finalTitle = if (taskTitle.isNotBlank()) taskTitle.trim() else "Study Session: $subject"

                    val finalEpochMillis = SmartReminderTimeHelper.toLocalEpochMillis(
                        year = reminderYear,
                        month0 = reminderMonth,
                        day = reminderDay,
                        hour = selectedHour,
                        minute = selectedMinute
                    )

                    val finalDateStr = SmartReminderTimeHelper.getDisplayDateLabel(reminderYear, reminderMonth, reminderDay)

                    onConfirm(
                        finalTitle,
                        subject.trim(),
                        finalDateStr,
                        formattedTimeStr,
                        finalEpochMillis,
                        repeatOption,
                        selectedType,
                        reminderToggleEnabled
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("reminder_dialog_confirm_btn")
            ) {
                Text(if (isEditMode) "Save Changes" else "Save Reminder")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reminder_dialog_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reschedule Dialog with MANUAL AND FLEXIBLE TIME PICKER.
 * Removed all fixed options (9 AM, 2 PM, 7 PM, 9 PM). Allows selecting ANY hour & minute.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleReminderDialog(
    task: StudyTask,
    onDismiss: () -> Unit,
    onConfirm: (epochMillis: Long, dateStr: String, timeStr: String, repeat: String) -> Unit
) {
    val context = LocalContext.current
    var selectedDateMode by remember { mutableIntStateOf(0) } // 0: Today, 1: Tomorrow, 2: In 2 Days

    val initialParts = remember(task) {
        SmartReminderTimeHelper.getInitialFutureReminderTime(task.reminderEpochMillis)
    }
    var selectedHour by remember { mutableIntStateOf(initialParts.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialParts.minute) }
    val repeatType by remember { mutableStateOf(task.reminderRepeat.ifBlank { "Once" }) }

    val targetDateParts = when (selectedDateMode) {
        0 -> SmartReminderTimeHelper.getTodayParts()
        1 -> SmartReminderTimeHelper.getTomorrowParts()
        else -> SmartReminderTimeHelper.getIn2DaysParts()
    }

    val isTimePassedForToday = SmartReminderTimeHelper.isSelectedTimePassed(
        year = targetDateParts.first,
        month0 = targetDateParts.second,
        day = targetDateParts.third,
        selectedHour = selectedHour,
        selectedMinute = selectedMinute
    )

    val calculatedEpochMillis = SmartReminderTimeHelper.toLocalEpochMillis(
        year = targetDateParts.first,
        month0 = targetDateParts.second,
        day = targetDateParts.third,
        hour = selectedHour,
        minute = selectedMinute
    )

    val formattedDateStr = SmartReminderTimeHelper.formatShortDate(
        targetDateParts.first,
        targetDateParts.second,
        targetDateParts.third
    )
    val formattedTimeStr = SmartReminderTimeHelper.format12Hour(selectedHour, selectedMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = null,
                    tint = AmberStreak,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reschedule Session", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Subject: ${task.subject} • ${task.reminderCategory.ifBlank { "Study session" }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Today", "Tomorrow", "+2 Days").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedDateMode == idx,
                            onClick = { selectedDateMode = idx },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Manual Time Picker",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberStreak.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberStreak.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                },
                                selectedHour,
                                selectedMinute,
                                false
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedTimeStr,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberStreak
                                )
                            )
                        }

                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedHour = hour
                                        selectedMinute = minute
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Change Time", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                if (isTimePassedForToday) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ This time has already passed for today.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AmberStreak,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberStreak.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rescheduleDateLabel = SmartReminderTimeHelper.getDisplayDateLabel(targetDateParts.first, targetDateParts.second, targetDateParts.third)
                    Text(
                        text = "New study alarm: $rescheduleDateLabel at $formattedTimeStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = AmberStreak
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (finalYear, finalMonth, finalDay) = if (isTimePassedForToday) {
                        SmartReminderTimeHelper.getTomorrowParts()
                    } else {
                        targetDateParts
                    }
                    val finalEpoch = SmartReminderTimeHelper.toLocalEpochMillis(
                        year = finalYear,
                        month0 = finalMonth,
                        day = finalDay,
                        hour = selectedHour,
                        minute = selectedMinute
                    )
                    val finalDate = SmartReminderTimeHelper.getDisplayDateLabel(finalYear, finalMonth, finalDay)
                    onConfirm(finalEpoch, finalDate, formattedTimeStr, repeatType)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberStreak)
            ) {
                Text("Confirm Reschedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
