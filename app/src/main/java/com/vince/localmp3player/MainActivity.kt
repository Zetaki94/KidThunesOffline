package com.vince.localmp3player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vince.localmp3player.data.AppPreferences
import com.vince.localmp3player.data.LibraryRepository
import com.vince.localmp3player.player.MusicPlayerController
import com.vince.localmp3player.player.DrumSoundEngine
import com.vince.localmp3player.player.PianoSoundEngine
import com.vince.localmp3player.player.PreviewAudioController
import com.vince.localmp3player.player.ShortAudioRecorder
import com.vince.localmp3player.player.SoundEffectPlayer
import com.vince.localmp3player.player.XylophoneSoundEngine
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
            blindTestPlayer = PreviewAudioController(applicationContext),
            pianoSoundEngine = PianoSoundEngine(applicationContext),
            drumSoundEngine = DrumSoundEngine(applicationContext),
            xylophoneSoundEngine = XylophoneSoundEngine(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )
        applyImmersiveMode()

        setContent {
            KidTunesTheme {
                KidTunesApp(viewModel = viewModel)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
