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
 * Durable dependency-free persistence for the manual library.
 *
 * Mutations are committed to disk before the in-memory StateFlow changes.
 * Failed writes therefore cannot be reported as successful state changes.
 */
class ManualRepository(private val context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val backupFile = File(context.filesDir, "$FILE_NAME.bak")
    private val mutex = Mutex()
    private val _manuals = MutableStateFlow<List<Manual>>(emptyList())
    private val _loaded = MutableStateFlow(false)

    val manuals: StateFlow<List<Manual>> = _manuals.asStateFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val libraryDir: File
        get() = File(context.filesDir, "library").apply { mkdirs() }

    suspend fun refresh() {
        val read = withContext(Dispatchers.IO) {
            runCatching { read(file) }.getOrElse { error ->
                if (backupFile.exists()) read(backupFile)
                else throw IllegalStateException("Manual library is corrupt and no backup is available", error)
            }
        }
        mutex.withLock {
            _manuals.value = read
            _loaded.value = true
        }
        cleanupOrphans()
    }

    suspend fun add(
        title: String,
        category: String,
        notes: String = "",
        vin: String = "",
        pages: List<ManualPage> = emptyList(),
    ): Manual {
        require(title.isNotBlank()) { "Manual title is required" }
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
        mutate { list ->
            require(list.any { it.id == manual.id }) { "Manual not found: ${manual.id}" }
            list.map { if (it.id == manual.id) manual.copy(updatedAt = System.currentTimeMillis()) else it }
        }

    suspend fun updateById(id: String, transform: (Manual) -> Manual) =
        mutate { list ->
            require(list.any { it.id == id }) { "Manual not found: $id" }
            list.map {
                if (it.id == id) transform(it.copy(updatedAt = System.currentTimeMillis())) else it
            }
        }

    suspend fun delete(id: String) {
        val removed = mutex.withLock {
            val existing = _manuals.value.firstOrNull { it.id == id }
            val next = _manuals.value.filterNot { it.id == id }
            write(next)
            _manuals.value = next
            existing
        }
        removed?.pages?.forEach { page ->
            val deleted = runCatching { File(page.uri).delete() }.getOrDefault(false)
            if (!deleted && File(page.uri).exists()) {
                throw IllegalStateException("Could not remove page file: ${File(page.uri).name}")
            }
        }
        cleanupOrphans()
    }

    suspend fun deleteAll() {
        mutex.withLock {
            write(emptyList())
            _manuals.value = emptyList()
        }
        cleanupOrphans()
    }

    suspend fun replaceAll(manuals: List<Manual>) = mutate { manuals }

    suspend fun cleanupOrphans() = withContext(Dispatchers.IO) {
        val referenced = _manuals.value.flatMap { it.pages }.map { File(it.uri).canonicalPath }.toSet()
        libraryDir.listFiles()?.forEach { file ->
            runCatching {
                if (file.isFile && file.canonicalPath !in referenced) file.delete()
            }
        }
    }

    private suspend fun mutate(block: (List<Manual>) -> List<Manual>) {
        mutex.withLock {
            val next = block(_manuals.value)
            write(next)
            _manuals.value = next
        }
        cleanupOrphans()
    }

    private fun read(source: File): List<Manual> {
        if (!source.exists()) return emptyList()
        return ManualCodec.decodeLibraryStrict(source.readText())
    }

    private fun write(manuals: List<Manual>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(ManualCodec.encodeLibrary(manuals))
        if (file.exists()) {
            if (backupFile.exists() && !backupFile.delete()) error("Could not replace library backup")
            if (!file.renameTo(backupFile)) error("Could not create library backup")
        }
        if (!tmp.renameTo(file)) {
            if (backupFile.exists() && !file.exists()) backupFile.renameTo(file)
            error("Could not commit manual library")
        }
        runCatching { backupFile.delete() }
    }

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
