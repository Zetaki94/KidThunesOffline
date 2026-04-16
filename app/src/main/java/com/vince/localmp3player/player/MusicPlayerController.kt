package com.vince.localmp3player.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vince.localmp3player.data.LibraryAudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatSetting {
    OFF,
    ALL,
    ONE,
}

data class PlayerUiState(
    val currentItem: LibraryAudioItem? = null,
    val queue: List<LibraryAudioItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatSetting: RepeatSetting = RepeatSetting.ALL,
    val shuffleEnabled: Boolean = false,
)

class MusicPlayerController(
    context: Context,
) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(PlayerUiState())
    private var onTrackEnded: ((LibraryAudioItem) -> Unit)? = null

    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    publishState()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    publishState()
                    if (playbackState == Player.STATE_ENDED) {
                        _uiState.value.currentItem?.let { endedItem ->
                            onTrackEnded?.invoke(endedItem)
                        }
                    }
                }
            },
        )

        scope.launch {
            while (isActive) {
                publishState()
                delay(300)
            }
        }
    }

    fun setOnTrackEndedListener(listener: (LibraryAudioItem) -> Unit) {
        onTrackEnded = listener
    }

    fun playNow(item: LibraryAudioItem) {
        playItem(item, _uiState.value.queue.indexOfFirst { queuedItem -> queuedItem.id == item.id })
    }

    fun enqueue(item: LibraryAudioItem) {
        if (_uiState.value.queue.any { it.id == item.id }) return
        _uiState.value = _uiState.value.copy(queue = _uiState.value.queue + item)
        publishState()
    }

    fun togglePlayPause() {
        val state = _uiState.value
        when {
            state.currentItem == null && state.queue.isNotEmpty() -> playFromQueue(0)
            state.currentItem == null -> Unit
            player.isPlaying -> player.pause()
            else -> {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                }
                player.play()
            }
        }
        publishState()
    }

    fun seekTo(positionMs: Long) {
        if (_uiState.value.currentItem == null) return
        player.seekTo(positionMs)
        publishState()
    }

    fun playFromQueue(index: Int) {
        val item = _uiState.value.queue.getOrNull(index) ?: return
        playItem(item, index)
    }

    fun cycleRepeatMode() {
        _uiState.value = _uiState.value.copy(
            repeatSetting = when (_uiState.value.repeatSetting) {
                RepeatSetting.OFF -> RepeatSetting.ALL
                RepeatSetting.ALL -> RepeatSetting.ONE
                RepeatSetting.ONE -> RepeatSetting.OFF
            },
        )
    }

    fun toggleShuffle() {
        _uiState.value = _uiState.value.copy(shuffleEnabled = !_uiState.value.shuffleEnabled)
    }

    fun removeQueueItem(index: Int) {
        val state = _uiState.value
        if (index !in state.queue.indices) return

        val updatedQueue = state.queue.toMutableList().apply { removeAt(index) }
        val updatedCurrentIndex = when {
            state.currentIndex == index -> -1
            state.currentIndex > index -> state.currentIndex - 1
            else -> state.currentIndex
        }

        _uiState.value = state.copy(
            queue = updatedQueue,
            currentIndex = updatedCurrentIndex,
        )
        publishState()
    }

    fun replayCurrent() {
        val currentItem = _uiState.value.currentItem ?: return
        playItem(currentItem, _uiState.value.currentIndex)
    }

    fun stopAtStart() {
        if (_uiState.value.currentItem == null) return
        player.pause()
        player.seekTo(0L)
        publishState()
    }

    fun release() {
        scope.cancel()
        player.release()
    }

    private fun playItem(item: LibraryAudioItem, queueIndex: Int) {
        player.setMediaItem(mediaItemFrom(item))
        player.prepare()
        player.playWhenReady = true
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            currentIndex = queueIndex,
        )
        publishState()
    }

    private fun publishState() {
        val state = _uiState.value
        _uiState.value = state.copy(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: state.currentItem?.durationMs ?: 0L,
        )
    }

    private fun mediaItemFrom(item: LibraryAudioItem): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.parse(item.audioUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .build(),
            )
            .build()
    }
}
