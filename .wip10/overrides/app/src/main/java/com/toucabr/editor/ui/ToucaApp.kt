package com.toucabr.editor.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toucabr.editor.EditorViewModel
import com.toucabr.editor.ui.camera.CameraCaptureScreen
import com.toucabr.editor.ui.audio.VoiceRecorderScreen

@Composable
fun ToucaApp(viewModel: EditorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importTouca)
    }
    val importMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach(viewModel::addMedia)
    }
    val importSubtitles = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importSubtitles)
    }
    val exportSrt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        uri?.let { viewModel.exportSubtitles(it, "srt") }
    }
    val exportVtt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vtt")) { uri ->
        uri?.let { viewModel.exportSubtitles(it, "vtt") }
    }
    val exportTouca = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportTouca)
    }
    val context = LocalContext.current
    var cameraOpen by remember { mutableStateOf(false) }
    var cameraAudioAllowed by remember { mutableStateOf(false) }
    var voiceRecorderOpen by remember { mutableStateOf(false) }
    val cameraPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        cameraAudioAllowed = audioGranted
        cameraOpen = cameraGranted
        if (!cameraGranted) viewModel.showMessage("Permissão de câmera negada. O editor continua funcionando sem câmera.")
    }
    fun requestCamera() {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            cameraAudioAllowed = audioGranted
            cameraOpen = true
        } else cameraPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        voiceRecorderOpen = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!voiceRecorderOpen) viewModel.showMessage("Permissão de microfone negada. Você ainda pode importar uma narração pronta.")
    }
    fun requestVoiceRecorder() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceRecorderOpen = true
        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        if (state.project == null) {
            HomeScreen(
                projects = state.projects,
                busy = state.busy,
                onCreate = viewModel::createProject,
                onOpen = viewModel::openProject,
                onImport = { importProject.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                onRename = viewModel::renameProject,
                onDuplicate = viewModel::duplicateProject,
                onDelete = viewModel::deleteProject,
            )
        } else {
            EditorScreen(
                state = state,
                onBack = viewModel::closeProject,
                onAddMedia = { importMedia.launch(arrayOf("image/*", "video/*", "audio/*")) },
                onOpenCamera = ::requestCamera,
                onRecordVoice = ::requestVoiceRecorder,
                onSaveElevenKey = viewModel::saveElevenApiKey,
                onRemoveElevenKey = viewModel::removeElevenApiKey,
                onRefreshElevenVoices = viewModel::refreshElevenVoices,
                onSelectElevenVoice = viewModel::selectElevenVoice,
                onGenerateEleven = viewModel::generateElevenNarration,
                onUseEleven = viewModel::useElevenNarration,
                onSaveElevenResult = viewModel::saveElevenNarrationToDownloads,
                onTranscribe = { viewModel.transcribeSelectedClip() },
                onTranscribeScript = viewModel::transcribeSelectedClipWithScript,
                onImportSubtitles = { importSubtitles.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")) },
                onExportSrt = { exportSrt.launch("legendas.srt") },
                onExportVtt = { exportVtt.launch("legendas.vtt") },
                onCancelTranscription = viewModel::cancelTranscription,
                onSelect = viewModel::selectClip,
                onSeek = viewModel::seek,
                onTogglePlay = viewModel::togglePlay,
                onPps = viewModel::setPps,
                onMoveClip = viewModel::moveClip,
                onTrimLeft = viewModel::trimLeft,
                onTrimRight = viewModel::trimRight,
                onBeginGesture = viewModel::beginTransaction,
                onCommitGesture = viewModel::commitTransaction,
                onCancelGesture = viewModel::cancelTransaction,
                onPose = viewModel::updateSelectedPose,
                onAnimation = viewModel::setAnimation,
                onAnimationDuration = viewModel::setAnimationDuration,
                onTransition = viewModel::setTransition,
                onTransitionDuration = viewModel::setTransitionDuration,
                onVolume = viewModel::setSelectedVolume,
                onMuted = viewModel::setSelectedMuted,
                onAudioEffect = viewModel::setSelectedAudioEffect,
                onCleanVoice = viewModel::cleanSelectedVoice,
                onCancelCleanVoice = viewModel::cancelVoiceClean,
                onRestoreOriginalAudio = viewModel::restoreSelectedOriginalAudio,
                onAddWideAngle = viewModel::addWideAngleEffect,
                onEffectPower = viewModel::setEffectPower,
                onSelectedEnabled = viewModel::setSelectedEnabled,
                onDelete = viewModel::deleteSelected,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onExport = viewModel::exportProject,
                onExportProject = {
                    val base = state.project?.name?.replace(Regex("[^A-Za-z0-9._ -]"), "_")?.trim()?.ifBlank { "projeto" } ?: "projeto"
                    exportTouca.launch("$base.touca")
                },
                onCancelExport = viewModel::cancelExport,
            )
        }
        if (cameraOpen) {
            CameraCaptureScreen(
                audioAllowed = cameraAudioAllowed,
                onCaptured = { file ->
                    cameraOpen = false
                    viewModel.addCapturedVideo(file)
                },
                onClose = { cameraOpen = false },
            )
        }
        if (voiceRecorderOpen) {
            VoiceRecorderScreen(
                onCaptured = { file ->
                    voiceRecorderOpen = false
                    viewModel.addRecordedAudio(file)
                },
                onClose = { voiceRecorderOpen = false },
            )
        }
        state.message?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(12.dp).align(androidx.compose.ui.Alignment.BottomCenter),
                action = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } }
            ) { Text(msg) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    projects: List<com.toucabr.editor.model.ToucaProject>,
    busy: Boolean,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onImport: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var menuProjectId by remember { mutableStateOf<String?>(null) }
    var renameProject by remember { mutableStateOf<com.toucabr.editor.model.ToucaProject?>(null) }
    var deleteProject by remember { mutableStateOf<com.toucabr.editor.model.ToucaProject?>(null) }
    var renameText by remember { mutableStateOf("") }

    renameProject?.let { project ->
        AlertDialog(
            onDismissRequest = { renameProject = null },
            title = { Text("Renomear projeto") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(120) },
                    singleLine = true,
                    label = { Text("Nome") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(project.projectId, renameText)
                    renameProject = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renameProject = null }) { Text("Cancelar") } },
        )
    }
    deleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteProject = null },
            title = { Text("Excluir projeto?") },
            text = { Text("${project.name}\n\nO projeto e as mídias copiadas para o Touca Editor serão apagados do aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(project.projectId)
                    deleteProject = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteProject = null }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Touca Editor") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Novo projeto") })
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Button(onClick = onImport, enabled = !busy) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Importar .touca")
            }
            Spacer(Modifier.height(16.dp))
            Text("Projetos", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (projects.isEmpty()) {
                Text("Nenhum projeto ainda.")
            } else {
                projects.forEach { project ->
                    ElevatedCard(
                        onClick = { onOpen(project.projectId) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium)
                                val duration = com.toucabr.editor.model.ProjectMath.duration(project)
                                val min = (duration / 60.0).toInt()
                                val sec = kotlin.math.floor(duration % 60.0).toInt()
                                Text("${project.ratio} · ${project.fps} fps · ${project.clips.size} clips · ${min}:${sec.toString().padStart(2, '0')}")
                            }
                            Box {
                                IconButton(onClick = { menuProjectId = project.projectId }) { Icon(Icons.Default.MoreVert, "Ações") }
                                DropdownMenu(
                                    expanded = menuProjectId == project.projectId,
                                    onDismissRequest = { menuProjectId = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Renomear") },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = {
                                            menuProjectId = null
                                            renameText = project.name
                                            renameProject = project
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicar") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                        onClick = { menuProjectId = null; onDuplicate(project.projectId) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir") },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { menuProjectId = null; deleteProject = project },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}