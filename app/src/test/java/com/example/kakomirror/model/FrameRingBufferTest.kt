package com.example.kakomirror.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameRingBufferTest {
  @Test
  fun add_prunesFramesOlderThanMaxDuration() {
    val buffer = FrameRingBuffer(maxDurationMillis = 1_000L)

    buffer.add(frameAt(0L))
    buffer.add(frameAt(500L))
    buffer.add(frameAt(1_200L))

    assertEquals(2, buffer.size)
    assertEquals(700L, buffer.durationMillis)
  }

  @Test
  fun closestTo_returnsNearestFrame() {
    val buffer = FrameRingBuffer()

    buffer.add(frameAt(1_000L))
    buffer.add(frameAt(1_200L))
    buffer.add(frameAt(1_500L))

    assertEquals(1_200L, buffer.closestTo(1_260L)?.timestampMillis)
  }

  @Test
  fun relativeRangeSeconds_usesAnchorAsZero() {
    val buffer = FrameRingBuffer()

    buffer.add(frameAt(1_000L))
    buffer.add(frameAt(1_500L))
    buffer.add(frameAt(2_500L))

    val range = buffer.relativeRangeSeconds(anchorTimestampMillis = 1_500L)

    assertEquals(-0.5f, range.start)
    assertEquals(1.0f, range.endInclusive)
    assertNotNull(buffer.latest())
    assertTrue(buffer.durationMillis > 0L)
  }

  private fun frameAt(timestampMillis: Long): MirrorFrame =
    MirrorFrame(timestampMillis = timestampMillis, rotationDegrees = 0, jpegBytes = byteArrayOf(1))
}
