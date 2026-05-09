package com.bananaleafnutrientcheck.app.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.bananaleafnutrientcheck.app.R
import com.bananaleafnutrientcheck.app.data.image.AppPrivateCaptureStore

private val CautionContainer = Color(0xFFFFF8E1)
private val CautionContent = Color(0xFF8A5000)
private val CautionBorder = Color(0xFFFFCA28)
private val InfoContainer = Color(0xFFF2F5EC)
private val SuccessContainer = Color(0xFFEAF5E8)
private val SuccessContent = Color(0xFF1B5E20)
private val LeafPanel = Color(0xFF0B3818)
private val LeafPanelSoft = Color(0xFF245D2D)
private val LeafVein = Color(0xFFD8DE74)
private val CardShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(8.dp)

@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContent(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_heading),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        BananaLeafHero(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )

        Text(
            text = stringResource(R.string.home_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        CautionCard(
            title = stringResource(R.string.home_caution_title),
            body = stringResource(R.string.home_caution_body),
            iconResId = R.drawable.ic_info_24,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                text = stringResource(R.string.home_scan_action),
                iconResId = R.drawable.ic_scan_24,
                onClick = onStartScan,
            )
            SecondaryActionButton(
                text = stringResource(R.string.home_about_action),
                iconResId = R.drawable.ic_book_24,
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
fun ScanScreen(
    uiState: ScanUiState = ScanUiState(),
    onImageSelected: (String?) -> Unit = {},
    onCameraImageCaptured: (String?) -> Unit = {},
    onClearImage: () -> Unit = {},
    onAnalyzeImage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            onImageSelected(uri?.toString())
        },
    )
    val launchImagePicker = {
        imagePickerLauncher.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
            ),
        )
    }

    ScreenContent(modifier = modifier) {
        when {
            uiState.result != null -> {
                ResultCard(
                    result = uiState.result,
                    selectedImageUri = uiState.selectedImageUri,
                    onScanAgain = onClearImage,
                    onChooseImage = launchImagePicker,
                )
            }

            uiState.isAnalyzing -> {
                AnalyzingPanel(selectedImageUri = uiState.selectedImageUri)
            }

            uiState.hasSelectedImage && uiState.selectedImageUri != null -> {
                PreviewReadyPanel(
                    selectedImageUri = uiState.selectedImageUri,
                    canAnalyze = uiState.canAnalyze,
                    onAnalyzeImage = onAnalyzeImage,
                    onChooseImage = launchImagePicker,
                    onClearImage = onClearImage,
                )
            }

            else -> {
                ScanSetupPanel()
            }
        }

        uiState.analysisError?.let {
            ErrorCard(
                title = stringResource(R.string.scan_error_title),
                body = stringResource(R.string.scan_error_body),
            )
        }

        if (!uiState.isAnalyzing) {
            SectionHeader(text = stringResource(R.string.scan_image_source_section))
            CameraCaptureCard(
                onImageCaptured = onCameraImageCaptured,
            )
            PhotoPickerCard(
                hasSelectedImage = uiState.hasSelectedImage,
                onChooseImage = launchImagePicker,
            )
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.about_heading))
        BodyText(text = stringResource(R.string.about_intro))

        TechnicalSpecsCard()

        InfoCard(
            title = stringResource(R.string.about_purpose_title),
            iconResId = R.drawable.ic_leaf_24,
        ) {
            BodyText(text = stringResource(R.string.about_purpose_body))
            BodyText(text = stringResource(R.string.about_privacy_body))
        }

        InfoCard(
            title = stringResource(R.string.about_model_title),
            iconResId = R.drawable.ic_lab_24,
        ) {
            BodyText(text = stringResource(R.string.about_model_body))
            BodyText(text = stringResource(R.string.about_model_score_note))
        }

        InfoCard(
            title = stringResource(R.string.about_limits_title),
            iconResId = R.drawable.ic_info_24,
        ) {
            BulletText(text = stringResource(R.string.about_limit_uncertainty))
            BulletText(text = stringResource(R.string.about_limit_overlap))
            BulletText(text = stringResource(R.string.about_limit_screening))
            BulletText(text = stringResource(R.string.about_limit_testing))
            BulletText(text = stringResource(R.string.about_limit_no_rates))
        }

        InfoCard(
            title = stringResource(R.string.about_classes_title),
            iconResId = R.drawable.ic_book_24,
        ) {
            BodyText(text = stringResource(R.string.about_classes_body))
            AboutClassInfo.items.forEachIndexed { index, item ->
                ClassInfoRow(item = item)
                if (index < AboutClassInfo.items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        CautionCard(
            title = stringResource(R.string.about_dataset_title),
            body = stringResource(R.string.about_dataset_body),
            iconResId = R.drawable.ic_warning_24,
        )
    }
}

@Composable
private fun ScanSetupPanel(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(iconResId = R.drawable.ic_leaf_24)
            Text(
                text = stringResource(R.string.scan_heading),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.scan_guidance_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewReadyPanel(
    selectedImageUri: String,
    canAnalyze: Boolean,
    onAnalyzeImage: () -> Unit,
    onChooseImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeading(text = stringResource(R.string.scan_preview_heading))
        SelectedImagePreview(selectedImageUri = selectedImageUri)
        PrimaryActionButton(
            text = stringResource(R.string.scan_analyze_action),
            iconResId = R.drawable.ic_lab_24,
            enabled = canAnalyze,
            onClick = onAnalyzeImage,
        )
        SecondaryActionButton(
            text = stringResource(R.string.scan_change_image_action),
            iconResId = R.drawable.ic_photo_24,
            onClick = onChooseImage,
        )
        SecondaryActionButton(
            text = stringResource(R.string.scan_clear_image_action),
            iconResId = R.drawable.ic_close_24,
            onClick = onClearImage,
        )
        PrivacyNote()
    }
}

@Composable
private fun AnalyzingPanel(
    selectedImageUri: String?,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(1f)
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(
                            R.string.scan_selected_image_content_description,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = stringResource(R.string.scan_analyzing_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.scan_analyzing_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TechnicalSpecsCard(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(iconResId = R.drawable.ic_lab_24, size = 40)
            CardTitle(text = stringResource(R.string.about_specs_title))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SpecRow(
            label = stringResource(R.string.about_architecture_label),
            value = stringResource(R.string.about_architecture_value),
            iconResId = R.drawable.ic_scan_24,
        )
        SpecRow(
            label = stringResource(R.string.about_classes_label),
            value = stringResource(R.string.about_classes_value),
            iconResId = R.drawable.ic_book_24,
        )
        SpecRow(
            label = stringResource(R.string.about_metric_test_accuracy).substringBefore(":"),
            value = stringResource(R.string.about_metric_test_accuracy).substringAfter(": "),
            iconResId = R.drawable.ic_check_24,
        )
        SpecRow(
            label = stringResource(R.string.about_metric_macro_f1).substringBefore(":"),
            value = stringResource(R.string.about_metric_macro_f1).substringAfter(": "),
            iconResId = R.drawable.ic_lab_24,
        )
        SpecRow(
            label = stringResource(R.string.about_metric_banana_macro_f1).substringBefore(":"),
            value = stringResource(R.string.about_metric_banana_macro_f1).substringAfter(": "),
            iconResId = R.drawable.ic_leaf_24,
            showDivider = false,
        )
    }
}

@Composable
private fun SpecRow(
    label: String,
    value: String,
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ClassInfoRow(
    item: AboutClassInfoItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(ChipShape)
                .background(InfoContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.modelOrder.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.displayLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.isSupplemental) {
                    StatusChip(
                        text = "Supplemental",
                        containerColor = CautionContainer,
                        contentColor = CautionContent,
                    )
                }
            }
            Text(
                text = stringResource(R.string.about_class_internal_label, item.internalLabel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(item.noteResId),
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isSupplemental) {
                    CautionContent
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun CameraCaptureCard(
    onImageCaptured: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val captureStore = remember(context) {
        AppPrivateCaptureStore(context.applicationContext)
    }
    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var permissionRequested by rememberSaveable {
        mutableStateOf(false)
    }
    var permissionDenied by rememberSaveable {
        mutableStateOf(false)
    }
    var showCamera by rememberSaveable {
        mutableStateOf(false)
    }
    var captureError by rememberSaveable {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            permissionRequested = true
            hasCameraPermission = isGranted
            permissionDenied = !isGranted
            captureError = false
            showCamera = isGranted
        },
    )
    val shouldShowRationale = activity?.let { currentActivity ->
        ActivityCompat.shouldShowRequestPermissionRationale(
            currentActivity,
            Manifest.permission.CAMERA,
        )
    } == true
    val permissionPermanentlyDenied = permissionRequested &&
        permissionDenied &&
        !shouldShowRationale &&
        !hasCameraPermission

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = context.hasCameraPermission()
                if (!hasCameraPermission) {
                    showCamera = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitleRow(
                title = stringResource(R.string.scan_take_photo_title),
                iconResId = R.drawable.ic_camera_24,
            )
            BodyText(text = stringResource(R.string.scan_take_photo_body))

            PrimaryActionButton(
                text = stringResource(R.string.scan_take_photo_action),
                iconResId = R.drawable.ic_camera_24,
                onClick = {
                    captureError = false
                    if (hasCameraPermission) {
                        permissionDenied = false
                        showCamera = true
                    } else {
                        permissionRequested = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            )

            if (!hasCameraPermission && (shouldShowRationale || permissionDenied)) {
                CameraPermissionNotice(
                    permanentlyDenied = permissionPermanentlyDenied,
                    permissionDenied = permissionDenied,
                    showRationale = shouldShowRationale,
                    onRetryPermission = {
                        permissionRequested = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        context.openAppPermissionSettings()
                    },
                )
            }

            if (captureError) {
                CameraStatusNotice(
                    title = stringResource(R.string.scan_camera_unavailable_title),
                    body = stringResource(R.string.scan_camera_unavailable_body),
                )
            }

            if (showCamera && hasCameraPermission) {
                CameraPreviewCapture(
                    captureStore = captureStore,
                    onImageCaptured = { imageUri ->
                        captureError = false
                        showCamera = false
                        onImageCaptured(imageUri)
                    },
                    onCaptureError = {
                        captureError = true
                        showCamera = false
                    },
                    onClose = {
                        showCamera = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewCapture(
    captureStore: AppPrivateCaptureStore,
    onImageCaptured: (String?) -> Unit,
    onCaptureError: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember {
        mutableStateOf<PreviewView?>(null)
    }
    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }
    var isCapturing by rememberSaveable {
        mutableStateOf(false)
    }
    val mainExecutor = remember(context) {
        ContextCompat.getMainExecutor(context)
    }
    val cameraProviderFuture = remember(context) {
        ProcessCameraProvider.getInstance(context)
    }

    DisposableEffect(cameraProviderFuture, lifecycleOwner, previewView) {
        val view = previewView ?: return@DisposableEffect onDispose { }
        var disposed = false
        val listener = Runnable {
            if (disposed) {
                return@Runnable
            }

            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(view.surfaceProvider)
                    }
                val captureUseCase = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    captureUseCase,
                )
                imageCapture = captureUseCase
            }.onFailure {
                imageCapture = null
                onCaptureError()
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            disposed = true
            imageCapture = null
            if (cameraProviderFuture.isDone) {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { view ->
                    previewView = view
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        PrimaryActionButton(
            text = stringResource(
                if (isCapturing) {
                    R.string.scan_capturing_photo_action
                } else {
                    R.string.scan_capture_photo_action
                },
            ),
            iconResId = R.drawable.ic_camera_24,
            enabled = imageCapture != null && !isCapturing,
            onClick = {
                val captureUseCase = imageCapture ?: return@PrimaryActionButton
                val captureFile = runCatching {
                    captureStore.createCaptureFile()
                }.getOrNull()
                if (captureFile == null) {
                    onCaptureError()
                    return@PrimaryActionButton
                }
                captureUseCase.targetRotation = previewView?.display?.rotation ?: Surface.ROTATION_0
                isCapturing = true
                val outputOptions = ImageCapture.OutputFileOptions.Builder(captureFile).build()
                captureUseCase.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(
                            outputFileResults: ImageCapture.OutputFileResults,
                        ) {
                            isCapturing = false
                            onImageCaptured(captureStore.uriFor(captureFile).toString())
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            runCatching {
                                captureFile.delete()
                            }
                            onCaptureError()
                        }
                    },
                )
            },
        )

        SecondaryActionButton(
            text = stringResource(R.string.scan_close_camera_action),
            iconResId = R.drawable.ic_close_24,
            enabled = !isCapturing,
            onClick = onClose,
        )
    }
}

@Composable
private fun CameraPermissionNotice(
    permanentlyDenied: Boolean,
    permissionDenied: Boolean,
    showRationale: Boolean,
    onRetryPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        permanentlyDenied -> stringResource(R.string.scan_camera_permission_blocked_title)
        permissionDenied -> stringResource(R.string.scan_camera_permission_denied_title)
        showRationale -> stringResource(R.string.scan_camera_permission_rationale_title)
        else -> stringResource(R.string.scan_camera_permission_denied_title)
    }
    val body = when {
        permanentlyDenied -> stringResource(R.string.scan_camera_permission_blocked_body)
        permissionDenied -> stringResource(R.string.scan_camera_permission_denied_body)
        showRationale -> stringResource(R.string.scan_camera_permission_rationale_body)
        else -> stringResource(R.string.scan_camera_permission_denied_body)
    }

    CameraStatusNotice(
        title = title,
        body = body,
        modifier = modifier,
    ) {
        if (permanentlyDenied) {
            SecondaryActionButton(
                text = stringResource(R.string.scan_camera_open_settings_action),
                iconResId = R.drawable.ic_info_24,
                onClick = onOpenSettings,
            )
        } else {
            SecondaryActionButton(
                text = stringResource(R.string.scan_camera_permission_retry_action),
                iconResId = R.drawable.ic_camera_24,
                onClick = onRetryPermission,
            )
        }
    }
}

@Composable
private fun CameraStatusNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CautionContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardTitleRow(
            title = title,
            iconResId = R.drawable.ic_warning_24,
            color = CautionContent,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = CautionContent,
        )
        action()
    }
}

@Composable
private fun PhotoPickerCard(
    hasSelectedImage: Boolean,
    onChooseImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitleRow(
                title = stringResource(R.string.scan_choose_image_title),
                iconResId = R.drawable.ic_photo_24,
            )
            BodyText(
                text = if (hasSelectedImage) {
                    stringResource(R.string.scan_selected_image_body)
                } else {
                    stringResource(R.string.scan_choose_image_body)
                },
            )
            SecondaryActionButton(
                text = stringResource(
                    if (hasSelectedImage) {
                        R.string.scan_change_image_action
                    } else {
                        R.string.scan_choose_image_action
                    },
                ),
                iconResId = R.drawable.ic_photo_24,
                onClick = onChooseImage,
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: ScanResultUiModel,
    selectedImageUri: String?,
    onScanAgain: () -> Unit,
    onChooseImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_possible_result_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = result.possibleResultText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        selectedImageUri?.let { uri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(
                        R.string.scan_selected_image_content_description,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                ConfidenceBadge(
                    text = "${result.confidenceText} (${result.topPrediction.scoreText})",
                    confidenceText = result.confidenceText,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelValueBlock(
                    label = stringResource(R.string.scan_model_score_label),
                    value = result.topPrediction.scoreText,
                )
                ConfidenceBadge(
                    text = result.confidenceText,
                    confidenceText = result.confidenceText,
                )
            }
        }

        if (result.showDatasetCaution) {
            CautionCard(
                title = stringResource(R.string.scan_dataset_caution_title),
                body = stringResource(R.string.scan_dataset_caution_body),
                iconResId = R.drawable.ic_warning_24,
            )
        }

        CautionCard(
            title = "Caution",
            body = "${stringResource(R.string.scan_result_limitation_body)} ${stringResource(R.string.scan_result_confirm_body)}",
            iconResId = R.drawable.ic_warning_24,
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CardTitle(text = stringResource(R.string.scan_other_classes_label))
                result.otherPossibleClasses.forEach { prediction ->
                    PredictionRow(prediction = prediction)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                text = stringResource(R.string.scan_scan_again_action),
                iconResId = R.drawable.ic_scan_24,
                onClick = onScanAgain,
            )
            SecondaryActionButton(
                text = stringResource(R.string.scan_change_image_action),
                iconResId = R.drawable.ic_photo_24,
                onClick = onChooseImage,
            )
        }
    }
}

@Composable
private fun PredictionRow(
    prediction: ScanPredictionUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(InfoContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = prediction.resultText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = prediction.internalLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = prediction.scoreText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabelValueBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SelectedImagePreview(
    selectedImageUri: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = selectedImageUri,
            contentDescription = stringResource(
                R.string.scan_selected_image_content_description,
            ),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun BananaLeafHero(modifier: Modifier = Modifier) {
    val contentDescription = stringResource(R.string.home_leaf_visual_content_description)
    Box(
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
            }
            .clip(CardShape)
            .background(LeafPanel),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawRect(color = LeafPanel)
            drawRect(color = LeafPanelSoft.copy(alpha = 0.38f))

            val midX = width * 0.5f
            drawLine(
                color = LeafVein,
                start = Offset(midX, 0f),
                end = Offset(midX, height),
                strokeWidth = width * 0.025f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(midX + width * 0.02f, 0f),
                end = Offset(midX + width * 0.02f, height),
                strokeWidth = width * 0.006f,
                cap = StrokeCap.Round,
            )

            repeat(13) { index ->
                val y = height * (index + 1) / 14f
                val leftEnd = Offset(0f, y + height * 0.22f)
                val rightEnd = Offset(width, y + height * 0.22f)
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(midX, y),
                    end = leftEnd,
                    strokeWidth = width * 0.008f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.22f),
                    start = Offset(midX, y + 5f),
                    end = Offset(leftEnd.x, leftEnd.y + 5f),
                    strokeWidth = width * 0.006f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(midX, y),
                    end = rightEnd,
                    strokeWidth = width * 0.008f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.22f),
                    start = Offset(midX, y + 5f),
                    end = Offset(rightEnd.x, rightEnd.y + 5f),
                    strokeWidth = width * 0.006f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun ScreenHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = R.drawable.ic_info_24,
) {
    InfoCard(
        title = title,
        modifier = modifier,
        iconResId = iconResId,
    ) {
        BodyText(text = body)
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = R.drawable.ic_info_24,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitleRow(title = title, iconResId = iconResId)
            content()
        }
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun CautionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = R.drawable.ic_warning_24,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CautionContainer,
            contentColor = CautionContent,
        ),
        border = BorderStroke(1.dp, CautionBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitleRow(
                title = title,
                iconResId = iconResId,
                color = CautionContent,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = CautionContent,
            )
        }
    }
}

@Composable
private fun ErrorCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitleRow(
                title = title,
                iconResId = R.drawable.ic_warning_24,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun CardTitleRow(
    title: String,
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = color,
        )
        CardTitle(text = title, color = color)
    }
}

@Composable
private fun CardTitle(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun BulletText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ActionContent(text = text, iconResId = iconResId)
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    @DrawableRes iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = CardShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ActionContent(text = text, iconResId = iconResId)
    }
}

@Composable
private fun ActionContent(
    text: String,
    @DrawableRes iconResId: Int,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IconBadge(
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
    size: Int = 52,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(ChipShape)
            .background(SuccessContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size((size * 0.56f).dp),
            tint = SuccessContent,
        )
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock_24),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.scan_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfidenceBadge(
    text: String,
    confidenceText: String,
    modifier: Modifier = Modifier,
) {
    val isCaution = confidenceText == ScanResultFormatter.LOW_CONFIDENCE ||
        confidenceText == ScanResultFormatter.AMBIGUOUS_RESULT
    val containerColor = if (isCaution) CautionContainer else SuccessContainer
    val contentColor = if (isCaution) CautionContent else SuccessContent
    val iconResId = if (isCaution) R.drawable.ic_warning_24 else R.drawable.ic_check_24

    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(ChipShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
    )
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openAppPermissionSettings() {
    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(settingsIntent)
}
