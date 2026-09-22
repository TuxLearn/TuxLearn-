package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardItem
import com.example.data.model.MemoryRecallOutcome
import com.example.data.model.StudiedTopic
import com.example.ui.screens.dialogs.MarkStudiedTopicDialog
import com.example.ui.screens.dialogs.MemoryTestDialog
import com.example.ui.screens.dialogs.SurpriseRevisionDialog
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkDialogSurface
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryDark
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedError
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun RevisionScreen(
    flashcards: List<FlashcardItem>,
    studiedTopics: List<StudiedTopic> = emptyList(),
    onMarkFlashcard: (FlashcardItem, Boolean) -> Unit,
    onAddFlashcard: (String, String, String, String) -> Unit,
    onDeleteFlashcard: (FlashcardItem) -> Unit,
    onSaveStudiedTopic: (subject: String, topic: String, durationMinutes: Int) -> Unit = { _, _, _ -> },
    onAnswerMemoryTest: (topicId: Int, outcome: MemoryRecallOutcome) -> Unit = { _, _ -> },
    onCompleteSurpriseRevision: (topicId: Int, outcome: MemoryRecallOutcome) -> Unit = { _, _ -> },
    onDeleteStudiedTopic: (StudiedTopic) -> Unit = {}
) {
    // 0 = Memory Tests (Active Recall Spaced Repetition), 1 = Flashcards Deck
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog states
    var activeTestTopic by remember { mutableStateOf<StudiedTopic?>(null) }
    var surpriseTopic by remember { mutableStateOf<StudiedTopic?>(null) }
    var showMarkStudiedDialog by remember { mutableStateOf(false) }
    var showAddFlashcardDialog by remember { mutableStateOf(false) }

    // Memory Test Filters
    var memoryStrengthFilter by remember { mutableStateOf("All") }
    var memorySubjectFilter by remember { mutableStateOf("All") }

    // Flashcards state
    var selectedFlashcardSubject by remember { mutableStateOf("All") }
    var currentFlashcardIndex by remember { mutableIntStateOf(0) }
    var isFlashcardFlipped by remember { mutableStateOf(false) }
    var showFlashcardHint by remember { mutableStateOf(false) }

    val flashcardSubjects = remember(flashcards) {
        listOf("All") + flashcards.map { it.subject }.distinct()
    }
    val filteredCards = remember(flashcards, selectedFlashcardSubject) {
        if (selectedFlashcardSubject == "All") flashcards
        else flashcards.filter { it.subject.equals(selectedFlashcardSubject, ignoreCase = true) }
    }
    val activeFlashcard = if (filteredCards.isNotEmpty()) {
        filteredCards[currentFlashcardIndex.coerceIn(0, filteredCards.size - 1)]
    } else null

    // Memory test topics filtering & sorting
    // Requirement 8: "Topics marked 'I Forgot' should appear more frequently in future Memory Tests."
    // StudoraRepository orders by priorityScore DESC, and we maintain this priority sort
    val memorySubjects = remember(studiedTopics) {
        listOf("All") + studiedTopics.map { it.subject }.distinct()
    }
    val filteredTopics = remember(studiedTopics, memoryStrengthFilter, memorySubjectFilter) {
        studiedTopics.filter { topic ->
            val matchesStrength = when (memoryStrengthFilter) {
                "Needs Revision" -> topic.memoryStrength.equals("Needs Revision", ignoreCase = true)
                "Medium" -> topic.memoryStrength.equals("Medium", ignoreCase = true)
                "Strong" -> topic.memoryStrength.equals("Strong", ignoreCase = true)
                else -> true
            }
            val matchesSubject = memorySubjectFilter == "All" || topic.subject.equals(memorySubjectFilter, ignoreCase = true)
            matchesStrength && matchesSubject
        }
    }

    val needsRevisionCount = remember(studiedTopics) {
        studiedTopics.count { it.memoryStrength.equals("Needs Revision", ignoreCase = true) }
    }
    val mediumCount = remember(studiedTopics) {
        studiedTopics.count { it.memoryStrength.equals("Medium", ignoreCase = true) }
    }
    val strongCount = remember(studiedTopics) {
        studiedTopics.count { it.memoryStrength.equals("Strong", ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showMarkStudiedDialog = true },
                    containerColor = IndigoPrimary,
                    contentColor = TextPrimaryDark,
                    shape = CircleShape,
                    modifier = Modifier.testTag("memory_fab_add")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "I Studied This"
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { showAddFlashcardDialog = true },
                    containerColor = PurpleAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("revision_fab_add")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Flashcard"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Section Segmented Tabs (Memory Tests vs Flashcards)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IndigoPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) IndigoPrimary else PurpleAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revision_tab_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Memory Tests",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (needsRevisionCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedError,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$needsRevisionCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_memory_tests")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Flashcards (${flashcards.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_flashcards")
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                // ==============================
                // MEMORY TESTS SECTION (Requirements 1-13)
                // ==============================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("memory_test_list"),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Memory Health Metrics Summary Cards
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemoryMetricItem(
                                    count = "${studiedTopics.size}",
                                    label = "Total Studied",
                                    color = IndigoPrimary
                                )
                                MemoryMetricItem(
                                    count = "$strongCount",
                                    label = "Strong",
                                    color = GreenSuccess
                                )
                                MemoryMetricItem(
                                    count = "$mediumCount",
                                    label = "Medium",
                                    color = AmberStreak
                                )
                                MemoryMetricItem(
                                    count = "$needsRevisionCount",
                                    label = "Needs Revision",
                                    color = RedError,
                                    isWarning = needsRevisionCount > 0
                                )
                            }
                        }
                    }

                    // 2. Surprise Revision 60-Second Challenge Banner (Requirement 10 & 11)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = PurpleAccent.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(PurpleAccent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(20.dp)
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
                                                    color = TextSecondaryDark,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberStreak.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "+35 XP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = AmberStreak
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Give your brain a surprise test on a random previously studied concept! XP is awarded only upon completing the recall challenge.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondaryDark,
                                        lineHeight = 18.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        // Pick a topic for surprise revision (prioritize Needs Revision or highest priority)
                                        val candidate = studiedTopics.filter { it.memoryStrength == "Needs Revision" }.randomOrNull()
                                            ?: studiedTopics.maxByOrNull { it.priorityScore }
                                            ?: studiedTopics.firstOrNull()
                                        if (candidate != null) {
                                            surpriseTopic = candidate
                                        } else {
                                            showMarkStudiedDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_start_surprise_revision")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Start 60s Challenge",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    // 3. Action bar & filter chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Studied Topics & Tests (${filteredTopics.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            )

                            OutlinedButton(
                                onClick = { showMarkStudiedDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("revision_log_studied_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("I Studied This", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Strength filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = listOf(
                                "All",
                                "Needs Revision",
                                "Medium",
                                "Strong"
                            )
                            items(options) { opt ->
                                val count = when (opt) {
                                    "Needs Revision" -> needsRevisionCount
                                    "Medium" -> mediumCount
                                    "Strong" -> strongCount
                                    else -> studiedTopics.size
                                }
                                val isSelected = memoryStrengthFilter == opt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { memoryStrengthFilter = opt },
                                    label = { Text("$opt ($count)", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = TextPrimaryDark
                                    ),
                                    modifier = Modifier.testTag("filter_strength_$opt")
                                )
                            }
                        }

                        if (memorySubjects.size > 2) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(memorySubjects) { subj ->
                                    val isSelected = memorySubjectFilter == subj
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { memorySubjectFilter = subj },
                                        label = { Text(subj, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IndigoLight.copy(alpha = 0.3f),
                                            selectedLabelColor = TextPrimaryDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Studied Topic Cards list
                    if (filteredTopics.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No topics found in this filter",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap 'I Studied This' to log a topic you recently studied and schedule a memory test!",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondaryDark,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTopics, key = { it.id }) { topic ->
                            StudiedTopicCard(
                                topic = topic,
                                onTakeTest = { activeTestTopic = topic },
                                onDelete = { onDeleteStudiedTopic(topic) }
                            )
                        }
                    }
                }
            } else {
                // ==============================
                // FLASHCARDS DECK SECTION (Existing Flashcard View)
                // ==============================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header stats
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${flashcards.count { it.isMastered }} / ${flashcards.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GreenSuccess
                                    )
                                )
                                Text(
                                    text = "Cards Mastered",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+15 XP",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberStreak
                                    )
                                )
                                Text(
                                    text = "Per Recall",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Flashcard Flip",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                )
                                Text(
                                    text = "Deck Mode",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Subject filter chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(flashcardSubjects) { subject ->
                            FilterChip(
                                selected = selectedFlashcardSubject == subject,
                                onClick = {
                                    selectedFlashcardSubject = subject
                                    currentFlashcardIndex = 0
                                    isFlashcardFlipped = false
                                    showFlashcardHint = false
                                },
                                label = { Text(subject) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredCards.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No flashcards in $selectedFlashcardSubject",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the '+' button to add your first active recall card",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else if (activeFlashcard != null) {
                        // Flashcard Navigation Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Card ${currentFlashcardIndex + 1} of ${filteredCards.size}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (activeFlashcard.isMastered) GreenSuccess.copy(alpha = 0.15f)
                                else AmberStreak.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (activeFlashcard.isMastered) "✓ Mastered" else "⏳ In Review",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeFlashcard.isMastered) GreenSuccess else AmberStreak
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // The Interactive Flashcard
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFlashcardFlipped) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable {
                                    isFlashcardFlipped = !isFlashcardFlipped
                                }
                                .testTag("interactive_flashcard")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = getRevisionSubjectColor(activeFlashcard.subject).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = activeFlashcard.subject,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = getRevisionSubjectColor(activeFlashcard.subject),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Row {
                                        if (activeFlashcard.hint.isNotBlank()) {
                                            IconButton(
                                                onClick = { showFlashcardHint = !showFlashcardHint },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.HelpOutline,
                                                    contentDescription = "Hint",
                                                    tint = if (showFlashcardHint) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDeleteFlashcard(activeFlashcard) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                // Center content
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isFlashcardFlipped) "ANSWER" else "QUESTION",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFlashcardFlipped) GreenSuccess else IndigoPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = if (isFlashcardFlipped) activeFlashcard.answer else activeFlashcard.question,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 26.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    if (showFlashcardHint && activeFlashcard.hint.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = AmberStreak.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "💡 Hint: ${activeFlashcard.hint}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = AmberStreak,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }

                                // Bottom tap prompt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flip,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFlashcardFlipped) "Tap to see question" else "Tap card to reveal answer",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons: "Needs Practice" vs "Got It Right!"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onMarkFlashcard(activeFlashcard, false)
                                    if (currentFlashcardIndex < filteredCards.size - 1) {
                                        currentFlashcardIndex++
                                        isFlashcardFlipped = false
                                        showFlashcardHint = false
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("practice_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = AmberStreak,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Needs Practice")
                            }

                            Button(
                                onClick = {
                                    onMarkFlashcard(activeFlashcard, true)
                                    if (currentFlashcardIndex < filteredCards.size - 1) {
                                        currentFlashcardIndex++
                                        isFlashcardFlipped = false
                                        showFlashcardHint = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("got_it_right_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Got It Right! (+15 XP)")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Take Memory Test Dialog
    if (activeTestTopic != null) {
        MemoryTestDialog(
            topic = activeTestTopic!!,
            onDismiss = { activeTestTopic = null },
            onSelectOutcome = { topicId, outcome ->
                onAnswerMemoryTest(topicId, outcome)
            }
        )
    }

    // 2. 60-Second Surprise Revision Dialog
    if (surpriseTopic != null) {
        SurpriseRevisionDialog(
            topic = surpriseTopic!!,
            onDismiss = { surpriseTopic = null },
            onCompleteChallenge = { topicId, outcome ->
                onCompleteSurpriseRevision(topicId, outcome)
            }
        )
    }

    // 3. Mark Studied Topic Dialog ("I Studied This")
    if (showMarkStudiedDialog) {
        MarkStudiedTopicDialog(
            initialSubject = "Biology",
            initialTopic = "",
            initialDuration = 30,
            onDismiss = { showMarkStudiedDialog = false },
            onConfirmStudied = { subject, topic, duration ->
                onSaveStudiedTopic(subject, topic, duration)
                showMarkStudiedDialog = false
            }
        )
    }

    // 4. Add Flashcard Dialog
    if (showAddFlashcardDialog) {
        AddFlashcardDialog(
            onDismiss = { showAddFlashcardDialog = false },
            onConfirm = { sub, q, a, hint ->
                onAddFlashcard(sub, q, a, hint)
                showAddFlashcardDialog = false
            }
        )
    }
}

@Composable
private fun MemoryMetricItem(
    count: String,
    label: String,
    color: Color,
    isWarning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            if (isWarning) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun StudiedTopicCard(
    topic: StudiedTopic,
    onTakeTest: () -> Unit,
    onDelete: () -> Unit
) {
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

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studied_topic_card_${topic.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Priority Alert Banner if marked "I Forgot" in previous test (Requirement 8)
            if (topic.forgotCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RedError.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedError.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RedError,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "High Priority Recall • Marked 'I Forgot' in previous test",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = RedError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Top Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getRevisionSubjectColor(topic.subject).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = topic.subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = getRevisionSubjectColor(topic.subject),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Strength Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = strengthColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = strengthLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = strengthColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_topic_btn_${topic.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Topic Name
            Text(
                text = topic.topic,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date & Duration
            Text(
                text = "Studied: ${topic.studiedDateFormatted} • Duration: ${topic.studyDurationMinutes}m",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            )

            // Active recall preview question
            if (topic.recallQuestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkCardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberStreak,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = topic.recallQuestion,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimaryDark,
                                lineHeight = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Take Memory Test Button
            Button(
                onClick = onTakeTest,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_take_memory_test_${topic.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Take Memory Test",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
            }
        }
    }
}

private fun getRevisionSubjectColor(subject: String): Color {
    return when (subject.lowercase().trim()) {
        "mathematics", "math" -> IndigoPrimary
        "physics" -> CyanSecondary
        "chemistry" -> PurpleAccent
        "biology" -> GreenSuccess
        "computer science", "cs" -> IndigoLight
        "literature" -> AmberStreak
        else -> Color(0xFF64748B)
    }
}

@Composable
fun AddFlashcardDialog(
    onDismiss: () -> Unit,
    onConfirm: (subject: String, question: String, answer: String, hint: String) -> Unit
) {
    var subject by remember { mutableStateOf("Mathematics") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Literature", "History")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Flashcard",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Concept") },
                    placeholder = { Text("e.g. What is the derivative of e^x?") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_question_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer / Formula") },
                    placeholder = { Text("e.g. e^x") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_answer_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Memory Hint (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onConfirm(subject, question.trim(), answer.trim(), hint.trim())
                    }
                },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_flashcard_button")
            ) {
                Text("Save Card")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
