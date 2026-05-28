package com.example.kakomirror.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kakomirror.R
import com.example.kakomirror.camera.CameraMirrorController
import com.example.kakomirror.model.MirrorFrame
import com.example.kakomirror.theme.KakoMirrorTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val controller = remember { CameraMirrorController(context.applicationContext) }
  var startAfterPermission by remember { mutableStateOf(false) }
  var autoStartRequested by remember { mutableStateOf(false) }
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }

  fun startCamera() {
    viewModel.startLive()
    controller.start(
      lifecycleOwner = lifecycleOwner,
      initialZoomRatio = cameraZoomRatio(state.zoomRatio, state.minZoomRatio, state.maxZoomRatio),
      onFrame = viewModel::onFrame,
      onZoomRange = viewModel::setZoomRange,
    )
    controller.setLightStrength(state.flashStrength)
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      if (granted && startAfterPermission) {
        startAfterPermission = false
        autoStartRequested = true
        startCamera()
      }
    }

  LaunchedEffect(Unit) {
    if (!autoStartRequested && hasCameraPermission) {
      autoStartRequested = true
      startCamera()
    }
  }

  LaunchedEffect(state.zoomRatio, state.mode) {
    if (state.mode == MirrorMode.Live && state.zoomRangeResolved) {
      controller.setZoomRatio(cameraZoomRatio(state.zoomRatio, state.minZoomRatio, state.maxZoomRatio))
    }
  }

  LaunchedEffect(state.flashStrength) {
    controller.setLightStrength(state.flashStrength)
  }

  DisposableEffect(Unit) {
    onDispose { controller.shutdown() }
  }

  KakoMirrorScreen(
    state = state,
    hasCameraPermission = hasCameraPermission,
    modifier = modifier,
    onStart = {
      if (hasCameraPermission) {
        startCamera()
      } else {
        startAfterPermission = true
        permissionLauncher.launch(Manifest.permission.CAMERA)
      }
    },
    onStop = {
      if (viewModel.stopToReview()) {
        controller.stop()
      }
    },
    onDelayChange = viewModel::setDelay,
    onMirrorToggle = viewModel::toggleMirror,
    onFullscreenToggle = viewModel::toggleFullscreenMirror,
    onFlashChange = { strength ->
      controller.setLightStrength(strength)
      viewModel.setFlash(strength)
    },
    onZoomChange = viewModel::setZoom,
    onReviewPositionChange = viewModel::setReviewPosition,
    onReviewPlayStart = viewModel::startReviewPlayback,
    onReviewPlayEnd = viewModel::stopReviewPlayback,
    onReviewSeekStep = viewModel::seekReviewBy,
    onReviewSeekStart = viewModel::startReviewSeek,
    onReviewSeekEnd = viewModel::stopReviewSeek,
    onLiveCoachSeen = viewModel::markLiveCoachSeen,
    onReviewCoachSeen = viewModel::markReviewCoachSeen,
  )
}

@Composable
internal fun KakoMirrorScreen(
  state: MirrorUiState,
  hasCameraPermission: Boolean,
  modifier: Modifier = Modifier,
  onStart: () -> Unit = {},
  onStop: () -> Unit = {},
  onDelayChange: (Float) -> Unit = {},
  onMirrorToggle: () -> Unit = {},
  onFullscreenToggle: () -> Unit = {},
  onFlashChange: (Float) -> Unit = {},
  onZoomChange: (Float) -> Unit = {},
  onReviewPositionChange: (Float) -> Unit = {},
  onReviewPlayStart: () -> Unit = {},
  onReviewPlayEnd: () -> Unit = {},
  onReviewSeekStep: (Float) -> Unit = {},
  onReviewSeekStart: (Float) -> Unit = {},
  onReviewSeekEnd: () -> Unit = {},
  onLiveCoachSeen: () -> Unit = {},
  onReviewCoachSeen: () -> Unit = {},
) {
  var delayPickerOpen by remember { mutableStateOf(false) }
  var manualCoachTour by remember { mutableStateOf<CoachTour?>(null) }
  var coachStepIndex by remember { mutableStateOf(0) }
  val coachTargets = remember(state.mode, delayPickerOpen) { mutableStateMapOf<CoachTarget, CoachTargetBounds>() }

  LaunchedEffect(state.mode) {
    if (state.mode != MirrorMode.Live) delayPickerOpen = false
  }
  val automaticCoachTour =
    when {
      state.mode == MirrorMode.Live && !state.liveCoachSeen -> CoachTour.LiveFirstRun
      state.mode == MirrorMode.Review && !state.reviewCoachSeen -> CoachTour.ReviewFirstRun
      else -> null
  }
  val activeCoachTour = manualCoachTour ?: automaticCoachTour
  val helpButtonTopPadding = helpButtonTopPadding()

  LaunchedEffect(activeCoachTour) {
    coachStepIndex = 0
    if (activeCoachTour == CoachTour.LiveFirstRun || activeCoachTour == CoachTour.LiveHelp) {
      delayPickerOpen = false
    }
  }

  fun finishCoachTour(tour: CoachTour) {
    when (tour) {
      CoachTour.LiveFirstRun -> onLiveCoachSeen()
      CoachTour.ReviewFirstRun -> onReviewCoachSeen()
      CoachTour.LiveHelp,
      CoachTour.ReviewHelp -> Unit
    }
    manualCoachTour = null
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black),
  ) {
    FramePreview(
      frame = state.currentFrame,
      mirrorFlip = state.mirrorFlip,
      zoomRatio = state.zoomRatio,
      minZoomRatio = state.minZoomRatio,
      maxZoomRatio = state.maxZoomRatio,
      onZoomChange = onZoomChange,
      flashStrength = state.flashStrength,
      fullscreenMirror = state.fullscreenMirror,
      modifier = Modifier.fillMaxSize(),
    )

    if (state.mode == MirrorMode.Idle && state.currentFrame == null) {
      StartPanel(
        hasCameraPermission = hasCameraPermission,
        onStart = onStart,
        modifier = Modifier.align(Alignment.Center),
      )
    }

    if (state.mode == MirrorMode.Live && state.currentFrame == null) {
      DelayLoadingOverlay(
        remainingSeconds =
          (state.delaySeconds - state.bufferSeconds)
            .takeIf { state.delaySeconds > 0.05f }
            ?.coerceAtLeast(0f),
        modifier = Modifier.align(Alignment.Center),
      )
    }

    if (state.mode == MirrorMode.Live && !delayPickerOpen) {
      PreviewSliders(
        state = state,
        onFlashChange = onFlashChange,
        onZoomChange = onZoomChange,
        coachTargets = coachTargets,
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (state.mode != MirrorMode.Idle) {
      ControlPanel(
        state = state,
        onStart = onStart,
        onStop = onStop,
        onMirrorToggle = onMirrorToggle,
        onFullscreenToggle = onFullscreenToggle,
        delayPickerOpen = delayPickerOpen,
        onDelayPickerToggle = { delayPickerOpen = !delayPickerOpen },
        onDelayChange = { seconds ->
          delayPickerOpen = false
          onDelayChange(seconds)
        },
        onFlashChange = onFlashChange,
        onReviewPositionChange = onReviewPositionChange,
        onReviewPlayStart = onReviewPlayStart,
        onReviewPlayEnd = onReviewPlayEnd,
        onReviewSeekStep = onReviewSeekStep,
        onReviewSeekStart = onReviewSeekStart,
        onReviewSeekEnd = onReviewSeekEnd,
        coachTargets = coachTargets,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding(),
      )
    }
    if ((state.mode == MirrorMode.Live || state.mode == MirrorMode.Review) && activeCoachTour == null) {
      CoachHelpButton(
        onClick = {
          if (state.mode == MirrorMode.Live) delayPickerOpen = false
          manualCoachTour = if (state.mode == MirrorMode.Review) CoachTour.ReviewHelp else CoachTour.LiveHelp
        },
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(top = helpButtonTopPadding, end = HelpButtonEndPadding),
      )
    }
    activeCoachTour?.let { tour ->
      val steps = coachStepsFor(tour)
      val step = steps[coachStepIndex.coerceIn(0, steps.lastIndex)]
      val stepTargetBounds = step.targets.mapNotNull { coachTargets[it] }
      if (stepTargetBounds.size == step.targets.size) {
        CoachMarkOverlay(
          step = step,
          targetBounds = stepTargetBounds,
          currentStep = coachStepIndex + 1,
          totalSteps = steps.size,
          onSkip = { finishCoachTour(tour) },
          onNext = {
            if (coachStepIndex >= steps.lastIndex) {
              finishCoachTour(tour)
            } else {
              coachStepIndex += 1
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun CoachHelpButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val label = stringResource(R.string.a11y_help)
  Box(
    modifier =
      modifier
        .size(HelpButtonTouchSize)
        .accessibleButtonSemantics(
          label = label,
          onClickLabel = label,
          onClickAction = currentOnClick,
        )
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(R.drawable.ui_secondary_glass_v1),
      contentDescription = null,
      modifier = Modifier.size(HelpButtonVisualSize),
      contentScale = ContentScale.Fit,
    )
    Text(
      text = "?",
      color = CoachRose,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun CoachMarkOverlay(
  step: CoachStep,
  targetBounds: List<CoachTargetBounds>,
  currentStep: Int,
  totalSteps: Int,
  onSkip: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentOnSkip by rememberUpdatedState(onSkip)
  val currentOnNext by rememberUpdatedState(onNext)
  val density = LocalDensity.current
  val targetRect = unionCoachTargetBounds(targetBounds)
  BoxWithConstraints(
    modifier =
      modifier
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            waitForUpOrCancellation()
          }
        },
  ) {
    val cardWidth = if (maxWidth < 352.dp) maxWidth - 32.dp else 320.dp
    var cardSize by remember(step) { mutableStateOf(IntSize.Zero) }
    val screenWidthPx = with(density) { maxWidth.toPx() }
    val screenHeightPx = with(density) { maxHeight.toPx() }
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val estimatedCardHeightPx =
      if (cardSize.height > 0) {
        cardSize.height.toFloat()
      } else {
        with(density) { 142.dp.toPx() }
      }
    val marginPx = with(density) { 16.dp.toPx() }
    val cardX =
      coachCardX(
        targetRect = targetRect,
        screenWidthPx = screenWidthPx,
        cardWidthPx = cardWidthPx,
        marginPx = marginPx,
      )
    val cardY =
      coachCardY(
        targetRect = targetRect,
        screenHeightPx = screenHeightPx,
        cardHeightPx = estimatedCardHeightPx,
        marginPx = marginPx,
        gapPx = with(density) { 20.dp.toPx() },
      )
    val cardRect = Rect(cardX, cardY, cardX + cardWidthPx, cardY + estimatedCardHeightPx)

    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
      drawRect(Color.Black.copy(alpha = 0.58f))
      targetBounds.forEach { drawCoachTargetCutout(it) }
      if (targetRect != null && step.showArrow) {
        if (targetBounds.size > 1) {
          targetBounds.forEach { drawMeasuredCoachArrow(cardRect, it.rect, CoachArrowEnd.Center) }
        } else {
          drawMeasuredCoachArrow(cardRect, targetRect, step.arrowEnd)
        }
      }
      targetBounds.forEach { drawMeasuredCoachTarget(it) }
    }

    CoachCard(
      step = step,
      currentStep = currentStep,
      totalSteps = totalSteps,
      onSkip = currentOnSkip,
      onNext = currentOnNext,
      modifier =
        Modifier
          .offset { IntOffset(cardX.roundToInt(), cardY.roundToInt()) }
          .width(cardWidth)
          .onSizeChanged { cardSize = it },
    )
  }
}

@Composable
private fun CoachCard(
  step: CoachStep,
  currentStep: Int,
  totalSteps: Int,
  onSkip: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cardShape = RoundedCornerShape(14.dp)
  Surface(
    modifier =
      modifier
        .graphicsLayer {
          shadowElevation = 10.dp.toPx()
          shape = cardShape
          clip = false
        },
    shape = cardShape,
    color = Color(0xFFFFF8FC).copy(alpha = 0.97f),
    border = BorderStroke(1.dp, CoachRose.copy(alpha = 0.44f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(step.titleResId),
        color = CoachRose,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(step.bodyResId),
        color = Color(0xFF3D343A),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (totalSteps > 1) {
          Text(
            text = stringResource(R.string.coach_step_counter, currentStep, totalSteps),
            color = CoachRose.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
        } else {
          Box(modifier = Modifier.width(1.dp))
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CoachSkipButton(onClick = onSkip)
          CoachTextButton(
            text = stringResource(if (currentStep == totalSteps) R.string.coach_ok else R.string.coach_next),
            primary = true,
            onClick = onNext,
          )
        }
      }
    }
  }
}

@Composable
private fun CoachSkipButton(onClick: () -> Unit) {
  val currentOnClick by rememberUpdatedState(onClick)
  Box(
    modifier =
      Modifier
        .height(48.dp)
        .width(74.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(R.string.coach_skip),
      color = Color(0xFF5F535A),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun CoachTextButton(
  text: String,
  primary: Boolean,
  onClick: () -> Unit,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  Surface(
    modifier =
      Modifier
        .height(48.dp)
        .width(76.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    shape = RoundedCornerShape(999.dp),
    color = if (primary) CoachRose else Color.Transparent,
    border = if (primary) null else BorderStroke(1.dp, CoachRose.copy(alpha = 0.44f)),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text,
        color = if (primary) Color.White else CoachRose,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

private fun coachStepsFor(tour: CoachTour): List<CoachStep> =
  when (tour) {
    CoachTour.LiveFirstRun,
    CoachTour.LiveHelp -> liveCoachSteps()
    CoachTour.ReviewFirstRun,
    CoachTour.ReviewHelp -> reviewCoachSteps()
  }

private fun liveCoachSteps(): List<CoachStep> =
  listOf(
    CoachStep(
      targets = listOf(CoachTarget.Delay),
      titleResId = R.string.coach_live_delay_title,
      bodyResId = R.string.coach_live_delay_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.Flip),
      titleResId = R.string.coach_live_flip_title,
      bodyResId = R.string.coach_live_flip_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.DisplayMode),
      titleResId = R.string.coach_live_display_title,
      bodyResId = R.string.coach_live_display_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.Light),
      titleResId = R.string.coach_live_light_title,
      bodyResId = R.string.coach_live_light_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.FineLight, CoachTarget.FineZoom),
      titleResId = R.string.coach_live_fine_title,
      bodyResId = R.string.coach_live_fine_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.Stop),
      titleResId = R.string.coach_live_stop_title,
      bodyResId = R.string.coach_live_stop_body,
    ),
  )

private fun reviewCoachSteps(): List<CoachStep> =
  listOf(
    CoachStep(
      targets = listOf(CoachTarget.ReviewSlider),
      titleResId = R.string.coach_review_slider_title,
      bodyResId = R.string.coach_review_slider_body,
      arrowEnd = CoachArrowEnd.TargetEdge,
    ),
    CoachStep(
      targets = listOf(CoachTarget.ReviewBack, CoachTarget.ReviewForward),
      titleResId = R.string.coach_review_step_title,
      bodyResId = R.string.coach_review_step_body,
    ),
    CoachStep(
      targets = listOf(CoachTarget.Live),
      titleResId = R.string.coach_review_live_title,
      bodyResId = R.string.coach_review_live_body,
    ),
  )

private data class CoachStep(
  val targets: List<CoachTarget>,
  @param:StringRes val titleResId: Int,
  @param:StringRes val bodyResId: Int,
  val showArrow: Boolean = true,
  val arrowEnd: CoachArrowEnd = CoachArrowEnd.Center,
)

private enum class CoachArrowEnd {
  Center,
  TargetEdge,
}

private enum class CoachTour {
  LiveFirstRun,
  LiveHelp,
  ReviewFirstRun,
  ReviewHelp,
}

private enum class CoachTarget {
  Stop,
  Delay,
  DisplayMode,
  Light,
  FineLight,
  FineZoom,
  Flip,
  ReviewSlider,
  ReviewBack,
  ReviewForward,
  Live,
}

private data class CoachTargetBounds(
  val rect: Rect,
  val shape: CoachTargetShape,
)

private enum class CoachTargetShape {
  Circle,
  Rounded,
}

private fun Modifier.coachTarget(
  target: CoachTarget,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>,
  shape: CoachTargetShape = CoachTargetShape.Circle,
): Modifier =
  onGloballyPositioned { coordinates ->
    val rect = coordinates.boundsInRoot()
    if (rect.width > 0f && rect.height > 0f) {
      coachTargets[target] = CoachTargetBounds(rect = rect, shape = shape)
    }
  }

private fun unionCoachTargetBounds(bounds: List<CoachTargetBounds>): Rect? {
  if (bounds.isEmpty()) return null
  return bounds
    .drop(1)
    .fold(bounds.first().rect) { acc, next ->
      Rect(
        left = minOf(acc.left, next.rect.left),
        top = minOf(acc.top, next.rect.top),
        right = maxOf(acc.right, next.rect.right),
        bottom = maxOf(acc.bottom, next.rect.bottom),
      )
    }
}

private fun coachCardX(
  targetRect: Rect?,
  screenWidthPx: Float,
  cardWidthPx: Float,
  marginPx: Float,
): Float {
  val preferred = (targetRect?.center?.x ?: screenWidthPx / 2f) - cardWidthPx / 2f
  val maxX = (screenWidthPx - cardWidthPx - marginPx).coerceAtLeast(marginPx)
  return preferred.coerceIn(marginPx, maxX)
}

private fun coachCardY(
  targetRect: Rect?,
  screenHeightPx: Float,
  cardHeightPx: Float,
  marginPx: Float,
  gapPx: Float,
): Float {
  if (targetRect == null) return (screenHeightPx - cardHeightPx) / 2f
  val preferred =
    if (targetRect.center.y > screenHeightPx * 0.50f) {
      targetRect.top - cardHeightPx - gapPx
    } else {
      targetRect.bottom + gapPx
    }
  val maxY = (screenHeightPx - cardHeightPx - marginPx).coerceAtLeast(marginPx)
  return preferred.coerceIn(marginPx, maxY)
}

private fun paddedRect(rect: Rect, padding: Float): Rect =
  Rect(
    left = rect.left - padding,
    top = rect.top - padding,
    right = rect.right + padding,
    bottom = rect.bottom + padding,
  )

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoachTargetCutout(bounds: CoachTargetBounds) {
  val padding = 10.dp.toPx()
  val rect = paddedRect(bounds.rect, padding)
  when (bounds.shape) {
    CoachTargetShape.Circle -> {
      val radius = max(rect.width, rect.height) / 2f
      drawCircle(
        color = Color.Transparent,
        radius = radius,
        center = rect.center,
        blendMode = BlendMode.Clear,
      )
    }
    CoachTargetShape.Rounded -> {
      drawRoundRect(
        color = Color.Transparent,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
        blendMode = BlendMode.Clear,
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMeasuredCoachTarget(bounds: CoachTargetBounds) {
  val padding = 10.dp.toPx()
  val rect = paddedRect(bounds.rect, padding)
  val stroke = 2.2.dp.toPx()
  val glow = 9.dp.toPx()
  when (bounds.shape) {
    CoachTargetShape.Circle -> {
      val radius = max(rect.width, rect.height) / 2f
      drawCircle(CoachRose.copy(alpha = 0.22f), radius + glow, rect.center, style = Stroke(width = glow))
      drawCircle(Color.White.copy(alpha = 0.96f), radius, rect.center, style = Stroke(width = stroke))
      drawCircle(CoachRose.copy(alpha = 0.86f), radius + 4.dp.toPx(), rect.center, style = Stroke(width = 1.3.dp.toPx()))
    }
    CoachTargetShape.Rounded -> {
      val corner = 22.dp.toPx()
      drawRoundRect(
        color = CoachRose.copy(alpha = 0.20f),
        topLeft = Offset(rect.left - glow / 2f, rect.top - glow / 2f),
        size = Size(rect.width + glow, rect.height + glow),
        cornerRadius = CornerRadius(corner + glow / 2f, corner + glow / 2f),
        style = Stroke(width = glow),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.96f),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = stroke),
      )
      drawRoundRect(
        color = CoachRose.copy(alpha = 0.86f),
        topLeft = Offset(rect.left - 4.dp.toPx(), rect.top - 4.dp.toPx()),
        size = Size(rect.width + 8.dp.toPx(), rect.height + 8.dp.toPx()),
        cornerRadius = CornerRadius(corner + 4.dp.toPx(), corner + 4.dp.toPx()),
        style = Stroke(width = 1.3.dp.toPx()),
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMeasuredCoachArrow(
  cardRect: Rect,
  targetRect: Rect,
  arrowEnd: CoachArrowEnd,
) {
  val targetPoint =
    when (arrowEnd) {
      CoachArrowEnd.Center -> targetRect.center
      CoachArrowEnd.TargetEdge -> {
        if (cardRect.center.y < targetRect.center.y) {
          Offset(targetRect.center.x, targetRect.top)
        } else {
          Offset(targetRect.center.x, targetRect.bottom)
        }
      }
    }
  val startPoint =
    when {
      targetPoint.y >= cardRect.bottom -> Offset(targetPoint.x.coerceIn(cardRect.left + 22.dp.toPx(), cardRect.right - 22.dp.toPx()), cardRect.bottom)
      targetPoint.y <= cardRect.top -> Offset(targetPoint.x.coerceIn(cardRect.left + 22.dp.toPx(), cardRect.right - 22.dp.toPx()), cardRect.top)
      targetPoint.x < cardRect.left -> Offset(cardRect.left, targetPoint.y.coerceIn(cardRect.top + 22.dp.toPx(), cardRect.bottom - 22.dp.toPx()))
      else -> Offset(cardRect.right, targetPoint.y.coerceIn(cardRect.top + 22.dp.toPx(), cardRect.bottom - 22.dp.toPx()))
    }
  drawLine(
    color = CoachRose.copy(alpha = 0.92f),
    start = startPoint,
    end = targetPoint,
    strokeWidth = 2.3.dp.toPx(),
    cap = StrokeCap.Round,
  )
}

@Composable
private fun FramePreview(
  frame: MirrorFrame?,
  mirrorFlip: Boolean,
  zoomRatio: Float,
  minZoomRatio: Float,
  maxZoomRatio: Float,
  onZoomChange: (Float) -> Unit,
  flashStrength: Float,
  fullscreenMirror: Boolean,
  modifier: Modifier = Modifier,
) {
  if (frame == null) {
    Box(modifier.background(Color.Black))
    return
  }

  val bitmap = remember(frame, mirrorFlip) { frame.toDisplayBitmap(mirrorFlip) }
  val currentZoomRatio by rememberUpdatedState(zoomRatio)
  val currentMinZoomRatio by rememberUpdatedState(minZoomRatio)
  val currentMaxZoomRatio by rememberUpdatedState(maxZoomRatio)
  val currentOnZoomChange by rememberUpdatedState(onZoomChange)
  val density = LocalDensity.current
  val lightActive = flashStrength > LightActiveThreshold
  val fullscreenLightHorizontalPadding = 12.dp + (14.dp * flashStrength.coerceIn(0f, 1f))
  val fullscreenLightTopPadding = 10.dp + (12.dp * flashStrength.coerceIn(0f, 1f))
  val fullscreenLightBottomPadding = 14.dp + (16.dp * flashStrength.coerceIn(0f, 1f))
  val displayZoom = displayZoomRatio(zoomRatio)
  val horizontalPaddingPx =
    with(density) {
      if (fullscreenMirror && lightActive) {
        fullscreenLightHorizontalPadding.toPx()
      } else {
        0f
      }
    }
  val topPaddingPx =
    with(density) {
      when {
        fullscreenMirror && lightActive -> fullscreenLightTopPadding.toPx()
        fullscreenMirror -> 0f
        else -> NativeCameraTopGap.toPx()
      }
    }
  val bottomPaddingPx =
    with(density) {
      if (fullscreenMirror && lightActive) {
        fullscreenLightBottomPadding.toPx()
      } else {
        0f
      }
    }
  var previewSize by remember { mutableStateOf(IntSize.Zero) }
  var panOffset by remember(fullscreenMirror) { mutableStateOf(Offset.Zero) }

  LaunchedEffect(
    displayZoom,
    fullscreenMirror,
    lightActive,
    previewSize,
    bitmap.width,
    bitmap.height,
  ) {
    panOffset =
      if (displayZoom <= 1f) {
        Offset.Zero
      } else {
        clampPreviewPan(
          desiredOffset = panOffset,
          previewSize = previewSize,
          bitmapWidth = bitmap.width,
          bitmapHeight = bitmap.height,
          displayZoom = displayZoom,
          fullscreenMirror = fullscreenMirror,
          horizontalPaddingPx = horizontalPaddingPx,
          topPaddingPx = topPaddingPx,
          bottomPaddingPx = bottomPaddingPx,
        )
      }
  }

  Box(
    modifier =
      modifier
        .background(if (lightActive) lightFrameColor(flashStrength) else Color.Black)
        .onSizeChanged { previewSize = it }
        .pointerInput(fullscreenMirror, lightActive, previewSize) {
          var gestureZoomRatio = currentZoomRatio
          detectTransformGestures { _, pan, zoom, _ ->
            if (abs(gestureZoomRatio - currentZoomRatio) > 0.2f) {
              gestureZoomRatio = currentZoomRatio
            }
            val nextZoomRatio =
              if (zoom == 1f) {
                gestureZoomRatio
              } else {
                (gestureZoomRatio * zoom).coerceIn(currentMinZoomRatio, currentMaxZoomRatio)
              }
            if (nextZoomRatio != gestureZoomRatio) {
              gestureZoomRatio = nextZoomRatio
              currentOnZoomChange(nextZoomRatio)
            }
            val activeDisplayZoom = displayZoomRatio(nextZoomRatio)
            panOffset =
              if (activeDisplayZoom <= 1f) {
                Offset.Zero
              } else {
                clampPreviewPan(
                  desiredOffset = panOffset + pan,
                  previewSize = previewSize,
                  bitmapWidth = bitmap.width,
                  bitmapHeight = bitmap.height,
                  displayZoom = activeDisplayZoom,
                  fullscreenMirror = fullscreenMirror,
                  horizontalPaddingPx = horizontalPaddingPx,
                  topPaddingPx = topPaddingPx,
                  bottomPaddingPx = bottomPaddingPx,
                )
              }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = null,
      modifier =
        Modifier
          .fillMaxSize()
          .then(
            when {
              fullscreenMirror && lightActive ->
                Modifier.padding(
                  start = fullscreenLightHorizontalPadding,
                  end = fullscreenLightHorizontalPadding,
                  top = fullscreenLightTopPadding,
                  bottom = fullscreenLightBottomPadding,
                )
              fullscreenMirror -> Modifier
              else -> Modifier.padding(top = NativeCameraTopGap)
            },
          )
          .graphicsLayer {
            scaleX = displayZoom
            scaleY = displayZoom
            translationX = panOffset.x
            translationY = panOffset.y
            transformOrigin =
              if (fullscreenMirror) {
                TransformOrigin.Center
              } else {
                TransformOrigin(0.5f, 0f)
              }
          },
      contentScale = if (fullscreenMirror) ContentScale.Crop else ContentScale.Fit,
      alignment = if (fullscreenMirror) Alignment.Center else Alignment.TopCenter,
    )
    if (lightActive) {
      Box(
        Modifier
          .fillMaxSize()
          .background(lightFrameUiShadeBrush(flashStrength)),
      )
    }
  }
}

private fun MirrorFrame.toDisplayBitmap(mirrorFlip: Boolean): Bitmap {
  val source = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
  val matrix = Matrix()
  if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
  if (mirrorFlip) matrix.postScale(-1f, 1f)
  return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun cameraZoomRatio(
  zoomRatio: Float,
  minZoomRatio: Float,
  maxZoomRatio: Float,
): Float = zoomRatio.coerceAtMost(1f).coerceIn(minZoomRatio, maxZoomRatio)

private fun displayZoomRatio(zoomRatio: Float): Float = zoomRatio.coerceAtLeast(1f)

private fun clampPreviewPan(
  desiredOffset: Offset,
  previewSize: IntSize,
  bitmapWidth: Int,
  bitmapHeight: Int,
  displayZoom: Float,
  fullscreenMirror: Boolean,
  horizontalPaddingPx: Float,
  topPaddingPx: Float,
  bottomPaddingPx: Float,
): Offset {
  if (
    previewSize.width <= 0 ||
    previewSize.height <= 0 ||
    bitmapWidth <= 0 ||
    bitmapHeight <= 0 ||
    displayZoom <= 1f
  ) {
    return Offset.Zero
  }

  val availableWidth = (previewSize.width.toFloat() - (horizontalPaddingPx * 2f)).coerceAtLeast(1f)
  val availableHeight = (previewSize.height.toFloat() - topPaddingPx - bottomPaddingPx).coerceAtLeast(1f)
  val bitmapWidthPx = bitmapWidth.toFloat()
  val bitmapHeightPx = bitmapHeight.toFloat()
  val baseScale =
    if (fullscreenMirror) {
      max(availableWidth / bitmapWidthPx, availableHeight / bitmapHeightPx)
    } else {
      min(availableWidth / bitmapWidthPx, availableHeight / bitmapHeightPx)
    }
  val scaledWidth = bitmapWidthPx * baseScale * displayZoom
  val scaledHeight = bitmapHeightPx * baseScale * displayZoom
  val maxTranslationX = ((scaledWidth - availableWidth) / 2f).coerceAtLeast(0f)
  val extraHeight = (scaledHeight - availableHeight).coerceAtLeast(0f)
  val minTranslationY: Float
  val maxTranslationY: Float
  if (fullscreenMirror) {
    val centeredTranslationY = extraHeight / 2f
    minTranslationY = -centeredTranslationY
    maxTranslationY = centeredTranslationY
  } else {
    minTranslationY = -extraHeight
    maxTranslationY = 0f
  }

  return Offset(
    x = desiredOffset.x.coerceIn(-maxTranslationX, maxTranslationX),
    y = desiredOffset.y.coerceIn(minTranslationY, maxTranslationY),
  )
}

@Composable
private fun DelayLoadingOverlay(
  remainingSeconds: Float?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      modifier = Modifier.size(66.dp),
      shape = CircleShape,
      color = Color.Black.copy(alpha = 0.18f),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
          modifier = Modifier.size(34.dp),
          color = Color.White.copy(alpha = 0.88f),
          strokeWidth = 3.dp,
        )
      }
    }
    if (remainingSeconds != null) {
      ReadableOverlayText(formatDecimalSecondsLabel(remainingSeconds))
    }
  }
}

@Composable
private fun StartPanel(
  hasCameraPermission: Boolean,
  onStart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val startLabel =
    if (hasCameraPermission) {
      stringResource(R.string.start)
    } else {
      stringResource(R.string.allow_camera)
    }
  Column(
    modifier = modifier.padding(horizontal = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = stringResource(R.string.ready_title),
      color = Color.White.copy(alpha = 0.94f),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = if (hasCameraPermission) stringResource(R.string.ready_body) else stringResource(R.string.permission_body),
      color = Color.White.copy(alpha = 0.72f),
      style = MaterialTheme.typography.bodyMedium,
    )
    PrimaryActionButton(
      icon = PrimaryActionIcon.Start,
      active = false,
      onClick = onStart,
      holdEnabled = false,
      onHoldStart = {},
      onHoldEnd = {},
      text = if (hasCameraPermission) stringResource(R.string.start_casual) else stringResource(R.string.allow_camera),
      accessibilityLabel = startLabel,
      modifier = Modifier.padding(top = 6.dp),
    )
    Text(
      text = stringResource(R.string.no_save_short),
      color = Color.White.copy(alpha = 0.46f),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun ControlPanel(
  state: MirrorUiState,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onMirrorToggle: () -> Unit,
  onFullscreenToggle: () -> Unit,
  delayPickerOpen: Boolean,
  onDelayPickerToggle: () -> Unit,
  onDelayChange: (Float) -> Unit,
  onFlashChange: (Float) -> Unit,
  onReviewPositionChange: (Float) -> Unit,
  onReviewPlayStart: () -> Unit,
  onReviewPlayEnd: () -> Unit,
  onReviewSeekStep: (Float) -> Unit,
  onReviewSeekStart: (Float) -> Unit,
  onReviewSeekEnd: () -> Unit,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>,
  modifier: Modifier = Modifier,
) {
  val isReview = state.mode == MirrorMode.Review
  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .height(if (delayPickerOpen && !isReview) 214.dp else 150.dp),
  ) {
    val compactControls = maxWidth < CompactControlWidth
    val tinyControls = maxWidth < TinyControlWidth
    val sideControlTouchSize =
      when {
        tinyControls -> TinySecondaryControlTouchSize
        compactControls -> CompactSecondaryControlTouchSize
        else -> SecondaryControlTouchSize
      }
    val sideControlVisualSize =
      when {
        tinyControls -> TinySecondaryControlVisualSize
        compactControls -> CompactSecondaryControlVisualSize
        else -> SecondaryControlVisualSize
      }
    val primaryControlTouchSize =
      when {
        tinyControls -> TinyPrimaryControlTouchSize
        compactControls -> CompactPrimaryControlTouchSize
        else -> PrimaryControlTouchSize
      }
    val primaryControlVisualSize =
      when {
        tinyControls -> TinyPrimaryControlVisualSize
        compactControls -> CompactPrimaryControlVisualSize
        else -> PrimaryControlVisualSize
      }
    val endControlPadding =
      when {
        tinyControls -> TinyControlEndPadding
        compactControls -> CompactControlEndPadding
        else -> ControlEndPadding
      }
    val controlGap =
      when {
        tinyControls -> TinyControlGap
        compactControls -> CompactControlGap
        else -> 0.dp
      }
    val innerControlOffset =
      if (compactControls) {
        responsiveInnerControlOffset(
          containerWidth = maxWidth,
          sideControlSize = sideControlTouchSize,
          primaryControlSize = primaryControlTouchSize,
          endPadding = endControlPadding,
          gap = controlGap,
        )
      } else {
        BottomInnerControlOffset
      }
    val outerControlOffset =
      if (compactControls) {
        responsiveOuterControlOffset(
          containerWidth = maxWidth,
          sideControlSize = sideControlTouchSize,
          endPadding = endControlPadding,
          gap = controlGap,
          innerOffset = innerControlOffset,
        )
      } else {
        BottomOuterControlOffset
      }

    if (isReview) {
      ReviewTransport(
        state = state,
        onReviewPositionChange = onReviewPositionChange,
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = 62.dp)
            .offset(y = (-18).dp),
        coachTargets = coachTargets,
      )
      RoundIconButton(
        icon = ControlIcon.Rewind,
        active = false,
        onClick = { onReviewSeekStep(-REVIEW_SEEK_TAP_SECONDS) },
        holdEnabled = true,
        onHoldStart = { onReviewSeekStart(-1f) },
        onHoldEnd = onReviewSeekEnd,
        stepText = formatDecimalSecondsLabel(REVIEW_SEEK_TAP_SECONDS),
        accessibilityLabel = stringResource(R.string.a11y_seek_back, formatDecimalSecondsLabel(REVIEW_SEEK_TAP_SECONDS)),
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .offset(x = -innerControlOffset)
            .padding(bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTarget = CoachTarget.ReviewBack,
        coachTargets = coachTargets,
      )
      RoundIconButton(
        icon = ControlIcon.FastForward,
        active = false,
        onClick = { onReviewSeekStep(REVIEW_SEEK_TAP_SECONDS) },
        holdEnabled = true,
        onHoldStart = { onReviewSeekStart(1f) },
        onHoldEnd = onReviewSeekEnd,
        stepText = formatDecimalSecondsLabel(REVIEW_SEEK_TAP_SECONDS),
        accessibilityLabel = stringResource(R.string.a11y_seek_forward, formatDecimalSecondsLabel(REVIEW_SEEK_TAP_SECONDS)),
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .offset(x = innerControlOffset)
            .padding(bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTarget = CoachTarget.ReviewForward,
        coachTargets = coachTargets,
      )
      RoundIconButton(
        icon = ControlIcon.Flip,
        active = state.mirrorFlip,
        onClick = onMirrorToggle,
        accessibilityLabel = stringResource(R.string.mirror_flip),
        accessibilityState = if (state.mirrorFlip) stringResource(R.string.a11y_flip_mirror_view) else stringResource(R.string.a11y_flip_other_view),
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = endControlPadding, bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTarget = CoachTarget.Flip,
        coachTargets = coachTargets,
      )
    } else {
      if (delayPickerOpen) {
        DelayChoiceBar(
          selectedDelaySeconds = state.delaySeconds,
          onDelayChange = onDelayChange,
          modifier =
            Modifier
              .align(Alignment.TopCenter)
              .padding(horizontal = 18.dp)
              .offset(y = 6.dp),
        )
      }
      RoundIconButton(
        icon = ControlIcon.Light,
        active = state.flashStrength > LightActiveThreshold,
        onClick = { onFlashChange(nextLightStrength(state.flashStrength)) },
        holdEnabled = true,
        onHoldStart = { onFlashChange(0f) },
        accessibilityLabel = stringResource(R.string.light),
        accessibilityState = if (state.flashStrength > LightActiveThreshold) stringResource(R.string.a11y_on) else stringResource(R.string.a11y_off),
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .offset(x = -outerControlOffset)
            .padding(bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTarget = CoachTarget.Light,
        coachTargets = coachTargets,
      )
      DisplayModeButton(
        fullscreenMirror = state.fullscreenMirror,
        onClick = onFullscreenToggle,
        accessibilityLabel = stringResource(R.string.a11y_display_mode),
        accessibilityState = if (state.fullscreenMirror) stringResource(R.string.fullscreen) else stringResource(R.string.full_size),
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .offset(x = -innerControlOffset)
            .padding(bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTargets = coachTargets,
      )
      DelayPresetButton(
        delaySeconds = state.delaySeconds,
        onClick = onDelayPickerToggle,
        accessibilityLabel = stringResource(R.string.a11y_delay_button, state.delaySeconds.toInt()),
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .offset(x = innerControlOffset)
            .padding(bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTargets = coachTargets,
      )
      RoundIconButton(
        icon = ControlIcon.Flip,
        active = state.mirrorFlip,
        onClick = onMirrorToggle,
        accessibilityLabel = stringResource(R.string.mirror_flip),
        accessibilityState = if (state.mirrorFlip) stringResource(R.string.a11y_flip_mirror_view) else stringResource(R.string.a11y_flip_other_view),
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = endControlPadding, bottom = 30.dp),
        touchSize = sideControlTouchSize,
        visualSize = sideControlVisualSize,
        coachTarget = CoachTarget.Flip,
        coachTargets = coachTargets,
      )
    }
    PrimaryActionButton(
      icon = if (isReview) PrimaryActionIcon.LiveResume else PrimaryActionIcon.StopClock,
      active = state.isPlaying,
      onClick = if (isReview) onStart else onStop,
      holdEnabled = isReview,
      onHoldStart = onReviewPlayStart,
      onHoldEnd = onReviewPlayEnd,
      text = if (isReview) stringResource(R.string.live_mode) else null,
      accessibilityLabel = if (isReview) stringResource(R.string.a11y_return_live) else stringResource(R.string.a11y_stop_to_review),
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 19.dp),
      touchSize = primaryControlTouchSize,
      visualSize = primaryControlVisualSize,
      coachTarget = if (isReview) CoachTarget.Live else CoachTarget.Stop,
      coachTargets = coachTargets,
    )
  }
}

@Composable
private fun DelayChoiceBar(
  selectedDelaySeconds: Float,
  onDelayChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val choices = listOf(0f, 2f, 3f, 5f)
  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .height(66.dp),
    contentAlignment = Alignment.Center,
  ) {
    val chipGap = if (maxWidth < DelayChoiceCompactWidth) CompactDelayChoiceGap else DelayChoiceGap
    val chipWidth =
      minOf(
        DelayChoiceChipWidth,
        (maxWidth - (chipGap * (choices.size - 1).toFloat())) / choices.size,
      )
    val safeChipWidth = maxOf(MinDelayChoiceChipWidth, chipWidth)
    val trayWidth = minOf(DelayChoiceTrayWidth, maxWidth)
    Image(
      painter = painterResource(R.drawable.ui_delay_tray_v1),
      contentDescription = null,
      modifier =
        Modifier
          .width(trayWidth)
          .height(66.dp),
      contentScale = ContentScale.Fit,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(chipGap),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      choices.forEach { seconds ->
        DelayChoiceChip(
          seconds = seconds,
          selected = abs(seconds - selectedDelaySeconds) < 0.1f,
          onClick = { onDelayChange(seconds) },
          width = safeChipWidth,
        )
      }
    }
  }
}

@Composable
private fun DelayChoiceChip(
  seconds: Float,
  selected: Boolean,
  onClick: () -> Unit,
  width: Dp,
  modifier: Modifier = Modifier,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  Box(
    modifier =
      modifier
        .width(width)
        .height(50.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(if (selected) R.drawable.ui_delay_chip_selected_v1 else R.drawable.ui_delay_chip_glass_v1),
      contentDescription = null,
      modifier =
        Modifier
          .width(maxOf(44.dp, width - 2.dp))
          .height(38.dp),
      contentScale = ContentScale.Fit,
    )
    Text(
      text = formatDelayPresetLabel(seconds),
      color = if (selected) Color(0xFF2E333A) else Color.White.copy(alpha = 0.88f),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun ReviewTransport(
  state: MirrorUiState,
  onReviewPositionChange: (Float) -> Unit,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>,
  modifier: Modifier = Modifier,
) {
  val hasRange = state.reviewMaxSeconds > state.reviewMinSeconds
  val totalSeconds = (state.reviewMaxSeconds - state.reviewMinSeconds).coerceAtLeast(0f)
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .coachTarget(CoachTarget.ReviewSlider, coachTargets, CoachTargetShape.Rounded),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(44.dp)
          .pointerInput(state.reviewMinSeconds, state.reviewMaxSeconds) {
            fun updateFromX(x: Float) {
              if (!hasRange) return
              val inset = 16.dp.toPx()
              val usable = (size.width.toFloat() - inset * 2f).coerceAtLeast(1f)
              val fraction = ((x - inset) / usable).coerceIn(0f, 1f)
              onReviewPositionChange(state.reviewMinSeconds + totalSeconds * fraction)
            }
            detectTapGestures { offset -> updateFromX(offset.x) }
          }
          .pointerInput(state.reviewMinSeconds, state.reviewMaxSeconds) {
            fun updateFromX(x: Float) {
              if (!hasRange) return
              val inset = 16.dp.toPx()
              val usable = (size.width.toFloat() - inset * 2f).coerceAtLeast(1f)
              val fraction = ((x - inset) / usable).coerceIn(0f, 1f)
              onReviewPositionChange(state.reviewMinSeconds + totalSeconds * fraction)
            }
            detectDragGestures(
              onDragStart = { offset -> updateFromX(offset.x) },
              onDrag = { change, _ -> updateFromX(change.position.x) },
            )
          },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 16.dp.toPx()
        val trackStart = inset
        val trackEnd = size.width - inset
        val centerY = size.height * 0.52f
        val fraction = timelineFraction(state)
        val x = trackStart + (trackEnd - trackStart) * fraction
        val trayHeight = 20.dp.toPx()
        drawRoundRect(
          color = Color.Black.copy(alpha = 0.28f),
          topLeft = Offset(trackStart - 8.dp.toPx(), centerY - trayHeight / 2f),
          size = Size((trackEnd - trackStart) + 16.dp.toPx(), trayHeight),
          cornerRadius = CornerRadius(trayHeight / 2f, trayHeight / 2f),
        )
        drawRoundRect(
          color = Color.White.copy(alpha = 0.18f),
          topLeft = Offset(trackStart - 7.dp.toPx(), centerY - trayHeight / 2f + 1.dp.toPx()),
          size = Size((trackEnd - trackStart) + 14.dp.toPx(), 1.2.dp.toPx()),
          cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
        drawLine(
          color = Color.Black.copy(alpha = 0.38f),
          start = Offset(trackStart, centerY),
          end = Offset(trackEnd, centerY),
          strokeWidth = 5.5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = Color.White.copy(alpha = 0.66f),
          start = Offset(trackStart, centerY),
          end = Offset(x, centerY),
          strokeWidth = 4.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawCircle(color = Color.Black.copy(alpha = 0.36f), radius = 13.dp.toPx(), center = Offset(x, centerY + 1.5.dp.toPx()))
        drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 11.dp.toPx(), center = Offset(x, centerY))
        drawCircle(color = Color(0xFFC7CCD2).copy(alpha = 0.88f), radius = 8.dp.toPx(), center = Offset(x, centerY))
        drawCircle(color = MetalControlInk.copy(alpha = 0.82f), radius = 3.5.dp.toPx(), center = Offset(x, centerY))
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TimelineTimeText(formatSignedSecondsLabel(state.reviewMinSeconds))
      TimelineTimeText(
        stringResource(
          R.string.review_position_and_total_format,
          formatSignedSecondsLabel(state.reviewPositionSeconds),
          formatDecimalSecondsLabel(totalSeconds),
        ),
      )
      TimelineTimeText(formatSignedSecondsLabel(state.reviewMaxSeconds))
    }
  }
}

@Composable
private fun TimelineTimeText(text: String) {
  ReadableOverlayText(text = text)
}

@Composable
private fun ReadableOverlayText(text: String) {
  Text(
    text = text,
    color = Color.White.copy(alpha = 0.92f),
    style =
      MaterialTheme.typography.labelMedium.copy(
        shadow =
          Shadow(
            color = Color.Black.copy(alpha = 0.55f),
            offset = Offset(0f, 1.5f),
            blurRadius = 5f,
          ),
      ),
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun PrimaryActionButton(
  icon: PrimaryActionIcon,
  active: Boolean,
  onClick: () -> Unit,
  holdEnabled: Boolean,
  onHoldStart: () -> Unit,
  onHoldEnd: () -> Unit,
  modifier: Modifier = Modifier,
  text: String? = null,
  touchSize: Dp = PrimaryControlTouchSize,
  visualSize: Dp = PrimaryControlVisualSize,
  accessibilityLabel: String,
  coachTarget: CoachTarget? = null,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>? = null,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val currentOnHoldStart by rememberUpdatedState(onHoldStart)
  val currentOnHoldEnd by rememberUpdatedState(onHoldEnd)

  Box(
    modifier =
      modifier
        .size(touchSize)
        .then(if (coachTarget != null && coachTargets != null) Modifier.coachTarget(coachTarget, coachTargets) else Modifier)
        .accessibleButtonSemantics(
          label = accessibilityLabel,
          onClickLabel = accessibilityLabel,
          onClickAction = currentOnClick,
        )
        .graphicsLayer {
          scaleX = if (active) 0.97f else 1f
          scaleY = if (active) 0.97f else 1f
        }
        .pointerInput(holdEnabled) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (holdEnabled) {
              val upBeforeLongPress =
                withTimeoutOrNull(PRIMARY_LONG_PRESS_MILLIS) {
                  waitForUpOrCancellation()
                }
              if (upBeforeLongPress != null) {
                currentOnClick()
              } else {
                currentOnHoldStart()
                waitForUpOrCancellation()
                currentOnHoldEnd()
              }
            } else {
              val up = waitForUpOrCancellation()
              if (up != null) currentOnClick()
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter =
        painterResource(
          when (icon) {
            PrimaryActionIcon.LiveResume -> R.drawable.ui_primary_cool_v1
            PrimaryActionIcon.Start,
            PrimaryActionIcon.StopClock -> R.drawable.ui_primary_warm_v1
          }
        ),
      contentDescription = null,
      modifier = Modifier.size(visualSize),
      contentScale = ContentScale.Fit,
    )
    if (text != null) {
      Text(
        text = text,
        color = Color.White.copy(alpha = 0.95f),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    } else {
      PrimaryGlyph(icon = icon, modifier = Modifier.size(30.dp))
    }
  }
}

private enum class PrimaryActionIcon {
  Start,
  StopClock,
  LiveResume,
}

private enum class ControlIcon {
  Light,
  Flip,
  Rewind,
  FastForward,
}

@Composable
private fun PrimaryGlyph(icon: PrimaryActionIcon, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val color = Color.White.copy(alpha = 0.93f)
    when (icon) {
      PrimaryActionIcon.StopClock -> {
        drawCircle(
          color = color,
          radius = size.minDimension * 0.34f,
          center = center,
          style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLine(
          color = color,
          start = center,
          end = Offset(center.x, size.height * 0.26f),
          strokeWidth = 2.4.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = color,
          start = center,
          end = Offset(size.width * 0.68f, size.height * 0.58f),
          strokeWidth = 2.4.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
      PrimaryActionIcon.Start,
      PrimaryActionIcon.LiveResume -> {
        val play = Path().apply {
          moveTo(size.width * 0.38f, size.height * 0.27f)
          lineTo(size.width * 0.38f, size.height * 0.73f)
          lineTo(size.width * 0.72f, size.height * 0.50f)
          close()
        }
        drawPath(play, color)
      }
    }
  }
}

@Composable
private fun RoundIconButton(
  icon: ControlIcon,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  holdEnabled: Boolean = false,
  onHoldStart: () -> Unit = {},
  onHoldEnd: () -> Unit = {},
  stepText: String? = null,
  touchSize: Dp = SecondaryControlTouchSize,
  visualSize: Dp = SecondaryControlVisualSize,
  accessibilityLabel: String,
  accessibilityState: String? = null,
  coachTarget: CoachTarget? = null,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>? = null,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val currentOnHoldStart by rememberUpdatedState(onHoldStart)
  val currentOnHoldEnd by rememberUpdatedState(onHoldEnd)

  Surface(
    modifier =
      modifier
        .size(touchSize)
        .then(if (coachTarget != null && coachTargets != null) Modifier.coachTarget(coachTarget, coachTargets) else Modifier)
        .accessibleButtonSemantics(
          label = accessibilityLabel,
          stateLabel = accessibilityState,
          onClickLabel = accessibilityLabel,
          onClickAction = currentOnClick,
        )
        .graphicsLayer {
          shadowElevation = 8.dp.toPx()
          shape = CircleShape
          clip = false
        }
        .pointerInput(holdEnabled) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (holdEnabled) {
              val upBeforeLongPress =
                withTimeoutOrNull(TRANSPORT_LONG_PRESS_MILLIS) {
                  waitForUpOrCancellation()
                }
              if (upBeforeLongPress != null) {
                currentOnClick()
              } else {
                currentOnHoldStart()
                waitForUpOrCancellation()
                currentOnHoldEnd()
              }
            } else {
              val up = waitForUpOrCancellation()
              if (up != null) currentOnClick()
            }
          }
        },
    shape = CircleShape,
    color = Color.Transparent,
    contentColor = if (active) MetalControlInk else Color.White.copy(alpha = 0.82f),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(if (active) R.drawable.ui_secondary_metal_v1 else R.drawable.ui_secondary_glass_v1),
        contentDescription = null,
        modifier = Modifier.size(visualSize),
        contentScale = ContentScale.Fit,
      )
      if (stepText == null) {
        ControlGlyph(icon = icon, active = active, modifier = Modifier.size(23.dp))
      } else {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
          ControlGlyph(icon = icon, active = active, modifier = Modifier.size(18.dp))
          Text(
            text = stepText,
            color = if (active) MetalControlInk else Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun DelayPresetButton(
  delaySeconds: Float,
  onClick: () -> Unit,
  accessibilityLabel: String,
  modifier: Modifier = Modifier,
  touchSize: Dp = SecondaryControlTouchSize,
  visualSize: Dp = SecondaryControlVisualSize,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>? = null,
) {
  val currentOnClick by rememberUpdatedState(onClick)

  Surface(
    modifier =
      modifier
        .size(touchSize)
        .then(if (coachTargets != null) Modifier.coachTarget(CoachTarget.Delay, coachTargets) else Modifier)
        .accessibleButtonSemantics(
          label = accessibilityLabel,
          onClickLabel = accessibilityLabel,
          onClickAction = currentOnClick,
        )
        .graphicsLayer {
          shadowElevation = 8.dp.toPx()
          shape = CircleShape
          clip = false
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    shape = CircleShape,
    color = Color.Transparent,
    contentColor = MetalControlInk,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(R.drawable.ui_secondary_metal_v1),
        contentDescription = null,
        modifier = Modifier.size(visualSize),
        contentScale = ContentScale.Fit,
      )
      Text(
        text = formatDelayPresetLabel(delaySeconds),
        color = MetalControlInk,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun DisplayModeButton(
  fullscreenMirror: Boolean,
  onClick: () -> Unit,
  accessibilityLabel: String,
  accessibilityState: String,
  modifier: Modifier = Modifier,
  touchSize: Dp = SecondaryControlTouchSize,
  visualSize: Dp = SecondaryControlVisualSize,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>? = null,
) {
  val currentOnClick by rememberUpdatedState(onClick)

  Surface(
    modifier =
      modifier
        .size(touchSize)
        .then(if (coachTargets != null) Modifier.coachTarget(CoachTarget.DisplayMode, coachTargets) else Modifier)
        .accessibleButtonSemantics(
          label = accessibilityLabel,
          stateLabel = accessibilityState,
          onClickLabel = accessibilityLabel,
          onClickAction = currentOnClick,
        )
        .graphicsLayer {
          shadowElevation = 8.dp.toPx()
          shape = CircleShape
          clip = false
        }
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) currentOnClick()
          }
        },
    shape = CircleShape,
    color = Color.Transparent,
    contentColor = if (fullscreenMirror) MetalControlInk else Color.White.copy(alpha = 0.82f),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(if (fullscreenMirror) R.drawable.ui_secondary_metal_v1 else R.drawable.ui_secondary_glass_v1),
        contentDescription = null,
        modifier = Modifier.size(visualSize),
        contentScale = ContentScale.Fit,
      )
      DisplayModeGlyph(
        fullscreenMirror = fullscreenMirror,
        color = if (fullscreenMirror) MetalControlInk else Color.White.copy(alpha = 0.82f),
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
private fun DisplayModeGlyph(
  fullscreenMirror: Boolean,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val stroke = 1.8.dp.toPx()
    if (fullscreenMirror) {
      drawRect(
        color = color,
        topLeft = Offset(size.width * 0.20f, size.height * 0.24f),
        size = Size(size.width * 0.60f, size.height * 0.52f),
        style = Stroke(width = stroke),
      )
      drawLine(color, Offset(size.width * 0.10f, size.height * 0.18f), Offset(size.width * 0.26f, size.height * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.10f, size.height * 0.18f), Offset(size.width * 0.10f, size.height * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.90f, size.height * 0.18f), Offset(size.width * 0.74f, size.height * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.90f, size.height * 0.18f), Offset(size.width * 0.90f, size.height * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.10f, size.height * 0.82f), Offset(size.width * 0.26f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.10f, size.height * 0.82f), Offset(size.width * 0.10f, size.height * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.90f, size.height * 0.82f), Offset(size.width * 0.74f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.90f, size.height * 0.82f), Offset(size.width * 0.90f, size.height * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
    } else {
      drawLine(color, Offset(size.width * 0.18f, size.height * 0.18f), Offset(size.width * 0.40f, size.height * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.18f, size.height * 0.18f), Offset(size.width * 0.18f, size.height * 0.40f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.82f, size.height * 0.18f), Offset(size.width * 0.60f, size.height * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.82f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.40f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.18f, size.height * 0.82f), Offset(size.width * 0.40f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.18f, size.height * 0.82f), Offset(size.width * 0.18f, size.height * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.82f, size.height * 0.82f), Offset(size.width * 0.60f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(color, Offset(size.width * 0.82f, size.height * 0.82f), Offset(size.width * 0.82f, size.height * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
  }
}

@Composable
private fun ControlGlyph(icon: ControlIcon, active: Boolean, modifier: Modifier = Modifier) {
  val color = if (active) MetalControlInk else Color.White.copy(alpha = 0.82f)
  Canvas(modifier = modifier) {
    when (icon) {
      ControlIcon.Light -> {
        drawCircle(color = color, radius = size.minDimension * 0.18f, center = center, style = Stroke(width = 2.dp.toPx()))
        drawLine(color, Offset(center.x, size.height * 0.06f), Offset(center.x, size.height * 0.18f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, size.height * 0.82f), Offset(center.x, size.height * 0.94f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.06f, center.y), Offset(size.width * 0.18f, center.y), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.82f, center.y), Offset(size.width * 0.94f, center.y), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.22f, size.height * 0.22f), Offset(size.width * 0.30f, size.height * 0.30f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.78f, size.height * 0.22f), Offset(size.width * 0.70f, size.height * 0.30f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
      }
      ControlIcon.Flip -> {
        val y1 = size.height * 0.38f
        val y2 = size.height * 0.62f
        drawLine(color, Offset(size.width * 0.22f, y1), Offset(size.width * 0.74f, y1), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.26f, y2), Offset(size.width * 0.78f, y2), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.22f, y1), Offset(size.width * 0.36f, y1 - size.height * 0.13f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.22f, y1), Offset(size.width * 0.36f, y1 + size.height * 0.13f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.78f, y2), Offset(size.width * 0.64f, y2 - size.height * 0.13f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.78f, y2), Offset(size.width * 0.64f, y2 + size.height * 0.13f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
      }
      ControlIcon.Rewind -> drawSeekGlyph(color = color, forward = false)
      ControlIcon.FastForward -> drawSeekGlyph(color = color, forward = true)
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeekGlyph(color: Color, forward: Boolean) {
  val leftA = if (forward) 0.24f else 0.76f
  val rightA = if (forward) 0.48f else 0.52f
  val leftB = if (forward) 0.48f else 0.52f
  val rightB = if (forward) 0.72f else 0.28f
  val first = Path().apply {
    moveTo(size.width * leftA, size.height * 0.26f)
    lineTo(size.width * leftA, size.height * 0.74f)
    lineTo(size.width * rightA, size.height * 0.50f)
    close()
  }
  val second = Path().apply {
    moveTo(size.width * leftB, size.height * 0.26f)
    lineTo(size.width * leftB, size.height * 0.74f)
    lineTo(size.width * rightB, size.height * 0.50f)
    close()
  }
  drawPath(first, color)
  drawPath(second, color)
}

@Composable
private fun PreviewSliders(
  state: MirrorUiState,
  onFlashChange: (Float) -> Unit,
  onZoomChange: (Float) -> Unit,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>,
  modifier: Modifier = Modifier,
) {
  var activeSlider by remember { mutableStateOf<PreviewSliderKind?>(null) }
  val expandedState = stringResource(R.string.a11y_expanded)
  val collapsedState = stringResource(R.string.a11y_collapsed)
  Box(modifier) {
    LiveFineControlRail(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(start = 90.dp, end = 90.dp, bottom = PreviewFineControlBottomGap)
          .height(44.dp)
          .fillMaxWidth(),
    )
    FloatingVerticalSlider(
      value = state.flashStrength,
      valueRange = 0f..1f,
      onValueChange = onFlashChange,
      symbol = "○",
      expanded = activeSlider == PreviewSliderKind.Light,
      accessibilityLabel = stringResource(R.string.a11y_brightness_control),
      accessibilityState = if (activeSlider == PreviewSliderKind.Light) expandedState else collapsedState,
      onLabelClick = {
        activeSlider =
          if (activeSlider == PreviewSliderKind.Light) null else PreviewSliderKind.Light
      },
      coachTarget = CoachTarget.FineLight,
      coachTargets = coachTargets,
      modifier =
        Modifier
          .align(Alignment.CenterStart)
          .padding(start = 18.dp, top = 124.dp, bottom = PreviewFineControlBottomGap),
    )
    FloatingVerticalSlider(
      value = state.zoomRatio,
      valueRange = state.minZoomRatio..state.maxZoomRatio,
      onValueChange = onZoomChange,
      symbol = "+",
      expanded = activeSlider == PreviewSliderKind.Zoom,
      accessibilityLabel = stringResource(R.string.a11y_zoom_control),
      accessibilityState = if (activeSlider == PreviewSliderKind.Zoom) expandedState else collapsedState,
      onLabelClick = {
        activeSlider =
          if (activeSlider == PreviewSliderKind.Zoom) null else PreviewSliderKind.Zoom
      },
      coachTarget = CoachTarget.FineZoom,
      coachTargets = coachTargets,
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 18.dp, top = 124.dp, bottom = PreviewFineControlBottomGap),
    )
  }
}

@Composable
private fun LiveFineControlRail(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val centerY = size.height / 2f
    drawLine(
      color = Color.Black.copy(alpha = 0.30f),
      start = Offset(0f, centerY),
      end = Offset(size.width, centerY),
      strokeWidth = 4.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color.White.copy(alpha = 0.28f),
      start = Offset(0f, centerY),
      end = Offset(size.width, centerY),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}

@Composable
private fun FloatingVerticalSlider(
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  symbol: String,
  expanded: Boolean,
  accessibilityLabel: String,
  accessibilityState: String,
  onLabelClick: () -> Unit,
  coachTarget: CoachTarget,
  coachTargets: MutableMap<CoachTarget, CoachTargetBounds>,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val currentOnLabelClick by rememberUpdatedState(onLabelClick)
  val thumbSize = 28.dp
  val thumbSizePx = with(density) { thumbSize.toPx() }
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    var trackHeightPx by remember { mutableStateOf(1) }
    val travelHeightPx = (trackHeightPx - thumbSizePx).coerceAtLeast(1f)
    val fraction =
      if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
      } else {
        0f
      }
    fun updateFromY(y: Float) {
      val thumbRadiusPx = thumbSizePx / 2f
      val clampedY = y.coerceIn(thumbRadiusPx, trackHeightPx - thumbRadiusPx)
      val fromBottom = 1f - ((clampedY - thumbRadiusPx) / travelHeightPx).coerceIn(0f, 1f)
      val next = valueRange.start + (valueRange.endInclusive - valueRange.start) * fromBottom
      onValueChange(next.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    Box(
      modifier =
        Modifier
          .weight(1f)
          .width(36.dp)
          .onSizeChanged { trackHeightPx = it.height.coerceAtLeast(1) },
      contentAlignment = Alignment.Center,
    ) {
      if (expanded) {
        Surface(
          modifier =
            Modifier
              .fillMaxSize()
              .pointerInput(valueRange) {
                detectDragGestures(
                  onDragStart = { offset -> updateFromY(offset.y) },
                  onDrag = { change, _ -> updateFromY(change.position.y) },
                )
              },
          shape = RoundedCornerShape(999.dp),
          color = Color.Transparent,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Canvas(
              modifier =
                Modifier
                  .width(16.dp)
                  .fillMaxHeight()
                  .padding(vertical = 14.dp),
            ) {
              val cx = size.width / 2f
              drawLine(
                color = Color.Black.copy(alpha = 0.28f),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
              )
              drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
              )
            }
            Box(
              modifier =
                Modifier
                  .align(Alignment.BottomCenter)
                  .graphicsLayer { translationY = -travelHeightPx * fraction }
                  .size(thumbSize),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                painter = painterResource(R.drawable.ui_secondary_metal_v1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
              )
              Box(
                modifier =
                  Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MetalControlInk.copy(alpha = 0.86f)),
              )
            }
          }
        }
      }
    }
    Box(
      modifier =
        Modifier
          .width(50.dp)
          .height(42.dp)
          .coachTarget(coachTarget, coachTargets)
          .accessibleButtonSemantics(
            label = accessibilityLabel,
            stateLabel = accessibilityState,
            onClickLabel = accessibilityLabel,
            onClickAction = currentOnLabelClick,
          )
          .pointerInput(Unit) {
            awaitEachGesture {
              awaitFirstDown(requireUnconsumed = false)
              val up = waitForUpOrCancellation()
              if (up != null) currentOnLabelClick()
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(if (expanded) R.drawable.ui_secondary_metal_v1 else R.drawable.ui_secondary_glass_v1),
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        contentScale = ContentScale.Fit,
      )
      Text(
        symbol,
        color = if (expanded) MetalControlInk else Color.White.copy(alpha = 0.82f),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

private enum class PreviewSliderKind {
  Light,
  Zoom,
}

private fun formatOneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

private fun Modifier.accessibleButtonSemantics(
  label: String,
  stateLabel: String? = null,
  onClickLabel: String? = null,
  onClickAction: (() -> Unit)? = null,
  onLongClickLabel: String? = null,
  onLongClickAction: (() -> Unit)? = null,
): Modifier =
  semantics(mergeDescendants = true) {
    role = Role.Button
    contentDescription = label
    if (stateLabel != null) {
      stateDescription = stateLabel
    }
    if (onClickAction != null) {
      onClick(label = onClickLabel) {
        onClickAction()
        true
      }
    }
    if (onLongClickAction != null) {
      onLongClick(label = onLongClickLabel) {
        onLongClickAction()
        true
      }
    }
  }

@Composable
private fun formatDecimalSecondsLabel(value: Float): String =
  stringResource(R.string.seconds_decimal_format, formatOneDecimal(value))

@Composable
private fun formatDelayPresetLabel(value: Float): String =
  stringResource(R.string.delay_preset_format, value.toInt())

@Composable
private fun formatSignedSecondsLabel(value: Float): String {
  val normalized = if (abs(value) < 0.05f) 0f else value
  val prefix = if (normalized > 0f) "+" else if (normalized < 0f) "-" else ""
  return stringResource(R.string.signed_seconds_format, prefix, formatOneDecimal(abs(normalized)))
}

private fun timelineFraction(state: MirrorUiState): Float {
  if (state.mode != MirrorMode.Review || state.reviewMaxSeconds <= state.reviewMinSeconds) return 0.5f
  return (
    (state.reviewPositionSeconds - state.reviewMinSeconds) /
      (state.reviewMaxSeconds - state.reviewMinSeconds)
    ).coerceIn(0f, 1f)
}

@Composable
private fun helpButtonTopPadding(): Dp {
  val density = LocalDensity.current
  val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
  val preferredTop = statusBarTop + HelpButtonTopPadding
  val maxTopInsideLetterbox = NativeCameraTopGap - HelpButtonTouchSize
  return preferredTop.coerceAtMost(maxTopInsideLetterbox).coerceAtLeast(0.dp)
}

private fun responsiveInnerControlOffset(
  containerWidth: Dp,
  sideControlSize: Dp,
  primaryControlSize: Dp,
  endPadding: Dp,
  gap: Dp,
): Dp {
  val minOffset = (primaryControlSize / 2) + (sideControlSize / 2) + gap
  val maxOffset = (containerWidth / 2) - endPadding - sideControlSize - (sideControlSize / 2) - gap
  return BottomInnerControlOffset.coerceBetween(minOffset, maxOffset)
}

private fun responsiveOuterControlOffset(
  containerWidth: Dp,
  sideControlSize: Dp,
  endPadding: Dp,
  gap: Dp,
  innerOffset: Dp,
): Dp {
  val minOffset = innerOffset + sideControlSize + gap
  val maxOffset = (containerWidth / 2) - (sideControlSize / 2) - endPadding
  return BottomOuterControlOffset.coerceBetween(minOffset, maxOffset)
}

private fun Dp.coerceBetween(minimum: Dp, maximum: Dp): Dp {
  val safeMaximum = maxOf(minimum, maximum)
  return minOf(maxOf(this, minimum), safeMaximum)
}

private fun nextLightStrength(current: Float): Float =
  when {
    current < LightActiveThreshold -> 0.35f
    current < 0.45f -> 0.65f
    current < 0.85f -> 1f
    else -> 0f
  }

private const val REVIEW_SEEK_TAP_SECONDS = 0.5f
private const val PRIMARY_LONG_PRESS_MILLIS = 320L
private const val TRANSPORT_LONG_PRESS_MILLIS = 260L
private const val LightActiveThreshold = 0.05f
private val NativeCameraTopGap = 90.dp
private val HelpButtonTouchSize = 48.dp
private val HelpButtonVisualSize = 40.dp
private val HelpButtonTopPadding = 8.dp
private val HelpButtonEndPadding = 18.dp
private val HelpButtonBottomClearance = 8.dp
private val PreviewFineControlBottomGap = 172.dp
private val CompactControlWidth = 376.dp
private val TinyControlWidth = 320.dp
private val PrimaryControlTouchSize = 84.dp
private val PrimaryControlVisualSize = 72.dp
private val CompactPrimaryControlTouchSize = 78.dp
private val CompactPrimaryControlVisualSize = 68.dp
private val TinyPrimaryControlTouchSize = 72.dp
private val TinyPrimaryControlVisualSize = 64.dp
private val SecondaryControlTouchSize = 58.dp
private val SecondaryControlVisualSize = 48.dp
private val CompactSecondaryControlTouchSize = 50.dp
private val CompactSecondaryControlVisualSize = 44.dp
private val TinySecondaryControlTouchSize = 48.dp
private val TinySecondaryControlVisualSize = 42.dp
private val ControlEndPadding = 20.dp
private val CompactControlEndPadding = 8.dp
private val TinyControlEndPadding = 4.dp
private val CompactControlGap = 4.dp
private val TinyControlGap = 2.dp
private val BottomInnerControlOffset = 78.dp
private val BottomOuterControlOffset = 142.dp
private val DelayChoiceTrayWidth = 310.dp
private val DelayChoiceChipWidth = 66.dp
private val MinDelayChoiceChipWidth = 48.dp
private val DelayChoiceGap = 6.dp
private val CompactDelayChoiceGap = 4.dp
private val DelayChoiceCompactWidth = 300.dp
private val MetalControlInk = Color(0xFF242A31)
private val CoachRose = Color(0xFFA62463)
private fun lightFrameColor(strength: Float): Color {
  val alpha = 0.18f + (strength.coerceIn(0f, 1f) * 0.72f)
  return Color(0xFFFFFBF4).copy(alpha = alpha)
}

private fun lightFrameUiShadeBrush(strength: Float): Brush {
  val clamped = strength.coerceIn(0f, 1f)
  val topShade = 0.1f + (0.08f * clamped)
  val bottomShade = 0.18f + (0.24f * clamped)
  return Brush.verticalGradient(
    colorStops =
      arrayOf(
        0f to Color.Black.copy(alpha = topShade),
        0.14f to Color.Transparent,
        0.7f to Color.Transparent,
        1f to Color.Black.copy(alpha = bottomShade),
      ),
  )
}

private fun nextDelayPreset(current: Float): Float {
  val presets = listOf(0f, 2f, 3f, 5f)
  val next = presets.firstOrNull { it > current + 0.1f }
  return next ?: presets.first()
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KakoMirrorIdlePreview() {
  KakoMirrorTheme { KakoMirrorScreen(state = MirrorUiState(), hasCameraPermission = false) }
}
