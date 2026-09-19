package com.workshop.manualorganiser.util

import android.content.Context
import android.net.Uri
import com.workshop.manualorganiser.data.Manual
import com.workshop.manualorganiser.data.ManualCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Real export/import for a single manual.
 *
 * An export is a ZIP containing `manual.json` plus every page file, written
 * through the Storage Access Framework so the user chooses the destination.
 * Reading the same archive back restores the manual, which makes the export a
 * genuine backup rather than a dead-end dump.
 */
object Exporter {

    private const val MANUAL_ENTRY = "manual.json"
    private const val PAGES_DIR = "pages/"

    fun writeZip(manual: Manual, out: OutputStream) {
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(MANUAL_ENTRY))
            zip.write(ManualCodec.encode(manual).toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            manual.pages.forEach { page ->
                val source = File(page.uri)
                if (!source.exists()) return@forEach
                zip.putNextEntry(ZipEntry("$PAGES_DIR${page.id}__${source.name}"))
                source.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Exports to a user-chosen SAF destination. */
    suspend fun export(context: Context, manual: Manual, target: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(target)?.use { writeZip(manual, it) }
                    ?: error("Could not open the chosen destination")
                target.toString()
            }
        }

    /** Builds a shareable ZIP in the cache directory. */
    suspend fun buildShareArchive(context: Context, manual: Manual): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "${safeName(manual.title)}.zip")
                file.outputStream().use { writeZip(manual, it) }
                file
            }
        }

    /** Reads an archive produced by [writeZip] and restores it as a new manual. */
    suspend fun importArchive(context: Context, uri: Uri): Result<Manual> =
        withContext(Dispatchers.IO) {
            runCatching { readArchive(context, uri) }
        }

    private fun readArchive(context: Context, uri: Uri): Manual {
        val library = MediaImporter.libraryDir(context)
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Could not open the archive")

        return stream.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var manifest: JSONObject? = null
                val restored = mutableMapOf<String, File>()

                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == MANUAL_ENTRY) {
                        manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                    } else if (name.startsWith(PAGES_DIR) && !entry.isDirectory) {
                        val fileName = name.removePrefix(PAGES_DIR)
                        val pageId = fileName.substringBefore("__")
                        val dest = File(library, "restored_${UUID.randomUUID()}_${fileName.substringAfter("__")}")
                        dest.outputStream().use { output -> zip.copyTo(output) }
                        restored[pageId] = dest
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                val json = manifest ?: error("Not a Workshop Manual Organiser export")
                val decoded = ManualCodec.decode(json)
                val pages = decoded.pages.mapNotNull { page ->
                    restored[page.id]?.let { page.copy(id = UUID.randomUUID().toString(), uri = it.absolutePath) }
                }
                decoded.copy(
                    id = UUID.randomUUID().toString(),
                    pages = pages,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    fun safeName(title: String): String {
        val cleaned = title.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_')
        return cleaned.ifBlank { "manual" }
    }
}
