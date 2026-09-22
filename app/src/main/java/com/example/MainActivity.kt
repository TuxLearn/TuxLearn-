package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.components.AllScreensSheet
import com.example.ui.components.StudoraBottomBar
import com.example.ui.components.StudoraTopBar
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.AskExplainScreen
import com.example.ui.screens.PhotoSolveScreen
import com.example.ui.screens.ExamReadinessScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarksCalculatorScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.RecoveryPlanScreen
import com.example.ui.screens.SmartReminderScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.StudoraTheme
import com.example.ui.viewmodel.StudoraViewModel

class MainActivity : ComponentActivity() {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudoraTheme {
                val viewModel: StudoraViewModel = viewModel()
                LaunchedEffect(intent) {
                    val navScreen = intent?.getStringExtra("nav_screen")
                    if (navScreen == "PLANNER") {
                        viewModel.navigateTo(AppScreen.PLANNER)
                    } else if (navScreen == "REMINDERS") {
                        viewModel.navigateTo(AppScreen.REMINDERS)
                    }
                }
                StudoraApp(viewModel = viewModel)
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun StudoraApp(
    viewModel: StudoraViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val timetable by viewModel.timetable.collectAsState()
    val weakAreas by viewModel.weakAreas.collectAsState()
    val subjectMarks by viewModel.subjectMarks.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val challengeSolved by viewModel.surpriseChallengeSolved.collectAsState()
    val selectedChallengeOption by viewModel.selectedChallengeAnswer.collectAsState()
    val masterReminderEnabled by viewModel.masterReminderEnabled.collectAsState()
    val currentExplanation by viewModel.currentExplanation.collectAsState()
    val isExplaining by viewModel.isExplaining.collectAsState()
    val explanationHistory by viewModel.explanationHistory.collectAsState()
    val currentPhotoSolveResult by viewModel.currentPhotoSolveResult.collectAsState()
    val selectedPhotoUri by viewModel.selectedPhotoUri.collectAsState()
    val isPhotoSolving by viewModel.isPhotoSolving.collectAsState()
    val photoSolveScanningPhase by viewModel.photoSolveScanningPhase.collectAsState()
    val photoSolveHistory by viewModel.photoSolveHistory.collectAsState()
    val studyStatusAssessment by viewModel.studyStatusAssessment.collectAsState()
    val activeRecoveryPlan by viewModel.activeRecoveryPlan.collectAsState()
    val studiedTopics by viewModel.studiedTopics.collectAsState()
    val feedbackMessage by viewModel.userFeedback.collectAsState()

    var showAllScreensSheet by remember { mutableStateOf(false) }
    var isSplashScreenVisible by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        delay(1200)
        isSplashScreenVisible = false
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tuxlearn_root_scaffold"),
        topBar = {
            StudoraTopBar(
                currentScreen = currentScreen,
                profile = profile,
                onNavigate = { viewModel.navigateTo(it) },
                onOpenAllTools = { showAllScreensSheet = true }
            )
        },
        bottomBar = {
            StudoraBottomBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) },
                onOpenMore = { showAllScreensSheet = true }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 70.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        tasks = tasks,
                        exams = exams,
                        flashcards = flashcards,
                        profile = profile,
                        surpriseChallengeSolved = challengeSolved,
                        selectedChallengeAnswer = selectedChallengeOption,
                        studyStatusAssessment = studyStatusAssessment,
                        onToggleTask = { viewModel.toggleTask(it) },
                        onAddTask = { title, sub, date, mins, priority ->
                            viewModel.addTask(title, sub, date, mins, priority)
                        },
                        onSolveChallenge = { idx, correct ->
                            viewModel.solveSurpriseChallenge(idx, correct)
                        },
                        onNavigate = { viewModel.navigateTo(it) }
                    )

                    AppScreen.PLANNER -> PlannerScreen(
                        tasks = tasks,
                        profile = profile,
                        onToggleTask = { viewModel.toggleTask(it) },
                        onAddTask = { title, sub, date, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, xpReward ->
                            viewModel.addTask(title, sub, date, mins, priority, remEnabled, remMillis, remDateStr, remTimeStr, remRepeat, "Study session", xpReward)
                        },
                        onEditTask = { task ->
                            viewModel.updateTask(task)
                        },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onNavigateToReminders = { viewModel.navigateTo(AppScreen.REMINDERS) },
                        studyStatusAssessment = studyStatusAssessment,
                        onNavigateToRecoveryPlan = { viewModel.navigateTo(AppScreen.RECOVERY_PLAN) },
                        onMarkTopicStudied = { subject, topic, duration ->
                            viewModel.markTopicStudied(subject, topic, duration)
                        }
                    )

                    AppScreen.REMINDERS -> SmartReminderScreen(
                        tasks = tasks,
                        exams = exams,
                        masterReminderEnabled = masterReminderEnabled,
                        onToggleMaster = { viewModel.setMasterReminderEnabled(it) },
                        onToggleReminder = { viewModel.toggleReminderEnabled(it) },
                        onSnoozeReminder = { task, mins -> viewModel.snoozeReminder(task, mins) },
                        onRescheduleReminder = { task, epoch, dateStr, timeStr, repeat ->
                            viewModel.rescheduleReminder(task, epoch, dateStr, timeStr, repeat)
                        },
                        onUpdateReminder = { task, title, sub, dateStr, timeStr, epoch, repeat, category, enabled ->
                            viewModel.updateReminder(task, title, sub, dateStr, timeStr, epoch, repeat, category, enabled)
                        },
                        onMarkDone = { viewModel.toggleTask(it) },
                        onDeleteReminder = { viewModel.deleteTask(it) },
                        onAddReminder = { title, sub, date, time, epoch, repeat, category ->
                            viewModel.addTask(
                                title = title,
                                subject = sub,
                                dueDate = date,
                                minutes = 30,
                                priority = "Medium",
                                reminderEnabled = true,
                                reminderEpochMillis = epoch,
                                reminderDateFormatted = date,
                                reminderTimeFormatted = time,
                                reminderRepeat = repeat,
                                reminderCategory = category
                            )
                        },
                        onSendTestNotification = { viewModel.sendTestNotification(it) },
                        onScheduleTestNotificationIn10Seconds = { viewModel.scheduleTestNotificationIn10Seconds() },
                        onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )

                    AppScreen.REVISION -> RevisionScreen(
                        flashcards = flashcards,
                        studiedTopics = studiedTopics,
                        onMarkFlashcard = { card, mastered ->
                            viewModel.markFlashcard(card, mastered)
                        },
                        onAddFlashcard = { sub, q, a, hint ->
                            viewModel.addFlashcard(sub, q, a, hint)
                        },
                        onDeleteFlashcard = { viewModel.deleteFlashcard(it) },
                        onSaveStudiedTopic = { subject, topic, duration ->
                            viewModel.markTopicStudied(subject, topic, duration)
                        },
                        onAnswerMemoryTest = { topicId, outcome ->
                            viewModel.answerMemoryTest(topicId, outcome)
                        },
                        onCompleteSurpriseRevision = { topicId, outcome ->
                            viewModel.completeSurpriseRevision(topicId, outcome)
                        },
                        onDeleteStudiedTopic = { viewModel.deleteStudiedTopic(it) }
                    )

                    AppScreen.READINESS -> ExamReadinessScreen(
                        exams = exams,
                        weakAreas = weakAreas,
                        tasks = tasks,
                        flashcards = flashcards,
                        onAddExam = { sub, name, date, days, total, cov, conf, target ->
                            viewModel.addExam(sub, name, date, days, total, cov, conf, target)
                        },
                        onUpdateExamProgress = { exam, cov, conf ->
                            viewModel.updateExamProgress(exam, cov, conf)
                        },
                        onDeleteExam = { viewModel.deleteExam(it) },
                        onAddWeakArea = { sub, topic, priority ->
                            viewModel.addWeakArea(sub, topic, priority)
                        },
                        onToggleWeakArea = { viewModel.toggleWeakArea(it) },
                        onDeleteWeakArea = { viewModel.deleteWeakArea(it) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onAddTask = { title, sub, date, mins, priority ->
                            viewModel.addTask(title, sub, date, mins, priority)
                        },
                        onMarkFlashcard = { card, mastered ->
                            viewModel.markFlashcard(card, mastered)
                        },
                        onAddFlashcard = { sub, q, a, hint ->
                            viewModel.addFlashcard(sub, q, a, hint)
                        },
                        onNavigateToScreen = { viewModel.navigateTo(it) }
                    )

                    AppScreen.NOTES -> NotesScreen(
                        notes = notes,
                        onAddNote = { title, sub, chapter, content, tags, isFavorite ->
                            viewModel.addNote(title, sub, chapter, content, tags, isFavorite)
                        },
                        onUpdateNote = { viewModel.updateNote(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onToggleFavorite = { viewModel.toggleFavoriteNote(it) },
                        onMarkTopicStudied = { subject, topic, duration ->
                            viewModel.markTopicStudied(subject, topic, duration)
                        }
                    )

                    AppScreen.TIMETABLE -> TimetableScreen(
                        slots = timetable,
                        onAddSlot = { slot ->
                            viewModel.addTimetableSlot(slot)
                        },
                        onUpdateSlot = { slot ->
                            viewModel.updateTimetableSlot(slot)
                        },
                        onDeleteSlot = { slot ->
                            viewModel.deleteTimetableSlot(slot)
                        }
                    )

                    AppScreen.CALCULATOR -> MarksCalculatorScreen(
                        subjectMarks = subjectMarks,
                        onAddSubject = { name, max, obtained ->
                            viewModel.addSubjectMark(name, max, obtained)
                        },
                        onUpdateSubject = { mark ->
                            viewModel.updateSubjectMark(mark)
                        },
                        onDeleteSubject = { mark ->
                            viewModel.deleteSubjectMark(mark)
                        },
                        onClearAll = {
                            viewModel.clearAllSubjectMarks()
                        }
                    )

                    AppScreen.ASK_EXPLAIN -> AskExplainScreen(
                        currentExplanation = currentExplanation,
                        isExplaining = isExplaining,
                        recentExplanations = explanationHistory,
                        onExplain = { q, sub -> viewModel.explainQuestion(q, sub) },
                        onClear = { viewModel.clearExplanation() },
                        onSaveToNotes = { explanation -> viewModel.saveExplanationToNotes(explanation) },
                        onAddToRevision = { explanation -> viewModel.addExplanationToRevision(explanation) }
                    )

                    AppScreen.PHOTO_SOLVE -> PhotoSolveScreen(
                        currentResult = currentPhotoSolveResult,
                        selectedPhotoUri = selectedPhotoUri,
                        isSolving = isPhotoSolving,
                        scanningPhase = photoSolveScanningPhase,
                        recentHistory = photoSolveHistory,
                        onSelectUri = { uri -> viewModel.setPhotoUri(uri) },
                        onSolvePhoto = { uri, rawQ, sampleId ->
                            viewModel.solvePhoto(uriString = uri, rawQuestion = rawQ, sampleId = sampleId)
                        },
                        onReSolveEditedQuestion = { editedQ ->
                            viewModel.reSolveEditedQuestion(editedQ)
                        },
                        onClear = { viewModel.clearPhotoSolve() },
                        onSaveToNotes = { result -> viewModel.savePhotoSolutionToNotes(result) },
                        onAddToRevision = { result -> viewModel.addPhotoSolutionToRevision(result) }
                    )

                    AppScreen.PROFILE -> ProfileScreen(
                        profile = profile,
                        onUpdateProfile = { name, grade, target ->
                            viewModel.updateProfileInfo(name, grade, target)
                        }
                    )

                    AppScreen.RECOVERY_PLAN -> RecoveryPlanScreen(
                        tasks = tasks,
                        exams = exams,
                        flashcards = flashcards,
                        assessment = studyStatusAssessment,
                        activePlan = activeRecoveryPlan,
                        onApplyPlan = { plan -> viewModel.applyRecoveryPlan(plan) },
                        onCancelActivePlan = { viewModel.cancelActiveRecoveryPlan() },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }
            }
        }
    }

    if (showAllScreensSheet) {
        AllScreensSheet(
            currentScreen = currentScreen,
            onSelectScreen = {
                viewModel.navigateTo(it)
                showAllScreensSheet = false
            },
            onDismiss = { showAllScreensSheet = false }
        )
    }

    AnimatedVisibility(
        visible = isSplashScreenVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TuxLearnSplashScreen()
    }
}

@Composable
fun TuxLearnSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D25B9),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_tuxlearn_logo),
                    contentDescription = "TuxLearn Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "TuxLearn",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Personal Study Assistant",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

