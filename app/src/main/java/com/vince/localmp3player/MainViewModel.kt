package com.vince.localmp3player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vince.localmp3player.data.AppAccessMode
import com.vince.localmp3player.data.AppPreferences
import com.vince.localmp3player.data.AppSettings
import com.vince.localmp3player.data.CategoryEntry
import com.vince.localmp3player.data.LibraryAudioItem
import com.vince.localmp3player.data.LibraryRepository
import com.vince.localmp3player.data.LibrarySection
import com.vince.localmp3player.data.LibrarySnapshot
import com.vince.localmp3player.player.DrumSoundEngine
import com.vince.localmp3player.player.MusicPlayerController
import com.vince.localmp3player.player.PianoSoundEngine
import com.vince.localmp3player.player.PlayerUiState
import com.vince.localmp3player.player.PreviewAudioController
import com.vince.localmp3player.player.RecordingUiState
import com.vince.localmp3player.player.RepeatSetting
import com.vince.localmp3player.player.ShortAudioRecorder
import com.vince.localmp3player.player.SoundEffectPlayer
import com.vince.localmp3player.player.XylophoneSoundEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

enum class BlindTestPhase {
    IDLE,
    NOT_ENOUGH_TRACKS,
    COUNTDOWN,
    PLAYING,
    ANSWERING,
    REVEAL,
    FINISHED,
}

private const val BLIND_TEST_ROUNDS = 10

data class BlindTestUiState(
    val phase: BlindTestPhase = BlindTestPhase.IDLE,
    val roundNumber: Int = 0,
    val totalRounds: Int = BLIND_TEST_ROUNDS,
    val countdownSeconds: Int = 3,
    val secondsLeft: Int = 0,
    val score: Int = 0,
    val currentTrackId: String? = null,
    val options: List<LibraryAudioItem> = emptyList(),
    val selectedAnswerId: String? = null,
    val revealedCorrectTrackId: String? = null,
    val feedbackText: String? = null,
)

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
    val blindTest: BlindTestUiState = BlindTestUiState(),
) {
    val favoriteIds: Set<String> = settings.favoriteTrackIds
    val recentIds: List<String> = settings.recentTrackIds
    val requestedSongTitles: List<String> = settings.requestedSongTitles
    val isAdultMode: Boolean = settings.accessMode == AppAccessMode.ADULT
}

class MainViewModel(
    private val preferences: AppPreferences,
    private val repository: LibraryRepository,
    private val playerController: MusicPlayerController,
    private val soundEffectPlayer: SoundEffectPlayer,
    private val recorder: ShortAudioRecorder,
    private val blindTestPlayer: PreviewAudioController,
    private val pianoSoundEngine: PianoSoundEngine,
    private val drumSoundEngine: DrumSoundEngine,
    private val xylophoneSoundEngine: XylophoneSoundEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    val playerState: StateFlow<PlayerUiState> = playerController.uiState
    val recordingState: StateFlow<RecordingUiState> = recorder.state

    private var lastObservedRootUri: String? = null
    private var currentRecordingOutputId: String? = null
    private var blindTestJob: Job? = null
    private var blindTestAnswer: CompletableDeferred<String?>? = null
    private var blindTestSkipPlayback: CompletableDeferred<Unit>? = null
    private var shouldResumeMusicAfterBlindTest = false

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
                postMessage("Le dossier racine n'a pas pu être mémorisé.")
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
        postMessage("\"${track.title}\" a été ajoutée à la file d'attente.")
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

    fun playPianoNote(noteIndex: Int) {
        pianoSoundEngine.play(noteIndex)
    }

    fun playDrumPad(padIndex: Int) {
        drumSoundEngine.play(padIndex)
    }

    fun playXylophoneNote(noteIndex: Int) {
        xylophoneSoundEngine.play(noteIndex)
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
            postMessage("Le code parent a été mis à jour.")
        }
    }

    fun setSoundboardRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSoundboardRecordingEnabled(enabled)
        }
    }

    fun setAccessMode(mode: AppAccessMode) {
        viewModelScope.launch {
            preferences.setAccessMode(mode)
        }
    }

    fun setAdultModeLocked(locked: Boolean) {
        viewModelScope.launch {
            preferences.setAdultModeLocked(locked)
        }
    }

    fun setInterfaceScale(scale: Float) {
        viewModelScope.launch {
            preferences.setInterfaceScale(scale)
        }
    }

    fun requestSong(title: String) {
        viewModelScope.launch {
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank()) {
                postMessage("Le nom de la musique est vide.")
                return@launch
            }
            preferences.addRequestedSong(cleanTitle)
            postMessage("Demande ajoutée : $cleanTitle")
        }
    }

    fun removeRequestedSong(title: String) {
        viewModelScope.launch {
            preferences.removeRequestedSong(title)
            postMessage("Demande supprimée : $title")
        }
    }

    fun setMusicSectionVisible(visible: Boolean) {
        viewModelScope.launch { preferences.setMusicSectionVisible(visible) }
    }

    fun setBlindTestSectionVisible(visible: Boolean) {
        viewModelScope.launch { preferences.setBlindTestSectionVisible(visible) }
    }

    fun setOrchestraSectionVisible(visible: Boolean) {
        viewModelScope.launch { preferences.setOrchestraSectionVisible(visible) }
    }

    fun setDrawingSectionVisible(visible: Boolean) {
        viewModelScope.launch { preferences.setDrawingSectionVisible(visible) }
    }

    fun setSoundboardSectionVisible(visible: Boolean) {
        viewModelScope.launch { preferences.setSoundboardSectionVisible(visible) }
    }

    fun verifyPin(pin: String): Boolean {
        return pin.trim() == _uiState.value.settings.adminPin
    }

    fun startRecording(preferredCategoryFolderUri: String?) {
        if (!_uiState.value.settings.soundboardRecordingEnabled) {
            postMessage("L'enregistrement est désactivé dans les paramètres.")
            return
        }

        viewModelScope.launch {
            val folderUri = repository.resolveRecordingFolderUri(
                rootUri = _uiState.value.settings.rootUri,
                preferredCategoryFolderUri = preferredCategoryFolderUri,
            )

            if (folderUri == null) {
                postMessage("Choisis d'abord un dossier racine ou une catégorie de sons.")
                return@launch
            }

            recorder.startRecording(folderUri)
                .onSuccess { outputId ->
                    currentRecordingOutputId = outputId
                    postMessage("Enregistrement lancé.")
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
                    postMessage("Son enregistré.")
                }
                .onFailure {
                    postMessage("L'enregistrement a échoué.")
                }
        }
    }

    fun handleRecordingEnded() {
        val targetSoundId = currentRecordingOutputId
        currentRecordingOutputId = null
        reloadLibrary(targetSoundId)
    }

    fun startBlindTest() {
        blindTestJob?.cancel()
        blindTestAnswer?.cancel()
        blindTestSkipPlayback?.cancel()
        blindTestPlayer.stop()

        val tracks = _uiState.value.library.musicTracks.distinctBy { it.id }
        if (tracks.size < 4) {
            _uiState.value = _uiState.value.copy(
                blindTest = BlindTestUiState(
                    phase = BlindTestPhase.NOT_ENOUGH_TRACKS,
                    feedbackText = "Ajoute au moins 4 musiques pour lancer un blind test.",
                ),
            )
            return
        }

        val isFreshBlindTestSession = _uiState.value.blindTest.phase == BlindTestPhase.IDLE ||
            _uiState.value.blindTest.phase == BlindTestPhase.NOT_ENOUGH_TRACKS
        if (isFreshBlindTestSession) {
            shouldResumeMusicAfterBlindTest = playerState.value.isPlaying
            if (shouldResumeMusicAfterBlindTest) {
                playerController.pausePlayback()
            }
        }

        val rounds = buildBlindTestRounds(tracks)
        blindTestJob = viewModelScope.launch {
            var score = 0
            try {
                rounds.forEachIndexed { index, track ->
                    val roundNumber = index + 1
                    blindTestPlayer.play(
                        uriString = track.audioUri,
                        startPositionMs = resolveBlindTestExcerptStart(track),
                    )
                    val skipDeferred = CompletableDeferred<Unit>()
                    blindTestSkipPlayback = skipDeferred
                    val playbackCountdownJob = viewModelScope.launch {
                        for (seconds in 20 downTo 1) {
                            _uiState.value = _uiState.value.copy(
                                blindTest = BlindTestUiState(
                                    phase = BlindTestPhase.PLAYING,
                                    roundNumber = roundNumber,
                                    score = score,
                                    secondsLeft = seconds,
                                    currentTrackId = track.id,
                                ),
                            )
                            delay(1_000)
                        }
                    }
                    withTimeoutOrNull(20_000) {
                        skipDeferred.await()
                    }
                    playbackCountdownJob.cancel()
                    blindTestSkipPlayback = null
                    blindTestPlayer.fadeOutAndStop()

                    val options = buildBlindTestOptions(correctTrack = track, allTracks = tracks)
                    val answerDeferred = CompletableDeferred<String?>()
                    blindTestAnswer = answerDeferred
                    _uiState.value = _uiState.value.copy(
                        blindTest = BlindTestUiState(
                            phase = BlindTestPhase.ANSWERING,
                            roundNumber = roundNumber,
                            score = score,
                            secondsLeft = 10,
                            currentTrackId = track.id,
                            options = options,
                        ),
                    )

                    val countdownJob = viewModelScope.launch {
                        for (seconds in 10 downTo 1) {
                            val current = _uiState.value.blindTest
                            _uiState.value = _uiState.value.copy(
                                blindTest = current.copy(secondsLeft = seconds),
                            )
                            delay(1_000)
                        }
                    }

                    val selectedAnswer = withTimeoutOrNull(10_000) {
                        answerDeferred.await()
                    }
                    countdownJob.cancel()

                    blindTestAnswer = null
                    val isCorrect = selectedAnswer == track.id
                    if (isCorrect) {
                        score += 1
                    }

                    _uiState.value = _uiState.value.copy(
                        blindTest = BlindTestUiState(
                            phase = BlindTestPhase.REVEAL,
                            roundNumber = roundNumber,
                            score = score,
                            currentTrackId = track.id,
                            options = options,
                            selectedAnswerId = selectedAnswer,
                            revealedCorrectTrackId = track.id,
                            feedbackText = if (isCorrect) {
                                "Bonne réponse !"
                            } else {
                                "La bonne réponse était : ${track.title}"
                            },
                        ),
                    )
                    delay(2_000)
                }

                _uiState.value = _uiState.value.copy(
                    blindTest = BlindTestUiState(
                        phase = BlindTestPhase.FINISHED,
                        score = score,
                        totalRounds = BLIND_TEST_ROUNDS,
                        feedbackText = "Score final : $score/$BLIND_TEST_ROUNDS",
                    ),
                )
            } finally {
                blindTestPlayer.stop()
                blindTestAnswer = null
                blindTestSkipPlayback = null
            }
        }
    }

    fun submitBlindTestAnswer(trackId: String) {
        val currentState = _uiState.value.blindTest
        if (currentState.phase != BlindTestPhase.ANSWERING || currentState.selectedAnswerId != null) return

        _uiState.value = _uiState.value.copy(
            blindTest = currentState.copy(selectedAnswerId = trackId),
        )
        blindTestAnswer?.complete(trackId)
    }

    fun skipBlindTestPlayback() {
        if (_uiState.value.blindTest.phase == BlindTestPhase.PLAYING) {
            blindTestSkipPlayback?.complete(Unit)
        }
    }

    fun stopBlindTest() {
        blindTestJob?.cancel()
        blindTestAnswer?.cancel()
        blindTestSkipPlayback?.cancel()
        blindTestAnswer = null
        blindTestSkipPlayback = null
        blindTestPlayer.stop()
        _uiState.value = _uiState.value.copy(blindTest = BlindTestUiState())
        restoreMusicAfterBlindTest()
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissPendingRecordedRename() {
        _uiState.value = _uiState.value.copy(pendingRecordedSoundId = null)
    }

    fun reloadLibrary(
        pendingRecordedSoundId: String? = _uiState.value.pendingRecordedSoundId,
        showLoading: Boolean = true,
    ) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
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
        blindTestJob?.cancel()
        blindTestAnswer?.cancel()
        blindTestSkipPlayback?.cancel()
        playerController.release()
        soundEffectPlayer.release()
        recorder.release()
        blindTestPlayer.release()
        pianoSoundEngine.release()
        drumSoundEngine.release()
        xylophoneSoundEngine.release()
        super.onCleared()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.settingsFlow.collectLatest { settings ->
                playerController.setVolume(1f)
                blindTestPlayer.setVolume(1f)
                pianoSoundEngine.setVolume(1f)

                val previousState = _uiState.value
                _uiState.value = previousState.copy(
                    settings = settings,
                    selectedMusicCategoryId = previousState.selectedMusicCategoryId ?: settings.lastMusicCategoryId,
                    selectedSoundCategoryId = previousState.selectedSoundCategoryId ?: settings.lastSoundCategoryId,
                )

                if (settings.rootUri != lastObservedRootUri) {
                    lastObservedRootUri = settings.rootUri
                    val cachedSnapshot = repository.loadCachedLibrary(settings.rootUri)
                    if (cachedSnapshot != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            library = cachedSnapshot,
                            selectedMusicCategoryId = _uiState.value.selectedMusicCategoryId
                                ?.takeIf { selectedId -> cachedSnapshot.musicCategories.any { it.id == selectedId } },
                            selectedSoundCategoryId = _uiState.value.selectedSoundCategoryId
                                ?.takeIf { selectedId -> cachedSnapshot.soundCategories.any { it.id == selectedId } },
                        )
                    }
                    reloadLibrary(showLoading = cachedSnapshot == null)
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
        when (playerState.value.repeatSetting) {
            RepeatSetting.ONE -> {
                playerController.replayCurrent()
            }

            else -> {
                if (playerState.value.shuffleEnabled) {
                    resolveRandomTrack(excludingId = completedItem.id)?.let(::playTrackInternal)
                        ?: playerController.stopAtStart()
                    return
                }

                val nextTrack = resolveNextTrackInCategory(completedItem)
                if (nextTrack != null) {
                    playTrackInternal(nextTrack)
                } else if (playerState.value.repeatSetting == RepeatSetting.ALL) {
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

    private fun buildBlindTestRounds(tracks: List<LibraryAudioItem>): List<LibraryAudioItem> {
        return if (tracks.size >= BLIND_TEST_ROUNDS) {
            tracks.shuffled().take(BLIND_TEST_ROUNDS)
        } else {
            List(BLIND_TEST_ROUNDS) { tracks.random() }
        }
    }

    private fun buildBlindTestOptions(
        correctTrack: LibraryAudioItem,
        allTracks: List<LibraryAudioItem>,
    ): List<LibraryAudioItem> {
        val distractors = allTracks
            .filterNot { it.id == correctTrack.id }
            .shuffled()
            .take(3)
        return (distractors + correctTrack).shuffled()
    }

    private fun resolveBlindTestExcerptStart(track: LibraryAudioItem): Long {
        val durationMs = track.durationMs ?: return 0L
        val excerptLengthMs = 20_000L
        if (durationMs <= excerptLengthMs + 3_000L) return 0L
        val maxStart = (durationMs - excerptLengthMs).coerceAtLeast(0L)
        return Random.nextLong(from = 0L, until = maxStart)
    }

    private fun restoreMusicAfterBlindTest() {
        if (shouldResumeMusicAfterBlindTest) {
            playerController.resumePlayback()
        }
        shouldResumeMusicAfterBlindTest = false
    }

    companion object {
        fun factory(
            preferences: AppPreferences,
            repository: LibraryRepository,
            playerController: MusicPlayerController,
            soundEffectPlayer: SoundEffectPlayer,
            recorder: ShortAudioRecorder,
            blindTestPlayer: PreviewAudioController,
            pianoSoundEngine: PianoSoundEngine,
            drumSoundEngine: DrumSoundEngine,
            xylophoneSoundEngine: XylophoneSoundEngine,
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
                        blindTestPlayer = blindTestPlayer,
                        pianoSoundEngine = pianoSoundEngine,
                        drumSoundEngine = drumSoundEngine,
                        xylophoneSoundEngine = xylophoneSoundEngine,
                    ) as T
                }
            }
        }
    }
}
