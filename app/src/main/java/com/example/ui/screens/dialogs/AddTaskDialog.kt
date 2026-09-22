package com.example.ui.screens.dialogs

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.StudyTask
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.IndigoPrimary
import com.example.util.DeviceTimeService
import com.example.util.LocalDateTimeParts
import com.example.util.SmartReminderTimeHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddTaskDialog(
    taskToEdit: StudyTask? = null,
    initialDueDate: String = "Today",
    onDismiss: () -> Unit,
    onConfirmTask: (
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
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var subject by remember { mutableStateOf(taskToEdit?.subject ?: "Mathematics") }
    var customSubject by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate ?: initialDueDate) }
    var minutes by remember { mutableIntStateOf(taskToEdit?.estimatedMinutes ?: 30) }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var xpReward by remember {
        mutableIntStateOf(
            taskToEdit?.xpReward ?: when (taskToEdit?.priority ?: "Medium") {
                "High" -> 35
                "Medium" -> 25
                else -> 20
            }
        )
    }
    var startTime by remember {
        mutableStateOf(taskToEdit?.reminderTimeFormatted ?: "")
    }

    // Smart Reminder States
    var reminderEnabled by remember { mutableStateOf(taskToEdit?.reminderEnabled ?: false) }
    var reminderRepeat by remember { mutableStateOf(taskToEdit?.reminderRepeat ?: "Once") }

    // Initialize date and time using device local time
    val initialTimeParts = remember(taskToEdit) {
        SmartReminderTimeHelper.getInitialFutureReminderTime(taskToEdit?.reminderEpochMillis)
    }

    var reminderYear by remember { mutableIntStateOf(initialTimeParts.year) }
    var reminderMonth by remember { mutableIntStateOf(initialTimeParts.month0) }
    var reminderDay by remember { mutableIntStateOf(initialTimeParts.day) }
    var reminderHour by remember { mutableIntStateOf(initialTimeParts.hour) }
    var reminderMinute by remember { mutableIntStateOf(initialTimeParts.minute) }

    val selectedEpochMillis = SmartReminderTimeHelper.toLocalEpochMillis(
        year = reminderYear,
        month0 = reminderMonth,
        day = reminderDay,
        hour = reminderHour,
        minute = reminderMinute
    )

    val isPastTime = reminderEnabled && SmartReminderTimeHelper.isSelectedTimePassed(
        year = reminderYear,
        month0 = reminderMonth,
        day = reminderDay,
        selectedHour = reminderHour,
        selectedMinute = reminderMinute
    )

    val formattedDate = SmartReminderTimeHelper.formatFullDate(reminderYear, reminderMonth, reminderDay)
    val formattedTime = SmartReminderTimeHelper.format12Hour(reminderHour, reminderMinute)

    // Notification Permission Handling (Android 13+)
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

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        showPermissionRationale = !isGranted
    }

    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Computer Science", "Biology", "Literature", "Other")
    val dueDates = listOf("Today", "Tomorrow", "This Week", "Next Week")
    val priorities = listOf("High", "Medium", "Low")
    val timeOptions = listOf(15, 25, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (taskToEdit == null) "Add Study Task" else "Edit Study Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Maths Chapter 1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
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

                if (subject == "Other") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customSubject,
                        onValueChange = { customSubject = it },
                        label = { Text("Enter Subject Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isCustomDueDate = dueDate !in dueDates
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(dueDates) { item ->
                        FilterChip(
                            selected = dueDate == item,
                            onClick = {
                                dueDate = item
                                if (item == "Tomorrow") {
                                    val (y, m, d) = com.example.util.DeviceTimeService.getTomorrowParts()
                                    reminderYear = y
                                    reminderMonth = m
                                    reminderDay = d
                                } else if (item == "Today") {
                                    val (y, m, d) = com.example.util.DeviceTimeService.getTodayParts()
                                    reminderYear = y
                                    reminderMonth = m
                                    reminderDay = d
                                }
                            },
                            label = { Text(item) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        reminderYear = y
                                        reminderMonth = m
                                        reminderDay = d
                                        dueDate = SmartReminderTimeHelper.formatFullDate(y, m, d)
                                    },
                                    reminderYear,
                                    reminderMonth,
                                    reminderDay
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isCustomDueDate) {
                                ButtonDefaults.outlinedButtonColors(containerColor = IndigoPrimary.copy(alpha = 0.12f))
                            } else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.testTag("task_pick_custom_date_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isCustomDueDate) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCustomDueDate) dueDate else "Pick Date",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isCustomDueDate) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCustomDueDate) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start Time & Duration
                Text(
                    text = "Start Time",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val hasCustomStartTime = startTime.isNotBlank()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        reminderHour = h
                                        reminderMinute = min
                                        startTime = SmartReminderTimeHelper.format12Hour(h, min)
                                    },
                                    reminderHour,
                                    reminderMinute,
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = if (hasCustomStartTime) {
                                ButtonDefaults.outlinedButtonColors(containerColor = IndigoPrimary.copy(alpha = 0.12f))
                            } else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.testTag("task_pick_start_time_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (hasCustomStartTime) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasCustomStartTime) startTime else formattedTime,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasCustomStartTime) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val nowTime = com.example.util.DeviceTimeService.currentLocalTime()
                                reminderHour = nowTime.hour
                                reminderMinute = nowTime.minute
                                startTime = SmartReminderTimeHelper.format12Hour(nowTime.hour, nowTime.minute)
                            },
                            label = { Text("Now") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val z = com.example.util.DeviceTimeService.nowZoned().plusMinutes(15)
                                reminderHour = z.hour
                                reminderMinute = z.minute
                                startTime = SmartReminderTimeHelper.format12Hour(z.hour, z.minute)
                            },
                            label = { Text("+15m") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val z = com.example.util.DeviceTimeService.nowZoned().plusMinutes(30)
                                reminderHour = z.hour
                                reminderMinute = z.minute
                                startTime = SmartReminderTimeHelper.format12Hour(z.hour, z.minute)
                            },
                            label = { Text("+30m") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val z = com.example.util.DeviceTimeService.nowZoned().plusHours(1)
                                reminderHour = z.hour
                                reminderMinute = z.minute
                                startTime = SmartReminderTimeHelper.format12Hour(z.hour, z.minute)
                            },
                            label = { Text("+1h") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(timeOptions) { mins ->
                        FilterChip(
                            selected = minutes == mins,
                            onClick = { minutes = mins },
                            label = { Text("${mins}m") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "XP Reward",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val xpOptions = listOf(15, 20, 25, 35, 50, 100)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(xpOptions) { xp ->
                        FilterChip(
                            selected = xpReward == xp,
                            onClick = { xpReward = xp },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (xpReward == xp) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("+$xp XP")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("xp_chip_$xp")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { item ->
                        FilterChip(
                            selected = priority == item,
                            onClick = {
                                priority = item
                                if (xpReward == 20 || xpReward == 25 || xpReward == 35) {
                                    xpReward = when (item) {
                                        "High" -> 35
                                        "Medium" -> 25
                                        else -> 20
                                    }
                                }
                            },
                            label = { Text(item) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==================== SMART STUDY REMINDER SECTION ====================
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reminderEnabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_section_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = "Reminder",
                                    tint = if (reminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Smart Study Reminder",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (reminderEnabled) "Notification scheduled" else "Reminder is OFF",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (reminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { checked ->
                                    reminderEnabled = checked
                                    if (checked) {
                                        // If past time, advance to 30 mins in future
                                        if (selectedEpochMillis <= System.currentTimeMillis()) {
                                            val futureParts = SmartReminderTimeHelper.getInitialFutureReminderTime()
                                            reminderYear = futureParts.year
                                            reminderMonth = futureParts.month0
                                            reminderDay = futureParts.day
                                            reminderHour = futureParts.hour
                                            reminderMinute = futureParts.minute
                                        }

                                        // Check notification permission on Android 13+
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("reminder_switch")
                            )
                        }

                        // Android 13+ Permission Explanatory Card
                        if (reminderEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Notification Permission",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "TuxLearn requires notification permission to alert you when your study time arrives.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    ) {
                                        Text("Allow", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Expanded controls when Reminder is ON
                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Reminder Date section
                            Text(
                                text = "Reminder Date",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isToday = SmartReminderTimeHelper.isToday(reminderYear, reminderMonth, reminderDay)
                                val isTomorrow = SmartReminderTimeHelper.isTomorrow(reminderYear, reminderMonth, reminderDay)

                                FilterChip(
                                    selected = isToday,
                                    onClick = {
                                        val (y, m, d) = SmartReminderTimeHelper.getTodayParts()
                                        reminderYear = y
                                        reminderMonth = m
                                        reminderDay = d
                                    },
                                    label = { Text("Today") },
                                    shape = RoundedCornerShape(8.dp)
                                )

                                FilterChip(
                                    selected = isTomorrow,
                                    onClick = {
                                        val (y, m, d) = SmartReminderTimeHelper.getTomorrowParts()
                                        reminderYear = y
                                        reminderMonth = m
                                        reminderDay = d
                                    },
                                    label = { Text("Tomorrow") },
                                    shape = RoundedCornerShape(8.dp)
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
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("pick_custom_date_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (!isToday && !isTomorrow) formattedDate else "Pick Date",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Reminder Time section
                            Text(
                                text = "Reminder Time",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val z = com.example.util.DeviceTimeService.nowZoned().plusMinutes(15)
                                            reminderYear = z.year
                                            reminderMonth = z.monthValue - 1
                                            reminderDay = z.dayOfMonth
                                            reminderHour = z.hour
                                            reminderMinute = z.minute
                                        },
                                        label = { Text("+15m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val z = com.example.util.DeviceTimeService.nowZoned().plusMinutes(30)
                                            reminderYear = z.year
                                            reminderMonth = z.monthValue - 1
                                            reminderDay = z.dayOfMonth
                                            reminderHour = z.hour
                                            reminderMinute = z.minute
                                        },
                                        label = { Text("+30m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val z = com.example.util.DeviceTimeService.nowZoned().plusHours(1)
                                            reminderYear = z.year
                                            reminderMonth = z.monthValue - 1
                                            reminderDay = z.dayOfMonth
                                            reminderHour = z.hour
                                            reminderMinute = z.minute
                                        },
                                        label = { Text("+1h") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                item {
                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, h, min ->
                                                    reminderHour = h
                                                    reminderMinute = min
                                                },
                                                reminderHour,
                                                reminderMinute,
                                                false
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("pick_custom_time_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Requirement 3: Repeat option (Once, Daily, Weekly)
                            Text(
                                text = "Repeat",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Once", "Daily", "Weekly").forEach { opt ->
                                    FilterChip(
                                        selected = reminderRepeat == opt,
                                        onClick = { reminderRepeat = opt },
                                        label = { Text(opt) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("repeat_chip_${opt.lowercase()}")
                                    )
                                }
                            }

                            // Summary pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Alarm,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reminder set for: $formattedDate at $formattedTime • $reminderRepeat",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // Past time validation error warning (Requirement 10)
                            if (isPastTime) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = "Warning",
                                            tint = Color(0xFFB91C1C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "This time has already passed for today.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFB91C1C),
                                                fontWeight = FontWeight.SemiBold,
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && (!reminderEnabled || !isPastTime)) {
                        val finalSubject = if (subject == "Other" && customSubject.isNotBlank()) customSubject else subject
                        val finalStartTime = if (reminderEnabled) formattedTime else if (startTime.isNotBlank()) startTime else formattedTime
                        onConfirmTask(
                            title.trim(),
                            finalSubject,
                            dueDate,
                            minutes,
                            priority,
                            reminderEnabled,
                            if (reminderEnabled) selectedEpochMillis else null,
                            if (reminderEnabled) formattedDate else "",
                            finalStartTime,
                            reminderRepeat,
                            xpReward
                        )
                    }
                },
                enabled = title.isNotBlank() && (!reminderEnabled || !isPastTime),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text(if (taskToEdit == null) "Add Task" else "Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Backward-compatible overload without xpReward
@Composable
fun AddTaskDialog(
    taskToEdit: StudyTask? = null,
    initialDueDate: String = "Today",
    onDismiss: () -> Unit,
    onConfirmTask: (
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
    ) -> Unit
) {
    AddTaskDialog(
        taskToEdit = taskToEdit,
        initialDueDate = initialDueDate,
        onDismiss = onDismiss,
        onConfirmTask = { title, subject, dueDate, mins, priority, remEnabled, remMillis, remDate, remTime, remRep, _ ->
            onConfirmTask(title, subject, dueDate, mins, priority, remEnabled, remMillis, remDate, remTime, remRep)
        }
    )
}

// Backward-compatible overload
@Composable
fun AddTaskDialog(
    initialDueDate: String = "Today",
    onDismiss: () -> Unit,
    onConfirm: (title: String, subject: String, dueDate: String, minutes: Int, priority: String) -> Unit
) {
    AddTaskDialog(
        taskToEdit = null,
        initialDueDate = initialDueDate,
        onDismiss = onDismiss,
        onConfirmTask = { title, subject, dueDate, mins, priority, _, _, _, _, _, _ ->
            onConfirm(title, subject, dueDate, mins, priority)
        }
    )
}
