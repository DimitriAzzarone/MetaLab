package com.dimitriazzarone.metalab.ui

import android.Manifest
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dimitriazzarone.metalab.PermissionResultBus
import com.dimitriazzarone.metalab.R
import com.dimitriazzarone.metalab.data.SessionRecord
import com.dimitriazzarone.metalab.data.SessionStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.dimitriazzarone.metalab.resolvePermissionStatus

private const val QUESTION_LIMIT = 500

enum class PermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

@Composable
fun MetaLabApp(
    activity: Activity,
    requestMicrophone: () -> Unit,
    requestCamera: () -> Unit,
    requestBoth: () -> Unit,
    startVoiceRecognition: ((String) -> Unit) -> Unit,
    openAppSettings: () -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }
    var transcript by rememberSaveable { mutableStateOf("") }
    var finalNote by rememberSaveable { mutableStateOf("") }
    val sessionStore = remember(activity) { SessionStore(activity) }
    var sessions by remember { mutableStateOf(sessionStore.load()) }
    var storageMessage by remember { mutableStateOf<String?>(null) }
    var microphoneRequested by rememberSaveable { mutableStateOf(false) }
    var cameraRequested by rememberSaveable { mutableStateOf(false) }
    var permissionRefresh by rememberSaveable { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        PermissionResultBus.onMicrophoneResult = {
            microphoneRequested = true
            permissionRefresh++
        }
        PermissionResultBus.onCameraResult = {
            cameraRequested = true
            permissionRefresh++
        }
        PermissionResultBus.onBothResult = { _, _ ->
            microphoneRequested = true
            cameraRequested = true
            permissionRefresh++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            PermissionResultBus.onMicrophoneResult = {}
            PermissionResultBus.onCameraResult = {}
            PermissionResultBus.onBothResult = { _, _ -> }
        }
    }

    val microphoneStatus = remember(permissionRefresh, microphoneRequested) {
        resolvePermissionStatus(
            activity,
            Manifest.permission.RECORD_AUDIO,
            microphoneRequested
        )
    }
    val cameraStatus = remember(permissionRefresh, cameraRequested) {
        resolvePermissionStatus(
            activity,
            Manifest.permission.CAMERA,
            cameraRequested
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 760.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wideLayout) 36.dp else 18.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1180.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    BrandHeader()
                    PersistentWarning()
                    QuestionPanel(
                        question = question,
                        onQuestionChange = { question = it.take(QUESTION_LIMIT) },
                        onClear = { question = "" }
                    )
                    SessionEditorPanel(
                        transcript = transcript,
                        finalNote = finalNote,
                        storageMessage = storageMessage,
                        canSave = question.isNotBlank() ||
                            transcript.isNotBlank() ||
                            finalNote.isNotBlank(),
                        onTranscriptChange = {
                            transcript = it
                            storageMessage = null
                        },
                        onFinalNoteChange = {
                            finalNote = it
                            storageMessage = null
                        },
                        onStartVoiceRecognition = startVoiceRecognition,
                        onSave = {
                            val record = SessionRecord(
                                question = question.trim(),
                                transcript = transcript.trim(),
                                detectedQuestions = detectQuestions(
                                    question = question,
                                    transcript = transcript
                                ),
                                finalNote = finalNote.trim()
                            )
                            sessionStore.append(record)
                            sessions = sessionStore.load()
                            question = ""
                            transcript = ""
                            finalNote = ""
                            storageMessage = "Sessione salvata."
                        }
                    )
                    SessionArchivePanel(
                        sessions = sessions,
                        onDelete = { id ->
                            sessionStore.delete(id)
                            sessions = sessionStore.load()
                            storageMessage = "Sessione eliminata."
                        }
                    )
                    PermissionPanel(
                        microphoneStatus = microphoneStatus,
                        cameraStatus = cameraStatus,
                        onRequestMicrophone = {
                            microphoneRequested = true
                            requestMicrophone()
                        },
                        onRequestCamera = {
                            cameraRequested = true
                            requestCamera()
                        },
                        onRequestBoth = {
                            microphoneRequested = true
                            cameraRequested = true
                            requestBoth()
                        },
                        onOpenSettings = openAppSettings
                    )
                    FuturePanels(wideLayout = wideLayout)
                    PrivacyPanel()
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF5B21B6))
                    ),
                    shape = RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_metalab_logo),
                contentDescription = "Logo MetaLab",
                tint = Color.Unspecified,
                modifier = Modifier.size(62.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MetaLab",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Laboratorio sperimentale audio e visivo",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PersistentWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2039))
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Avviso importante",
                tint = Color(0xFFFFD166)
            )
            Text(
                text = "I risultati sono sperimentali e non conclusivi. Rumori, parole riconosciute, immagini o variazioni visive possono avere spiegazioni ordinarie.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QuestionPanel(
    question: String,
    onQuestionChange: (String) -> Unit,
    onClear: () -> Unit
) {
    SectionCard(title = "Domanda", icon = Icons.Default.Science) {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            label = { Text("Scrivi la tua domanda") },
            placeholder = { Text("La domanda resta soltanto sul dispositivo e non viene inviata.") },
            minLines = 4,
            maxLines = 8,
            supportingText = {
                Text(
                    text = "${question.length}/$QUESTION_LIMIT caratteri",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        )
        OutlinedButton(
            onClick = onClear,
            enabled = question.isNotEmpty(),
            modifier = Modifier.heightIn(min = 52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null
            )
            Text("Cancella domanda", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SessionEditorPanel(
    transcript: String,
    finalNote: String,
    storageMessage: String?,
    canSave: Boolean,
    onTranscriptChange: (String) -> Unit,
    onFinalNoteChange: (String) -> Unit,
    onStartVoiceRecognition: ((String) -> Unit) -> Unit,
    onSave: () -> Unit
) {
    SectionCard(title = "Sessione", icon = Icons.Default.GraphicEq) {
        OutlinedTextField(
            value = transcript,
            onValueChange = onTranscriptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            label = { Text("Trascrizione") },
            placeholder = {
                Text("La trascrizione vocale verrà collegata nel prossimo blocco.")
            },
            minLines = 4,
            maxLines = 10
        )
        OutlinedButton(
            onClick = {
                onStartVoiceRecognition { recognizedText ->
                    onTranscriptChange(
                        listOf(transcript.trim(), recognizedText.trim())
                            .filter(String::isNotEmpty)
                            .joinToString("\n")
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null
            )
            Text(
                text = "Avvia trascrizione vocale",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        OutlinedTextField(
            value = finalNote,
            onValueChange = onFinalNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            label = { Text("Nota finale") },
            minLines = 3,
            maxLines = 6
        )
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Salva sessione con data e ora")
        }
        storageMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SessionArchivePanel(
    sessions: List<SessionRecord>,
    onDelete: (String) -> Unit
) {
    SectionCard(title = "Archivio sessioni", icon = Icons.Default.Lock) {
        if (sessions.isEmpty()) {
            Text(
                text = "Nessuna sessione salvata.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sessions.forEach { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatSessionDate(session.createdAtEpochMillis),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (session.question.isNotBlank()) {
                            Text("Domanda: ${session.question}")
                        }
                        if (session.transcript.isNotBlank()) {
                            Text("Trascrizione: ${session.transcript}")
                        }
                        if (session.detectedQuestions.isNotEmpty()) {
                            Text(
                                "Domande rilevate: " +
                                    session.detectedQuestions.joinToString(" • ")
                            )
                        }
                        if (session.finalNote.isNotBlank()) {
                            Text("Nota finale: ${session.finalNote}")
                        }
                        OutlinedButton(
                            onClick = { onDelete(session.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Elimina sessione")
                        }
                    }
                }
            }
        }
    }
}

private fun detectQuestions(
    question: String,
    transcript: String
): List<String> {
    val detected = mutableListOf<String>()

    question.trim()
        .takeIf(String::isNotEmpty)
        ?.let(detected::add)

    Regex("""[^?]+\?""")
        .findAll(transcript)
        .map { it.value.trim() }
        .filter(String::isNotEmpty)
        .forEach(detected::add)

    return detected.distinct()
}

private fun formatSessionDate(epochMillis: Long): String =
    SESSION_DATE_FORMATTER.format(
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
    )

private val SESSION_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
private fun PermissionPanel(
    microphoneStatus: PermissionStatus,
    cameraStatus: PermissionStatus,
    onRequestMicrophone: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestBoth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    SectionCard(title = "Autorizzazioni", icon = Icons.Default.Lock) {
        PermissionRow(
            name = "Microfono",
            status = microphoneStatus,
            icon = Icons.Default.Mic,
            actionLabel = "Autorizza microfono",
            onRequest = onRequestMicrophone
        )
        PermissionRow(
            name = "Fotocamera",
            status = cameraStatus,
            icon = Icons.Default.CameraAlt,
            actionLabel = "Autorizza fotocamera",
            onRequest = onRequestCamera
        )
        Button(
            onClick = onRequestBoth,
            enabled = microphoneStatus != PermissionStatus.GRANTED ||
                cameraStatus != PermissionStatus.GRANTED,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
        ) {
            Text("Autorizza entrambi")
        }

        if (
            microphoneStatus == PermissionStatus.PERMANENTLY_DENIED ||
            cameraStatus == PermissionStatus.PERMANENTLY_DENIED
        ) {
            Text(
                text = "Almeno un permesso è stato negato definitivamente. Puoi abilitarlo dalle impostazioni Android dell’app.",
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("Apri impostazioni dell’app")
            }
        } else if (
            microphoneStatus == PermissionStatus.DENIED ||
            cameraStatus == PermissionStatus.DENIED
        ) {
            Text(
                text = "Il permesso negato serve soltanto nelle fasi future. In questa fase MetaLab non registra audio e non apre la fotocamera.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRow(
    name: String,
    status: PermissionStatus,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2D))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = permissionColor(status)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.SemiBold)
                    Text(
                        permissionLabel(status),
                        color = permissionColor(status)
                    )
                }
            }
            if (status != PermissionStatus.GRANTED) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun FuturePanels(wideLayout: Boolean) {
    val panels = listOf(
        Triple(
            "Metafonia",
            "Generazione sonora, registrazione e trascrizione saranno aggiunte nelle fasi successive.",
            Icons.Default.GraphicEq
        ),
        Triple(
            "Metavisione",
            "Anteprima fotocamera e analisi delle variazioni visive non sono ancora implementate.",
            Icons.Default.CameraAlt
        ),
        Triple(
            "Sessione combinata",
            "Audio e video combinati saranno disponibili dopo la verifica dei moduli separati.",
            Icons.Default.Science
        ),
        Triple(
            "Archivio",
            "Il diario delle sessioni non è ancora disponibile.",
            Icons.Default.Lock
        )
    )

    if (wideLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            panels.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    pair.forEach { panel ->
                        FuturePanel(
                            title = panel.first,
                            message = panel.second,
                            icon = panel.third,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            panels.forEach { panel ->
                FuturePanel(
                    title = panel.first,
                    message = panel.second,
                    icon = panel.third,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FuturePanel(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172033))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Limiti, privacy e interpretazione",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "MetaLab è uno strumento sperimentale. I risultati non costituiscono prova di comunicazioni paranormali. Rumori, trascrizioni e variazioni visive possono dipendere da interferenze, riverbero, errori software, compressione, riflessi, polvere, insetti, cambi di luce o interpretazione soggettiva.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "In questa fase nessun audio viene registrato, nessuna immagine viene acquisita e nessun dato viene inviato online. Non sono richiesti account e non sono presenti pubblicità, telemetria o analytics.",
                color = Color(0xFF34D399)
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

private fun permissionLabel(status: PermissionStatus): String = when (status) {
    PermissionStatus.NOT_REQUESTED -> "Non richiesto"
    PermissionStatus.GRANTED -> "Concesso"
    PermissionStatus.DENIED -> "Negato"
    PermissionStatus.PERMANENTLY_DENIED -> "Negato definitivamente"
}

private fun permissionColor(status: PermissionStatus): Color = when (status) {
    PermissionStatus.GRANTED -> Color(0xFF34D399)
    PermissionStatus.DENIED,
    PermissionStatus.PERMANENTLY_DENIED -> Color(0xFFFB7185)
    PermissionStatus.NOT_REQUESTED -> Color(0xFFCBD5E1)
}
