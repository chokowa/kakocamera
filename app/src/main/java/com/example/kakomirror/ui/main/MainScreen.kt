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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
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
    contentScale = ContentScale.Fit,
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
  val width = (8 + strength * 28).dp
  val color = Color.White.copy(alpha = 0.22f + strength * 0.58f)
  Box(modifier) {
    Box(
      Modifier
        .align(Alignment.CenterStart)
        .fillMaxHeight()
        .width(width)
        .background(color),
    )
    Box(
      Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .width(width)
        .background(color),
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
  Surface(
    modifier = modifier,
    color = Color.Black.copy(alpha = 0.34f),
    contentColor = Color.White,
    shape = MaterialTheme.shapes.large,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = "${formatOneDecimal(state.delaySeconds)}秒")
      Text(text = "${formatOneDecimal(state.zoomRatio)}x")
      if (state.mirrorFlip) Text(text = stringResource(R.string.mirror_flip))
    }
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
    color = Color(0xE6121418),
    contentColor = Color.White,
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = if (state.mode == MirrorMode.Live) onStop else onStart,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (state.mode == MirrorMode.Live) stringResource(R.string.stop) else stringResource(R.string.start))
        }
        FilledTonalButton(
          onClick = onReviewClick,
          enabled = state.canReview,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (state.canReview) stringResource(R.string.buffer_ready) else stringResource(R.string.buffer_empty))
        }
      }

      LabeledSlider(
        label = stringResource(R.string.delay),
        valueText = "${formatOneDecimal(state.delaySeconds)}秒",
        value = state.delaySeconds,
        range = 0f..10f,
        onValueChange = onDelayChange,
      )
      LabeledSlider(
        label = stringResource(R.string.light),
        valueText = "${(state.flashStrength * 100).toInt()}%",
        value = state.flashStrength,
        range = 0f..1f,
        onValueChange = onFlashChange,
      )
      LabeledSlider(
        label = stringResource(R.string.zoom),
        valueText = "${formatOneDecimal(state.zoomRatio)}x",
        value = state.zoomRatio,
        range = state.minZoomRatio..state.maxZoomRatio,
        onValueChange = onZoomChange,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onMirrorToggle) {
          Text("${stringResource(R.string.mirror_flip)} ${if (state.mirrorFlip) "ON" else "OFF"}")
        }
        if (state.mode == MirrorMode.Review) {
          TextButton(onClick = onPlayPause) {
            Text(if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
          }
        }
      }

      if (state.mode == MirrorMode.Review) {
        ReviewSlider(state = state, onReviewPositionChange = onReviewPositionChange)
      }
    }
  }
}

@Composable
private fun LabeledSlider(
  label: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.labelLarge)
      Text(valueText, style = MaterialTheme.typography.labelLarge)
    }
    if (range.endInclusive > range.start) {
      Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range)
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KakoMirrorIdlePreview() {
  KakoMirrorTheme { KakoMirrorScreen(state = MirrorUiState(), hasCameraPermission = true) }
}
