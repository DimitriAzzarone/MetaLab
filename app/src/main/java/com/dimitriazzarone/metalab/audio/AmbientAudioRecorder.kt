package com.dimitriazzarone.metalab.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.util.UUID

class AmbientAudioRecorder(context: Context) {

    private val audioDirectory =
        File(context.applicationContext.filesDir, AUDIO_DIRECTORY_NAME)

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(): String {
        check(!isRecording) { "Una registrazione ambientale è già attiva." }

        if (!audioDirectory.exists() && !audioDirectory.mkdirs()) {
            error("Impossibile creare la cartella delle registrazioni.")
        }

        val outputFile = File(
            audioDirectory,
            "ambient_${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a"
        )

        val newRecorder = MediaRecorder()

        try {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(128_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setOutputFile(outputFile.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
        } catch (error: Exception) {
            runCatching { newRecorder.release() }
            outputFile.delete()
            throw error
        }

        recorder = newRecorder
        currentFile = outputFile

        return outputFile.name
    }

    fun stop(): String {
        val activeRecorder =
            recorder ?: error("Nessuna registrazione ambientale attiva.")
        val outputFile =
            currentFile ?: error("File della registrazione non disponibile.")

        try {
            activeRecorder.stop()
        } catch (error: RuntimeException) {
            outputFile.delete()
            throw error
        } finally {
            runCatching { activeRecorder.release() }
            recorder = null
            currentFile = null
        }

        return outputFile.name
    }

    fun cancel() {
        val activeRecorder = recorder
        val outputFile = currentFile

        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.release() }
        }

        outputFile?.delete()
        recorder = null
        currentFile = null
    }

    private companion object {
        const val AUDIO_DIRECTORY_NAME = "ambient_audio"
    }
}
