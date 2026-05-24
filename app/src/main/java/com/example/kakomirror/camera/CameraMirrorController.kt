package com.example.kakomirror.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
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

class CameraMirrorController(private val context: Context) {
  private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private var camera: Camera? = null
  private var provider: ProcessCameraProvider? = null
  private var lastFrameMillis = 0L

  fun start(
    lifecycleOwner: LifecycleOwner,
    initialZoomRatio: Float,
    onFrame: (MirrorFrame) -> Unit,
    onZoomRange: (Float, Float) -> Unit,
  ) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener(
      {
        val cameraProvider = providerFuture.get()
        provider = cameraProvider
        cameraProvider.unbindAll()
        lastFrameMillis = 0L

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

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, analysis)
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

  fun stop() {
    provider?.unbindAll()
    camera = null
  }

  fun shutdown() {
    stop()
    analysisExecutor.shutdown()
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

  private companion object {
    const val FRAME_INTERVAL_MILLIS = 66L
    const val JPEG_QUALITY = 88
  }
}
