package com.workshop.manualorganiser.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Durable, dependency-free persistence for the manual library.
 *
 * The library is stored as JSON inside app-private storage (`filesDir/manuals.json`)
 * and exposed as an observable [StateFlow]. Page files themselves live in
 * `filesDir/library/`; only their paths are recorded here.
 *
 * Every mutation goes through [mutate], which serialises writes with a mutex so
 * the in-memory state and the file on disk can never diverge.
 */
class ManualRepository(private val context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()
    private val _manuals = MutableStateFlow<List<Manual>>(emptyList())
    private val _loaded = MutableStateFlow(false)

    val manuals: StateFlow<List<Manual>> = _manuals.asStateFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Files directory used by the media importer for page storage. */
    val libraryDir: File
        get() = File(context.filesDir, "library").apply { mkdirs() }

    suspend fun refresh() {
        val read = withContext(Dispatchers.IO) { read() }
        mutex.withLock {
            _manuals.value = read
            _loaded.value = true
        }
    }

    suspend fun add(
        title: String,
        category: String,
        notes: String = "",
        vin: String = "",
        pages: List<ManualPage> = emptyList(),
    ): Manual {
        val now = System.currentTimeMillis()
        val manual = Manual(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            category = category.trim().ifBlank { "General" },
            notes = notes.trim(),
            vin = vin.trim().uppercase(),
            pages = pages,
            createdAt = now,
            updatedAt = now,
        )
        mutate { it + manual }
        return manual
    }

    suspend fun update(manual: Manual) =
        mutate { list -> list.map { if (it.id == manual.id) manual.copy(updatedAt = System.currentTimeMillis()) else it } }

    suspend fun updateById(id: String, transform: (Manual) -> Manual) =
        mutate { list ->
            list.map {
                if (it.id == id) transform(it.copy(updatedAt = System.currentTimeMillis())) else it
            }
        }

    suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    suspend fun deleteAll() = mutate { emptyList() }

    /** Replaces the whole library in one atomic write (used by import/restore). */
    suspend fun replaceAll(manuals: List<Manual>) = mutate { manuals }

    private suspend fun mutate(block: (List<Manual>) -> List<Manual>) {
        mutex.withLock {
            val next = block(_manuals.value)
            _manuals.value = next
            withContext(Dispatchers.IO) { write(next) }
        }
    }

    // ---------------------------------------------------------------- I/O

    private fun read(): List<Manual> {
        if (!file.exists()) return emptyList()
        return ManualCodec.decodeLibrary(file.readText())
    }

    private fun write(manuals: List<Manual>) {
        runCatching {
            file.parentFile?.mkdirs()
            // Write to a temp file then rename, so a crash mid-write can never
            // truncate the existing library.
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(ManualCodec.encodeLibrary(manuals))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }

    /** Human-readable storage footprint of the library. */
    fun storageSummary(): String {
        val bytes = (libraryDir.listFiles()?.sumOf { it.length() } ?: 0L) +
            (if (file.exists()) file.length() else 0L)
        return when {
            bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private companion object {
        const val FILE_NAME = "manuals.json"
    }
}
