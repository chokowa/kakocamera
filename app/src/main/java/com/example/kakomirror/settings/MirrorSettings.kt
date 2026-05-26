package com.example.kakomirror.settings

data class MirrorSettings(
  val delaySeconds: Float = 5f,
  val mirrorFlip: Boolean = true,
  val flashStrength: Float = 0f,
  val zoomRatio: Float = 1f,
  val fullscreenMirror: Boolean = false,
  val liveCoachSeen: Boolean = false,
  val reviewCoachSeen: Boolean = false,
)
