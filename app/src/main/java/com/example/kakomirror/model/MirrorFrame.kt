package com.example.kakomirror.model

data class MirrorFrame(
  val timestampMillis: Long,
  val rotationDegrees: Int,
  val jpegBytes: ByteArray,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as MirrorFrame
    return timestampMillis == other.timestampMillis &&
      rotationDegrees == other.rotationDegrees &&
      jpegBytes.contentEquals(other.jpegBytes)
  }

  override fun hashCode(): Int {
    var result = timestampMillis.hashCode()
    result = 31 * result + rotationDegrees
    result = 31 * result + jpegBytes.contentHashCode()
    return result
  }
}
