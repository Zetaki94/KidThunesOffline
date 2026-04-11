package com.vince.localmp3player.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class SoundEffectPlayer(
    private val context: Context,
) {
    private val activePlayers = mutableSetOf<MediaPlayer>()

    fun play(uriString: String) {
        val player = MediaPlayer()
        activePlayers += player

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        player.setOnPreparedListener { preparedPlayer ->
            preparedPlayer.start()
        }
        player.setOnCompletionListener { finishedPlayer ->
            activePlayers -= finishedPlayer
            finishedPlayer.release()
        }
        player.setOnErrorListener { erroredPlayer, _, _ ->
            activePlayers -= erroredPlayer
            erroredPlayer.release()
            true
        }

        runCatching {
            player.setDataSource(context, Uri.parse(uriString))
            player.prepareAsync()
        }.onFailure {
            activePlayers -= player
            player.release()
        }
    }

    fun release() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }
}
