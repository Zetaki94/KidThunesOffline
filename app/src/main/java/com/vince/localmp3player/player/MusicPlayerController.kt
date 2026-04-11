package com.vince.localmp3player.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
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
    val queue: List<LibraryAudioItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatSetting: RepeatSetting = RepeatSetting.ALL,
    val shuffleEnabled: Boolean = false,
) {
    val currentItem: LibraryAudioItem?
        get() = queue.getOrNull(currentIndex)
}

class MusicPlayerController(
    context: Context,
) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    publishState()
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

    fun playQueue(queue: List<LibraryAudioItem>, startIndex: Int) {
        if (queue.isEmpty()) return

        val mediaItems = queue.map { item ->
            MediaItem.Builder()
                .setUri(Uri.parse(item.audioUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .build(),
                )
                .build()
        }

        player.setMediaItems(mediaItems, startIndex.coerceIn(0, queue.lastIndex), 0L)
        player.prepare()
        player.playWhenReady = true

        _uiState.value = _uiState.value.copy(
            queue = queue,
            currentIndex = startIndex.coerceIn(0, queue.lastIndex),
        )
    }

    fun togglePlayPause() {
        if (_uiState.value.queue.isEmpty()) return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        publishState()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        publishState()
    }

    fun playFromQueue(index: Int) {
        if (index !in _uiState.value.queue.indices) return
        player.seekTo(index, 0L)
        player.playWhenReady = true
        publishState()
    }

    fun skipPrevious() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        val currentIndex = player.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
        if (player.currentPosition > 3_000) {
            player.seekTo(currentIndex, 0L)
        } else {
            val targetIndex = if (currentIndex <= 0) queue.lastIndex else currentIndex - 1
            player.seekTo(targetIndex, 0L)
        }
        player.playWhenReady = true
        publishState()
    }

    fun skipNext() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        val currentIndex = player.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
        val targetIndex = if (currentIndex >= queue.lastIndex) 0 else currentIndex + 1
        player.seekTo(targetIndex, 0L)
        player.playWhenReady = true
        publishState()
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        publishState()
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publishState()
    }

    fun release() {
        scope.cancel()
        player.release()
    }

    private fun publishState() {
        val queue = _uiState.value.queue
        val currentIndex = player.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: -1
        _uiState.value = _uiState.value.copy(
            queue = queue,
            currentIndex = currentIndex,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            repeatSetting = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatSetting.ONE
                Player.REPEAT_MODE_ALL -> RepeatSetting.ALL
                else -> RepeatSetting.OFF
            },
            shuffleEnabled = player.shuffleModeEnabled,
        )
    }
}
