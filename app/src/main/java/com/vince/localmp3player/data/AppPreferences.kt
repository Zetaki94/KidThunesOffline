package com.vince.localmp3player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appDataStore by preferencesDataStore(name = "kid_tunes_settings")

class AppPreferences(
    private val context: Context,
) {
    private val delimiter = '\u001F'

    val settingsFlow: Flow<AppSettings> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                rootUri = preferences[rootUriKey]?.takeIf { it.isNotBlank() },
                adminPin = preferences[adminPinKey] ?: DEFAULT_ADMIN_PIN,
                favoriteTrackIds = preferences[favoritesKey].orEmpty(),
                recentTrackIds = decodeList(preferences[recentsKey]),
                requestedSongTitles = decodeList(preferences[requestedSongsKey]),
                lastMusicCategoryId = preferences[lastMusicCategoryKey]?.takeIf { it.isNotBlank() },
                lastSoundCategoryId = preferences[lastSoundCategoryKey]?.takeIf { it.isNotBlank() },
                soundboardRecordingEnabled = preferences[soundboardRecordingEnabledKey] ?: true,
                musicVolume = preferences[musicVolumeKey] ?: DEFAULT_MUSIC_VOLUME,
                accessMode = preferences[accessModeKey]
                    ?.let { storedMode -> AppAccessMode.entries.firstOrNull { it.name == storedMode } }
                    ?: AppAccessMode.CHILD,
                adultModeLocked = preferences[adultModeLockedKey] ?: true,
                interfaceScale = preferences[interfaceScaleKey] ?: DEFAULT_INTERFACE_SCALE,
                musicSectionVisible = preferences[musicSectionVisibleKey] ?: true,
                blindTestSectionVisible = preferences[blindTestSectionVisibleKey] ?: true,
                orchestraSectionVisible = preferences[orchestraSectionVisibleKey] ?: true,
                drawingSectionVisible = preferences[drawingSectionVisibleKey] ?: true,
                soundboardSectionVisible = preferences[soundboardSectionVisibleKey] ?: true,
            )
        }

    suspend fun setRootUri(rootUri: String?) {
        context.appDataStore.edit { preferences ->
            if (rootUri.isNullOrBlank()) {
                preferences.remove(rootUriKey)
            } else {
                preferences[rootUriKey] = rootUri
            }
        }
    }

    suspend fun setAdminPin(pin: String) {
        context.appDataStore.edit { preferences ->
            preferences[adminPinKey] = pin
        }
    }

    suspend fun toggleFavorite(trackId: String) {
        context.appDataStore.edit { preferences ->
            val updated = preferences[favoritesKey].orEmpty().toMutableSet()
            if (!updated.add(trackId)) {
                updated.remove(trackId)
            }
            preferences[favoritesKey] = updated
        }
    }

    suspend fun pushRecent(trackId: String) {
        context.appDataStore.edit { preferences ->
            val updated = decodeList(preferences[recentsKey])
                .filterNot { it == trackId }
                .toMutableList()
            updated.add(0, trackId)
            preferences[recentsKey] = encodeList(updated.take(20))
        }
    }

    suspend fun replaceTrackId(oldId: String, newId: String) {
        context.appDataStore.edit { preferences ->
            val favorites = preferences[favoritesKey].orEmpty().toMutableSet()
            if (favorites.remove(oldId)) {
                favorites.add(newId)
            }
            preferences[favoritesKey] = favorites

            val recents = decodeList(preferences[recentsKey]).map {
                if (it == oldId) newId else it
            }
            preferences[recentsKey] = encodeList(recents)
        }
    }

    suspend fun addRequestedSong(title: String) {
        context.appDataStore.edit { preferences ->
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank()) return@edit
            val existing = decodeList(preferences[requestedSongsKey]).toMutableList()
            if (existing.none { it.equals(cleanTitle, ignoreCase = true) }) {
                existing.add(cleanTitle)
                preferences[requestedSongsKey] = encodeList(existing)
            }
        }
    }

    suspend fun removeRequestedSong(title: String) {
        context.appDataStore.edit { preferences ->
            val updated = decodeList(preferences[requestedSongsKey])
                .filterNot { it.equals(title, ignoreCase = true) }
            preferences[requestedSongsKey] = encodeList(updated)
        }
    }

    suspend fun setLastMusicCategoryId(categoryId: String?) {
        context.appDataStore.edit { preferences ->
            if (categoryId.isNullOrBlank()) {
                preferences.remove(lastMusicCategoryKey)
            } else {
                preferences[lastMusicCategoryKey] = categoryId
            }
        }
    }

    suspend fun setLastSoundCategoryId(categoryId: String?) {
        context.appDataStore.edit { preferences ->
            if (categoryId.isNullOrBlank()) {
                preferences.remove(lastSoundCategoryKey)
            } else {
                preferences[lastSoundCategoryKey] = categoryId
            }
        }
    }

    suspend fun setSoundboardRecordingEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[soundboardRecordingEnabledKey] = enabled
        }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.appDataStore.edit { preferences ->
            preferences[musicVolumeKey] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun setAccessMode(mode: AppAccessMode) {
        context.appDataStore.edit { preferences ->
            preferences[accessModeKey] = mode.name
        }
    }

    suspend fun setAdultModeLocked(locked: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[adultModeLockedKey] = locked
        }
    }

    suspend fun setInterfaceScale(scale: Float) {
        context.appDataStore.edit { preferences ->
            preferences[interfaceScaleKey] = scale.coerceIn(0.7f, 1.15f)
        }
    }

    suspend fun setMusicSectionVisible(visible: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[musicSectionVisibleKey] = visible
        }
    }

    suspend fun setBlindTestSectionVisible(visible: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[blindTestSectionVisibleKey] = visible
        }
    }

    suspend fun setOrchestraSectionVisible(visible: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[orchestraSectionVisibleKey] = visible
        }
    }

    suspend fun setDrawingSectionVisible(visible: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[drawingSectionVisibleKey] = visible
        }
    }

    suspend fun setSoundboardSectionVisible(visible: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[soundboardSectionVisibleKey] = visible
        }
    }

    private fun encodeList(values: List<String>): String {
        return values.joinToString(delimiter.toString())
    }

    private fun decodeList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(delimiter).filter { it.isNotBlank() }
    }

    private companion object {
        val rootUriKey = stringPreferencesKey("root_uri")
        val adminPinKey = stringPreferencesKey("admin_pin")
        val favoritesKey = stringSetPreferencesKey("favorite_track_ids")
        val recentsKey = stringPreferencesKey("recent_track_ids")
        val requestedSongsKey = stringPreferencesKey("requested_song_titles")
        val lastMusicCategoryKey = stringPreferencesKey("last_music_category_id")
        val lastSoundCategoryKey = stringPreferencesKey("last_sound_category_id")
        val soundboardRecordingEnabledKey = booleanPreferencesKey("soundboard_recording_enabled")
        val musicVolumeKey = floatPreferencesKey("music_volume")
        val accessModeKey = stringPreferencesKey("access_mode")
        val adultModeLockedKey = booleanPreferencesKey("adult_mode_locked")
        val interfaceScaleKey = floatPreferencesKey("interface_scale")
        val musicSectionVisibleKey = booleanPreferencesKey("music_section_visible")
        val blindTestSectionVisibleKey = booleanPreferencesKey("blind_test_section_visible")
        val orchestraSectionVisibleKey = booleanPreferencesKey("orchestra_section_visible")
        val drawingSectionVisibleKey = booleanPreferencesKey("drawing_section_visible")
        val soundboardSectionVisibleKey = booleanPreferencesKey("soundboard_section_visible")
    }
}
