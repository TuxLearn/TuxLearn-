package com.example.ui.screens.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MemoryRecallOutcome
import com.example.data.model.StudiedTopic
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkDialogSurface
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedError
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay

@Composable
fun SurpriseRevisionDialog(
    topic: StudiedTopic,
    onDismiss: () -> Unit,
    onCompleteChallenge: (topicId: Int, outcome: MemoryRecallOutcome) -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var isRevealed by remember { mutableStateOf(false) }
    var completedOutcome by remember { mutableStateOf<MemoryRecallOutcome?>(null) }
    var userNotes by remember { mutableStateOf("") }

    // 60-second countdown timer
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        if (secondsLeft == 0 && !isRevealed) {
            // Auto-reveal when time is up
            isRevealed = true
            isTimerRunning = false
        }
    }

    val progress = secondsLeft / 60f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")
    val timerColor = when {
        secondsLeft > 30 -> CyanSecondary
        secondsLeft > 10 -> AmberStreak
        else -> RedError
    }

    Dialog(
        onDismissRequest = {
            // Dismissing without completing awards 0 XP (Strict Requirement 11)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("surprise_revision_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkDialogSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurpleAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Surprise Revision",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )
                            Text(
                                text = "60-Second Active Recall Challenge",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondaryDark
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_surprise_revision_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = timerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (secondsLeft > 0) "Time Remaining" else "Time's Up!",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondaryDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Text(
                                text = "${secondsLeft}s",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = timerColor
                                ),
                                modifier = Modifier.testTag("surprise_timer_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = timerColor,
                            trackColor = DarkDialogSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Concept & Recall Question Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PurpleAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PurpleAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PurpleAccent.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = topic.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PurpleAccent,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberStreak.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "+35 XP upon completion",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberStreak,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = topic.topic,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = topic.recallQuestion.ifBlank {
                                "Without looking at your notes, state the main function and key mechanism of ${topic.topic}."
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.testTag("surprise_question_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mental Scratchpad
                if (!isRevealed) {
                    OutlinedTextField(
                        value = userNotes,
                        onValueChange = { userNotes = it },
                        placeholder = { Text("Quick mental notes / thoughts (optional)...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("surprise_scratchpad_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = DarkCardSurface,
                            unfocusedContainerColor = DarkCardSurface,
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = TextSecondaryDark.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isRevealed = true
                            isTimerRunning = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("surprise_reveal_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reveal Correct Answer",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                } else {
                    // Solution & Self-Rating Section
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkCardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("surprise_answer_container")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GreenSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "CORRECT ANSWER",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GreenSuccess
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = topic.correctAnswer.ifBlank {
                                            "Essential principles of ${topic.topic}."
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                    )

                                    if (topic.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = topic.explanation,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondaryDark,
                                                lineHeight = 18.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "How well did you recall it within 60s?",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Three recall response buttons
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SurpriseRateButton(
                                    title = "I Remembered",
                                    subtitle = "Fully recalled accurately (+35 XP)",
                                    icon = Icons.Default.CheckCircle,
                                    color = GreenSuccess,
                                    isSelected = completedOutcome == MemoryRecallOutcome.REMEMBERED,
                                    testTag = "surprise_opt_remembered",
                                    onClick = {
                                        completedOutcome = MemoryRecallOutcome.REMEMBERED
                                        onCompleteChallenge(topic.id, MemoryRecallOutcome.REMEMBERED)
                                    }
                                )

                                SurpriseRateButton(
                                    title = "Partially Remembered",
                                    subtitle = "Needed a quick hint (+35 XP)",
                                    icon = Icons.Default.HourglassEmpty,
                                    color = AmberStreak,
                                    isSelected = completedOutcome == MemoryRecallOutcome.PARTIALLY_REMEMBERED,
                                    testTag = "surprise_opt_partial",
                                    onClick = {
                                        completedOutcome = MemoryRecallOutcome.PARTIALLY_REMEMBERED
                                        onCompleteChallenge(topic.id, MemoryRecallOutcome.PARTIALLY_REMEMBERED)
                                    }
                                )

                                SurpriseRateButton(
                                    title = "I Forgot",
                                    subtitle = "Could not recall • Queued for revision (+35 XP)",
                                    icon = Icons.Default.Warning,
                                    color = RedError,
                                    isSelected = completedOutcome == MemoryRecallOutcome.FORGOT,
                                    testTag = "surprise_opt_forgot",
                                    onClick = {
                                        completedOutcome = MemoryRecallOutcome.FORGOT
                                        onCompleteChallenge(topic.id, MemoryRecallOutcome.FORGOT)
                                    }
                                )
                            }
                        }
                    }
                }

                if (completedOutcome != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("surprise_finish_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Finish & Collect +35 XP",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("surprise_cancel_btn")
                        ) {
                            Text(
                                text = "Cancel Challenge (0 XP)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondaryDark
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurpriseRateButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) color else color.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
