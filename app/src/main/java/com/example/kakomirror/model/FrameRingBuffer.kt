package com.example.kakomirror.model

import kotlin.math.abs

class FrameRingBuffer(private val maxDurationMillis: Long = 10_000L) {
  private val frames = ArrayDeque<MirrorFrame>()

  val size: Int
    get() = frames.size

  val durationMillis: Long
    get() = if (frames.size < 2) 0L else frames.last().timestampMillis - frames.first().timestampMillis

  fun clear() {
    frames.clear()
  }

  fun add(frame: MirrorFrame) {
    frames.addLast(frame)
    val oldestAllowed = frame.timestampMillis - maxDurationMillis
    while (frames.isNotEmpty() && frames.first().timestampMillis < oldestAllowed) {
      frames.removeFirst()
    }
  }

  fun closestTo(timestampMillis: Long): MirrorFrame? =
    frames.minByOrNull { abs(it.timestampMillis - timestampMillis) }

  fun latest(): MirrorFrame? = frames.lastOrNull()

  fun relativeRangeSeconds(anchorTimestampMillis: Long): ClosedFloatingPointRange<Float> {
    val first = frames.firstOrNull()?.timestampMillis ?: anchorTimestampMillis
    val last = frames.lastOrNull()?.timestampMillis ?: anchorTimestampMillis
    return ((first - anchorTimestampMillis) / 1000f)..((last - anchorTimestampMillis) / 1000f)
  }
}
