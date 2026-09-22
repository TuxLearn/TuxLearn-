package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyNote
import com.example.ui.screens.dialogs.MarkStudiedTopicDialog
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Predefined subject organization categories per requirements
val SUPPORTED_NOTE_SUBJECTS = listOf(
    "Physics",
    "Chemistry",
    "Biology",
    "Mathematics",
    "Computer Science",
    "Other"
)

@Composable
fun NotesScreen(
    notes: List<StudyNote>,
    onAddNote: (title: String, subject: String, chapter: String, content: String, tags: String, isFavorite: Boolean) -> Unit,
    onUpdateNote: (StudyNote) -> Unit,
    onDeleteNote: (StudyNote) -> Unit,
    onToggleFavorite: (StudyNote) -> Unit = {},
    onMarkTopicStudied: (subject: String, topic: String, durationMinutes: Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Notes, 1 = Imported Docs (PDFs)
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNote?>(null) }
    var viewingNote by remember { mutableStateOf<StudyNote?>(null) }
    var notePendingDelete by remember { mutableStateOf<StudyNote?>(null) }
    var noteToMarkStudied by remember { mutableStateOf<StudyNote?>(null) }

    // Subject Filter Categories: "All", "⭐ Favorites", + standard subjects
    val filterCategories = remember {
        listOf("All", "⭐ Favorites") + SUPPORTED_NOTE_SUBJECTS
    }

    // Partition notes vs imported PDFs/documents
    val (importedDocs, standardNotes) = remember(notes) {
        notes.partition { it.tags.contains("PDF", ignoreCase = true) || it.tags.contains("Document", ignoreCase = true) }
    }

    // Filter and search logic for active tab
    val displayedNotes = if (currentTab == 0) notes else importedDocs

    val filteredNotes = remember(displayedNotes, searchQuery, selectedFilter) {
        displayedNotes.filter { note ->
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "⭐ Favorites" -> note.isFavorite
                else -> note.subject.equals(selectedFilter, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.subject.contains(searchQuery, ignoreCase = true) ||
                    note.chapter.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    note.tags.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedByDescending { it.updatedAt }
    }

    // Recent notes: 4 most recently created or updated
    val recentNotes = remember(notes) {
        notes.sortedByDescending { it.updatedAt }.take(4)
    }

    val totalFavorites = remember(notes) { notes.count { it.isFavorite } }
    val uniqueSubjectsCount = remember(notes) { notes.map { it.subject }.distinct().size }

    Scaffold(
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF Document Import Button
                FloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = PurpleAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("notes_fab_import_pdf")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.FileUpload, contentDescription = "Import PDF / Document")
                        Text("Import PDF", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Add Note Button
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("notes_fab_add")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create Note")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Notes & PDF Organizer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Organize class summaries, formulas & imported PDF documents",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Dashboard Metrics Summary
            NotesDashboardMetrics(
                totalNotes = notes.size,
                totalFavorites = totalFavorites,
                activeSubjects = uniqueSubjectsCount,
                importedDocsCount = importedDocs.size
            )

            // Tabs: All Notes vs Imported Documents
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IndigoPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("All Notes (${notes.size})")
                        }
                    },
                    modifier = Modifier.testTag("notes_tab_all")
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Imported PDFs (${importedDocs.size})")
                        }
                    },
                    modifier = Modifier.testTag("notes_tab_imported_docs")
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (currentTab == 0) "Search title, subject, chapter, content..." else "Search imported PDFs & docs...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("notes_search_input")
            )

            // Subject & Favorites Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { category ->
                    val count = when (category) {
                        "All" -> displayedNotes.size
                        "⭐ Favorites" -> displayedNotes.count { it.isFavorite }
                        else -> displayedNotes.count { it.subject.equals(category, ignoreCase = true) }
                    }
                    val isSelected = selectedFilter == category

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = {
                            Text(
                                text = "$category ($count)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = IndigoPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("notes_filter_${category.lowercase().replace(" ", "_").replace("⭐_", "")}")
                    )
                }
            }

            // Main Content Area
            if (filteredNotes.isEmpty()) {
                if (notes.isEmpty()) {
                    NotesEmptyState(
                        title = "No Study Notes Yet",
                        message = "Create quick revision notes, formulas, and cheatsheets, or import PDF lecture summaries.",
                        buttonText = "Create First Note",
                        onButtonClick = { showCreateDialog = true },
                        buttonTestTag = "notes_empty_create_btn"
                    )
                } else if (currentTab == 1 && importedDocs.isEmpty()) {
                    NotesEmptyState(
                        title = "No Imported Documents Yet",
                        message = "Import PDF textbooks, homework sheets, and syllabi from your device to organize them by subject.",
                        buttonText = "Import PDF Now",
                        onButtonClick = { showImportDialog = true },
                        buttonTestTag = "notes_empty_import_pdf_btn"
                    )
                } else {
                    NotesEmptyState(
                        title = "No Matching Notes Found",
                        message = "No notes matched your search \"$searchQuery\" in $selectedFilter.",
                        buttonText = "Clear Search & Filter",
                        onButtonClick = {
                            searchQuery = ""
                            selectedFilter = "All"
                        },
                        buttonTestTag = "notes_clear_filter_btn"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recent Notes Horizontal Carousel (shown on "All" filter when not searching in Notes tab)
                    if (currentTab == 0 && selectedFilter == "All" && searchQuery.isBlank() && recentNotes.isNotEmpty()) {
                        item {
                            RecentNotesSection(
                                recentNotes = recentNotes,
                                onOpenNote = { viewingNote = it },
                                onToggleFavorite = { note ->
                                    if (onToggleFavorite != {}) {
                                        onToggleFavorite(note)
                                    } else {
                                        onUpdateNote(note.copy(isFavorite = !note.isFavorite))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Notes (${filteredNotes.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            }
                        }
                    } else if (searchQuery.isNotBlank() || selectedFilter != "All") {
                        item {
                            Text(
                                text = "Results in $selectedFilter (${filteredNotes.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Notes & PDF Cards
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onOpen = { viewingNote = note },
                            onEdit = { editingNote = note },
                            onDelete = { notePendingDelete = note },
                            onMarkStudied = { noteToMarkStudied = note },
                            onToggleFavorite = {
                                if (onToggleFavorite != {}) {
                                    onToggleFavorite(note)
                                } else {
                                    onUpdateNote(note.copy(isFavorite = !note.isFavorite))
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (noteToMarkStudied != null) {
        val note = noteToMarkStudied!!
        MarkStudiedTopicDialog(
            initialSubject = note.subject,
            initialTopic = if (note.chapter.isNotBlank()) "${note.title} (${note.chapter})" else note.title,
            initialDuration = 30,
            onDismiss = { noteToMarkStudied = null },
            onConfirmStudied = { subject, topic, duration ->
                onMarkTopicStudied(subject, topic, duration)
                noteToMarkStudied = null
            }
        )
    }

    // Create Note Dialog
    if (showCreateDialog) {
        NoteEditorDialog(
            note = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, sub, chapter, content, tags, isFav ->
                onAddNote(title, sub, chapter, content, tags, isFav)
                showCreateDialog = false
            }
        )
    }

    // Import Document / PDF Dialog
    if (showImportDialog) {
        ImportDocumentDialog(
            onDismiss = { showImportDialog = false },
            onSaveDocument = { title, subject, chapter, summary, tags, isFav ->
                onAddNote(title, subject, chapter, summary, tags, isFav)
                showImportDialog = false
                currentTab = 1 // Switch to imported docs tab to see the newly imported document
            }
        )
    }

    // Edit Note Dialog
    editingNote?.let { note ->
        NoteEditorDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { title, sub, chapter, content, tags, isFav ->
                onUpdateNote(
                    note.copy(
                        title = title,
                        subject = sub,
                        chapter = chapter,
                        content = content,
                        tags = tags,
                        isFavorite = isFav,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                editingNote = null
            }
        )
    }

    // View Note Dialog
    viewingNote?.let { note ->
        NoteDetailDialog(
            note = note,
            onDismiss = { viewingNote = null },
            onEdit = {
                val target = note
                viewingNote = null
                editingNote = target
            },
            onDelete = {
                val target = note
                viewingNote = null
                notePendingDelete = target
            },
            onMarkStudied = {
                val target = note
                viewingNote = null
                noteToMarkStudied = target
            },
            onToggleFavorite = {
                val updated = note.copy(isFavorite = !note.isFavorite)
                if (onToggleFavorite != {}) {
                    onToggleFavorite(note)
                } else {
                    onUpdateNote(updated)
                }
                viewingNote = updated
            }
        )
    }

    // Delete Confirmation Dialog
    notePendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { notePendingDelete = null },
            title = {
                Text(
                    text = "Delete Study Note?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${note.title}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(note)
                        notePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("note_confirm_delete_btn")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { notePendingDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

// Backwards-compatible overload
@Composable
fun NotesScreen(
    notes: List<StudyNote>,
    onAddNote: (String, String, String, String) -> Unit,
    onUpdateNote: (StudyNote) -> Unit,
    onDeleteNote: (StudyNote) -> Unit
) {
    NotesScreen(
        notes = notes,
        onAddNote = { title, sub, _, content, tags, _ -> onAddNote(title, sub, content, tags) },
        onUpdateNote = onUpdateNote,
        onDeleteNote = onDeleteNote,
        onToggleFavorite = { onUpdateNote(it.copy(isFavorite = !it.isFavorite)) }
    )
}

@Composable
private fun NotesDashboardMetrics(
    totalNotes: Int,
    totalFavorites: Int,
    activeSubjects: Int,
    importedDocsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricSummaryCard(
            modifier = Modifier.weight(1f),
            label = "Total Notes",
            value = "$totalNotes",
            icon = Icons.Filled.Description,
            iconTint = IndigoPrimary
        )
        MetricSummaryCard(
            modifier = Modifier.weight(1f),
            label = "Favorites",
            value = "$totalFavorites",
            icon = Icons.Filled.Star,
            iconTint = AmberStreak
        )
        MetricSummaryCard(
            modifier = Modifier.weight(1f),
            label = "Subjects",
            value = "$activeSubjects",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = PurpleAccent
        )
        MetricSummaryCard(
            modifier = Modifier.weight(1f),
            label = "PDF Docs",
            value = "$importedDocsCount",
            icon = Icons.Filled.PictureAsPdf,
            iconTint = Color(0xFFEF4444)
        )
    }
}

@Composable
private fun MetricSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentNotesSection(
    recentNotes: List<StudyNote>,
    onOpenNote: (StudyNote) -> Unit,
    onToggleFavorite: (StudyNote) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Recent Notes",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(recentNotes, key = { "recent_${it.id}" }) { note ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            getSubjectNoteColor(note.subject).copy(alpha = 0.35f)
                        )
                    ),
                    modifier = Modifier
                        .width(230.dp)
                        .clickable { onOpenNote(note) }
                        .testTag("recent_note_card_${note.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = getSubjectNoteColor(note.subject).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = getSubjectNoteColor(note.subject),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { onToggleFavorite(note) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (note.isFavorite) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (note.chapter.isNotBlank()) {
                            Text(
                                text = note.chapter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Edited ${formatRelativeTime(note.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    note: StudyNote,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkStudied: () -> Unit = {},
    onToggleFavorite: () -> Unit
) {
    val isPdfOrDoc = note.tags.contains("PDF", ignoreCase = true) || note.tags.contains("Document", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Subject badge, Chapter pill, Star and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getSubjectNoteColor(note.subject).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = note.subject,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = getSubjectNoteColor(note.subject),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (isPdfOrDoc) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PDF Document",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    if (note.chapter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = note.chapter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("note_btn_favorite_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (note.isFavorite) "Remove Favorite" else "Add Favorite",
                            tint = if (note.isFavorite) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("note_btn_edit_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("note_btn_delete_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isPdfOrDoc) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("note_title_${note.id}")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content Snippet
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            if (note.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("note_tags_${note.id}")
                ) {
                    note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = tag,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom timestamp & word count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updated ${formatRelativeTime(note.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )

                val wordCount = note.content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                Text(
                    text = "$wordCount words",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IndigoPrimary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .clickable { onMarkStudied() }
                    .testTag("note_studied_${note.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(13.dp)
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
    }
}

@Composable
private fun NotesEmptyState(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    buttonTestTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(IndigoPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag(buttonTestTag)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText)
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    note: StudyNote?,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, chapter: String, content: String, tags: String, isFavorite: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(note?.title ?: "") }
    var subject by remember { mutableStateOf(note?.subject ?: "Physics") }
    var chapter by remember { mutableStateOf(note?.chapter ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }
    var isFavorite by remember { mutableStateOf(note?.isFavorite ?: false) }
    var importedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, extracted) = importDocumentDetails(context, it)
            importedFileName = name
            if (title.isBlank()) {
                title = name.substringBeforeLast(".")
            }
            if (extracted.isNotBlank()) {
                content = if (content.isBlank()) extracted else "$content\n\n$extracted"
            }
            if (!tags.contains("Document", ignoreCase = true)) {
                tags = if (tags.isBlank()) "Document" else "$tags, Document"
            }
        }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (note == null) "Create Note" else "Edit Note",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Subject selection chips
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(SUPPORTED_NOTE_SUBJECTS) { item ->
                        val isSelected = subject == item
                        FilterChip(
                            selected = isSelected,
                            onClick = { subject = item },
                            label = { Text(item) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getSubjectNoteColor(item).copy(alpha = 0.2f),
                                selectedLabelColor = getSubjectNoteColor(item)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("notes_input_subject_${item.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    placeholder = { Text("e.g. Newton's Laws & Friction") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chapter / Topic Input
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter / Topic") },
                    placeholder = { Text("e.g. Chapter 3: Dynamics") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_chapter")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Attach/Import Document Button inside editor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PurpleAccent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*")) }
                        .testTag("notes_editor_attach_doc_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = importedFileName ?: "Attach PDF or Document",
                                style = MaterialTheme.typography.bodySmall.copy(color = PurpleAccent, fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("Browse", style = MaterialTheme.typography.labelSmall.copy(color = PurpleAccent, fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content *") },
                    placeholder = { Text("Formulas, key concepts, derivations, summaries...") },
                    minLines = 5,
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_content")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags Input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("e.g. ExamPrep, Formulas, Unit1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_tags")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Favorite Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { isFavorite = !isFavorite }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("notes_favorite_toggle_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Mark as Favorite ⭐",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberStreak),
                        modifier = Modifier.testTag("notes_input_favorite")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title.trim(), subject, chapter.trim(), content.trim(), tags.trim(), isFavorite)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("notes_btn_save")
            ) {
                Text(if (note == null) "Create Note" else "Save Changes")
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

/**
 * Dedicated dialog for importing a PDF or document into the student's organizer.
 */
@Composable
fun ImportDocumentDialog(
    onDismiss: () -> Unit,
    onSaveDocument: (title: String, subject: String, chapter: String, summary: String, tags: String, isFavorite: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }
    var chapter by remember { mutableStateOf("") }
    var notesSummary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("PDF, Textbook") }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<String?>(null) }
    var localFilePath by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, extracted, sizeStr, savedPath) = copyAndReadDocument(context, it)
            selectedFileName = name
            selectedFileSize = sizeStr
            localFilePath = savedPath
            if (title.isBlank()) {
                title = name.substringBeforeLast(".")
            }
            if (notesSummary.isBlank() && extracted.isNotBlank()) {
                notesSummary = extracted
            }
        }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                Text("Import PDF / Document", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // File Picker trigger card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*")) }
                        .testTag("import_pdf_pick_file_button")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selectedFileName != null) Icons.Filled.PictureAsPdf else Icons.Filled.FileUpload,
                            contentDescription = null,
                            tint = if (selectedFileName != null) Color(0xFFEF4444) else IndigoPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedFileName ?: "Tap to choose a PDF or document",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFileName != null) MaterialTheme.colorScheme.onSurface else IndigoPrimary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedFileSize != null) {
                            Text(
                                text = "File Size: $selectedFileSize",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Selector
                Text(
                    text = "Assign Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(SUPPORTED_NOTE_SUBJECTS) { item ->
                        val isSelected = subject == item
                        FilterChip(
                            selected = isSelected,
                            onClick = { subject = item },
                            label = { Text(item) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getSubjectNoteColor(item).copy(alpha = 0.2f),
                                selectedLabelColor = getSubjectNoteColor(item)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Document Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title *") },
                    placeholder = { Text("e.g. Kinematics Lecture Slides") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_pdf_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chapter
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter / Unit") },
                    placeholder = { Text("e.g. Unit 2: Motion in 2D") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Summary / Notes
                OutlinedTextField(
                    value = notesSummary,
                    onValueChange = { notesSummary = it },
                    label = { Text("Document Description & Key Points *") },
                    placeholder = { Text("Add key formulas, syllabus pointers or study notes for this PDF...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_pdf_summary_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { selectedFileName ?: "Imported Document" }
                    val finalContent = notesSummary.ifBlank {
                        "File: ${selectedFileName ?: "Document"}\nSize: ${selectedFileSize ?: "N/A"}\nStored locally on device."
                    }
                    val finalTags = if (tags.contains("PDF", ignoreCase = true)) tags else "$tags, PDF"
                    onSaveDocument(finalTitle.trim(), subject, chapter.trim(), finalContent.trim(), finalTags.trim(), isFavorite)
                },
                enabled = title.isNotBlank() || selectedFileName != null,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("import_pdf_confirm_button")
            ) {
                Text("Save to Organizer")
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

@Composable
fun NoteDetailDialog(
    note: StudyNote,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkStudied: () -> Unit = {},
    onToggleFavorite: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copyNoticeVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val isPdfOrDoc = note.tags.contains("PDF", ignoreCase = true) || note.tags.contains("Document", ignoreCase = true)

    val wordCount = remember(note.content) {
        note.content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val charCount = note.content.length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = getSubjectNoteColor(note.subject).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = note.subject,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = getSubjectNoteColor(note.subject),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (isPdfOrDoc) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "PDF Document",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        if (note.chapter.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = note.chapter,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("note_detail_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) AmberStreak else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Updated: ${formatFullDate(note.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "$wordCount words ($charCount chars)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Content display with rich background
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontFamily = if (note.subject == "Computer Science" || note.content.contains("•") || note.content.contains("∫")) {
                                FontFamily.Monospace
                            } else {
                                FontFamily.Default
                            }
                        ),
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (note.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = IndigoPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = copyNoticeVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = GreenSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ Copied to clipboard",
                            style = MaterialTheme.typography.labelSmall.copy(color = GreenSuccess),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar inside viewer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("${note.title}\n\n${note.content}"))
                            copyNoticeVisible = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMarkStudied,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("note_detail_studied_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I Studied This")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Subject color mapping helper
fun getSubjectNoteColor(subject: String): Color {
    return when (subject.lowercase().trim()) {
        "physics" -> Color(0xFF3B82F6) // Blue
        "chemistry" -> Color(0xFF14B8A6) // Teal
        "biology" -> Color(0xFF10B981) // Emerald Green
        "mathematics" -> Color(0xFFF59E0B) // Amber
        "computer science" -> Color(0xFFA855F7) // Purple
        else -> Color(0xFF64748B) // Slate Gray
    }
}

// Formats relative time ("2m ago", "1h ago", "Yesterday", "MMM dd")
fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    if (diff < 60_000L) return "Just now"
    if (diff < 3600_000L) return "${diff / 60_000L}m ago"
    if (diff < 86400_000L) return "${diff / 3600_000L}h ago"
    if (diff < 86400_000L * 2) return "Yesterday"
    if (diff < 86400_000L * 7) return "${diff / 86400_000L}d ago"
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

// Formats full date
fun formatFullDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

// Helper to query filename from Uri
private fun queryFileName(context: Context, uri: Uri): String {
    var name = "Document"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: "Document"
            }
        }
    } catch (_: Exception) {}
    return name
}

// Helper to import document text or metadata
private fun importDocumentDetails(context: Context, uri: Uri): Pair<String, String> {
    val name = queryFileName(context, uri)
    var content = ""
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            if (name.endsWith(".txt", ignoreCase = true)) {
                content = stream.bufferedReader().use { it.readText() }
            } else {
                content = "Imported file: $name\nDocument saved for student review."
            }
        }
    } catch (e: Exception) {
        content = "File: $name (Imported document)"
    }
    return Pair(name, content)
}

// Data tuple helper for document import
data class DocumentImportResult(
    val fileName: String,
    val extractedContent: String,
    val fileSizeFormatted: String,
    val savedLocalPath: String
)

// Copies document to local app storage for permanent access and reads snippet
private fun copyAndReadDocument(context: Context, uri: Uri): DocumentImportResult {
    val fileName = queryFileName(context, uri)
    var fileSize = 0L
    var extractedText = ""

    val docsDir = File(context.filesDir, "imported_docs")
    if (!docsDir.exists()) docsDir.mkdirs()
    val localFile = File(docsDir, "${System.currentTimeMillis()}_$fileName")

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    fileSize += read
                }
            }
        }
        if (fileName.endsWith(".txt", ignoreCase = true)) {
            extractedText = localFile.readText().take(2000)
        } else {
            extractedText = "PDF Document: $fileName\nSize: ${formatFileSize(fileSize)}\nOrganized locally for offline study access."
        }
    } catch (e: Exception) {
        extractedText = "Document: $fileName\nOrganized locally for offline study access."
    }

    return DocumentImportResult(
        fileName = fileName,
        extractedContent = extractedText,
        fileSizeFormatted = formatFileSize(fileSize),
        savedLocalPath = localFile.absolutePath
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}
