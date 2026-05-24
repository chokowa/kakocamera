package com.example.kakomirror.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
      onDelayChange = onDelayChange,
      onMirrorToggle = onMirrorToggle,
      onFlashChange = onFlashChange,
      onZoomChange = onZoomChange,
      onReviewClick = onReviewClick,
      onReviewPositionChange = onReviewPositionChange,
      onPlayPause = onPlayPause,
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(12.dp),
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
  Image(
    bitmap = bitmap.asImageBitmap(),
    contentDescription = null,
    modifier =
      modifier.graphicsLayer {
        scaleX = reviewZoom
        scaleY = reviewZoom
      },
    contentScale = ContentScale.Crop,
    alignment = Alignment.Center,
  )
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
  val glowWidth = (34 + strength * 28).dp
  val coreWidth = (8 + strength * 8).dp
  val glowColor = Color.White.copy(alpha = 0.10f + strength * 0.22f)
  val coreColor = Color.White.copy(alpha = 0.48f + strength * 0.42f)
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
private fun TopStatus(state: MirrorUiState, modifier: Modifier = Modifier) {
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
    Text(
      text = "${formatOneDecimal(state.delaySeconds)}秒遅れ",
      color = Color.White.copy(alpha = 0.88f),
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun ControlPanel(
  state: MirrorUiState,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onDelayChange: (Float) -> Unit,
  onMirrorToggle: () -> Unit,
  onFlashChange: (Float) -> Unit,
  onZoomChange: (Float) -> Unit,
  onReviewClick: () -> Unit,
  onReviewPositionChange: (Float) -> Unit,
  onPlayPause: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xF4FAFAFA),
    contentColor = Color(0xFF42464F),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
  ) {
    Column(
      modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 14.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Box(
        Modifier
          .align(Alignment.CenterHorizontally)
          .width(52.dp)
          .height(5.dp)
          .clip(RoundedCornerShape(99.dp))
          .background(Color(0x33000000)),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RoundControlButton(
          symbol = if (state.flashStrength > 0.05f) "●" else "○",
          label = stringResource(R.string.light),
          active = state.flashStrength > 0.05f,
          onClick = { onFlashChange(if (state.flashStrength > 0.05f) 0f else 0.78f) },
        )
        Button(
          onClick = if (state.mode == MirrorMode.Live) onStop else onStart,
          modifier =
            Modifier
              .weight(1f)
              .height(58.dp)
              .padding(horizontal = 18.dp),
          shape = RoundedCornerShape(999.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF5D94E6),
              contentColor = Color.White,
            ),
        ) {
          Text(
            if (state.mode == MirrorMode.Live) stringResource(R.string.stop) else stringResource(R.string.start),
            style = MaterialTheme.typography.titleLarge,
          )
        }
        RoundControlButton(
          symbol = "↔",
          label = stringResource(R.string.mirror_flip),
          active = state.mirrorFlip,
          onClick = onMirrorToggle,
        )
      }

      CompactDelayButton(state = state, onDelayChange = onDelayChange)

      if (state.mode == MirrorMode.Review) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          TextButton(onClick = onPlayPause) {
            Text(if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
          }
        }
        ReviewSlider(state = state, onReviewPositionChange = onReviewPositionChange)
      } else {
        FilledTonalButton(
          onClick = onReviewClick,
          enabled = state.canReview,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(48.dp),
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(if (state.canReview) "▶ ${stringResource(R.string.review)}" else stringResource(R.string.buffer_empty))
        }
      }
    }
  }
}

@Composable
private fun RoundControlButton(
  symbol: String,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.92f),
      contentColor = if (active) Color(0xFF4F87DA) else Color(0xFF5D626B),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
      }
    }
    Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D626B))
  }
}

@Composable
private fun CompactDelayButton(state: MirrorUiState, onDelayChange: (Float) -> Unit) {
  Surface(
    onClick = { onDelayChange(nextDelayPreset(state.delaySeconds)) },
    color = Color.White.copy(alpha = 0.72f),
    shape = RoundedCornerShape(18.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("◷", color = Color(0xFF5D626B), style = MaterialTheme.typography.titleLarge)
      Text(stringResource(R.string.delay), style = MaterialTheme.typography.labelLarge)
      Box(Modifier.weight(1f))
      Surface(shape = RoundedCornerShape(999.dp), color = Color.White, contentColor = Color(0xFF4F87DA)) {
        Text(
          text = "${formatDelay(state.delaySeconds)}秒",
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
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
      label = stringResource(R.string.light),
      symbol = "○",
      modifier =
        Modifier
          .align(Alignment.CenterStart)
          .padding(start = 18.dp, top = 120.dp, bottom = 310.dp),
    )
    FloatingVerticalSlider(
      value = state.zoomRatio,
      valueRange = state.minZoomRatio..state.maxZoomRatio,
      onValueChange = onZoomChange,
      label = stringResource(R.string.zoom),
      symbol = "+",
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 18.dp, top = 120.dp, bottom = 310.dp),
    )
  }
}

@Composable
private fun FloatingVerticalSlider(
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  label: String,
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
          .width(42.dp)
          .onSizeChanged { trackHeightPx = it.height.coerceAtLeast(1) }
          .pointerInput(valueRange) {
            detectDragGestures(
              onDragStart = { offset -> updateFromY(offset.y) },
              onDrag = { change, _ -> updateFromY(change.position.y) },
            )
          },
      shape = RoundedCornerShape(999.dp),
      color = Color.White.copy(alpha = 0.16f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .width(4.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.48f)),
        )
        Box(
          Modifier
            .align(Alignment.BottomCenter)
            .graphicsLayer { translationY = -trackHeightPx * fraction }
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.90f)),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(Color(0xFF4F87DA)),
          )
        }
      }
    }
    Surface(
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.86f),
      contentColor = Color(0xFF4F87DA),
    ) {
      Text(symbol, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
    }
    Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
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

private fun formatDelay(value: Float): String =
  if (value % 1f == 0f) value.toInt().toString() else formatOneDecimal(value)

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
