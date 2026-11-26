package info.chikoski.pixelfray

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect as AndroidRect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import info.chikoski.pixelfray.pngglitch.exports.chikoski.png_glitch.glitch.FilterInfo
import info.chikoski.pixelfray.pngglitch.exports.chikoski.png_glitch.glitch.ParamInfo
import info.chikoski.pixelfray.pngglitch.exports.chikoski.png_glitch.glitch.ParamKind
import info.chikoski.pixelfray.pngglitch.exports.chikoski.png_glitch.live.SeedMode
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

private val ColorBgPrimary = Color(0xFF0F1310)
private val ColorSurfaceDark = Color(0xFF1B211A)
private val ColorSurfaceElevated = Color(0xFF2B3327)
private val ColorAccentLime = Color(0xFFE1EDAE)
private val ColorOnAccentDark = Color(0xFF0F1310)
private val ColorTextPrimary = Color(0xFFF4F5ED)
private val ColorTextSecondary = Color(0xFFB1BAA8)
private val ColorDragHandle = Color(0xFF64705D)
private val ColorModalBackdrop = Color.Black.copy(alpha = 0.28f)
private val ColorHingeKeepClear = Color(0xFF050605)
private val ColorRecordAccent = Color(0xFFFF826F)
private val ColorDivider = Color(0xFF30372F)
private val ColorTrackInactive = Color(0xFF3C433B)

private enum class CaptureControlsStyle {
    COMPACT_BAR,
    MEDIUM_PLUS_CARD,
    TABLETOP_CARD,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlitchCameraScreen(viewModel: GlitchViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState = viewModel.uiState.collectAsState()
    val state = uiState.value
    // Not delegated: reading `previewFrame.value` here would recompose the whole screen on every
    // camera frame. Only the FPS badge and the frame statistics read it.
    val previewFrame = viewModel.previewFrame.collectAsState()

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showEngineSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    val displayRotation = rememberDisplayRotation()
    var recordingLockedRotation by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.isRecording, displayRotation) {
        if (state.isRecording) {
            if (recordingLockedRotation == null) {
                recordingLockedRotation = displayRotation
            }
        } else {
            recordingLockedRotation = null
        }
    }
    val effectiveTargetRotation = recordingLockedRotation ?: displayRotation
    val latestTargetRotation by rememberUpdatedState(effectiveTargetRotation)

    val hasCameraPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasCameraPermission by hasCameraPermissionState

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.ensureBaseAssetsLoaded(context)
    }

    LaunchedEffect(state.isBenchmarkWarmup) {
        if (!hasCameraPermission && !state.isBenchmarkWarmup) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.statusMessage, state.isRecording) {
        val msg = state.statusMessage ?: return@LaunchedEffect
        if (
            msg != GlitchViewModel.SYNTHETIC_RUN_DONE &&
            msg != GlitchViewModel.FINALIZING_RECORDING_MESSAGE &&
            !state.isRecording
        ) {
            delay(3_000L)
            viewModel.clearStatusMessage(msg)
        }
    }

    if (hasCameraPermission && !state.isBenchmarkWarmup) {
        DisposableEffect(lifecycleOwner, state.lensFacing, state.resolution) {
            var disposed = false
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                if (disposed) return@Runnable
                val cameraProvider = runCatching { providerFuture.get() }.getOrNull() ?: return@Runnable
                // The processing sizes are 16:9. Without an aspect ratio strategy CameraX prefers
                // 4:3 for ImageAnalysis, and may deliver frames much larger than needed (such as
                // 1280x960 for 540p), which costs time in CameraX's YUV to RGBA conversion and in
                // ours.
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(state.resolution.longSide, state.resolution.shortSide),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build()

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(latestTargetRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(viewModel.analysisExecutor) { imageProxy ->
                    viewModel.analyzeImage(imageProxy)
                }

                val selector = CameraSelector.Builder()
                    .requireLensFacing(state.lensFacing)
                    .build()

                runCatching {
                    cameraProvider.unbindAll()
                    boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, analysis)
                    boundAnalysis = analysis
                }
            }
            providerFuture.addListener(listener, mainExecutor)

            onDispose {
                disposed = true
                boundAnalysis = null
                boundCamera = null
                if (providerFuture.isDone) {
                    runCatching {
                        providerFuture.get().unbindAll()
                    }
                }
            }
        }
    }

    LaunchedEffect(boundAnalysis, effectiveTargetRotation) {
        boundAnalysis?.targetRotation = effectiveTargetRotation
    }

    LaunchedEffect(boundCamera, state.zoomOption) {
        val cam = boundCamera ?: return@LaunchedEffect
        val zoomState = cam.cameraInfo.zoomState.value
        val minZoom = zoomState?.minZoomRatio ?: 0.5f
        val maxZoom = zoomState?.maxZoomRatio ?: 4.0f
        val targetZoom = state.zoomOption.ratio.coerceIn(minZoom, maxZoom)
        runCatching {
            cam.cameraControl.setZoomRatio(targetZoom)
        }
    }

    val hingeBounds = rememberHorizontalHalfOpenedHinge()
    val density = LocalDensity.current
    val minHingeGapPx = with(density) { TABLETOP_MIN_HINGE_GAP_DP.dp.roundToPx() }
    val minPaneHeightPx = with(density) { TABLETOP_MIN_PANE_HEIGHT_DP.dp.roundToPx() }

    var rootTopInWindowPx by remember { mutableIntStateOf(0) }
    var rootHeightPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgPrimary)
            .systemBarsPadding()
            .onGloballyPositioned { coords ->
                rootTopInWindowPx = coords.positionInWindow().y.roundToInt()
                rootHeightPx = coords.size.height
            },
    ) {
        val tabletopSplit = remember(
            hingeBounds,
            rootTopInWindowPx,
            rootHeightPx,
            minHingeGapPx,
            minPaneHeightPx,
        ) {
            hingeBounds?.let { bounds ->
                computeTabletopSplitPx(
                    contentTopPx = rootTopInWindowPx,
                    contentHeightPx = rootHeightPx,
                    hingeTopPx = bounds.top,
                    hingeBottomPx = bounds.bottom,
                    minHingeGapPx = minHingeGapPx,
                    minPaneHeightPx = minPaneHeightPx,
                )
            }
        }
        val layoutMode = resolveLayoutMode(maxWidth.value, tabletopSplit)

        LaunchedEffect(layoutMode) {
            if (layoutMode != PixelfrayLayoutMode.COMPACT) {
                showFilterSheet = false
            }
        }

        val movablePreviewPane = remember {
            movableContentOf { cornerRadius: Dp, paneModifier: Modifier ->
                val currentState = uiState.value
                CameraPreviewPane(
                    resolution = currentState.resolution,
                    effectPass = currentState.effectPass,
                    segmentationStatus = currentState.segmentationStatus,
                    zoomOption = currentState.zoomOption,
                    statusMessage = currentState.statusMessage,
                    showPermissionPrompt = !hasCameraPermissionState.value && !currentState.isBenchmarkWarmup,
                    previewFrame = previewFrame,
                    previewRenderer = viewModel.previewRenderer,
                    cornerRadius = cornerRadius,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSelectResolution = viewModel::selectResolution,
                    onSelectEffectPass = viewModel::selectEffectPass,
                    onSelectZoom = viewModel::selectZoom,
                    modifier = paneModifier,
                )
            }
        }

        val hideMainSemanticsForModal = layoutMode == PixelfrayLayoutMode.COMPACT && showFilterSheet

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hideMainSemanticsForModal) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            if (shouldShowAppBar(layoutMode)) {
                PixelfrayAppBar(
                    actionDescription = when {
                        layoutMode != PixelfrayLayoutMode.COMPACT -> "エンジン設定"
                        showFilterSheet -> "フィルターを閉じる"
                        else -> "フィルターを開く"
                    },
                    onActionClick = {
                        if (layoutMode == PixelfrayLayoutMode.COMPACT) {
                            showFilterSheet = !showFilterSheet
                        } else {
                            showEngineSettingsSheet = true
                        }
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (layoutMode) {
                    PixelfrayLayoutMode.COMPACT -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            movablePreviewPane(
                                0.dp,
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )

                            CaptureControlsPanel(
                                style = CaptureControlsStyle.COMPACT_BAR,
                                isRecording = state.isRecording,
                                onToggleRecording = { viewModel.toggleVideoRecording(context) },
                                onCapturePng = { viewModel.captureStillPng(context) },
                                onToggleLens = viewModel::toggleCameraLens,
                            )
                        }
                    }

                    PixelfrayLayoutMode.MEDIUM_PLUS -> {
                        val sidePanelWidth = (this@BoxWithConstraints.maxWidth * 0.33f).coerceIn(280.dp, 340.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            movablePreviewPane(
                                20.dp,
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )

                            Column(
                                modifier = Modifier
                                    .width(sidePanelWidth)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilterPanelContent(
                                    state = state,
                                    layoutMode = PixelfrayLayoutMode.MEDIUM_PLUS,
                                    showDoneButton = false,
                                    onDismiss = {},
                                    onOpenEngineSettings = { showEngineSettingsSheet = true },
                                    onSelectControlMode = viewModel::selectControlMode,
                                    onSelectPreset = viewModel::selectPreset,
                                    onSelectFilter = viewModel::selectSingleFilter,
                                    onUpdateParam = viewModel::updateFilterParam,
                                    onResetParams = viewModel::resetFilterParams,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ColorSurfaceDark),
                                )

                                CaptureControlsPanel(
                                    style = CaptureControlsStyle.MEDIUM_PLUS_CARD,
                                    isRecording = state.isRecording,
                                    onToggleRecording = { viewModel.toggleVideoRecording(context) },
                                    onCapturePng = { viewModel.captureStillPng(context) },
                                    onToggleLens = viewModel::toggleCameraLens,
                                )
                            }
                        }
                    }

                    PixelfrayLayoutMode.TABLETOP -> {
                        val split = tabletopSplit ?: return@Box
                        val topPaneDp = with(density) { split.topPanePx.toDp() }
                        val hingeGapDp = with(density) { split.hingeGapPx.toDp() }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(topPaneDp),
                            ) {
                                movablePreviewPane(0.dp, Modifier.fillMaxSize())
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hingeGapDp)
                                    .background(ColorHingeKeepClear),
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                FilterPanelContent(
                                    state = state,
                                    layoutMode = PixelfrayLayoutMode.TABLETOP,
                                    showDoneButton = false,
                                    onDismiss = {},
                                    onOpenEngineSettings = { showEngineSettingsSheet = true },
                                    onSelectControlMode = viewModel::selectControlMode,
                                    onSelectPreset = viewModel::selectPreset,
                                    onSelectFilter = viewModel::selectSingleFilter,
                                    onUpdateParam = viewModel::updateFilterParam,
                                    onResetParams = viewModel::resetFilterParams,
                                    modifier = Modifier
                                        .weight(568f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ColorSurfaceDark),
                                )

                                CaptureControlsPanel(
                                    style = CaptureControlsStyle.TABLETOP_CARD,
                                    isRecording = state.isRecording,
                                    onToggleRecording = { viewModel.toggleVideoRecording(context) },
                                    onCapturePng = { viewModel.captureStillPng(context) },
                                    onToggleLens = viewModel::toggleCameraLens,
                                    modifier = Modifier
                                        .weight(224f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (layoutMode == PixelfrayLayoutMode.COMPACT) {
            CompactFilterBottomSheetOverlay(
                visible = showFilterSheet,
                state = state,
                onDismiss = { showFilterSheet = false },
                onOpenEngineSettings = {
                    showFilterSheet = false
                    showEngineSettingsSheet = true
                },
                onSelectControlMode = viewModel::selectControlMode,
                onSelectPreset = viewModel::selectPreset,
                onSelectFilter = viewModel::selectSingleFilter,
                onUpdateParam = viewModel::updateFilterParam,
                onResetParams = viewModel::resetFilterParams,
            )
        }

        if (showEngineSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEngineSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = ColorSurfaceDark,
                contentColor = ColorTextPrimary,
            ) {
                EngineSettingsSheetContent(
                    pngFilterOption = state.pngFilterOption,
                    seedMode = state.seedMode,
                    seed = state.seed,
                    previewFrame = previewFrame,
                    onSelectPngFilter = viewModel::selectPngFilterOption,
                    onSelectSeedMode = viewModel::selectSeedMode,
                    onRandomizeSeed = viewModel::randomizeSeed,
                )
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

private fun resolveDisplayRotation(context: Context, view: View): Int {
    view.display?.rotation?.let { return it }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { context.display.rotation }.getOrNull()?.let { return it }
    }
    @Suppress("DEPRECATION")
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    @Suppress("DEPRECATION")
    return wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
}

@Composable
private fun rememberDisplayRotation(): Int {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    var rotation by remember { mutableIntStateOf(resolveDisplayRotation(context, view)) }

    LaunchedEffect(configuration, context, view) {
        rotation = resolveDisplayRotation(context, view)
    }

    DisposableEffect(context, view) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (displayManager == null) {
            onDispose {}
        } else {
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = Unit
                override fun onDisplayRemoved(displayId: Int) = Unit
                override fun onDisplayChanged(displayId: Int) {
                    val currentDisplayId = view.display?.displayId ?: Display.DEFAULT_DISPLAY
                    if (displayId == currentDisplayId) {
                        rotation = resolveDisplayRotation(context, view)
                    }
                }
            }
            displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
            onDispose {
                displayManager.unregisterDisplayListener(listener)
            }
        }
    }

    return rotation
}

@Composable
private fun rememberHorizontalHalfOpenedHinge(): AndroidRect? {
    val activity = LocalContext.current.findComponentActivity() ?: return null
    val hingeBounds by produceState<AndroidRect?>(initialValue = null, key1 = activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { layoutInfo ->
                val feature = layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull {
                        it.state == FoldingFeature.State.HALF_OPENED &&
                            it.orientation == FoldingFeature.Orientation.HORIZONTAL
                    }
                value = feature?.bounds
            }
    }
    return hingeBounds
}

@Composable
private fun PixelfrayAppBar(
    actionDescription: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorBgPrimary)
            .padding(start = 18.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Pixelfray",
            color = ColorAccentLime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onActionClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = actionDescription,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

/**
 * The camera preview and its overlay controls. It takes only the fields of [GlitchUiState] it shows
 * (all stable), so that changes elsewhere in the state, such as dragging a filter parameter slider,
 * skip this section instead of recomposing it.
 */
@Composable
private fun CameraPreviewPane(
    resolution: CameraResolutionOption,
    effectPass: EffectTargetPass,
    segmentationStatus: SegmentationStatus,
    zoomOption: CameraZoomOption,
    statusMessage: String?,
    showPermissionPrompt: Boolean,
    previewFrame: State<PreviewFrame>,
    previewRenderer: PreviewRenderer,
    cornerRadius: Dp,
    onRequestPermission: () -> Unit,
    onSelectResolution: (CameraResolutionOption) -> Unit,
    onSelectEffectPass: (EffectTargetPass) -> Unit,
    onSelectZoom: (CameraZoomOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .clipToBounds()
            .background(ColorSurfaceDark),
    ) {
        // Frames are drawn into this surface by PreviewRenderer on the presentation thread
        // (glitch-present), so a new frame involves neither recomposition nor a Compose draw
        // pass.
        AndroidExternalSurface(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            previewRenderer.backgroundColor = ColorSurfaceDark.toArgb()
            onSurface { surface, width, height ->
                previewRenderer.attach(surface, width, height)
                surface.onChanged { newWidth, newHeight -> previewRenderer.resize(this, newWidth, newHeight) }
                surface.onDestroyed { previewRenderer.detach(this) }
            }
        }

        RuleOfThirdsGrid(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                ),
        ) {
            // Top-left resolution and subject pass dropdown pills
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp, start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResolutionDropdownPill(
                    selected = resolution,
                    onSelect = onSelectResolution,
                )

                EffectPassDropdownPill(
                    selected = effectPass,
                    segmentationStatus = segmentationStatus,
                    onSelect = onSelectEffectPass,
                )
            }

            // Top-right FPS badge pill
            FpsBadgePill(
                previewFrame = previewFrame,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 16.dp),
            )

            if (showPermissionPrompt) {
                Surface(
                    color = ColorSurfaceDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 56.dp, start = 16.dp)
                        .clickable(onClick = onRequestPermission),
                ) {
                    Text(
                        text = "カメラ権限を許可",
                        color = ColorAccentLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    color = ColorSurfaceDark.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 68.dp)
                        .widthIn(max = 240.dp),
                ) {
                    Text(
                        text = msg,
                        color = ColorTextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            LensZoomSelectorPill(
                selected = zoomOption,
                onSelect = onSelectZoom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun RuleOfThirdsGrid(modifier: Modifier = Modifier) {
    val gridColor = ColorTextPrimary.copy(alpha = 0.13f)
    Canvas(modifier = modifier) {
        val strokePx = 0.5.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = gridColor,
            start = Offset(w / 3f, 0f),
            end = Offset(w / 3f, h),
            strokeWidth = strokePx,
        )
        drawLine(
            color = gridColor,
            start = Offset(w * 2f / 3f, 0f),
            end = Offset(w * 2f / 3f, h),
            strokeWidth = strokePx,
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, h / 3f),
            end = Offset(w, h / 3f),
            strokeWidth = strokePx,
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, h * 2f / 3f),
            end = Offset(w, h * 2f / 3f),
            strokeWidth = strokePx,
        )
    }
}

@Composable
private fun ResolutionDropdownPill(
    selected: CameraResolutionOption,
    onSelect: (CameraResolutionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 76.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurfaceDark)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = selected.label,
                color = ColorTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_chevron_down),
                contentDescription = "解像度を選択",
                modifier = Modifier.size(12.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ColorSurfaceDark,
        ) {
            CameraResolutionOption.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (isSelected) ColorAccentLime else ColorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun EffectPassDropdownPill(
    selected: EffectTargetPass,
    segmentationStatus: SegmentationStatus,
    onSelect: (EffectTargetPass) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val statusDescription = segmentationStatusLabel(segmentationStatus)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 76.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurfaceDark)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 10.dp)
                .semantics { contentDescription = "${selected.label}（$statusDescription）" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = selected.label,
                    color = ColorTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
                if (segmentationStatus == SegmentationStatus.UNAVAILABLE) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ColorRecordAccent),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ColorSurfaceDark,
        ) {
            EffectTargetPass.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (isSelected) ColorAccentLime else ColorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }

            if (selected.requiresSegmentation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorDivider),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SegmentationStatusIcon(status = segmentationStatus)
                    Text(
                        text = statusDescription,
                        color = if (segmentationStatus == SegmentationStatus.UNAVAILABLE) {
                            ColorRecordAccent
                        } else {
                            ColorTextSecondary
                        },
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

private fun segmentationStatusLabel(status: SegmentationStatus): String = when (status) {
    SegmentationStatus.DETECTED -> "人物検出中"
    SegmentationStatus.NO_SUBJECT -> "人物未検出"
    SegmentationStatus.INACTIVE -> "セグメンテーション無効"
    SegmentationStatus.UNAVAILABLE -> "セグメンテーション利用不可"
}

@Composable
private fun SegmentationStatusIcon(
    status: SegmentationStatus,
    modifier: Modifier = Modifier,
) {
    val description = segmentationStatusLabel(status)
    val silhouetteColor = when (status) {
        SegmentationStatus.DETECTED -> ColorAccentLime
        SegmentationStatus.NO_SUBJECT -> ColorTextSecondary
        SegmentationStatus.INACTIVE -> ColorTextSecondary.copy(alpha = 0.45f)
        SegmentationStatus.UNAVAILABLE -> ColorRecordAccent.copy(alpha = 0.85f)
    }
    val shouldersPath = remember { Path() }
    Canvas(
        modifier = modifier
            .size(18.dp)
            .semantics { contentDescription = description },
    ) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = silhouetteColor,
            radius = w * 0.17f,
            center = Offset(w * 0.46f, h * 0.31f),
        )
        shouldersPath.reset()
        shouldersPath.moveTo(w * 0.14f, h * 0.84f)
        shouldersPath.cubicTo(
            w * 0.14f, h * 0.57f,
            w * 0.78f, h * 0.57f,
            w * 0.78f, h * 0.84f,
        )
        shouldersPath.close()
        drawPath(path = shouldersPath, color = silhouetteColor)

        when (status) {
            SegmentationStatus.DETECTED -> {
                drawCircle(
                    color = ColorAccentLime,
                    radius = w * 0.10f,
                    center = Offset(w * 0.83f, h * 0.22f),
                )
            }
            SegmentationStatus.UNAVAILABLE -> {
                val start = Offset(w * 0.14f, h * 0.18f)
                val end = Offset(w * 0.84f, h * 0.86f)
                drawLine(
                    color = ColorSurfaceDark,
                    start = start,
                    end = end,
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = ColorRecordAccent,
                    start = start,
                    end = end,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            SegmentationStatus.NO_SUBJECT,
            SegmentationStatus.INACTIVE,
            -> Unit
        }
    }
}

@Composable
private fun LensZoomSelectorPill(
    selected: CameraZoomOption,
    onSelect: (CameraZoomOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(144.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurfaceDark)
            .padding(horizontal = 6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraZoomOption.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) ColorAccentLime else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isSelected) option.selectedLabel else option.unselectedLabel,
                    color = if (isSelected) ColorOnAccentDark else ColorTextSecondary,
                    fontSize = if (isSelected) 13.sp else 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun FpsBadgePill(
    previewFrame: State<PreviewFrame>,
    modifier: Modifier = Modifier,
) {
    // PreviewFrame changes on every camera frame, but the badge shows whole frames per second.
    // Reading it only inside derivedStateOf recomposes the badge when the text changes, not on
    // every frame. (The engine time, which changes every frame, is shown in the settings sheet.)
    val fpsText by remember(previewFrame) {
        derivedStateOf {
            val fps = previewFrame.value.displayFps
            if (fps > 0.0) "${fps.roundToInt().coerceIn(1, 120)} fps" else "-- fps"
        }
    }
    val perfDesc by remember(previewFrame) {
        derivedStateOf {
            val frame = previewFrame.value
            if (frame.width > 0) "${frame.width}x${frame.height} ($fpsText)" else fpsText
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 62.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ColorSurfaceDark)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = perfDesc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fpsText,
            color = ColorTextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun CaptureControlsPanel(
    style: CaptureControlsStyle,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onCapturePng: () -> Unit,
    onToggleLens: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoButton: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(ColorSurfaceDark)
                .then(
                    if (isRecording) {
                        Modifier.border(2.dp, ColorRecordAccent, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onToggleRecording),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_video_mode),
                contentDescription = if (isRecording) "録画を停止" else "録画を開始",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(22.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 11.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) ColorRecordAccent else ColorAccentLime),
            )
        }
    }

    val shutterControl: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCapturePng),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_shutter_outer),
                    contentDescription = "写真を撮影",
                    modifier = Modifier.size(66.dp),
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_shutter_inner),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "写真",
                color = ColorAccentLime,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    val switchCameraButton: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(ColorSurfaceDark)
                .clickable(onClick = onToggleLens),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_flip_camera),
                contentDescription = "カメラ切替",
                modifier = Modifier.size(24.dp),
            )
        }
    }

    when (style) {
        CaptureControlsStyle.COMPACT_BAR -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(ColorBgPrimary),
                horizontalArrangement = Arrangement.spacedBy(61.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                videoButton()
                shutterControl()
                switchCameraButton()
            }
        }

        CaptureControlsStyle.MEDIUM_PLUS_CARD -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorSurfaceDark)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                videoButton()
                shutterControl()
                switchCameraButton()
            }
        }

        CaptureControlsStyle.TABLETOP_CARD -> {
            Column(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorSurfaceDark),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                videoButton()
                shutterControl()
                switchCameraButton()
            }
        }
    }
}

@Composable
private fun CompactFilterBottomSheetOverlay(
    visible: Boolean,
    state: GlitchUiState,
    onDismiss: () -> Unit,
    onOpenEngineSettings: () -> Unit,
    onSelectControlMode: (ControlMode) -> Unit,
    onSelectPreset: (RecipePreset) -> Unit,
    onSelectFilter: (FilterInfo) -> Unit,
    onUpdateParam: (String, String) -> Unit,
    onResetParams: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 72.dp.toPx() }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            dragOffsetPx = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorModalBackdrop)
                    .semantics {
                        contentDescription = "フィルターシートを閉じる"
                        onClick(label = "閉じる") {
                            onDismiss()
                            true
                        }
                    }
                    .pointerInput(onDismiss) {
                        detectTapGestures { onDismiss() }
                    },
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = dragOffsetPx }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(ColorSurfaceDark)
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "ドラッグしてフィルターシートを閉じる"
                            onClick(label = "閉じる") {
                                onDismiss()
                                true
                            }
                        }
                        .pointerInput(dismissThresholdPx) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetPx > dismissThresholdPx) {
                                        onDismiss()
                                    } else {
                                        dragOffsetPx = 0f
                                    }
                                },
                                onDragCancel = { dragOffsetPx = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                                },
                            )
                        }
                        .padding(top = 10.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ColorDragHandle),
                    )
                }

                FilterPanelContent(
                    state = state,
                    layoutMode = PixelfrayLayoutMode.COMPACT,
                    showDoneButton = true,
                    onDismiss = onDismiss,
                    onOpenEngineSettings = onOpenEngineSettings,
                    onSelectControlMode = onSelectControlMode,
                    onSelectPreset = onSelectPreset,
                    onSelectFilter = onSelectFilter,
                    onUpdateParam = onUpdateParam,
                    onResetParams = onResetParams,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterPanelContent(
    state: GlitchUiState,
    layoutMode: PixelfrayLayoutMode,
    showDoneButton: Boolean,
    onDismiss: () -> Unit,
    onOpenEngineSettings: () -> Unit,
    onSelectControlMode: (ControlMode) -> Unit,
    onSelectPreset: (RecipePreset) -> Unit,
    onSelectFilter: (FilterInfo) -> Unit,
    onUpdateParam: (String, String) -> Unit,
    onResetParams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = if (layoutMode == PixelfrayLayoutMode.COMPACT) 4.dp else 24.dp
    val bottomPadding = if (layoutMode == PixelfrayLayoutMode.COMPACT) 0.dp else 20.dp

    Column(
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding,
        ),
    ) {
        // Header row: "フィルター" + Applied indicator + Engine Settings + optional "完了" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "フィルター",
                color = ColorTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.width(12.dp))

            val isEffectActive = state.effectPass != EffectTargetPass.NONE
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorSurfaceElevated)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isEffectActive) ColorAccentLime else ColorTextSecondary),
                )
                Text(
                    text = if (isEffectActive) "適用中" else "オフ",
                    color = if (isEffectActive) ColorAccentLime else ColorTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorSurfaceElevated)
                    .clickable(onClick = onOpenEngineSettings),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "エンジン設定",
                    modifier = Modifier.size(18.dp),
                )
            }

            if (showDoneButton) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 60.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorSurfaceElevated)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "完了",
                        color = ColorAccentLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented tab bar: "プリセット" / "シングル"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ColorSurfaceElevated)
                .padding(3.dp)
                .selectableGroup(),
        ) {
            ControlMode.entries.forEach { mode ->
                val isSelected = state.controlMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) ColorAccentLime else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelectControlMode(mode) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) ColorOnAccentDark else ColorTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (layoutMode) {
            PixelfrayLayoutMode.COMPACT -> {
                if (state.controlMode == ControlMode.PRESETS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecipePresets.curated.forEach { preset ->
                            FilterThumbnailCard(
                                title = preset.title,
                                thumbnail = state.presetThumbnails[preset.id] ?: state.fallbackPreviewBitmap,
                                isSelected = state.selectedPreset.id == preset.id,
                                labelFontSize = 11.sp,
                                badgeInset = 4.dp,
                                onClick = { onSelectPreset(preset) },
                                modifier = Modifier.width(81.dp),
                                imageModifier = Modifier
                                    .width(81.dp)
                                    .height(82.dp),
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.availableFilters.forEach { filter ->
                            FilterThumbnailCard(
                                title = filter.name,
                                thumbnail = state.filterThumbnails[filter.name] ?: state.fallbackPreviewBitmap,
                                isSelected = state.selectedFilter?.name == filter.name,
                                labelFontSize = 11.sp,
                                badgeInset = 4.dp,
                                onClick = { onSelectFilter(filter) },
                                modifier = Modifier.width(81.dp),
                                imageModifier = Modifier
                                    .width(81.dp)
                                    .height(82.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorDivider),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SingleFilterDetailControls(
                            state = state,
                            onUpdateParam = onUpdateParam,
                            onResetParams = onResetParams,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            PixelfrayLayoutMode.MEDIUM_PLUS -> {
                if (state.controlMode == ControlMode.PRESETS) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val rows = RecipePresets.curated.chunked(2)
                        rows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { preset ->
                                    FilterThumbnailCard(
                                        title = preset.title,
                                        thumbnail = state.presetThumbnails[preset.id] ?: state.fallbackPreviewBitmap,
                                        isSelected = state.selectedPreset.id == preset.id,
                                        labelFontSize = 12.sp,
                                        badgeInset = 6.dp,
                                        onClick = { onSelectPreset(preset) },
                                        modifier = Modifier.weight(1f),
                                        imageModifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(136f / 132f),
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            state.availableFilters.forEach { filter ->
                                FilterThumbnailCard(
                                    title = filter.name,
                                    thumbnail = state.filterThumbnails[filter.name] ?: state.fallbackPreviewBitmap,
                                    isSelected = state.selectedFilter?.name == filter.name,
                                    labelFontSize = 11.sp,
                                    badgeInset = 4.dp,
                                    onClick = { onSelectFilter(filter) },
                                    modifier = Modifier.width(76.dp),
                                    imageModifier = Modifier.size(76.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ColorDivider),
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            SingleFilterDetailControls(
                                state = state,
                                onUpdateParam = onUpdateParam,
                                onResetParams = onResetParams,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            PixelfrayLayoutMode.TABLETOP -> {
                if (state.controlMode == ControlMode.PRESETS) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RecipePresets.curated.forEach { preset ->
                                FilterThumbnailCard(
                                    title = preset.title,
                                    thumbnail = state.presetThumbnails[preset.id] ?: state.fallbackPreviewBitmap,
                                    isSelected = state.selectedPreset.id == preset.id,
                                    labelFontSize = 12.sp,
                                    badgeInset = 6.dp,
                                    onClick = { onSelectPreset(preset) },
                                    modifier = Modifier.width(124.dp),
                                    imageModifier = Modifier.size(124.dp),
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            state.availableFilters.forEach { filter ->
                                FilterThumbnailCard(
                                    title = filter.name,
                                    thumbnail = state.filterThumbnails[filter.name] ?: state.fallbackPreviewBitmap,
                                    isSelected = state.selectedFilter?.name == filter.name,
                                    labelFontSize = 11.sp,
                                    badgeInset = 4.dp,
                                    onClick = { onSelectFilter(filter) },
                                    modifier = Modifier.width(80.dp),
                                    imageModifier = Modifier.size(80.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ColorDivider),
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SingleFilterDetailControls(
                            state = state,
                            onUpdateParam = onUpdateParam,
                            onResetParams = onResetParams,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Text(
            text = if (state.controlMode == ControlMode.PRESETS) {
                "フィルターは写真・動画に適用されます"
            } else {
                "変更はプレビューにリアルタイムで反映されます"
            },
            color = ColorTextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SingleFilterDetailControls(
    state: GlitchUiState,
    onUpdateParam: (String, String) -> Unit,
    onResetParams: () -> Unit,
) {
    val filter = state.selectedFilter ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = filter.name,
            color = ColorTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "リセット",
            color = ColorTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onResetParams)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = filter.doc,
        color = ColorTextSecondary,
        fontSize = 11.sp,
    )
    Spacer(modifier = Modifier.height(14.dp))

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        filter.params.forEach { param ->
            key(filter.name, param.name) {
                PixelfrayParameterControl(
                    param = param,
                    currentValue = state.filterParams[param.name]
                        ?: RecipePresets.defaultParamValue(filter, param),
                    onValueChange = { newVal -> onUpdateParam(param.name, newVal) },
                )
            }
        }
    }
}

@Composable
private fun FilterThumbnailCard(
    title: String,
    thumbnail: Bitmap?,
    isSelected: Boolean,
    labelFontSize: TextUnit,
    badgeInset: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
    ) {
        Box(
            modifier = imageModifier
                .clip(RoundedCornerShape(9.dp))
                .background(ColorSurfaceDark)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, ColorAccentLime, RoundedCornerShape(9.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (thumbnail != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    val img = thumbnail.asImageBitmap()
                    val scale = max(
                        size.width / img.width.toFloat(),
                        size.height / img.height.toFloat(),
                    )
                    val dstW = (img.width * scale).roundToInt()
                    val dstH = (img.height * scale).roundToInt()
                    val offsetX = ((size.width - dstW) / 2f).roundToInt()
                    val offsetY = ((size.height - dstH) / 2f).roundToInt()
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(offsetX, offsetY),
                        dstSize = IntSize(dstW, dstH),
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = badgeInset, end = badgeInset)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ColorAccentLime),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_selected_check),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            color = if (isSelected) ColorAccentLime else ColorTextSecondary,
            fontSize = labelFontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PixelfrayParameterControl(
    param: ParamInfo,
    currentValue: String,
    onValueChange: (String) -> Unit,
) {
    when (param.kind) {
        ParamKind.FLOAT -> {
            val range = RecipePresets.sliderRangeFor(param)
            val numeric = (currentValue.toFloatOrNull() ?: range.start).coerceIn(range.start, range.endInclusive)
            val displayValue = if (range.start == 0f && range.endInclusive == 1f) {
                "${(numeric * 100f).roundToInt()}%"
            } else {
                RecipePresets.formatFloat(numeric.toDouble())
            }
            PixelfraySliderRow(
                label = param.name,
                valueText = displayValue,
                value = numeric,
                valueRange = range,
                onValueChange = { onValueChange(RecipePresets.formatFloat(it.toDouble())) },
            )
        }
        ParamKind.INT -> {
            val range = RecipePresets.sliderRangeFor(param)
            val numeric = (currentValue.toFloatOrNull() ?: range.start).coerceIn(range.start, range.endInclusive)
            PixelfraySliderRow(
                label = param.name,
                valueText = numeric.roundToInt().toString(),
                value = numeric,
                valueRange = range,
                onValueChange = { onValueChange(it.roundToInt().toString()) },
            )
        }
        ParamKind.ENUM, ParamKind.STRATEGY -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = param.name,
                        color = ColorTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = currentValue,
                        color = ColorAccentLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    param.choices.forEach { choice ->
                        val isSelected = currentValue == choice
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ColorAccentLime else ColorSurfaceElevated)
                                .clickable { onValueChange(choice) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = choice,
                                color = if (isSelected) ColorOnAccentDark else ColorTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelfraySliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $valueText"
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    val clamped = target.coerceIn(valueRange.start, valueRange.endInclusive)
                    currentOnValueChange(clamped)
                    true
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = ColorTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = valueText,
                color = ColorAccentLime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { size ->
                    trackWidthPx = size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val f = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        currentOnValueChange(valueRange.start + f * span)
                    }
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val f = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        currentOnValueChange(valueRange.start + f * span)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Inactive track (4dp height, #3C433B)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ColorTrackInactive),
            )

            // Active track (4dp height, #E1EDAE)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ColorAccentLime),
            )

            // Slider thumb (16dp, R.drawable.ic_slider_thumb)
            val thumbOffsetDp = with(density) {
                val thumbRadiusPx = 8.dp.toPx()
                val rawPx = (fraction * trackWidthPx - thumbRadiusPx)
                    .coerceIn(0f, (trackWidthPx - thumbRadiusPx * 2f).coerceAtLeast(0f))
                rawPx.toDp()
            }
            Image(
                painter = painterResource(id = R.drawable.ic_slider_thumb),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun EngineSettingsSheetContent(
    pngFilterOption: PngFilterOption,
    seedMode: SeedMode,
    seed: ULong,
    previewFrame: State<PreviewFrame>,
    onSelectPngFilter: (PngFilterOption) -> Unit,
    onSelectSeedMode: (SeedMode) -> Unit,
    onRandomizeSeed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "エンジン設定",
            color = ColorTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PNG Filter Strategy",
                color = ColorTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PngFilterOption.entries.forEach { option ->
                    val isSelected = pngFilterOption == option
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ColorAccentLime else ColorSurfaceElevated)
                            .clickable { onSelectPngFilter(option) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) ColorOnAccentDark else ColorTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Seed Mode",
                color = ColorTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isPerFrame = seedMode == SeedMode.PER_FRAME
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPerFrame) ColorAccentLime else ColorSurfaceElevated)
                        .clickable { onSelectSeedMode(SeedMode.PER_FRAME) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "PerFrame",
                        color = if (isPerFrame) ColorOnAccentDark else ColorTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                val isFixed = seedMode == SeedMode.FIXED
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFixed) ColorAccentLime else ColorSurfaceElevated)
                        .clickable { onSelectSeedMode(SeedMode.FIXED) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Fixed ($seed)",
                        color = if (isFixed) ColorOnAccentDark else ColorTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ColorAccentLime, RoundedCornerShape(10.dp))
                        .clickable(onClick = onRandomizeSeed)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "New Seed",
                        color = ColorAccentLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        FrameStatsText(previewFrame = previewFrame)

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** Frame statistics; reads [previewFrame] itself so that only this text recomposes per frame. */
@Composable
private fun FrameStatsText(previewFrame: State<PreviewFrame>) {
    val frame = previewFrame.value
    if (frame.width > 0) {
        Text(
            text = String.format(
                Locale.US,
                "Frame: %dx%d  ·  %.1f ms/frame in engine  ·  %.1f fps",
                frame.width,
                frame.height,
                frame.processingTimeMs,
                frame.displayFps,
            ),
            color = ColorTextSecondary,
            fontSize = 11.sp,
        )
    }
}
