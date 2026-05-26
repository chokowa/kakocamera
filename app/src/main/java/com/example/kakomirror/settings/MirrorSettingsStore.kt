package com.example.kakomirror.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "mirror_settings")

class MirrorSettingsStore(context: Context) {
  private val appContext = context.applicationContext

  val settings: Flow<MirrorSettings> =
    appContext.settingsDataStore.data.map { preferences ->
      MirrorSettings(
        delaySeconds = (preferences[Keys.DelaySeconds] ?: 5f).coerceIn(0f, MAX_DELAY_SECONDS),
        mirrorFlip = preferences[Keys.MirrorFlip] ?: true,
        flashStrength = preferences[Keys.FlashStrength] ?: 0f,
        zoomRatio = preferences[Keys.ZoomRatio] ?: 1f,
        fullscreenMirror = preferences[Keys.FullscreenMirror] ?: false,
        liveCoachSeen = preferences[Keys.LiveCoachSeen] ?: false,
        reviewCoachSeen = preferences[Keys.ReviewCoachSeen] ?: false,
      )
    }

  suspend fun save(settings: MirrorSettings) {
    appContext.settingsDataStore.edit { preferences ->
      preferences[Keys.DelaySeconds] = settings.delaySeconds.coerceIn(0f, MAX_DELAY_SECONDS)
      preferences[Keys.MirrorFlip] = settings.mirrorFlip
      preferences[Keys.FlashStrength] = settings.flashStrength.coerceIn(0f, 1f)
      preferences[Keys.ZoomRatio] = settings.zoomRatio.coerceAtLeast(1f)
      preferences[Keys.FullscreenMirror] = settings.fullscreenMirror
      preferences[Keys.LiveCoachSeen] = settings.liveCoachSeen
      preferences[Keys.ReviewCoachSeen] = settings.reviewCoachSeen
    }
  }

  private object Keys {
    val DelaySeconds = floatPreferencesKey("delay_seconds")
    val MirrorFlip = booleanPreferencesKey("mirror_flip")
    val FlashStrength = floatPreferencesKey("flash_strength")
    val ZoomRatio = floatPreferencesKey("zoom_ratio")
    val FullscreenMirror = booleanPreferencesKey("fullscreen_mirror")
    val LiveCoachSeen = booleanPreferencesKey("live_coach_seen")
    val ReviewCoachSeen = booleanPreferencesKey("review_coach_seen")
  }
}

private const val MAX_DELAY_SECONDS = 5f
