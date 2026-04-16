package com.vince.localmp3player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
                lastMusicCategoryId = preferences[lastMusicCategoryKey]?.takeIf { it.isNotBlank() },
                lastSoundCategoryId = preferences[lastSoundCategoryKey]?.takeIf { it.isNotBlank() },
                soundboardRecordingEnabled = preferences[soundboardRecordingEnabledKey] ?: true,
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
        val lastMusicCategoryKey = stringPreferencesKey("last_music_category_id")
        val lastSoundCategoryKey = stringPreferencesKey("last_sound_category_id")
        val soundboardRecordingEnabledKey = booleanPreferencesKey("soundboard_recording_enabled")
    }
}
