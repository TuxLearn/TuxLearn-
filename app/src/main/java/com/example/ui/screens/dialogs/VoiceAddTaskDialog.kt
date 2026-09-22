package com.example.ui.screens.dialogs

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryDark
import com.example.util.DeviceTimeService
import com.example.util.ParsedVoiceTask
import com.example.util.VoiceTaskParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SpeechLangOption(
    val id: String,
    val label: String,
    val tag: String
)

@Composable
fun VoiceAddTaskDialog(
    initialSpokenSentence: String = "",
    onDismiss: () -> Unit,
    onSaveTask: (
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
    var spokenInput by remember { mutableStateOf(initialSpokenSentence) }
    var parsedTask by remember { mutableStateOf<ParsedVoiceTask?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isEditingDetails by remember { mutableStateOf(false) }
    var liveRmsLevel by remember { mutableFloatStateOf(0f) }
    var speechStatusText by remember { mutableStateOf<String?>(null) }
    var speechErrorMessage by remember { mutableStateOf<String?>(null) }

    // System language auto detection
    val defaultDeviceLocale = remember { Locale.getDefault() }
    val deviceLanguageTag = remember {
        val tag = defaultDeviceLocale.toLanguageTag()
        if (tag.isNotBlank()) tag else "en-US"
    }

    val languageOptions = remember {
        listOf(
            SpeechLangOption("AUTO", "Auto (${defaultDeviceLocale.displayLanguage.ifBlank { "Device" }})", deviceLanguageTag),
            SpeechLangOption("EN", "English", "en-IN"),
            SpeechLangOption("HI", "Hindi (हिन्दी)", "hi-IN"),
            SpeechLangOption("HINGLISH", "Hinglish", "en-IN")
        )
    }
    var selectedLanguageOption by remember { mutableStateOf("AUTO") }

    // In-app SpeechRecognizer reference
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Clean up SpeechRecognizer on dismissal
    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    // Editable extracted fields
    var editableTitle by remember { mutableStateOf("") }
    var editableSubject by remember { mutableStateOf("Physics") }
    var editableDueDate by remember { mutableStateOf("Tomorrow") }
    var editableDateDisplay by remember { mutableStateOf("") }
    var editableTimeDisplay by remember { mutableStateOf("07:00 AM") }
    var editableHour by remember { mutableIntStateOf(7) }
    var editableMinute by remember { mutableIntStateOf(0) }
    var editableDuration by remember { mutableIntStateOf(30) }
    var editablePriority by remember { mutableStateOf("Medium") }
    var editableXp by remember { mutableIntStateOf(25) }
    var editableReminderEnabled by remember { mutableStateOf(true) }
    var editableEpochMillis by remember { mutableLongStateOf(0L) }

    val standardSubjects = listOf(
        "Physics",
        "Mathematics",
        "Chemistry",
        "Biology",
        "Computer Science",
        "English",
        "History"
    )

    val sampleSpokenSentences = listOf(
        "Tomorrow at 7 AM revise Physics Chapter 3 for 30 minutes.",
        "Today at 5 PM solve Chemistry stoichiometry worksheet for 45 minutes urgent",
        "Friday at 6 PM complete Maths calculus problem set for 60 minutes",
        "Tonight at 8 PM read Biology cell division chapter for 25 minutes",
        "Tomorrow at 4 PM practice Computer Science QuickSort algorithms for 40 minutes"
    )

    fun applyParsedTask(parsed: ParsedVoiceTask) {
        parsedTask = parsed
        editableTitle = parsed.title
        editableSubject = parsed.subject
        editableDueDate = parsed.dueDate
        editableDateDisplay = parsed.dateDisplay
        editableTimeDisplay = parsed.timeDisplay
        editableHour = parsed.hour
        editableMinute = parsed.minute
        editableDuration = parsed.durationMinutes
        editablePriority = parsed.priority
        editableXp = parsed.xpReward
        editableReminderEnabled = parsed.reminderEnabled
        editableEpochMillis = parsed.reminderEpochMillis ?: System.currentTimeMillis()
    }

    // Helper to build speech recognition intent
    fun buildSpeechIntent(): Intent {
        val effectiveLangTag = when (selectedLanguageOption) {
            "AUTO" -> deviceLanguageTag
            "EN" -> "en-IN"
            "HI" -> "hi-IN"
            "HINGLISH" -> "en-IN"
            else -> deviceLanguageTag
        }

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, effectiveLangTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, effectiveLangTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            // Extra hints for multilingual Hindi/English/Hinglish recognition
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US", "hi"))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak homework task (e.g. 'Tomorrow at 7 AM revise Physics for 30 minutes')")
        }
    }

    // Optional external system speech recognition activity launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val topSpoken = spokenMatches?.firstOrNull { it.isNotBlank() }
            if (!topSpoken.isNullOrBlank()) {
                spokenInput = topSpoken
                speechErrorMessage = null
                val parsed = VoiceTaskParser.parse(topSpoken)
                applyParsedTask(parsed)
            } else {
                speechErrorMessage = "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
            }
        } else {
            speechErrorMessage = "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
        }
    }

    // Speech recognition execution
    fun startListeningInternal() {
        speechErrorMessage = null
        isListening = true
        speechStatusText = "Listening... Speak your homework now"

        try {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}

            var recognizer: SpeechRecognizer? = null

            // 1. Try on-device speech recognizer on API 31+ for fast, popup-free recognition
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                try {
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (_: Exception) {}
            }

            // 2. Fall back to standard in-app SpeechRecognizer
            if (recognizer == null) {
                try {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                } catch (_: Exception) {}
            }

            if (recognizer != null) {
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        speechStatusText = "Listening... Speak your homework now"
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                        speechStatusText = "Hearing your voice..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        liveRmsLevel = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        speechStatusText = "Processing spoken task..."
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap 'Try Again' and speak clearly, or type below."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required to speak tasks."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout. You can type or tap a sample task below."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer was busy. Tap 'Try Again' in a moment."
                            SpeechRecognizer.ERROR_CLIENT -> "Listening was interrupted. Tap 'Try Again' or type below."
                            else -> "Didn't catch that. Tap 'Try Again' or type your homework below."
                        }
                        speechErrorMessage = message
                        speechStatusText = null
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        speechStatusText = null
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val top = matches?.firstOrNull { it.isNotBlank() }
                        if (!top.isNullOrBlank()) {
                            spokenInput = top
                            speechErrorMessage = null
                            speechStatusText = "Recognized: \"$top\""
                            val parsed = VoiceTaskParser.parse(top)
                            applyParsedTask(parsed)
                        } else {
                            speechErrorMessage = "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = partialList?.firstOrNull { it.isNotBlank() }
                        if (!partial.isNullOrBlank()) {
                            spokenInput = partial
                            speechStatusText = "\"$partial\""
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = buildSpeechIntent()
                recognizer.startListening(intent)
            } else {
                // SpeechRecognizer service unavailable: inform user gently without popping up the disruptive external dialog
                isListening = false
                speechStatusText = null
                speechErrorMessage = "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
            }
        } catch (e: Exception) {
            isListening = false
            speechStatusText = null
            speechErrorMessage = "Didn't catch that. Tap 'Try Again' and speak clearly, or type your homework below."
        }
    }

    // Microphone runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            isListening = false
            speechErrorMessage = "Microphone permission is needed to speak tasks. Please grant permission or type below."
        }
    }

    fun startListening() {
        speechErrorMessage = null
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            startListeningInternal()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopListening() {
        isListening = false
        speechStatusText = null
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    // Parse initial input if provided
    LaunchedEffect(initialSpokenSentence) {
        if (initialSpokenSentence.isNotBlank() && parsedTask == null) {
            val parsed = VoiceTaskParser.parse(initialSpokenSentence)
            applyParsedTask(parsed)
        }
    }

    // Pulsing animation for microphone listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("voice_add_task_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Task",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (parsedTask == null) "Voice Homework Entry" else "Confirm Task Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        )
                        Text(
                            text = if (parsedTask == null) "Speak homework or study task naturally" else "Extracted from your voice note",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("voice_add_close_btn")
                ) {
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
                if (parsedTask == null) {
                    // --- STEP 1: VOICE INPUT & SPEAKING STAGE ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Language Selection Chips (Auto / English / Hindi / Hinglish)
                        Text(
                            text = "Speech Language:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("voice_language_chips_row")
                        ) {
                            items(languageOptions) { opt ->
                                FilterChip(
                                    selected = selectedLanguageOption == opt.id,
                                    onClick = { selectedLanguageOption = opt.id },
                                    label = { Text(opt.label, fontSize = 11.sp) },
                                    leadingIcon = if (selectedLanguageOption == opt.id) {
                                        { Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = IndigoPrimary
                                    ),
                                    modifier = Modifier.testTag("voice_lang_${opt.id.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large interactive pulsing mic button
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(if (isListening) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) AmberStreak else IndigoPrimary
                                )
                                .clickable {
                                    if (isListening) {
                                        stopListening()
                                    } else {
                                        startListening()
                                    }
                                }
                                .testTag("voice_mic_record_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                                contentDescription = if (isListening) "Listening" else "Tap to Speak",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when {
                                isListening -> speechStatusText ?: "Listening for your task..."
                                speechErrorMessage != null -> "Voice input needed retry"
                                else -> "Tap the microphone to speak"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) AmberStreak else MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Say things like:\n\"Tomorrow at 7 AM revise Physics for 30 minutes.\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        // If speech recognition failed: Show clear Try Again banner with Try Again button and fallback
                        if (speechErrorMessage != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("voice_speech_error_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = speechErrorMessage ?: "Didn't catch that",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { startListening() },
                                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("voice_try_again_button")
                                        ) {
                                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Try Again", fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                speechErrorMessage = null
                                                try {
                                                    val intent = buildSpeechIntent()
                                                    speechLauncher.launch(intent)
                                                } catch (_: Exception) {
                                                    speechErrorMessage = "System voice recognition unavailable. Please type below."
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("voice_system_dialog_button")
                                        ) {
                                            Text("System Dialog", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Sample Spoken Voice Homework sentences (1-tap testing & fast entry)
                        Text(
                            text = "Or tap a sample spoken sentence:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = IndigoPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )

                        sampleSpokenSentences.forEachIndexed { index, sample ->
                            Card(
                                onClick = {
                                    spokenInput = sample
                                    val parsed = VoiceTaskParser.parse(sample)
                                    applyParsedTask(parsed)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .testTag("voice_sample_chip_$index")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sample,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manual voice transcript input field (Shows recognized text or allows typing fallback)
                        OutlinedTextField(
                            value = spokenInput,
                            onValueChange = { spokenInput = it },
                            label = { Text("Or type/paste spoken sentence") },
                            placeholder = { Text("e.g. Tomorrow at 7 AM revise Physics for 30 minutes") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_spoken_text_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (spokenInput.isNotBlank()) {
                                    val parsed = VoiceTaskParser.parse(spokenInput)
                                    applyParsedTask(parsed)
                                } else {
                                    Toast.makeText(context, "Please speak or enter a homework sentence", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = spokenInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_convert_task_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Convert Spoken Sentence to Task")
                        }
                    }
                } else {
                    // --- STEP 2: PREVIEW / CONFIRMATION SCREEN WHERE STUDENT CAN EDIT EXTRACTED DETAILS ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Recognized Spoken Sentence in text field before creating task (Requirement 7 & 9)
                        OutlinedTextField(
                            value = spokenInput,
                            onValueChange = {
                                spokenInput = it
                                if (it.isNotBlank()) {
                                    val reParsed = VoiceTaskParser.parse(it)
                                    applyParsedTask(reParsed)
                                }
                            },
                            label = { Text("Recognized Spoken Sentence") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        parsedTask = null
                                        isEditingDetails = false
                                        startListening()
                                    },
                                    modifier = Modifier.testTag("voice_respeak_btn")
                                ) {
                                    Text("Re-speak", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("voice_recognized_sentence_field")
                        )

                        // 1. Task Title
                        Text(
                            text = "Task Title",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            placeholder = { Text("e.g. Revise Physics") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("planner_voice_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Subject Selector
                        Text(
                            text = "Subject",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("planner_voice_subject_row")
                        ) {
                            items(standardSubjects) { subject ->
                                FilterChip(
                                    selected = editableSubject.equals(subject, ignoreCase = true),
                                    onClick = { editableSubject = subject },
                                    label = { Text(subject) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("voice_subject_chip_$subject")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Date & Time Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Picker Box
                            Card(
                                onClick = {
                                    val nowCal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            editableDueDate = DeviceTimeService.getDisplayDateLabel(y, m, d)
                                            editableDateDisplay = DeviceTimeService.formatShortDate(y, m, d)
                                            editableEpochMillis = DeviceTimeService.toLocalEpochMillis(y, m, d, editableHour, editableMinute)
                                        },
                                        nowCal.get(Calendar.YEAR),
                                        nowCal.get(Calendar.MONTH),
                                        nowCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("planner_voice_date_card")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarMonth,
                                            contentDescription = null,
                                            tint = IndigoPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Date",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (editableDateDisplay.isNotBlank()) editableDateDisplay else editableDueDate,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = IndigoPrimary
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Time Picker Box
                            Card(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            editableHour = h
                                            editableMinute = m
                                            editableTimeDisplay = DeviceTimeService.format12Hour(h, m)
                                            val parts = DeviceTimeService.fromEpochMillis(editableEpochMillis)
                                            editableEpochMillis = DeviceTimeService.toLocalEpochMillis(parts.year, parts.month0, parts.day, h, m)
                                        },
                                        editableHour,
                                        editableMinute,
                                        false
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("planner_voice_time_card")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = IndigoPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Time",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = editableTimeDisplay,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = IndigoPrimary
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Quick Date Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Today", "Tomorrow", "Friday").forEach { dayLabel ->
                                FilterChip(
                                    selected = editableDueDate.contains(dayLabel, ignoreCase = true),
                                    onClick = {
                                        editableDueDate = dayLabel
                                        if (dayLabel == "Today") {
                                            editableDateDisplay = "Today"
                                            val today = DeviceTimeService.getTodayParts()
                                            editableEpochMillis = DeviceTimeService.toLocalEpochMillis(today.first, today.second, today.third, editableHour, editableMinute)
                                        } else if (dayLabel == "Tomorrow") {
                                            editableDateDisplay = "Tomorrow"
                                            val tom = DeviceTimeService.getTomorrowParts()
                                            editableEpochMillis = DeviceTimeService.toLocalEpochMillis(tom.first, tom.second, tom.third, editableHour, editableMinute)
                                        }
                                    },
                                    label = { Text(dayLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = IndigoPrimary
                                    ),
                                    modifier = Modifier.testTag("voice_date_chip_$dayLabel")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 4. Duration and Priority Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Duration
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("planner_voice_duration_card")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = null,
                                            tint = AmberStreak,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Duration",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$editableDuration mins",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberStreak
                                        )
                                    )
                                }
                            }

                            // Priority
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("planner_voice_priority_card")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = when (editablePriority) {
                                                "High" -> Color(0xFFE53935)
                                                "Low" -> GreenSuccess
                                                else -> AmberStreak
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Priority",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = editablePriority,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (editablePriority) {
                                                "High" -> Color(0xFFE53935)
                                                "Low" -> GreenSuccess
                                                else -> AmberStreak
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        // Quick Duration & Priority Adjustments
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    FilterChip(
                                        selected = editableDuration == mins,
                                        onClick = {
                                            editableDuration = mins
                                            editableXp = when {
                                                mins >= 60 -> 40
                                                mins >= 45 -> 35
                                                mins >= 30 -> 25
                                                else -> 20
                                            } + (if (editablePriority == "High") 10 else 0)
                                        },
                                        label = { Text("${mins}m", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("voice_dur_chip_$mins")
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Low", "Medium", "High").forEach { p ->
                                    FilterChip(
                                        selected = editablePriority == p,
                                        onClick = {
                                            editablePriority = p
                                            editableXp = when {
                                                editableDuration >= 60 -> 40
                                                editableDuration >= 45 -> 35
                                                editableDuration >= 30 -> 25
                                                else -> 20
                                            } + (if (p == "High") 10 else if (p == "Low") -5 else 0)
                                        },
                                        label = { Text(p, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("voice_priority_chip_$p")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 5. Reminder Notifications Switch & Summary
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
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
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (editableReminderEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Study Reminder Alarm",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = if (editableReminderEnabled) {
                                                "Alert set for $editableTimeDisplay on $editableDueDate"
                                            } else {
                                                "Reminder is turned off"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Switch(
                                    checked = editableReminderEnabled,
                                    onCheckedChange = { editableReminderEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IndigoPrimary,
                                        checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("planner_voice_reminder_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 6. XP Points Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberStreak.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberStreak.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = AmberStreak, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "XP Earned on Completion",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Text(
                                    text = "+$editableXp XP",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberStreak
                                    )
                                )
                            }
                        }

                        // Collapsible manual adjustments section
                        AnimatedVisibility(visible = isEditingDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Custom XP points:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                OutlinedTextField(
                                    value = editableXp.toString(),
                                    onValueChange = { editableXp = it.toIntOrNull()?.coerceIn(5, 500) ?: editableXp },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("voice_custom_xp_input")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (parsedTask != null) {
                // The three requested buttons: Save Task, Edit, Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("planner_voice_btn_cancel")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Edit button
                    OutlinedButton(
                        onClick = {
                            isEditingDetails = !isEditingDetails
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("planner_voice_btn_edit")
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditingDetails) "Done Editing" else "Edit")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Save Task button
                    Button(
                        onClick = {
                            val finalTitle = editableTitle.ifBlank { "Revise $editableSubject" }
                            onSaveTask(
                                finalTitle,
                                editableSubject,
                                editableDueDate,
                                editableDuration,
                                editablePriority,
                                editableReminderEnabled,
                                if (editableReminderEnabled) editableEpochMillis else null,
                                editableDateDisplay.ifBlank { editableDueDate },
                                editableTimeDisplay,
                                "Once",
                                editableXp
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("planner_voice_btn_save")
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Task")
                    }
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("planner_voice_btn_cancel_initial")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
