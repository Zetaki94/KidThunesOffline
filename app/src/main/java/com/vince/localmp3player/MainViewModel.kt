package com.vince.localmp3player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vince.localmp3player.data.AppPreferences
import com.vince.localmp3player.data.AppSettings
import com.vince.localmp3player.data.CategoryEntry
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
import kotlin.random.Random

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val library: LibrarySnapshot = LibrarySnapshot(rootUri = null),
    val isLoading: Boolean = true,
    val musicSearch: String = "",
    val soundSearch: String = "",
    val selectedMusicCategoryId: String? = null,
    val selectedSoundCategoryId: String? = null,
    val message: String? = null,
    val pendingRecordedSoundId: String? = null,
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
    private var currentRecordingOutputId: String? = null

    init {
        playerController.setOnTrackEndedListener(::handleTrackCompletion)
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

    fun playTrack(track: LibraryAudioItem) {
        playTrackInternal(track)
    }

    fun enqueueTrack(track: LibraryAudioItem) {
        playerController.enqueue(track)
        postMessage("\"${track.title}\" ajoutee a la file d'attente.")
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun skipPrevious() {
        val currentItem = playerState.value.currentItem
        val target = when {
            currentItem == null && playerState.value.queue.isNotEmpty() -> {
                playerController.playFromQueue(0)
                null
            }

            currentItem == null -> null
            playerState.value.shuffleEnabled -> resolveRandomTrack(excludingId = currentItem.id)
            else -> resolveNeighborInCategory(currentItem, step = -1)
        }

        target?.let(::playTrackInternal)
    }

    fun skipNext() {
        val currentItem = playerState.value.currentItem
        val target = when {
            currentItem == null && playerState.value.queue.isNotEmpty() -> {
                playerController.playFromQueue(0)
                null
            }

            currentItem == null -> null
            playerState.value.shuffleEnabled -> resolveRandomTrack(excludingId = currentItem.id)
            else -> resolveNeighborInCategory(currentItem, step = 1)
        }

        target?.let(::playTrackInternal)
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    fun toggleShuffle() {
        playerController.toggleShuffle()
        if (playerState.value.shuffleEnabled && playerState.value.currentItem == null) {
            resolveRandomTrack()?.let(::playTrackInternal)
        }
    }

    fun playQueueIndex(index: Int) = playerController.playFromQueue(index)

    fun removeTrackFromQueue(index: Int) = playerController.removeQueueItem(index)

    fun playSoundEffect(item: LibraryAudioItem) {
        soundEffectPlayer.play(item.audioUri)
    }

    fun toggleFavorite(item: LibraryAudioItem) {
        viewModelScope.launch {
            preferences.toggleFavorite(item.id)
        }
    }

    fun deleteItem(item: LibraryAudioItem) {
        viewModelScope.launch {
            val result = repository.deleteItemPair(item)
            postMessage(result.message)
            if (result.success) {
                reloadLibrary()
            }
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

    fun deleteCategory(category: CategoryEntry) {
        viewModelScope.launch {
            val result = repository.deleteCategory(category)
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

    fun setSoundboardRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSoundboardRecordingEnabled(enabled)
        }
    }

    fun verifyPin(pin: String): Boolean {
        return pin.trim() == _uiState.value.settings.adminPin
    }

    fun startRecording(preferredCategoryFolderUri: String?) {
        if (!_uiState.value.settings.soundboardRecordingEnabled) {
            postMessage("L'enregistrement est desactive dans les parametres.")
            return
        }

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
                .onSuccess { outputId ->
                    currentRecordingOutputId = outputId
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
                }
                .onFailure {
                    postMessage("L'enregistrement a echoue.")
                }
        }
    }

    fun handleRecordingEnded() {
        val targetSoundId = currentRecordingOutputId
        currentRecordingOutputId = null
        reloadLibrary(targetSoundId)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissPendingRecordedRename() {
        _uiState.value = _uiState.value.copy(pendingRecordedSoundId = null)
    }

    fun reloadLibrary(pendingRecordedSoundId: String? = _uiState.value.pendingRecordedSoundId) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val snapshot = repository.scanLibrary(_uiState.value.settings.rootUri)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                library = snapshot,
                pendingRecordedSoundId = pendingRecordedSoundId
                    ?.takeIf { targetId -> snapshot.soundPads.any { it.id == targetId } },
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

    private fun playTrackInternal(track: LibraryAudioItem) {
        playerController.playNow(track)
        viewModelScope.launch {
            preferences.pushRecent(track.id)
        }
    }

    private fun handleTrackCompletion(completedItem: LibraryAudioItem) {
        when {
            playerState.value.repeatSetting == com.vince.localmp3player.player.RepeatSetting.ONE -> {
                playerController.replayCurrent()
            }

            playerState.value.shuffleEnabled -> {
                resolveRandomTrack(excludingId = completedItem.id)?.let(::playTrackInternal)
                    ?: playerController.stopAtStart()
            }

            else -> {
                val nextTrack = resolveNextTrackInCategory(completedItem)
                if (nextTrack != null) {
                    playTrackInternal(nextTrack)
                } else if (playerState.value.repeatSetting == com.vince.localmp3player.player.RepeatSetting.ALL) {
                    resolveFirstTrackInCategory(completedItem)?.let(::playTrackInternal)
                        ?: playerController.stopAtStart()
                } else {
                    playerController.stopAtStart()
                }
            }
        }
    }

    private fun resolveNeighborInCategory(
        currentItem: LibraryAudioItem,
        step: Int,
    ): LibraryAudioItem? {
        val categoryTracks = _uiState.value.library.musicTracks.filter { track ->
            track.categoryId == currentItem.categoryId
        }
        if (categoryTracks.isEmpty()) return null

        val currentIndex = categoryTracks.indexOfFirst { track -> track.id == currentItem.id }
        if (currentIndex == -1) return categoryTracks.firstOrNull()

        val targetIndex = when {
            currentIndex + step < 0 -> categoryTracks.lastIndex
            currentIndex + step > categoryTracks.lastIndex -> 0
            else -> currentIndex + step
        }
        return categoryTracks.getOrNull(targetIndex)
    }

    private fun resolveNextTrackInCategory(currentItem: LibraryAudioItem): LibraryAudioItem? {
        val categoryTracks = _uiState.value.library.musicTracks.filter { track ->
            track.categoryId == currentItem.categoryId
        }
        if (categoryTracks.isEmpty()) return null

        val currentIndex = categoryTracks.indexOfFirst { track -> track.id == currentItem.id }
        if (currentIndex == -1 || currentIndex >= categoryTracks.lastIndex) return null
        return categoryTracks.getOrNull(currentIndex + 1)
    }

    private fun resolveFirstTrackInCategory(currentItem: LibraryAudioItem): LibraryAudioItem? {
        return _uiState.value.library.musicTracks.firstOrNull { track ->
            track.categoryId == currentItem.categoryId
        }
    }

    private fun resolveRandomTrack(excludingId: String? = null): LibraryAudioItem? {
        val candidates = _uiState.value.library.musicTracks.filter { track ->
            excludingId == null || _uiState.value.library.musicTracks.size == 1 || track.id != excludingId
        }
        if (candidates.isEmpty()) return null
        return candidates[Random.nextInt(candidates.size)]
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
