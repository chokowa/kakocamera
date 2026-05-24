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
        delaySeconds = preferences[Keys.DelaySeconds] ?: 5f,
        mirrorFlip = preferences[Keys.MirrorFlip] ?: true,
        flashStrength = preferences[Keys.FlashStrength] ?: 0f,
        zoomRatio = preferences[Keys.ZoomRatio] ?: 1f,
      )
    }

  suspend fun save(settings: MirrorSettings) {
    appContext.settingsDataStore.edit { preferences ->
      preferences[Keys.DelaySeconds] = settings.delaySeconds.coerceIn(0f, 10f)
      preferences[Keys.MirrorFlip] = settings.mirrorFlip
      preferences[Keys.FlashStrength] = settings.flashStrength.coerceIn(0f, 1f)
      preferences[Keys.ZoomRatio] = settings.zoomRatio.coerceAtLeast(1f)
    }
  }

  private object Keys {
    val DelaySeconds = floatPreferencesKey("delay_seconds")
    val MirrorFlip = booleanPreferencesKey("mirror_flip")
    val FlashStrength = floatPreferencesKey("flash_strength")
    val ZoomRatio = floatPreferencesKey("zoom_ratio")
  }
}
