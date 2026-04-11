package com.vince.localmp3player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vince.localmp3player.data.AppPreferences
import com.vince.localmp3player.data.AppSettings
import com.vince.localmp3player.data.LibraryAudioItem
import com.vince.localmp3player.data.LibraryRepository
import com.vince.localmp3player.data.LibrarySection
import com.vince.localmp3player.data.LibrarySnapshot
import com.vince.localmp3player.player.MusicPlayerController
import com.vince.localmp3player.player.PlayerUiState
import com.vince.localmp3player.player.RecordingUiState
import com.vince.localmp3player.player.ShortAudioRecorder
import com.vince.localmp3player.player.SoundEffectPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val library: LibrarySnapshot = LibrarySnapshot(rootUri = null),
    val isLoading: Boolean = true,
    val musicSearch: String = "",
    val soundSearch: String = "",
    val selectedMusicCategoryId: String? = null,
    val selectedSoundCategoryId: String? = null,
    val message: String? = null,
) {
    val favoriteIds: Set<String> = settings.favoriteTrackIds
    val recentIds: List<String> = settings.recentTrackIds
}

class MainViewModel(
    private val preferences: AppPreferences,
    private val repository: LibraryRepository,
    private val playerController: MusicPlayerController,
    private val soundEffectPlayer: SoundEffectPlayer,
    private val recorder: ShortAudioRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    val playerState: StateFlow<PlayerUiState> = playerController.uiState
    val recordingState: StateFlow<RecordingUiState> = recorder.state

    private var lastObservedRootUri: String? = null

    init {
        observePreferences()
    }

    fun onRootFolderPicked(uri: Uri, flags: Int) {
        viewModelScope.launch {
            runCatching {
                repository.persistRootPermission(uri, flags)
                preferences.setRootUri(uri.toString())
            }.onFailure {
                postMessage("Le dossier racine n'a pas pu etre memorise.")
            }
        }
    }

    fun setMusicSearch(query: String) {
        _uiState.value = _uiState.value.copy(musicSearch = query)
    }

    fun setSoundSearch(query: String) {
        _uiState.value = _uiState.value.copy(soundSearch = query)
    }

    fun selectMusicCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedMusicCategoryId = categoryId)
        viewModelScope.launch {
            preferences.setLastMusicCategoryId(categoryId)
        }
    }

    fun selectSoundCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedSoundCategoryId = categoryId)
        viewModelScope.launch {
            preferences.setLastSoundCategoryId(categoryId)
        }
    }

    fun playTrack(track: LibraryAudioItem, queue: List<LibraryAudioItem>) {
        playerController.playQueue(queue, queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
        viewModelScope.launch {
            preferences.pushRecent(track.id)
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun skipPrevious() = playerController.skipPrevious()

    fun skipNext() = playerController.skipNext()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    fun toggleShuffle() = playerController.toggleShuffle()

    fun playQueueIndex(index: Int) = playerController.playFromQueue(index)

    fun playSoundEffect(item: LibraryAudioItem) {
        soundEffectPlayer.play(item.audioUri)
    }

    fun toggleFavorite(item: LibraryAudioItem) {
        viewModelScope.launch {
            preferences.toggleFavorite(item.id)
        }
    }

    fun renameTrack(item: LibraryAudioItem, newBaseName: String) {
        viewModelScope.launch {
            val result = repository.renameTrackPair(item, newBaseName)
            if (result.success && result.updatedTrackId != null && result.updatedTrackId != item.id) {
                preferences.replaceTrackId(item.id, result.updatedTrackId)
            }
            postMessage(result.message)
            reloadLibrary()
        }
    }

    fun moveTrack(item: LibraryAudioItem, destinationFolderUri: String) {
        viewModelScope.launch {
            val result = repository.moveTrackPair(item, destinationFolderUri)
            if (result.success && result.updatedTrackId != null && result.updatedTrackId != item.id) {
                preferences.replaceTrackId(item.id, result.updatedTrackId)
            }
            postMessage(result.message)
            reloadLibrary()
        }
    }

    fun createCategory(section: LibrarySection, name: String) {
        viewModelScope.launch {
            val result = repository.createCategory(_uiState.value.settings.rootUri, section, name)
            postMessage(result.message)
            if (result.success) {
                reloadLibrary()
            }
        }
    }

    fun changePin(newPin: String) {
        viewModelScope.launch {
            val safePin = newPin.filter(Char::isDigit)
            if (safePin.length < 4) {
                postMessage("Le code parent doit contenir au moins 4 chiffres.")
                return@launch
            }
            preferences.setAdminPin(safePin)
            postMessage("Le code parent a ete mis a jour.")
        }
    }

    fun verifyPin(pin: String): Boolean {
        return pin.trim() == _uiState.value.settings.adminPin
    }

    fun startRecording(preferredCategoryFolderUri: String?) {
        viewModelScope.launch {
            val folderUri = repository.resolveRecordingFolderUri(
                rootUri = _uiState.value.settings.rootUri,
                preferredCategoryFolderUri = preferredCategoryFolderUri,
            )

            if (folderUri == null) {
                postMessage("Choisis d'abord un dossier racine ou une categorie de sons.")
                return@launch
            }

            recorder.startRecording(folderUri)
                .onSuccess {
                    postMessage("Enregistrement lance.")
                }
                .onFailure {
                    postMessage("Impossible de lancer l'enregistrement.")
                }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            recorder.stopRecording()
                .onSuccess {
                    postMessage("Son enregistre.")
                    reloadLibrary()
                }
                .onFailure {
                    postMessage("L'enregistrement a echoue.")
                }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun reloadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val snapshot = repository.scanLibrary(_uiState.value.settings.rootUri)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                library = snapshot,
                selectedMusicCategoryId = _uiState.value.selectedMusicCategoryId
                    ?.takeIf { selectedId -> snapshot.musicCategories.any { it.id == selectedId } },
                selectedSoundCategoryId = _uiState.value.selectedSoundCategoryId
                    ?.takeIf { selectedId -> snapshot.soundCategories.any { it.id == selectedId } },
            )
        }
    }

    override fun onCleared() {
        playerController.release()
        soundEffectPlayer.release()
        recorder.release()
        super.onCleared()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.settingsFlow.collectLatest { settings ->
                val previousState = _uiState.value
                _uiState.value = previousState.copy(
                    settings = settings,
                    selectedMusicCategoryId = previousState.selectedMusicCategoryId ?: settings.lastMusicCategoryId,
                    selectedSoundCategoryId = previousState.selectedSoundCategoryId ?: settings.lastSoundCategoryId,
                )

                if (settings.rootUri != lastObservedRootUri) {
                    lastObservedRootUri = settings.rootUri
                    reloadLibrary()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    private fun postMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    companion object {
        fun factory(
            preferences: AppPreferences,
            repository: LibraryRepository,
            playerController: MusicPlayerController,
            soundEffectPlayer: SoundEffectPlayer,
            recorder: ShortAudioRecorder,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        preferences = preferences,
                        repository = repository,
                        playerController = playerController,
                        soundEffectPlayer = soundEffectPlayer,
                        recorder = recorder,
                    ) as T
                }
            }
        }
    }
}
