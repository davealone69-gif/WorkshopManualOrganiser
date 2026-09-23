package com.workshop.manualorganiser.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workshop.manualorganiser.data.Manual
import com.workshop.manualorganiser.data.ManualPage
import com.workshop.manualorganiser.data.ManualRepository
import com.workshop.manualorganiser.util.Exporter
import com.workshop.manualorganiser.util.MediaImporter
import com.workshop.manualorganiser.util.Taxonomy
import com.workshop.manualorganiser.util.VinDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Single view-model for the whole app. Every screen reads [manuals] and calls an
 * intent method; all persistence goes through [ManualRepository].
 */
class WorkshopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ManualRepository(application)

    val manuals: StateFlow<List<Manual>> = repository.manuals
    val loaded: StateFlow<Boolean> = repository.loaded

    private val _message = MutableStateFlow<String?>(null)

    /** Transient one-shot message for a snackbar. */
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun manual(id: String?): Manual? = manuals.value.firstOrNull { it.id == id }

    fun storageSummary(): String = repository.storageSummary()

    // ------------------------------------------------------------- importing

    /** Imports one picked image. Returns null (and posts a message) on failure. */
    suspend fun importImage(uri: Uri): ManualPage? = runCatching {
        MediaImporter.importImage(getApplication(), uri)
    }.getOrElse {
        _message.value = "Import failed: ${it.message ?: "unknown error"}"
        null
    }

    /** Imports a PDF and rasterises its pages. */
    suspend fun importPdf(uri: Uri): List<ManualPage> = runCatching {
        MediaImporter.importPdf(getApplication(), uri)
    }.getOrElse {
        _message.value = "Import failed: ${it.message ?: "unknown error"}"
        emptyList()
    }

    /** Imports a freshly captured photo. */
    suspend fun importCapture(file: File): ManualPage? = runCatching {
        val dest = File(MediaImporter.libraryDir(getApplication()), "capture_${UUID.randomUUID()}.jpg")
        file.inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
        ManualPage(id = UUID.randomUUID().toString(), uri = dest.absolutePath)
    }.getOrElse {
        _message.value = "Import failed: ${it.message ?: "unknown error"}"
        null
    }

    // ------------------------------------------------------------ mutations

    fun addManual(
        title: String,
        category: String,
        notes: String,
        vin: String,
        pages: List<ManualPage>,
        onCreated: (Manual) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { repository.add(title, category, notes, vin, pages) }.fold(
                onSuccess = { manual ->
                    _message.value = "Added “${manual.title}”."
                    onCreated(manual)
                },
                onFailure = { _message.value = "Could not save manual: ${it.message ?: "unknown error"}" },
            )
        }
    }

    fun updateManual(manual: Manual) {
        viewModelScope.launch {
            runCatching { repository.update(manual) }
                .onFailure { _message.value = "Could not save changes: ${it.message ?: "unknown error"}" }
                .onSuccess { _message.value = "Changes saved." }
        }
    }

    fun deleteManual(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { _message.value = "Delete failed: ${it.message ?: "unknown error"}" }
                .onSuccess { _message.value = "Manual deleted." }
        }
    }

    fun appendPages(manualId: String, pages: List<ManualPage>) {
        if (pages.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.updateById(manualId) { it.copy(pages = it.pages + pages) }
            }.fold(
                onSuccess = { _message.value = "Added ${pages.size} page(s)." },
                onFailure = { _message.value = "Could not add pages: ${it.message ?: "unknown error"}" },
            )
        }
    }

    fun removePage(manualId: String, pageId: String) {
        viewModelScope.launch {
            repository.updateById(manualId) { manual ->
                manual.copy(pages = manual.pages.filterNot { it.id == pageId })
            }
        }
    }

    fun movePage(manualId: String, from: Int, to: Int) {
        viewModelScope.launch {
            repository.updateById(manualId) { manual ->
                val pages = manual.pages.toMutableList()
                if (from in pages.indices && to in pages.indices) {
                    pages.add(to, pages.removeAt(from))
                }
                manual.copy(pages = pages)
            }
        }
    }

    fun updatePageLabel(manualId: String, pageId: String, label: String) {
        viewModelScope.launch {
            repository.updateById(manualId) { manual ->
                manual.copy(pages = manual.pages.map { if (it.id == pageId) it.copy(label = label) else it })
            }
        }
    }

    /** Applies a taxonomy suggestion: category plus page ordering. */
    fun applySuggestion(manualId: String, suggestion: Taxonomy.Suggestion) {
        viewModelScope.launch {
            repository.updateById(manualId) { manual ->
                manual.copy(
                    category = suggestion.category,
                    pages = Taxonomy.orderPages(manual, suggestion.sections),
                )
            }
            _message.value = "Applied “${suggestion.category}”."
        }
    }

    // -------------------------------------------------------------- VIN

    fun decodeVin(input: String): VinDecoder.Result = VinDecoder.decode(input)

    fun attachVin(manualId: String, vin: String) {
        viewModelScope.launch {
            repository.updateById(manualId) { it.copy(vin = vin.uppercase()) }
            _message.value = "VIN attached."
        }
    }

    // ----------------------------------------------------------- export

    fun exportManual(manual: Manual, target: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = Exporter.export(getApplication(), manual, target)
            _busy.value = false
            _message.value = result.fold(
                onSuccess = { "Exported “${manual.title}”." },
                onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
            )
        }
    }

    fun shareArchive(manual: Manual, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val result = Exporter.buildShareArchive(getApplication(), manual)
            _busy.value = false
            result.fold(
                onSuccess = onReady,
                onFailure = { _message.value = "Export failed: ${it.message ?: "unknown error"}" },
            )
        }
    }

    fun importArchive(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val result = Exporter.importArchive(getApplication(), uri)
            _busy.value = false
            result.fold(
                onSuccess = { manual ->
                    repository.replaceAll(repository.manuals.value + manual)
                    _message.value = "Restored “${manual.title}”."
                },
                onFailure = { _message.value = "Restore failed: ${it.message ?: "unknown error"}" },
            )
        }
    }
}
