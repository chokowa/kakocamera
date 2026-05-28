package com.example.kakomirror.settings

data class MirrorSettings(
  val delaySeconds: Float = 2f,
  val mirrorFlip: Boolean = true,
  val flashStrength: Float = 0f,
  val zoomRatio: Float = 1f,
  val stabilizationEnabled: Boolean = true,
  val fullscreenMirror: Boolean = false,
  val liveCoachSeen: Boolean = false,
  val reviewCoachSeen: Boolean = false,
  val adsRemoved: Boolean = false,
)
