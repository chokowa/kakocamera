package com.example.kakomirror.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
      initialZoomRatio = state.zoomRatio,
      onFrame = viewModel::onFrame,
      onZoomRange = viewModel::setZoomRange,
    )
    controller.setZoomRatio(state.zoomRatio)
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      if (granted && startAfterPermission) {
        startAfterPermission = false
        startCamera()
      }
    }

  LaunchedEffect(state.zoomRatio, state.mode) {
    if (state.mode == MirrorMode.Live) {
      controller.setZoomRatio(state.zoomRatio)
    }
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
      controller.stop()
      viewModel.stopToReview()
    },
    onDelayChange = viewModel::setDelay,
    onMirrorToggle = viewModel::toggleMirror,
    onFlashChange = viewModel::setFlash,
    onZoomChange = viewModel::setZoom,
    onReviewClick = {
      controller.stop()
      viewModel.stopToReview()
    },
    onReviewPositionChange = viewModel::setReviewPosition,
    onPlayPause = viewModel::togglePlayback,
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
  onFlashChange: (Float) -> Unit = {},
  onZoomChange: (Float) -> Unit = {},
  onReviewClick: () -> Unit = {},
  onReviewPositionChange: (Float) -> Unit = {},
  onPlayPause: () -> Unit = {},
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(state.zoomRatio, state.mode) {
          detectTransformGestures { _, _, zoom, _ ->
            val next = state.zoomRatio * zoom
            onZoomChange(next.coerceIn(state.minZoomRatio, state.maxZoomRatio))
          }
        },
  ) {
    FramePreview(
      frame = state.currentFrame,
      mirrorFlip = state.mirrorFlip,
      reviewZoom = if (state.mode == MirrorMode.Review) state.zoomRatio else 1f,
      modifier = Modifier.fillMaxSize(),
    )
    PseudoFlash(strength = state.flashStrength, modifier = Modifier.fillMaxSize())

    if (state.mode == MirrorMode.Idle && state.currentFrame == null) {
      StartPanel(
        hasCameraPermission = hasCameraPermission,
        onStart = onStart,
        modifier =
          Modifier
            .align(Alignment.Center)
            .offset(y = (-170).dp),
      )
    }

    TopStatus(
      state = state,
      onDelayClick = { onDelayChange(nextDelayPreset(state.delaySeconds)) },
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(16.dp),
    )

    if (state.mode != MirrorMode.Idle || state.currentFrame != null) {
      PreviewSliders(
        state = state,
        onFlashChange = onFlashChange,
        onZoomChange = onZoomChange,
        modifier = Modifier.fillMaxSize(),
      )
    }

    ControlPanel(
      state = state,
      onStart = onStart,
      onStop = onStop,
      onMirrorToggle = onMirrorToggle,
      onFlashChange = onFlashChange,
      onReviewClick = onReviewClick,
      onReviewPositionChange = onReviewPositionChange,
      onPlayPause = onPlayPause,
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding(),
    )
  }
}

@Composable
private fun FramePreview(
  frame: MirrorFrame?,
  mirrorFlip: Boolean,
  reviewZoom: Float,
  modifier: Modifier = Modifier,
) {
  if (frame == null) {
    Box(modifier, contentAlignment = Alignment.Center) {
      Text(
        text = stringResource(R.string.no_frame),
        color = Color.White.copy(alpha = 0.62f),
        style = MaterialTheme.typography.bodyLarge,
      )
    }
    return
  }

  val bitmap = remember(frame, mirrorFlip) { frame.toDisplayBitmap(mirrorFlip) }
  Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = null,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = 0.42f },
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center,
    )
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = null,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = reviewZoom
            scaleY = reviewZoom
          },
      contentScale = ContentScale.Fit,
      alignment = Alignment.Center,
    )
  }
}

private fun MirrorFrame.toDisplayBitmap(mirrorFlip: Boolean): Bitmap {
  val source = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
  val matrix = Matrix()
  if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
  if (mirrorFlip) matrix.postScale(-1f, 1f)
  return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

@Composable
private fun PseudoFlash(strength: Float, modifier: Modifier = Modifier) {
  if (strength <= 0.01f) return
  val glowWidth = 38.dp
  val coreWidth = 9.dp
  val glowColor = Color.White.copy(alpha = 0.08f + strength * 0.24f)
  val coreColor = Color.White.copy(alpha = 0.36f + strength * 0.58f)
  Box(modifier) {
    FlashBar(Alignment.CenterStart, glowWidth, coreWidth, glowColor, coreColor)
    FlashBar(Alignment.CenterEnd, glowWidth, coreWidth, glowColor, coreColor)
  }
}

@Composable
private fun BoxScope.FlashBar(
  alignment: Alignment,
  glowWidth: androidx.compose.ui.unit.Dp,
  coreWidth: androidx.compose.ui.unit.Dp,
  glowColor: Color,
  coreColor: Color,
) {
  Box(
    Modifier
      .align(alignment)
      .padding(horizontal = 18.dp, vertical = 68.dp)
      .fillMaxHeight()
      .width(glowWidth)
      .clip(RoundedCornerShape(999.dp))
      .background(glowColor),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .fillMaxHeight()
        .width(coreWidth)
        .clip(RoundedCornerShape(999.dp))
        .background(coreColor),
    )
  }
}

@Composable
private fun StartPanel(
  hasCameraPermission: Boolean,
  onStart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = if (hasCameraPermission) stringResource(R.string.ready_title) else stringResource(R.string.permission_title),
      color = Color.White,
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
    )
    Text(
      text = if (hasCameraPermission) stringResource(R.string.ready_body) else stringResource(R.string.permission_body),
      color = Color.White.copy(alpha = 0.74f),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    Button(
      onClick = onStart,
      modifier = Modifier.size(132.dp),
      shape = CircleShape,
    ) {
      Text(
        text = if (hasCameraPermission) stringResource(R.string.start) else stringResource(R.string.allow_camera),
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun TopStatus(
  state: MirrorUiState,
  onDelayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.ready_title),
      color = Color.White,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Surface(
      onClick = onDelayClick,
      color = Color.Transparent,
      contentColor = Color.White.copy(alpha = 0.88f),
      shape = RoundedCornerShape(999.dp),
    ) {
      Text(
        text = "◷ ${formatOneDecimal(state.delaySeconds)}秒遅れ",
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}

@Composable
private fun ControlPanel(
  state: MirrorUiState,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onMirrorToggle: () -> Unit,
  onFlashChange: (Float) -> Unit,
  onReviewClick: () -> Unit,
  onReviewPositionChange: (Float) -> Unit,
  onPlayPause: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isReview = state.mode == MirrorMode.Review
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(if (isReview) 218.dp else 156.dp),
  ) {
    GlassControlsBackground(modifier = Modifier.fillMaxSize())
    Box(
      Modifier
        .align(Alignment.TopCenter)
        .offset(y = 82.dp)
        .width(48.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(99.dp))
        .background(Color(0x22000000)),
    )
    ReviewDial(
      state = state,
      onReviewClick = onReviewClick,
      onPlayPause = onPlayPause,
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .offset(y = 15.dp),
    )
    RoundControlButton(
      symbol = "○",
      label = stringResource(R.string.light),
      active = state.flashStrength > 0.05f,
      onClick = { onFlashChange(if (state.flashStrength > 0.05f) 0f else 0.78f) },
      modifier =
        Modifier
          .align(Alignment.BottomStart)
          .padding(start = 58.dp, bottom = if (isReview) 68.dp else 8.dp),
    )
    Button(
      onClick = if (state.mode == MirrorMode.Live) onStop else onStart,
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = if (isReview) 80.dp else 24.dp)
          .width(136.dp)
          .height(44.dp),
      shape = RoundedCornerShape(999.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = Color(0xFF5D94E6),
          contentColor = Color.White,
        ),
    ) {
      Text(
        if (state.mode == MirrorMode.Live) stringResource(R.string.stop) else stringResource(R.string.start),
        style = MaterialTheme.typography.titleMedium,
      )
    }
    RoundControlButton(
      symbol = "↔",
      label = stringResource(R.string.mirror_flip),
      active = state.mirrorFlip,
      onClick = onMirrorToggle,
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 58.dp, bottom = if (isReview) 68.dp else 8.dp),
    )

    if (isReview) {
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 8.dp),
      ) {
        ReviewSlider(state = state, onReviewPositionChange = onReviewPositionChange)
      }
    }
  }
}

@Composable
private fun GlassControlsBackground(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val panelTop = h * 0.56f
    val radius = min(w * 0.205f, panelTop * 0.98f)
    val center = Offset(w / 2f, panelTop)
    val glass = Color.White.copy(alpha = 0.52f)

    drawRoundRect(
      color = glass,
      topLeft = Offset(0f, panelTop),
      size = Size(w, h - panelTop),
      cornerRadius = CornerRadius(54f, 54f),
    )
    drawArc(
      color = glass,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = true,
      topLeft = Offset(center.x - radius, center.y - radius),
      size = Size(radius * 2f, radius * 2f),
    )
    drawArc(
      color = Color.White.copy(alpha = 0.58f),
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(center.x - radius, center.y - radius),
      size = Size(radius * 2f, radius * 2f),
      style = Stroke(width = 2.4f, cap = StrokeCap.Round),
    )

    val tickOuter = radius - 18f
    val tickInner = radius - 30f
    repeat(23) { index ->
      val degrees = 204f + index * (132f / 22f)
      val radians = Math.toRadians(degrees.toDouble())
      val start = Offset(
        center.x + cos(radians).toFloat() * tickInner,
        center.y + sin(radians).toFloat() * tickInner,
      )
      val end = Offset(
        center.x + cos(radians).toFloat() * tickOuter,
        center.y + sin(radians).toFloat() * tickOuter,
      )
      drawLine(Color(0x885D626B), start, end, strokeWidth = 1.4f, cap = StrokeCap.Round)
    }

    drawCircle(
      color = Color(0xFF5D94E6),
      radius = 6.5f,
      center = Offset(center.x, center.y - radius + 16f),
    )
  }
}

@Composable
private fun ReviewDial(
  state: MirrorUiState,
  onReviewClick: () -> Unit,
  onPlayPause: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val enabled = state.canReview || state.mode == MirrorMode.Review
  Surface(
    onClick = if (state.mode == MirrorMode.Review) onPlayPause else onReviewClick,
    enabled = enabled,
    modifier =
      modifier
        .width(138.dp)
        .height(72.dp),
    shape = RoundedCornerShape(999.dp),
    color = Color.Transparent,
    contentColor = if (enabled) Color(0xFF5D626B) else Color(0x775D626B),
  ) {
    Column(
      modifier = Modifier.padding(top = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        if (state.mode == MirrorMode.Review && state.isPlaying) "⏸" else "▶",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        if (enabled) stringResource(R.string.review) else stringResource(R.string.buffer_empty),
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
private fun RoundControlButton(
  symbol: String,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(48.dp),
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.82f),
      contentColor = if (active) Color(0xFF4F87DA) else Color(0xFF5D626B),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      }
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D626B))
  }
}

@Composable
private fun PreviewSliders(
  state: MirrorUiState,
  onFlashChange: (Float) -> Unit,
  onZoomChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    FloatingVerticalSlider(
      value = state.flashStrength,
      valueRange = 0f..1f,
      onValueChange = onFlashChange,
      symbol = "○",
      modifier =
        Modifier
          .align(Alignment.CenterStart)
          .padding(start = 18.dp, top = 124.dp, bottom = 206.dp),
    )
    FloatingVerticalSlider(
      value = state.zoomRatio,
      valueRange = state.minZoomRatio..state.maxZoomRatio,
      onValueChange = onZoomChange,
      symbol = "+",
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 18.dp, top = 124.dp, bottom = 206.dp),
    )
  }
}

@Composable
private fun FloatingVerticalSlider(
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  symbol: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    var trackHeightPx by remember { mutableStateOf(1) }
    val fraction =
      if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
      } else {
        0f
      }
    fun updateFromY(y: Float) {
      val fromBottom = 1f - (y / trackHeightPx).coerceIn(0f, 1f)
      val next = valueRange.start + (valueRange.endInclusive - valueRange.start) * fromBottom
      onValueChange(next.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    Surface(
      modifier =
        Modifier
          .weight(1f)
          .width(24.dp)
          .onSizeChanged { trackHeightPx = it.height.coerceAtLeast(1) }
          .pointerInput(valueRange) {
            detectDragGestures(
              onDragStart = { offset -> updateFromY(offset.y) },
              onDrag = { change, _ -> updateFromY(change.position.y) },
            )
      },
      shape = RoundedCornerShape(999.dp),
      color = Color.White.copy(alpha = 0.035f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .width(2.dp)
            .fillMaxHeight()
            .padding(vertical = 14.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.22f)),
        )
        Box(
          Modifier
            .align(Alignment.BottomCenter)
            .graphicsLayer { translationY = -trackHeightPx * fraction }
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.72f)),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF4F87DA)),
          )
        }
      }
    }
    Surface(
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.46f),
      contentColor = Color(0xFF4F87DA),
    ) {
      Text(symbol, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun ReviewSlider(state: MirrorUiState, onReviewPositionChange: (Float) -> Unit) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(stringResource(R.string.review_before), style = MaterialTheme.typography.labelMedium)
      Text(stringResource(R.string.review_anchor), style = MaterialTheme.typography.labelMedium)
      Text(stringResource(R.string.review_after), style = MaterialTheme.typography.labelMedium)
    }
    if (state.reviewMaxSeconds > state.reviewMinSeconds) {
      Slider(
        value = state.reviewPositionSeconds.coerceIn(state.reviewMinSeconds, state.reviewMaxSeconds),
        onValueChange = onReviewPositionChange,
        valueRange = state.reviewMinSeconds..state.reviewMaxSeconds,
      )
    }
  }
}

private fun formatOneDecimal(value: Float): String = String.format(Locale.JAPAN, "%.1f", value)

private fun nextDelayPreset(current: Float): Float {
  val presets = listOf(0f, 2f, 5f, 10f)
  val next = presets.firstOrNull { it > current + 0.1f }
  return next ?: presets.first()
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KakoMirrorIdlePreview() {
  KakoMirrorTheme { KakoMirrorScreen(state = MirrorUiState(), hasCameraPermission = true) }
}
