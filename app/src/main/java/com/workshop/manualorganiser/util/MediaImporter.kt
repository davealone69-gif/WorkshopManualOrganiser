package com.workshop.manualorganiser.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.workshop.manualorganiser.data.ManualPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Turns picked or captured content into page files inside app-private storage.
 *
 * Both the camera capture and the system Photo Picker / document picker hand back
 * content URIs whose read permission is temporary, so every page is copied into
 * `filesDir/library/` immediately. That is also why the app needs no storage
 * permission at all.
 *
 * Imported PDFs are rasterised with the platform [PdfRenderer] so pages display
 * without any third-party PDF dependency.
 */
object MediaImporter {

    private const val MAX_PDF_PAGES = 60
    private const val MAX_PAGE_EDGE_PX = 1800

    fun libraryDir(context: Context): File = File(context.filesDir, "library").apply { mkdirs() }

    fun captureDir(context: Context): File = File(context.filesDir, "captures").apply { mkdirs() }

    /** Destination file for an in-progress camera capture. */
    fun newCaptureFile(context: Context): File =
        File(captureDir(context), "capture_${System.currentTimeMillis()}.jpg")

    suspend fun importImage(context: Context, uri: Uri): ManualPage = withContext(Dispatchers.IO) {
        val dest = File(libraryDir(context), "img_${UUID.randomUUID()}.${extensionFor(context, uri)}")
        copyUri(context, uri, dest)
        ManualPage(id = UUID.randomUUID().toString(), uri = dest.absolutePath, kind = ManualPage.KIND_IMAGE)
    }

    suspend fun importImages(context: Context, uris: List<Uri>): List<ManualPage> =
        uris.mapNotNull { uri -> runCatching { importImage(context, uri) }.getOrNull() }

    suspend fun importPdf(context: Context, uri: Uri): List<ManualPage> = withContext(Dispatchers.IO) {
        val dest = File(libraryDir(context), "pdf_${UUID.randomUUID()}.pdf")
        copyUri(context, uri, dest)
        renderPdf(context, dest)
    }

    /** Rasterises every page of [pdf] into PNG page files. */
    fun renderPdf(context: Context, pdf: File): List<ManualPage> {
        val pages = mutableListOf<ManualPage>()
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val count = minOf(renderer.pageCount, MAX_PDF_PAGES)
                for (index in 0 until count) {
                    renderer.openPage(index).use { page ->
                        val scale = scaleFor(page.width, page.height)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val out = File(libraryDir(context), "pdfpage_${UUID.randomUUID()}.png")
                        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
                        bitmap.recycle()
                        pages += ManualPage(
                            id = UUID.randomUUID().toString(),
                            uri = out.absolutePath,
                            kind = ManualPage.KIND_IMAGE,
                        )
                    }
                }
            }
        }
        return pages
    }

    private fun scaleFor(widthPx: Int, heightPx: Int): Float {
        val largest = maxOf(widthPx, heightPx)
        return if (largest <= MAX_PAGE_EDGE_PX) 1f else MAX_PAGE_EDGE_PX.toFloat() / largest
    }

    private fun copyUri(context: Context, uri: Uri, dest: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open ${uri.lastPathSegment ?: uri}")
    }

    private fun extensionFor(context: Context, uri: Uri): String {
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            mime.contains("bmp") -> "bmp"
            else -> "jpg"
        }
    }
}
