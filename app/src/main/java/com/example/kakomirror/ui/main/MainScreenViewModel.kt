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
  private var seekJob: Job? = null
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
            fullscreenMirror = settings.fullscreenMirror,
            liveCoachSeen = settings.liveCoachSeen,
            reviewCoachSeen = settings.reviewCoachSeen,
          )
        }
      }
    }
  }

  fun startLive() {
    playbackJob?.cancel()
    seekJob?.cancel()
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
    val bufferSeconds = buffer.durationMillis / 1000f
    val targetMillis = frame.timestampMillis - (state.delaySeconds * 1000f).toLong()
    val hasDelayBuffer =
      state.delaySeconds <= MIN_DELAY_WAIT_SECONDS ||
        bufferSeconds >= requiredBufferSecondsFor(state.delaySeconds)
    val delayedFrame =
      if (hasDelayBuffer) {
        buffer.closestTo(targetMillis) ?: buffer.latest()
      } else {
        null
      }
    _uiState.update {
      it.copy(
        currentFrame = delayedFrame,
        bufferSeconds = bufferSeconds,
      )
    }
  }

  fun stopToReview(): Boolean {
    playbackJob?.cancel()
    seekJob?.cancel()
    val anchor = _uiState.value.currentFrame ?: buffer.latest() ?: return false
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
    return true
  }

  fun setReviewPosition(seconds: Float) {
    val anchor = reviewAnchorMillis ?: return
    val state = _uiState.value
    val clamped = seconds.coerceIn(state.reviewMinSeconds, state.reviewMaxSeconds)
    val frame = buffer.closestTo(anchor + (clamped * 1000f).toLong()) ?: state.currentFrame
    _uiState.update { it.copy(reviewPositionSeconds = clamped, currentFrame = frame) }
  }

  fun togglePlayback() {
    setReviewPlayback(!_uiState.value.isPlaying)
  }

  fun startReviewPlayback() {
    setReviewPlayback(true)
  }

  fun stopReviewPlayback() {
    setReviewPlayback(false)
  }

  fun seekReviewBy(deltaSeconds: Float) {
    if (_uiState.value.mode != MirrorMode.Review) return
    stopReviewPlayback()
    moveReviewPositionBy(deltaSeconds)
  }

  fun startReviewSeek(direction: Float) {
    if (_uiState.value.mode != MirrorMode.Review) return
    stopReviewPlayback()
    seekJob?.cancel()
    seekJob =
      viewModelScope.launch {
        val step = REVIEW_PLAYBACK_STEP_SECONDS * direction.coerceIn(-1f, 1f)
        while (_uiState.value.mode == MirrorMode.Review) {
          val moved = moveReviewPositionBy(step)
          if (!moved) break
          delay(REVIEW_PLAYBACK_INTERVAL_MILLIS)
        }
      }
  }

  fun stopReviewSeek() {
    seekJob?.cancel()
    seekJob = null
  }

  private fun setReviewPlayback(playing: Boolean) {
    if (_uiState.value.mode != MirrorMode.Review) return
    seekJob?.cancel()
    seekJob = null
    if (!playing) {
      playbackJob?.cancel()
      playbackJob = null
      _uiState.update { it.copy(isPlaying = false) }
      return
    }
    if (_uiState.value.reviewMaxSeconds <= _uiState.value.reviewMinSeconds) return
    playbackJob?.cancel()
    _uiState.update { it.copy(isPlaying = true) }
    playbackJob =
      viewModelScope.launch {
        while (_uiState.value.mode == MirrorMode.Review && _uiState.value.isPlaying) {
          delay(REVIEW_PLAYBACK_INTERVAL_MILLIS)
          val next = _uiState.value.reviewPositionSeconds + REVIEW_PLAYBACK_STEP_SECONDS
          if (next > _uiState.value.reviewMaxSeconds) {
            setReviewPosition(_uiState.value.reviewMaxSeconds)
            _uiState.update { it.copy(isPlaying = false) }
          } else {
            setReviewPosition(next)
          }
        }
      }
  }

  private fun moveReviewPositionBy(deltaSeconds: Float): Boolean {
    val state = _uiState.value
    if (state.mode != MirrorMode.Review || state.reviewMaxSeconds <= state.reviewMinSeconds) return false
    val next = (state.reviewPositionSeconds + deltaSeconds).coerceIn(state.reviewMinSeconds, state.reviewMaxSeconds)
    if (next == state.reviewPositionSeconds) return false
    setReviewPosition(next)
    return true
  }

  fun setDelay(seconds: Float) {
    val next = seconds.coerceIn(0f, MAX_DELAY_SECONDS)
    _uiState.update {
      val waitingForDelay =
        it.mode == MirrorMode.Live &&
          next > MIN_DELAY_WAIT_SECONDS &&
          it.bufferSeconds < requiredBufferSecondsFor(next)
      it.copy(delaySeconds = next, currentFrame = if (waitingForDelay) null else it.currentFrame)
    }
    saveSettings()
  }

  fun toggleMirror() {
    _uiState.update { it.copy(mirrorFlip = !it.mirrorFlip) }
    saveSettings()
  }

  fun toggleFullscreenMirror() {
    _uiState.update { it.copy(fullscreenMirror = !it.fullscreenMirror) }
    saveSettings()
  }

  fun markLiveCoachSeen() {
    if (_uiState.value.liveCoachSeen) return
    _uiState.update { it.copy(liveCoachSeen = true) }
    saveSettings()
  }

  fun markReviewCoachSeen() {
    if (_uiState.value.reviewCoachSeen) return
    _uiState.update { it.copy(reviewCoachSeen = true) }
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
          fullscreenMirror = state.fullscreenMirror,
          liveCoachSeen = state.liveCoachSeen,
          reviewCoachSeen = state.reviewCoachSeen,
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
  val delaySeconds: Float = 2f,
  val mirrorFlip: Boolean = true,
  val flashStrength: Float = 0f,
  val zoomRatio: Float = 1f,
  val fullscreenMirror: Boolean = false,
  val liveCoachSeen: Boolean = false,
  val reviewCoachSeen: Boolean = false,
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

private const val REVIEW_PLAYBACK_INTERVAL_MILLIS = 66L
private const val REVIEW_PLAYBACK_STEP_SECONDS = 0.066f
private const val MIN_DELAY_WAIT_SECONDS = 0.05f
private const val DELAY_READY_TOLERANCE_SECONDS = 0.25f
private const val MAX_DELAY_SECONDS = 5f

private fun requiredBufferSecondsFor(delaySeconds: Float): Float =
  (delaySeconds - DELAY_READY_TOLERANCE_SECONDS).coerceAtLeast(0f)
