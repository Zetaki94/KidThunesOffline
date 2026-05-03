package com.vince.localmp3player.player

import android.content.ContentResolver
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Int = 0,
    val outputUri: String? = null,
)

class ShortAudioRecorder(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(RecordingUiState())
    val state: StateFlow<RecordingUiState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var outputUri: Uri? = null
    private var outputFileDescriptor: ParcelFileDescriptor? = null
    private var timerJob: kotlinx.coroutines.Job? = null

    suspend fun startRecording(parentFolderUri: String): Result<String> = withContext(Dispatchers.Main) {
        if (recorder != null) {
            return@withContext Result.failure(IllegalStateException("Un enregistrement est deja en cours."))
        }

        val outputDocumentUri = DocumentsContract.createDocument(
            resolver,
            Uri.parse(parentFolderUri),
            "audio/mp4",
            buildFileName(),
        ) ?: return@withContext Result.failure(IllegalStateException("Impossible de creer le fichier d'enregistrement."))

        val fileDescriptor = resolver.openFileDescriptor(outputDocumentUri, "w")
            ?: return@withContext Result.failure(IllegalStateException("Impossible d'ouvrir le fichier d'enregistrement."))

        val mediaRecorder = buildRecorder(fileDescriptor)
        runCatching {
            mediaRecorder.prepare()
            mediaRecorder.start()
        }.onFailure { throwable ->
            fileDescriptor.close()
            DocumentFile.fromSingleUri(appContext, outputDocumentUri)?.delete()
            return@withContext Result.failure(throwable)
        }

        recorder = mediaRecorder
        outputUri = outputDocumentUri
        outputFileDescriptor = fileDescriptor
        _state.value = RecordingUiState(
            isRecording = true,
            elapsedMs = 0,
            outputUri = outputDocumentUri.toString(),
        )
        startTimer()
        Result.success(outputDocumentUri.toString())
    }

    suspend fun stopRecording(): Result<String> = withContext(Dispatchers.Main) {
        val activeRecorder = recorder
            ?: return@withContext Result.failure(IllegalStateException("Aucun enregistrement en cours."))

        val finalOutputUri = outputUri
            ?: return@withContext Result.failure(IllegalStateException("Fichier de sortie introuvable."))

        timerJob?.cancel()

        val stopResult = runCatching {
            activeRecorder.stop()
        }

        cleanupRecorder()

        stopResult.fold(
            onSuccess = {
                Result.success(finalOutputUri.toString())
            },
            onFailure = { throwable ->
                DocumentFile.fromSingleUri(appContext, finalOutputUri)?.delete()
                Result.failure(throwable)
            },
        )
    }

    fun release() {
        timerJob?.cancel()
        cleanupRecorder()
        scope.cancel()
    }

    private fun buildRecorder(fileDescriptor: ParcelFileDescriptor): MediaRecorder {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setMaxDuration(MAX_RECORDING_MS)
            setOutputFile(fileDescriptor.fileDescriptor)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    scope.launch {
                        stopRecording()
                    }
                }
            }
        }

        return mediaRecorder
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var elapsed = 0
            while (isActive && recorder != null && elapsed <= MAX_RECORDING_MS) {
                _state.value = _state.value.copy(elapsedMs = elapsed)
                delay(100)
                elapsed += 100
            }
        }
    }

    private fun cleanupRecorder() {
        recorder?.release()
        recorder = null
        outputFileDescriptor?.close()
        outputFileDescriptor = null
        outputUri = null
        _state.value = RecordingUiState()
    }

    private fun buildFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "record_${formatter.format(Date())}.m4a"
    }

    companion object {
        const val MAX_RECORDING_MS = 10 * 60 * 1000
    }
}
