package com.example.kakomirror.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import android.util.Size
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.kakomirror.model.MirrorFrame
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class CameraMirrorController(private val context: Context) {
  private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private var camera: Camera? = null
  private var provider: ProcessCameraProvider? = null
  private var lastFrameMillis = 0L
  private var lightStrength = 0f
  private var lastLoggedStabilizationResult: String? = null
  private var lastLoggedFpsRangeResult: String? = null

  fun start(
    lifecycleOwner: LifecycleOwner,
    initialZoomRatio: Float,
    stabilizationEnabled: Boolean,
    onFrame: (MirrorFrame) -> Unit,
    onZoomRange: (Float, Float) -> Unit,
    onStabilizationSupported: (Boolean) -> Unit,
  ) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener(
      {
        val cameraProvider = providerFuture.get()
        provider = cameraProvider
        cameraProvider.unbindAll()
        lastFrameMillis = 0L
        lastLoggedStabilizationResult = null
        lastLoggedFpsRangeResult = null

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA

        // --- FROZEN: Preview Stabilization logic (deactivated in favor of FPS Lock) ---
        // val stabilizationInfo = queryPreviewStabilizationInfo(cameraProvider, selector)
        // val requestPreviewStabilization = stabilizationEnabled && stabilizationInfo.supported
        // -------------------------------------------------------------------------------

        val cameraInfo = runCatching { cameraProvider.getCameraInfo(selector) }.getOrNull()
        val availableFpsRanges = cameraInfo?.let {
          runCatching {
            Camera2CameraInfo.from(it)
              .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
          }.getOrNull()
        }
        val target30FpsRange = select30FpsTargetRange(availableFpsRanges)
        val isFpsLockSupported = target30FpsRange != null
        onStabilizationSupported(isFpsLockSupported)

        Log.d(
          STABILIZATION_TAG,
          "start requested=$stabilizationEnabled supported=$isFpsLockSupported " +
            "availableFpsRanges=${availableFpsRanges?.contentToString() ?: "null"} sdk=${Build.VERSION.SDK_INT}",
        )
        val analysis =
          ImageAnalysis.Builder()
            .setResolutionSelector(
              ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                  ResolutionStrategy(
                    Size(1440, 1080),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                  ),
                )
                .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .also { builder ->
              val interop = Camera2Interop.Extender(builder)
              interop.setSessionCaptureCallback(
                object : CameraCaptureSession.CaptureCallback() {
                  override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                  ) {
                    logStabilizationResult(request, result) // Kept for frozen logs
                    logFpsLockResult(request, result)
                  }
                },
              )
              // --- FROZEN: Preview Stabilization option (deactivated in favor of FPS Lock) ---
              // if (requestPreviewStabilization) {
              //   interop.setCaptureRequestOption(
              //     CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
              //     CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
              //   )
              // }
              // --------------------------------------------------------------------------------

              if (stabilizationEnabled && target30FpsRange != null) {
                interop.setCaptureRequestOption(
                  CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                  target30FpsRange,
                )
              }
            }
            .build()

        analysis.setAnalyzer(analysisExecutor) { image ->
          val now = System.currentTimeMillis()
          if (now - lastFrameMillis < FRAME_INTERVAL_MILLIS) {
            image.close()
            return@setAnalyzer
          }
          lastFrameMillis = now
          val frame = image.toMirrorFrame(now)
          image.close()
          if (frame != null) {
            onFrame(frame)
          }
        }

        camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, analysis)
        applyExposureBoost()
        camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
          onZoomRange(zoomState.minZoomRatio, zoomState.maxZoomRatio)
          camera?.cameraControl?.setZoomRatio(initialZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio))
        }
        camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
          onZoomRange(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  fun setZoomRatio(ratio: Float) {
    camera?.cameraControl?.setZoomRatio(ratio)
  }

  fun setLightStrength(strength: Float) {
    lightStrength = strength.coerceIn(0f, 1f)
    applyExposureBoost()
  }

  fun stop() {
    provider?.unbindAll()
    camera = null
    lastLoggedStabilizationResult = null
  }

  fun shutdown() {
    stop()
    analysisExecutor.shutdown()
  }

  private fun applyExposureBoost() {
    val boundCamera = camera ?: return
    val exposureState = boundCamera.cameraInfo.exposureState
    if (!exposureState.isExposureCompensationSupported) return
    val range = exposureState.exposureCompensationRange
    val positiveMax = range.upper.coerceAtLeast(0)
    val targetIndex = (positiveMax * lightStrength * MAX_EXPOSURE_BOOST_FRACTION).roundToInt()
    boundCamera.cameraControl.setExposureCompensationIndex(targetIndex.coerceIn(range.lower, range.upper))
  }

  private fun ImageProxy.toMirrorFrame(receivedAtMillis: Long): MirrorFrame? {
    val jpeg = toJpegBytes() ?: return null
    return MirrorFrame(
      timestampMillis = receivedAtMillis,
      rotationDegrees = imageInfo.rotationDegrees,
      jpegBytes = jpeg,
    )
  }

  private fun ImageProxy.toJpegBytes(): ByteArray? {
    if (format != ImageFormat.YUV_420_888) return null
    val nv21 = toNv21()
    val output = ByteArrayOutputStream()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val ok = yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, output)
    return if (ok) output.toByteArray() else null
  }

  private fun ImageProxy.toNv21(): ByteArray {
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]
    val ySize = width * height
    val chromaWidth = width / 2
    val chromaHeight = height / 2
    val nv21 = ByteArray(ySize + ySize / 2)

    var outputOffset = 0
    for (row in 0 until height) {
      val rowOffset = row * yPlane.rowStride
      yPlane.buffer.position(rowOffset)
      yPlane.buffer.get(nv21, outputOffset, width)
      outputOffset += width
    }

    var chromaOffset = ySize
    for (row in 0 until chromaHeight) {
      for (col in 0 until chromaWidth) {
        val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
        val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
        nv21[chromaOffset++] = vPlane.buffer.get(vIndex)
        nv21[chromaOffset++] = uPlane.buffer.get(uIndex)
      }
    }
    return nv21
  }

  private fun queryPreviewStabilizationInfo(
    cameraProvider: ProcessCameraProvider,
    cameraSelector: CameraSelector,
  ): PreviewStabilizationInfo {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return PreviewStabilizationInfo(supported = false, availableModes = null)
    }
    val cameraInfo =
      runCatching { cameraProvider.getCameraInfo(cameraSelector) }.getOrNull()
        ?: return PreviewStabilizationInfo(supported = false, availableModes = null)
    val availableModes =
      runCatching {
        Camera2CameraInfo
          .from(cameraInfo)
          .getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
      }.getOrNull()
    return PreviewStabilizationInfo(
      supported = supportsPreviewStabilizationMode(Build.VERSION.SDK_INT, availableModes),
      availableModes = availableModes,
    )
  }

  private fun select30FpsTargetRange(availableRanges: Array<Range<Int>>?): Range<Int>? {
    if (availableRanges == null) return null
    val exact = availableRanges.firstOrNull { it.lower == 30 && it.upper == 30 }
    if (exact != null) return exact
    return availableRanges
      .filter { it.upper >= 30 }
      .maxByOrNull { it.lower }
  }

  private fun logFpsLockResult(
    request: CaptureRequest,
    result: TotalCaptureResult,
  ) {
    val requestRange = request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
    val resultRange = result.get(CaptureResult.CONTROL_AE_TARGET_FPS_RANGE)
    val label = "req=$requestRange res=$resultRange"
    if (label == lastLoggedFpsRangeResult) return
    lastLoggedFpsRangeResult = label
    Log.d(
      STABILIZATION_TAG,
      "FPS lock active: $label",
    )
  }

  private fun logStabilizationResult(
    request: CaptureRequest,
    result: TotalCaptureResult,
  ) {
    val requestLabel = stabilizationModeLabel(request.get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE))
    val resultLabel = stabilizationModeLabel(result.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE))
    if (resultLabel == lastLoggedStabilizationResult) return
    lastLoggedStabilizationResult = resultLabel
    Log.d(
      STABILIZATION_TAG,
      "capture request=$requestLabel result=$resultLabel",
    )
  }

  private companion object {
    const val FRAME_INTERVAL_MILLIS = 33L
    const val JPEG_QUALITY = 88
    const val MAX_EXPOSURE_BOOST_FRACTION = 0.82f
    const val STABILIZATION_TAG = "KakoStabilization"
  }
}

private data class PreviewStabilizationInfo(
  val supported: Boolean,
  val availableModes: IntArray?,
)

internal fun supportsPreviewStabilizationMode(
  sdkInt: Int,
  availableModes: IntArray?,
): Boolean =
  sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    availableModes?.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION) == true

internal fun stabilizationModeLabel(mode: Int?): String =
  when (mode) {
    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "OFF"
    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "ON"
    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION -> "PREVIEW_STABILIZATION"
    null -> "UNSET"
    else -> "UNKNOWN($mode)"
  }
