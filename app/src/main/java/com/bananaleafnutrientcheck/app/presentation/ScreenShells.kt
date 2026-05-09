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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.home_heading))
        BodyText(text = stringResource(R.string.home_body))

        CautionCard(
            title = stringResource(R.string.home_caution_title),
            body = stringResource(R.string.home_caution_body),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.home_scan_action))
            }
            OutlinedButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.home_about_action))
            }
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

    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.scan_heading))
        BodyText(text = stringResource(R.string.scan_intro))

        InfoCard(
            title = stringResource(R.string.scan_guidance_title),
            body = stringResource(R.string.scan_guidance_body),
        )

        CameraCaptureCard(
            onImageCaptured = onCameraImageCaptured,
        )

        PhotoPickerCard(
            selectedImageUri = uiState.selectedImageUri,
            hasSelectedImage = uiState.hasSelectedImage,
            onChooseImage = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
            onClearImage = onClearImage,
        )

        Button(
            onClick = onAnalyzeImage,
            enabled = uiState.canAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = CardShape,
        ) {
            Text(
                text = stringResource(
                    if (uiState.isAnalyzing) {
                        R.string.scan_analyzing_action
                    } else {
                        R.string.scan_analyze_action
                    },
                ),
            )
        }

        if (uiState.isAnalyzing) {
            InfoCard(
                title = stringResource(R.string.scan_analyzing_title),
                body = stringResource(R.string.scan_analyzing_body),
            )
        }

        uiState.analysisError?.let {
            ErrorCard(
                title = stringResource(R.string.scan_error_title),
                body = stringResource(R.string.scan_error_body),
            )
        }

        uiState.result?.let { result ->
            ResultCard(result = result)

            if (result.showDatasetCaution) {
                CautionCard(
                    title = stringResource(R.string.scan_dataset_caution_title),
                    body = stringResource(R.string.scan_dataset_caution_body),
                )
            }
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    ScreenContent(modifier = modifier) {
        ScreenHeading(text = stringResource(R.string.about_heading))
        BodyText(text = stringResource(R.string.about_intro))

        InfoCard(title = stringResource(R.string.about_purpose_title)) {
            BodyText(text = stringResource(R.string.about_purpose_body))
            BodyText(text = stringResource(R.string.about_privacy_body))
        }

        InfoCard(title = stringResource(R.string.about_model_title)) {
            BodyText(text = stringResource(R.string.about_model_body))
            BulletText(text = stringResource(R.string.about_metric_test_accuracy))
            BulletText(text = stringResource(R.string.about_metric_macro_f1))
            BulletText(text = stringResource(R.string.about_metric_banana_macro_f1))
            BodyText(text = stringResource(R.string.about_model_score_note))
        }

        InfoCard(title = stringResource(R.string.about_limits_title)) {
            BulletText(text = stringResource(R.string.about_limit_uncertainty))
            BulletText(text = stringResource(R.string.about_limit_overlap))
            BulletText(text = stringResource(R.string.about_limit_screening))
            BulletText(text = stringResource(R.string.about_limit_testing))
            BulletText(text = stringResource(R.string.about_limit_no_rates))
        }

        InfoCard(title = stringResource(R.string.about_classes_title)) {
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
        )
    }
}

@Composable
private fun ClassInfoRow(
    item: AboutClassInfoItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${item.modelOrder}. ${item.displayLabel}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitle(text = stringResource(R.string.scan_take_photo_title))
            BodyText(text = stringResource(R.string.scan_take_photo_body))

            Button(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.scan_take_photo_action))
            }

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

        Button(
            onClick = {
                val captureUseCase = imageCapture ?: return@Button
                val captureFile = runCatching {
                    captureStore.createCaptureFile()
                }.getOrNull()
                if (captureFile == null) {
                    onCaptureError()
                    return@Button
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
            enabled = imageCapture != null && !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = CardShape,
        ) {
            Text(
                text = stringResource(
                    if (isCapturing) {
                        R.string.scan_capturing_photo_action
                    } else {
                        R.string.scan_capture_photo_action
                    },
                ),
            )
        }

        OutlinedButton(
            onClick = onClose,
            enabled = !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = CardShape,
        ) {
            Text(text = stringResource(R.string.scan_close_camera_action))
        }
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
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.scan_camera_open_settings_action))
            }
        } else {
            OutlinedButton(
                onClick = onRetryPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(text = stringResource(R.string.scan_camera_permission_retry_action))
            }
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
        CardTitle(text = title, color = CautionContent)
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
    selectedImageUri: String?,
    hasSelectedImage: Boolean,
    onChooseImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitle(text = stringResource(R.string.scan_choose_image_title))
            BodyText(
                text = if (hasSelectedImage) {
                    stringResource(R.string.scan_selected_image_body)
                } else {
                    stringResource(R.string.scan_choose_image_body)
                },
            )

            if (hasSelectedImage && selectedImageUri != null) {
                SelectedImagePreview(selectedImageUri = selectedImageUri)
            }

            Button(
                onClick = onChooseImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CardShape,
            ) {
                Text(
                    text = stringResource(
                        if (hasSelectedImage) {
                            R.string.scan_change_image_action
                        } else {
                            R.string.scan_choose_image_action
                        },
                    ),
                )
            }

            if (hasSelectedImage) {
                OutlinedButton(
                    onClick = onClearImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = CardShape,
                ) {
                    Text(text = stringResource(R.string.scan_clear_image_action))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: ScanResultUiModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitle(text = stringResource(R.string.scan_result_title))
            LabelValueBlock(
                label = stringResource(R.string.scan_possible_result_label),
                value = result.possibleResultText,
            )
            LabelValueBlock(
                label = stringResource(R.string.scan_model_score_label),
                value = result.topPrediction.scoreText,
            )
            Text(
                text = result.confidenceText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            BodyText(text = stringResource(R.string.scan_result_limitation_body))
            BodyText(text = stringResource(R.string.scan_result_confirm_body))

            CardTitle(text = stringResource(R.string.scan_other_classes_label))
            result.otherPossibleClasses.forEach { prediction ->
                PredictionRow(prediction = prediction)
            }
        }
    }
}

@Composable
private fun PredictionRow(
    prediction: ScanPredictionUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prediction.resultText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.scan_selected_image_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
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
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
) {
    InfoCard(
        title = title,
        modifier = modifier,
    ) {
        BodyText(text = body)
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title)
            content()
        }
    }
}

@Composable
private fun CautionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = CautionContainer,
            contentColor = CautionContent,
        ),
        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title, color = CautionContent)
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
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardTitle(text = title, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
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
