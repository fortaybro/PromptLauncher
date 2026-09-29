package com.forrest.titanlauncher.notes

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class NoteCategory {
    PERSONAL,
    WORK,
    IDEAS,
    JOURNAL
}

enum class NoteTextStyle {
    BOLD,
    ITALIC,
    UNDERLINE
}

data class NoteStyleRange(
    val start: Int,
    val end: Int,
    val style: NoteTextStyle
)

data class NoteItem(
    val id: Long,
    val title: String,
    val body: String,
    val category: NoteCategory,
    val styleRanges: List<NoteStyleRange>,
    val createdAt: Long,
    val updatedAt: Long
)

class NoteStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "titan_notes",
            Context.MODE_PRIVATE
        )

    private val _notes =
        MutableStateFlow(
            emptyList<NoteItem>()
        )

    /*
     * Emits after every write, so any screen observing it updates the
     * moment a note is added from the command bar, the editor, or
     * anywhere else. A single NoteStore is shared app-wide, so every
     * observer sees the same stream.
     */
    val notes: StateFlow<List<NoteItem>> =
        _notes.asStateFlow()

    init {
        _notes.value =
            getNotes()
    }

    fun addNote(
        body: String
    ): NoteItem {
        val cleaned = body.trim()
        val firstLine =
            cleaned
                .lineSequence()
                .firstOrNull()
                .orEmpty()
                .trim()

        val title =
            firstLine
                .ifBlank { "New note" }
                .take(42)

        return addNote(
            title = title,
            body = cleaned,
            category = NoteCategory.PERSONAL
        )
    }

    fun addNote(
        title: String,
        body: String,
        category: NoteCategory
    ): NoteItem {
        val now = System.currentTimeMillis()
        val note =
            NoteItem(
                id = now,
                title = title.trim().ifBlank { "New note" },
                body = body,
                category = category,
                styleRanges = emptyList(),
                createdAt = now,
                updatedAt = now
            )

        val notes =
            getNotes()
                .toMutableList()

        notes.add(note)
        saveNotes(notes)
        return note
    }

    fun createBlankNote(
        category: NoteCategory
    ): NoteItem {
        return addNote(
            title = "New note",
            body = "",
            category = category
        )
    }

    fun updateNote(
        note: NoteItem
    ): NoteItem {
        val sanitizedRanges =
            note.styleRanges
                .mapNotNull { range ->
                    val start = range.start.coerceIn(0, note.body.length)
                    val end = range.end.coerceIn(start, note.body.length)
                    if (end <= start) {
                        null
                    } else {
                        range.copy(
                            start = start,
                            end = end
                        )
                    }
                }

        val updated =
            note.copy(
                title = note.title.trimStart().take(80),
                styleRanges = sanitizedRanges,
                updatedAt = System.currentTimeMillis()
            )

        val notes =
            getNotes()
                .toMutableList()

        val index =
            notes.indexOfFirst {
                it.id == updated.id
            }

        if (index >= 0) {
            notes[index] = updated
        } else {
            notes.add(updated)
        }

        saveNotes(notes)
        return updated
    }

    fun getNotes(): List<NoteItem> {
        val raw =
            preferences.getString(
                "notes",
                "[]"
            )
                ?: "[]"

        return try {
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val item =
                        array.optJSONObject(index)
                            ?: continue

                    val body = item.optString("body")
                    val legacyTitle =
                        body
                            .lineSequence()
                            .firstOrNull()
                            .orEmpty()
                            .trim()
                            .take(42)

                    val title =
                        item.optString("title")
                            .ifBlank {
                                legacyTitle.ifBlank { "New note" }
                            }

                    val category =
                        runCatching {
                            NoteCategory.valueOf(
                                item.optString(
                                    "category",
                                    NoteCategory.PERSONAL.name
                                )
                            )
                        }
                            .getOrDefault(
                                NoteCategory.PERSONAL
                            )

                    val styleRanges =
                        buildList {
                            val ranges =
                                item.optJSONArray("styleRanges")
                                    ?: JSONArray()

                            for (rangeIndex in 0 until ranges.length()) {
                                val rangeObject =
                                    ranges.optJSONObject(rangeIndex)
                                        ?: continue

                                val style =
                                    runCatching {
                                        NoteTextStyle.valueOf(
                                            rangeObject.optString("style")
                                        )
                                    }
                                        .getOrNull()
                                        ?: continue

                                val start =
                                    rangeObject.optInt("start")
                                        .coerceIn(0, body.length)
                                val end =
                                    rangeObject.optInt("end")
                                        .coerceIn(start, body.length)

                                if (end > start) {
                                    add(
                                        NoteStyleRange(
                                            start = start,
                                            end = end,
                                            style = style
                                        )
                                    )
                                }
                            }
                        }

                    val createdAt =
                        item.optLong(
                            "createdAt",
                            item.optLong("id")
                        )

                    add(
                        NoteItem(
                            id = item.optLong("id"),
                            title = title,
                            body = body,
                            category = category,
                            styleRanges = styleRanges,
                            createdAt = createdAt,
                            updatedAt =
                                item.optLong(
                                    "updatedAt",
                                    createdAt
                                )
                        )
                    )
                }
            }
                .sortedByDescending {
                    it.updatedAt
                }

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteNote(
        noteId: Long
    ) {
        val updated =
            getNotes()
                .filterNot {
                    it.id == noteId
                }

        saveNotes(updated)
    }

    private fun saveNotes(
        notes: List<NoteItem>
    ) {
        val array = JSONArray()

        notes.forEach { note ->
            val ranges = JSONArray()

            note.styleRanges.forEach { range ->
                ranges.put(
                    JSONObject()
                        .apply {
                            put("start", range.start)
                            put("end", range.end)
                            put("style", range.style.name)
                        }
                )
            }

            array.put(
                JSONObject()
                    .apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("body", note.body)
                        put("category", note.category.name)
                        put("styleRanges", ranges)
                        put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt)
                    }
            )
        }

        preferences
            .edit()
            .putString(
                "notes",
                array.toString()
            )
            .apply()

        /*
         * Every mutation funnels through here, so this is the single
         * place the observable list needs refreshing.
         */
        _notes.value =
            notes
    }
}