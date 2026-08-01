package com.dimitriazzarone.metalab.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val question: String,
    val transcript: String,
    val detectedQuestions: List<String>,
    val finalNote: String
)

class SessionStore(context: Context) {
    private val storageFile = File(context.applicationContext.filesDir, FILE_NAME)

    @Synchronized
    fun load(): List<SessionRecord> {
        if (!storageFile.isFile) return emptyList()

        val content = storageFile.readText(Charsets.UTF_8)
        if (content.isBlank()) return emptyList()

        val array = JSONArray(content)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val questionsArray = item.optJSONArray(KEY_DETECTED_QUESTIONS) ?: JSONArray()

                val questions = buildList {
                    for (questionIndex in 0 until questionsArray.length()) {
                        add(questionsArray.getString(questionIndex))
                    }
                }

                add(
                    SessionRecord(
                        id = item.getString(KEY_ID),
                        createdAtEpochMillis = item.getLong(KEY_CREATED_AT),
                        question = item.optString(KEY_QUESTION),
                        transcript = item.optString(KEY_TRANSCRIPT),
                        detectedQuestions = questions,
                        finalNote = item.optString(KEY_FINAL_NOTE)
                    )
                )
            }
        }.sortedByDescending(SessionRecord::createdAtEpochMillis)
    }

    @Synchronized
    fun append(record: SessionRecord) {
        val updated = load().toMutableList().apply {
            removeAll { it.id == record.id }
            add(record)
        }
        write(updated)
    }

    @Synchronized
    fun delete(id: String) {
        write(load().filterNot { it.id == id })
    }

    private fun write(records: List<SessionRecord>) {
        val array = JSONArray()

        records.sortedBy(SessionRecord::createdAtEpochMillis).forEach { record ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, record.id)
                    put(KEY_CREATED_AT, record.createdAtEpochMillis)
                    put(KEY_QUESTION, record.question)
                    put(KEY_TRANSCRIPT, record.transcript)
                    put(KEY_DETECTED_QUESTIONS, JSONArray(record.detectedQuestions))
                    put(KEY_FINAL_NOTE, record.finalNote)
                }
            )
        }

        val temporaryFile = File(storageFile.parentFile, "$FILE_NAME.tmp")
        temporaryFile.writeText(array.toString(), Charsets.UTF_8)

        if (storageFile.exists() && !storageFile.delete()) {
            temporaryFile.delete()
            error("Impossibile sostituire l'archivio delle sessioni.")
        }

        if (!temporaryFile.renameTo(storageFile)) {
            temporaryFile.delete()
            error("Impossibile salvare l'archivio delle sessioni.")
        }
    }

    private companion object {
        const val FILE_NAME = "metalab_sessions.json"
        const val KEY_ID = "id"
        const val KEY_CREATED_AT = "createdAtEpochMillis"
        const val KEY_QUESTION = "question"
        const val KEY_TRANSCRIPT = "transcript"
        const val KEY_DETECTED_QUESTIONS = "detectedQuestions"
        const val KEY_FINAL_NOTE = "finalNote"
    }
}
