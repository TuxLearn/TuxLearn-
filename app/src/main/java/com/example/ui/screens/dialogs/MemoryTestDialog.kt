package com.example.ui.screens.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.theme.RedError
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun MemoryTestDialog(
    topic: StudiedTopic,
    onDismiss: () -> Unit,
    onSelectOutcome: (topicId: Int, outcome: MemoryRecallOutcome) -> Unit
) {
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var selectedOutcome by remember { mutableStateOf<MemoryRecallOutcome?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("memory_test_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkDialogSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Top header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = IndigoLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Memory Test",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )
                            Text(
                                text = "Active Recall Practice",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondaryDark
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_memory_test_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Topic & Subject Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Subject badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = IndigoPrimary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = topic.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = IndigoLight,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Current Memory Strength badge
                            val strengthColor: Color = when (topic.memoryStrength) {
                                "Strong" -> GreenSuccess
                                "Medium" -> AmberStreak
                                else -> RedError
                            }
                            val strengthLabel: String = when (topic.memoryStrength) {
                                "Strong" -> "Strong"
                                "Medium" -> "Medium"
                                else -> "Needs Revision"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = strengthColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, strengthColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Current: $strengthLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = strengthColor,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Studied: ${topic.studiedDateFormatted} • Duration: ${topic.studyDurationMinutes}m",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Active Recall Question Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IndigoPrimary.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_question_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AmberStreak,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ACTIVE RECALL PROMPT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberStreak,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = topic.recallQuestion.ifBlank {
                                "Without looking at your notes, what is the core mechanism and key definition of ${topic.topic}?"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.testTag("memory_question_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "💡 Try to mentally retrieve the answer or state it aloud before revealing the solution.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryDark,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reveal Answer Button / Answer Display
                if (!isAnswerRevealed) {
                    Button(
                        onClick = { isAnswerRevealed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("memory_reveal_answer_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reveal Answer & Explanation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                    }
                } else {
                    // Answer & Explanation Card
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkCardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("memory_answer_container")
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GreenSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "CORRECT ANSWER",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GreenSuccess,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = topic.correctAnswer.ifBlank {
                                            "Core principles and key formulas for ${topic.topic}."
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            lineHeight = 22.sp
                                        ),
                                        modifier = Modifier.testTag("memory_answer_text")
                                    )

                                    if (topic.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Explanation:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondaryDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = topic.explanation,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondaryDark,
                                                lineHeight = 20.sp
                                            ),
                                            modifier = Modifier.testTag("memory_explanation_text")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Recall Self-Evaluation Options
                            Text(
                                text = "How well did you recall this concept?",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Three recall response buttons: I Remembered, Partially Remembered, I Forgot
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. I Remembered (Strong)
                                RecallOptionButton(
                                    title = "I Remembered",
                                    subtitle = "Mastered • Strength: Strong (+25 XP)",
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = GreenSuccess,
                                    isSelected = selectedOutcome == MemoryRecallOutcome.REMEMBERED,
                                    testTag = "memory_opt_remembered",
                                    onClick = {
                                        selectedOutcome = MemoryRecallOutcome.REMEMBERED
                                        onSelectOutcome(topic.id, MemoryRecallOutcome.REMEMBERED)
                                    }
                                )

                                // 2. Partially Remembered (Medium)
                                RecallOptionButton(
                                    title = "Partially Remembered",
                                    subtitle = "Some hesitation • Strength: Medium (+20 XP)",
                                    icon = Icons.Default.HourglassEmpty,
                                    accentColor = AmberStreak,
                                    isSelected = selectedOutcome == MemoryRecallOutcome.PARTIALLY_REMEMBERED,
                                    testTag = "memory_opt_partial",
                                    onClick = {
                                        selectedOutcome = MemoryRecallOutcome.PARTIALLY_REMEMBERED
                                        onSelectOutcome(topic.id, MemoryRecallOutcome.PARTIALLY_REMEMBERED)
                                    }
                                )

                                // 3. I Forgot (Needs Revision - higher frequency)
                                RecallOptionButton(
                                    title = "I Forgot",
                                    subtitle = "Could not recall • High priority test scheduled (+15 XP)",
                                    icon = Icons.Default.Warning,
                                    accentColor = RedError,
                                    isSelected = selectedOutcome == MemoryRecallOutcome.FORGOT,
                                    testTag = "memory_opt_forgot",
                                    onClick = {
                                        selectedOutcome = MemoryRecallOutcome.FORGOT
                                        onSelectOutcome(topic.id, MemoryRecallOutcome.FORGOT)
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedOutcome != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("memory_test_done_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecallOptionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) accentColor else accentColor.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
