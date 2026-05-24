package com.example.kakomirror.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kakomirror.model.FrameRingBuffer
import com.example.kakomirror.model.MirrorFrame
import com.example.kakomirror.settings.MirrorSettings
import com.example.kakomirror.settings.MirrorSettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val settingsStore = MirrorSettingsStore(application)
  private val buffer = FrameRingBuffer()
  private val _uiState = MutableStateFlow(MirrorUiState())
  val uiState: StateFlow<MirrorUiState> = _uiState.asStateFlow()
  private var reviewAnchorMillis: Long? = null
  private var playbackJob: Job? = null
  private var settingsSaveJob: Job? = null

  init {
    viewModelScope.launch {
      settingsStore.settings.collect { settings ->
        _uiState.update {
          it.copy(
            delaySeconds = settings.delaySeconds,
            mirrorFlip = settings.mirrorFlip,
            flashStrength = settings.flashStrength,
            zoomRatio = settings.zoomRatio.coerceIn(it.minZoomRatio, it.maxZoomRatio),
          )
        }
      }
    }
  }

  fun startLive() {
    playbackJob?.cancel()
    buffer.clear()
    reviewAnchorMillis = null
    _uiState.update {
      it.copy(
        mode = MirrorMode.Live,
        currentFrame = null,
        bufferSeconds = 0f,
        reviewPositionSeconds = 0f,
        reviewMinSeconds = 0f,
        reviewMaxSeconds = 0f,
        isPlaying = false,
        errorMessage = null,
      )
    }
  }

  fun onFrame(frame: MirrorFrame) {
    if (_uiState.value.mode != MirrorMode.Live) return
    buffer.add(frame)
    val state = _uiState.value
    val targetMillis = frame.timestampMillis - (state.delaySeconds * 1000f).toLong()
    val delayedFrame = buffer.closestTo(targetMillis) ?: buffer.latest()
    _uiState.update {
      it.copy(
        currentFrame = delayedFrame,
        bufferSeconds = buffer.durationMillis / 1000f,
      )
    }
  }

  fun stopToReview() {
    playbackJob?.cancel()
    val anchor = _uiState.value.currentFrame ?: buffer.latest() ?: return
    val range = buffer.relativeRangeSeconds(anchor.timestampMillis)
    reviewAnchorMillis = anchor.timestampMillis
    _uiState.update {
      it.copy(
        mode = MirrorMode.Review,
        currentFrame = anchor,
        reviewPositionSeconds = 0f,
        reviewMinSeconds = range.start,
        reviewMaxSeconds = range.endInclusive,
        isPlaying = false,
      )
    }
  }

  fun setReviewPosition(seconds: Float) {
    val anchor = reviewAnchorMillis ?: return
    val state = _uiState.value
    val clamped = seconds.coerceIn(state.reviewMinSeconds, state.reviewMaxSeconds)
    val frame = buffer.closestTo(anchor + (clamped * 1000f).toLong()) ?: state.currentFrame
    _uiState.update { it.copy(reviewPositionSeconds = clamped, currentFrame = frame) }
  }

  fun togglePlayback() {
    if (_uiState.value.mode != MirrorMode.Review) return
    if (_uiState.value.isPlaying) {
      playbackJob?.cancel()
      _uiState.update { it.copy(isPlaying = false) }
      return
    }
    _uiState.update { it.copy(isPlaying = true) }
    playbackJob =
      viewModelScope.launch {
        while (_uiState.value.mode == MirrorMode.Review && _uiState.value.isPlaying) {
          delay(66L)
          val next = _uiState.value.reviewPositionSeconds + 0.066f
          if (next > _uiState.value.reviewMaxSeconds) {
            _uiState.update { it.copy(isPlaying = false) }
          } else {
            setReviewPosition(next)
          }
        }
      }
  }

  fun setDelay(seconds: Float) {
    val next = seconds.coerceIn(0f, 10f)
    _uiState.update { it.copy(delaySeconds = next) }
    saveSettings()
  }

  fun toggleMirror() {
    _uiState.update { it.copy(mirrorFlip = !it.mirrorFlip) }
    saveSettings()
  }

  fun setFlash(strength: Float) {
    _uiState.update { it.copy(flashStrength = strength.coerceIn(0f, 1f)) }
    saveSettings()
  }

  fun setZoom(ratio: Float) {
    _uiState.update { it.copy(zoomRatio = ratio.coerceIn(it.minZoomRatio, it.maxZoomRatio)) }
    saveSettings()
  }

  fun setZoomRange(min: Float, max: Float) {
    val safeMin = min.coerceAtLeast(0.1f)
    val safeMax = max.coerceAtLeast(safeMin)
    _uiState.update {
      it.copy(
        minZoomRatio = safeMin,
        maxZoomRatio = safeMax,
        zoomRatio = it.zoomRatio.coerceIn(safeMin, safeMax),
      )
    }
  }

  fun showError(message: String) {
    _uiState.update { it.copy(errorMessage = message) }
  }

  private fun saveSettings() {
    val state = _uiState.value
    settingsSaveJob?.cancel()
    settingsSaveJob = viewModelScope.launch {
      delay(180L)
      settingsStore.save(
        MirrorSettings(
          delaySeconds = state.delaySeconds,
          mirrorFlip = state.mirrorFlip,
          flashStrength = state.flashStrength,
          zoomRatio = state.zoomRatio,
        ),
      )
    }
  }
}

enum class MirrorMode {
  Idle,
  Live,
  Review,
}

data class MirrorUiState(
  val mode: MirrorMode = MirrorMode.Idle,
  val delaySeconds: Float = 5f,
  val mirrorFlip: Boolean = true,
  val flashStrength: Float = 0f,
  val zoomRatio: Float = 1f,
  val minZoomRatio: Float = 1f,
  val maxZoomRatio: Float = 4f,
  val bufferSeconds: Float = 0f,
  val currentFrame: MirrorFrame? = null,
  val reviewPositionSeconds: Float = 0f,
  val reviewMinSeconds: Float = 0f,
  val reviewMaxSeconds: Float = 0f,
  val isPlaying: Boolean = false,
  val errorMessage: String? = null,
) {
  val canReview: Boolean = bufferSeconds >= 1f && mode == MirrorMode.Live
}
