package com.example.lifeorganizer.core.smartadd

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

/** How the dialog should start: plain, or immediately with voice / scan (used by widget & shortcuts). */
enum class SmartAddStart { NONE, VOICE, SCAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddDialog(
    contextData: SmartAddEngine.ContextData,
    initialText: String = "",
    initialImageUri: Uri? = null,
    startWith: SmartAddStart = SmartAddStart.NONE,
    onDismiss: () -> Unit,
    onResult: (List<SmartResult>) -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    var isLoading by remember { mutableStateOf(false) }
    var isImageProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val focusRequester = remember { FocusRequester() }

    fun submit(input: String) {
        if (input.isBlank() || isLoading) return
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = SmartAddEngine.process(input, contextData)
                isLoading = false
                if (result != null) onResult(result) else errorMessage = Str.parseFailed.of(lang)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: Str.parseFailed.of(lang)
            }
        }
    }

    fun appendText(extra: String) {
        val current = text.text
        val merged = if (current.isBlank()) extra else "$current\n\n$extra"
        text = TextFieldValue(merged, TextRange(merged.length))
    }

    // Reads text from an image with ML Kit (on-device). Optionally submits right away.
    fun recognize(uri: Uri, autoSubmit: Boolean) {
        isImageProcessing = true
        errorMessage = null
        try {
            val image = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { visionText ->
                    isImageProcessing = false
                    val extracted = visionText.text
                    if (extracted.isBlank()) {
                        errorMessage = Str.noTextFound.of(lang)
                    } else {
                        appendText(extracted)
                        if (autoSubmit) submit(text.text)
                    }
                }
                .addOnFailureListener { e ->
                    isImageProcessing = false
                    errorMessage = e.localizedMessage
                }
        } catch (e: Exception) {
            isImageProcessing = false
            errorMessage = e.localizedMessage
        }
    }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        val uri = result.uriContent
        if (result.isSuccessful && uri != null) recognize(uri, autoSubmit = false)
    }

    // Lets the user mark the relevant part (e.g. one chat bubble) before reading the text.
    fun crop(uri: Uri) = cropImageLauncher.launch(
        CropImageContractOptions(
            uri = uri,
            cropImageOptions = CropImageOptions(imageSourceIncludeGallery = false, imageSourceIncludeCamera = false)
        )
    )

    // The source is picked in a small menu at the scan button instead of the cropper's full-screen chooser.
    var showSourceMenu by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) crop(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        cameraUri?.takeIf { saved }?.let { crop(it) }
    }
    fun takePhoto() {
        val file = java.io.File(context.cacheDir, "smart_add_scan_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePhoto() }

    fun startCamera() {
        if (hasCameraPermission(context)) takePhoto() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    fun pickFromGallery() =
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun startScan() { showSourceMenu = true }

    // ---- Voice: recognised inside the dialog with live text ----
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    fun appendSpoken(spoken: String) {
        val current = text.text
        val merged = if (current.isBlank()) spoken else "$current $spoken"
        text = TextFieldValue(merged, TextRange(merged.length))
    }
    val voice = remember {
        VoiceInput(
            context,
            onPartial = { partial = it },
            onFinal = { appendSpoken(it) },
            onEnd = { error ->
                listening = false
                partial = ""
                if (error != null && error != android.speech.SpeechRecognizer.ERROR_NO_MATCH &&
                    error != android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                ) errorMessage = Str.voiceUnavailable.of(lang)
            }
        )
    }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    // Fallback for devices without an in-app recogniser: the system speech dialog.
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let { appendSpoken(it) }
        }
    }

    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    fun listen() {
        errorMessage = null
        // Speaking, not typing: keep the keyboard out of the way.
        keyboard?.hide()
        focusManager.clearFocus()
        if (voice.isAvailable) {
            listening = true
            voice.start(lang)
        } else {
            try {
                speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceInput.languageTag(lang))
                })
            } catch (_: ActivityNotFoundException) {
                errorMessage = Str.voiceUnavailable.of(lang)
            }
        }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else errorMessage = Str.micPermission.of(lang)
    }

    fun startVoice() {
        if (listening) { voice.stop(); listening = false; partial = ""; return }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted || !voice.isAvailable) listen() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        when {
            initialImageUri != null -> recognize(initialImageUri, autoSubmit = true)
            startWith == SmartAddStart.VOICE -> startVoice()
            startWith == SmartAddStart.SCAN -> startScan()
            else -> runCatching { focusRequester.requestFocus() }
        }
    }

    val busy = isLoading || isImageProcessing

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        // The dialog draws behind the keyboard itself, so it can move up instead of being covered
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier.fillMaxSize().systemBarsPadding().imePadding()
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { if (!isLoading) onDismiss() },
            contentAlignment = Alignment.Center
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                // Taps inside the card must not close the dialog
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(Str.smartAdd.text(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Icon(Icons.Default.Close, contentDescription = Str.cancel.text())
                    }
                }

                Text(
                    Str.smartAddHint.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text(Str.smartAddPlaceholder.text()) },
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isLoading
                )

                AnimatedVisibility(visible = isImageProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(Str.readingImage.text(), style = MaterialTheme.typography.bodySmall)
                    }
                }

                AnimatedVisibility(visible = listening) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
                            initialValue = 0.4f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "mic_alpha"
                        )
                        Icon(
                            Icons.Default.Mic, null,
                            Modifier.size(18.dp).graphicsLayer { alpha = pulse },
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            partial.ifBlank { Str.listening.text() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { startVoice() },
                        enabled = !busy,
                        colors = if (listening) IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) else IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = Str.speak.text())
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        FilledTonalIconButton(onClick = { startScan() }, enabled = !busy) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = Str.scanImage.text())
                        }
                        DropdownMenu(expanded = showSourceMenu, onDismissRequest = { showSourceMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(Str.takePhoto.text()) },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, null) },
                                onClick = { showSourceMenu = false; startCamera() }
                            )
                            DropdownMenuItem(
                                text = { Text(Str.fromGallery.text()) },
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                                onClick = { showSourceMenu = false; pickFromGallery() }
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { submit(text.text) },
                        enabled = !busy && text.text.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Str.process.text())
                        }
                    }
                }
            }
        }
        }
    }
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
