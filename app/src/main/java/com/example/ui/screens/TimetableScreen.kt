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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.TimetableSlot
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.util.DeviceTimeService
import com.example.util.parseTimeStringToMinutes

// Category color definition
val TIMETABLE_COLORS = listOf(
    "Indigo" to IndigoPrimary,
    "Emerald" to GreenSuccess,
    "Amber" to AmberStreak,
    "Rose" to CoralRed,
    "Cyan" to CyanSecondary,
    "Purple" to PurpleAccent
)

fun getTimetableCategoryColor(category: String): Color {
    return TIMETABLE_COLORS.firstOrNull { it.first.equals(category, ignoreCase = true) }?.second
        ?: IndigoPrimary
}

val DAYS_OF_WEEK = listOf(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
)

fun getShortDayLabel(day: String): String {
    return when (DeviceTimeService.normalizeDay(day)) {
        "Monday" -> "Mon"
        "Tuesday" -> "Tue"
        "Wednesday" -> "Wed"
        "Thursday" -> "Thu"
        "Friday" -> "Fri"
        "Saturday" -> "Sat"
        "Sunday" -> "Sun"
        else -> day.take(3)
    }
}

fun checkTimeConflict(
    existingSlots: List<TimetableSlot>,
    targetDay: String,
    startTimeStr: String,
    endTimeStr: String,
    currentSlotId: Int? = null
): TimetableSlot? {
    val startMin = parseTimeStringToMinutes(startTimeStr)
    val endMin = parseTimeStringToMinutes(endTimeStr)
    if (startMin >= endMin) return null

    val targetDayNorm = DeviceTimeService.normalizeDay(targetDay)
    val sameDaySlots = existingSlots.filter {
        DeviceTimeService.normalizeDay(it.dayOfWeek).equals(targetDayNorm, ignoreCase = true) &&
                (currentSlotId == null || it.id != currentSlotId)
    }

    return sameDaySlots.firstOrNull { existing ->
        val exStart = parseTimeStringToMinutes(existing.startTime)
        val exEnd = parseTimeStringToMinutes(existing.endTime)
        // Overlap: startMin < exEnd && exStart < endMin
        startMin < exEnd && exStart < endMin
    }
}

@Composable
fun TimetableScreen(
    slots: List<TimetableSlot>,
    onAddSlot: (TimetableSlot) -> Unit,
    onUpdateSlot: (TimetableSlot) -> Unit,
    onDeleteSlot: (TimetableSlot) -> Unit
) {
    val todayFullName = remember { DeviceTimeService.currentDayOfWeekName() }
    var selectedDay by remember { mutableStateOf(todayFullName) }

    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var slotToDelete by remember { mutableStateOf<TimetableSlot?>(null) }

    // Filter and sort slots chronologically
    val daySlots = remember(slots, selectedDay) {
        slots.filter {
            DeviceTimeService.normalizeDay(it.dayOfWeek).equals(
                DeviceTimeService.normalizeDay(selectedDay),
                ignoreCase = true
            )
        }.sortedBy { parseTimeStringToMinutes(it.startTime) }
    }

    // Determine current class and next class based on device local time
    val nowLocalTime = DeviceTimeService.currentLocalTime()
    val nowMinutes = nowLocalTime.hour * 60 + nowLocalTime.minute

    val todaySlots = remember(slots, todayFullName) {
        slots.filter {
            DeviceTimeService.normalizeDay(it.dayOfWeek).equals(todayFullName, ignoreCase = true)
        }.sortedBy { parseTimeStringToMinutes(it.startTime) }
    }

    val currentClass = remember(todaySlots, nowMinutes) {
        todaySlots.firstOrNull { slot ->
            val start = parseTimeStringToMinutes(slot.startTime)
            val end = parseTimeStringToMinutes(slot.endTime)
            start <= nowMinutes && nowMinutes < end
        }
    }

    val nextClass = remember(todaySlots, nowMinutes) {
        todaySlots.firstOrNull { slot ->
            val start = parseTimeStringToMinutes(slot.startTime)
            start > nowMinutes
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("timetable_fab_add")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Class / Session")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("timetable_screen_root")
        ) {
            // Live Header Status Card (Current / Next Class Banner)
            TimetableLiveStatusHeader(
                todayFullName = todayFullName,
                todaySlotsCount = todaySlots.size,
                currentClass = currentClass,
                nextClass = nextClass,
                nowMinutes = nowMinutes
            )

            // Weekly Day Selector (Monday to Sunday)
            WeeklyDaySelector(
                daysOfWeek = DAYS_OF_WEEK,
                selectedDay = selectedDay,
                todayFullName = todayFullName,
                allSlots = slots,
                onSelectDay = { selectedDay = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Day schedule list or empty state
            if (daySlots.isEmpty()) {
                TimetableEmptyState(
                    day = selectedDay,
                    onAddClass = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("timetable_slots_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(daySlots, key = { it.id }) { slot ->
                        val isToday = DeviceTimeService.normalizeDay(selectedDay).equals(todayFullName, ignoreCase = true)
                        val startMin = parseTimeStringToMinutes(slot.startTime)
                        val endMin = parseTimeStringToMinutes(slot.endTime)
                        val isHappeningNow = isToday && startMin <= nowMinutes && nowMinutes < endMin

                        TimetableSlotCard(
                            slot = slot,
                            isHappeningNow = isHappeningNow,
                            nowMinutes = nowMinutes,
                            onEdit = { slotToEdit = slot },
                            onDelete = { slotToDelete = slot }
                        )
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        TimetableSlotDialog(
            title = "Add Timetable Entry",
            confirmButtonText = "Save Entry",
            initialSlot = null,
            initialDay = selectedDay,
            allSlots = slots,
            onDismiss = { showAddDialog = false },
            onConfirm = { newSlot ->
                onAddSlot(newSlot)
                showAddDialog = false
            }
        )
    }

    // Edit Dialog
    slotToEdit?.let { editingSlot ->
        TimetableSlotDialog(
            title = "Edit Timetable Entry",
            confirmButtonText = "Update Entry",
            initialSlot = editingSlot,
            initialDay = editingSlot.dayOfWeek,
            allSlots = slots,
            onDismiss = { slotToEdit = null },
            onConfirm = { updatedSlot ->
                onUpdateSlot(updatedSlot)
                slotToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    slotToDelete?.let { deletingSlot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = CoralRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Class Entry?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Remove ${deletingSlot.subject} (${deletingSlot.startTime} – ${deletingSlot.endTime}) from ${deletingSlot.dayOfWeek}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSlot(deletingSlot)
                        slotToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    modifier = Modifier.testTag("confirm_delete_slot_button")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { slotToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// Backwards-compatible overload
@Composable
fun TimetableScreen(
    slots: List<TimetableSlot>,
    onAddSlot: (String, String, String, String, String, Boolean) -> Unit,
    onDeleteSlot: (TimetableSlot) -> Unit
) {
    TimetableScreen(
        slots = slots,
        onAddSlot = { slot ->
            onAddSlot(
                slot.dayOfWeek,
                slot.startTime,
                slot.endTime,
                slot.subject,
                if (slot.roomOrClass.isNotBlank() || slot.teacherName.isNotBlank()) {
                    listOf(slot.roomOrClass, slot.teacherName).filter { it.isNotBlank() }.joinToString(" • ")
                } else slot.roomOrTeacher,
                slot.isStudySession
            )
        },
        onUpdateSlot = { _ -> },
        onDeleteSlot = onDeleteSlot
    )
}

/**
 * Top Status Card showing Today's current and next class live based on device local time.
 */
@Composable
private fun TimetableLiveStatusHeader(
    todayFullName: String,
    todaySlotsCount: Int,
    currentClass: TimetableSlot?,
    nextClass: TimetableSlot?,
    nowMinutes: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (currentClass != null) GreenSuccess.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("timetable_live_status_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Today,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today • $todayFullName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$todaySlotsCount ${if (todaySlotsCount == 1) "class" else "classes"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (currentClass != null) {
                // Class happening right now
                val start = parseTimeStringToMinutes(currentClass.startTime)
                val end = parseTimeStringToMinutes(currentClass.endTime)
                val duration = (end - start).coerceAtLeast(1)
                val elapsed = (nowMinutes - start).coerceIn(0, duration)
                val progress = elapsed.toFloat() / duration.toFloat()
                val remainingMins = (end - nowMinutes).coerceAtLeast(0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GreenSuccess.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HAPPENING NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenSuccess,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }
                        Text(
                            text = "$remainingMins min left",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentClass.subject,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val locationInfo = if (currentClass.roomOrClass.isNotBlank() || currentClass.teacherName.isNotBlank()) {
                        listOf(currentClass.roomOrClass, currentClass.teacherName).filter { it.isNotBlank() }.joinToString(" • ")
                    } else currentClass.roomOrTeacher

                    if (locationInfo.isNotBlank()) {
                        Text(
                            text = locationInfo,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = GreenSuccess,
                        trackColor = GreenSuccess.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Next class display
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = if (nextClass != null) CyanSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (nextClass != null) {
                    Text(
                        text = "Next: ${nextClass.subject} at ${nextClass.startTime}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CyanSecondary
                        ),
                        modifier = Modifier.testTag("timetable_next_class_label")
                    )
                } else if (currentClass != null) {
                    Text(
                        text = "Next: No further classes today",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else if (todaySlotsCount > 0) {
                    Text(
                        text = "All classes completed for today 🎉",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GreenSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    Text(
                        text = "No classes scheduled for today",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

/**
 * Weekly Day Selector strip with indicators for Today and number of classes per day.
 */
@Composable
private fun WeeklyDaySelector(
    daysOfWeek: List<String>,
    selectedDay: String,
    todayFullName: String,
    allSlots: List<TimetableSlot>,
    onSelectDay: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timetable_weekly_strip"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysOfWeek) { day ->
                val isSelected = DeviceTimeService.normalizeDay(selectedDay).equals(day, ignoreCase = true)
                val isToday = DeviceTimeService.normalizeDay(todayFullName).equals(day, ignoreCase = true)
                val count = allSlots.count {
                    DeviceTimeService.normalizeDay(it.dayOfWeek).equals(day, ignoreCase = true)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isSelected -> IndigoPrimary
                        isToday -> IndigoPrimary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = if (isToday && !isSelected) BorderStroke(1.dp, IndigoPrimary) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectDay(day) }
                        .testTag("timetable_day_${getShortDayLabel(day)}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getShortDayLabel(day),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else IndigoPrimary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty State when a day has no classes scheduled.
 */
@Composable
private fun TimetableEmptyState(
    day: String,
    onAddClass: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("timetable_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No classes scheduled",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.testTag("timetable_empty_title")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enjoy your free time or schedule a class / study session for $day",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddClass,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("timetable_empty_add_class")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Class")
            }
        }
    }
}

/**
 * Card representing a single Timetable Slot with edit and delete controls.
 */
@Composable
private fun TimetableSlotCard(
    slot: TimetableSlot,
    isHappeningNow: Boolean,
    nowMinutes: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = getTimetableCategoryColor(slot.colorCategory)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHappeningNow) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isHappeningNow) {
            BorderStroke(2.dp, GreenSuccess)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHappeningNow) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_slot_${slot.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // "Happening Now" top banner strip if active
            if (isHappeningNow) {
                Surface(
                    color = GreenSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GreenSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HAPPENING NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Text(
                        text = slot.startTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                    Text(
                        text = "to",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = slot.endTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Vertical accent line
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Subject details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.subject,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (slot.isStudySession) "Study Block" else slot.colorCategory,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (slot.reminderEnabled) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Reminder active",
                                tint = AmberStreak,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Location or Teacher
                    val teacher = slot.teacherName.ifBlank { "" }
                    val room = slot.roomOrClass.ifBlank { "" }
                    val combinedLegacy = slot.roomOrTeacher

                    val displayTeacherRoom = when {
                        teacher.isNotBlank() && room.isNotBlank() -> "$room • $teacher"
                        room.isNotBlank() -> room
                        teacher.isNotBlank() -> teacher
                        combinedLegacy.isNotBlank() -> combinedLegacy
                        else -> null
                    }

                    if (displayTeacherRoom != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (slot.isStudySession) Icons.Filled.MenuBook else Icons.Filled.Room,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayTeacherRoom,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Optional Note
                    if (slot.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = slot.note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Edit and Delete action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_slot_${slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Slot",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_slot_${slot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Slot",
                            tint = CoralRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add / Edit Timetable Slot Dialog with live conflict detection, presets, and validation.
 */
@Composable
private fun TimetableSlotDialog(
    title: String,
    confirmButtonText: String,
    initialSlot: TimetableSlot?,
    initialDay: String,
    allSlots: List<TimetableSlot>,
    onDismiss: () -> Unit,
    onConfirm: (TimetableSlot) -> Unit
) {
    var day by remember { mutableStateOf(DeviceTimeService.normalizeDay(initialSlot?.dayOfWeek ?: initialDay)) }
    var subject by remember { mutableStateOf(initialSlot?.subject ?: "") }
    var teacherName by remember { mutableStateOf(initialSlot?.teacherName ?: "") }
    var roomOrClass by remember { mutableStateOf(initialSlot?.roomOrClass ?: (initialSlot?.roomOrTeacher ?: "")) }
    var startTime by remember { mutableStateOf(initialSlot?.startTime ?: "09:00 AM") }
    var endTime by remember { mutableStateOf(initialSlot?.endTime ?: "10:15 AM") }
    var colorCategory by remember { mutableStateOf(initialSlot?.colorCategory ?: "Indigo") }
    var note by remember { mutableStateOf(initialSlot?.note ?: "") }
    var isStudySession by remember { mutableStateOf(initialSlot?.isStudySession ?: false) }
    var reminderEnabled by remember { mutableStateOf(initialSlot?.reminderEnabled ?: false) }

    var showConflictOverrideWarning by remember { mutableStateOf(false) }

    val subjectSuggestions = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "English", "History")
    val timePresets = listOf("08:30 AM", "09:00 AM", "10:00 AM", "11:15 AM", "01:00 PM", "02:30 PM")

    // Live conflict checking
    val conflict = remember(day, startTime, endTime, initialSlot) {
        checkTimeConflict(
            existingSlots = allSlots,
            targetDay = day,
            startTimeStr = startTime,
            endTimeStr = endTime,
            currentSlotId = initialSlot?.id
        )
    }

    val startMin = parseTimeStringToMinutes(startTime)
    val endMin = parseTimeStringToMinutes(endTime)
    val isTimeValid = startMin < endMin
    val isFormValid = subject.isNotBlank() && isTimeValid

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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day selector
                Text(
                    text = "Day of Week",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DAYS_OF_WEEK) { item ->
                        FilterChip(
                            selected = day.equals(item, ignoreCase = true),
                            onClick = { day = item },
                            label = { Text(getShortDayLabel(item)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Subject Name & Quick suggestions
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject *") },
                    placeholder = { Text("e.g. Mathematics") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_input_subject")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjectSuggestions) { s ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subject = s }
                        ) {
                            Text(
                                text = s,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Times (Start & End)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timetable_input_start_time")
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time *") },
                        singleLine = true,
                        isError = !isTimeValid && endTime.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timetable_input_end_time")
                    )
                }

                // Start Time Quick Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(timePresets) { tp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (startTime == tp) IndigoPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    startTime = tp
                                    // Default 60 min after
                                    val sm = parseTimeStringToMinutes(tp)
                                    val em = sm + 60
                                    val eh = (em / 60) % 24
                                    val emin = em % 60
                                    val ampm = if (eh >= 12) "PM" else "AM"
                                    val h12 = if (eh % 12 == 0) 12 else eh % 12
                                    endTime = String.format(java.util.Locale.US, "%02d:%02d %s", h12, emin, ampm)
                                }
                        ) {
                            Text(
                                text = tp,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!isTimeValid && endTime.isNotBlank()) {
                    Text(
                        text = "End time must be later than start time",
                        style = MaterialTheme.typography.bodySmall.copy(color = CoralRed)
                    )
                }

                // Conflict Detection Warning Box
                if (conflict != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CoralRed.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timetable_conflict_warning_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Conflict",
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Time Conflict Detected",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRed
                                    )
                                )
                                Text(
                                    text = "Overlaps with ${conflict.subject} (${conflict.startTime} – ${conflict.endTime}) on $day",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // Teacher and Room
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Teacher Name (Optional)") },
                    placeholder = { Text("e.g. Dr. Patel") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_input_teacher")
                )

                OutlinedTextField(
                    value = roomOrClass,
                    onValueChange = { roomOrClass = it },
                    label = { Text("Room / Class (Optional)") },
                    placeholder = { Text("e.g. Room 302, Lab 4") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_input_room")
                )

                // Category / Color selector
                Text(
                    text = "Category & Color",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TIMETABLE_COLORS) { (catName, catColor) ->
                        val isCatSelected = colorCategory.equals(catName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCatSelected) catColor else catColor.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { colorCategory = catName }
                                .testTag("timetable_color_$catName")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isCatSelected) Color.White else catColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Optional Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Topics (Optional)") },
                    placeholder = { Text("e.g. Bring lab notebook, Chapter 4 quiz") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timetable_input_note")
                )

                // Study session toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Self-Study Session",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Mark as personal revision or quiet study",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = isStudySession,
                        onCheckedChange = { isStudySession = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = IndigoPrimary)
                    )
                }

                // Set Reminder Toggle (Smart Reminder Integration)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Reminder",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Alert via Smart Reminders before this class",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = IndigoPrimary),
                        modifier = Modifier.testTag("timetable_switch_reminder")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        if (conflict != null && !showConflictOverrideWarning) {
                            showConflictOverrideWarning = true
                        } else {
                            val savedSlot = TimetableSlot(
                                id = initialSlot?.id ?: 0,
                                dayOfWeek = day,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                subject = subject.trim(),
                                teacherName = teacherName.trim(),
                                roomOrClass = roomOrClass.trim(),
                                roomOrTeacher = listOf(roomOrClass.trim(), teacherName.trim()).filter { it.isNotBlank() }.joinToString(" • "),
                                colorCategory = colorCategory,
                                note = note.trim(),
                                isStudySession = isStudySession,
                                reminderEnabled = reminderEnabled
                            )
                            onConfirm(savedSlot)
                        }
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (conflict != null) AmberStreak else IndigoPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("timetable_dialog_confirm_button")
            ) {
                Text(if (conflict != null && showConflictOverrideWarning) "Save With Conflict" else confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
