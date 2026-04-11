package com.vince.localmp3player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vince.localmp3player.data.AppPreferences
import com.vince.localmp3player.data.LibraryRepository
import com.vince.localmp3player.player.MusicPlayerController
import com.vince.localmp3player.player.ShortAudioRecorder
import com.vince.localmp3player.player.SoundEffectPlayer
import com.vince.localmp3player.ui.KidTunesApp
import com.vince.localmp3player.ui.theme.KidTunesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(
            preferences = AppPreferences(applicationContext),
            repository = LibraryRepository(applicationContext),
            playerController = MusicPlayerController(applicationContext),
            soundEffectPlayer = SoundEffectPlayer(applicationContext),
            recorder = ShortAudioRecorder(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KidTunesTheme {
                KidTunesApp(viewModel = viewModel)
            }
        }
    }
}
