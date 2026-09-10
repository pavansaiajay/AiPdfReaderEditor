package pavansaiajayx.aipdfreadereditor.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A pool to manage multiple PdfRenderer instances for concurrent page rendering.
 * Concurrency is bounded via Semaphore (max 4) and native resources are strictly
 * managed with lifecycle-bound cleanup per URI and safe error recovery.
 */
@Singleton
class PdfRendererPool @Inject constructor() {
    private val maxConcurrent = 4
    private val semaphore = Semaphore(maxConcurrent)
    private val renderers = mutableMapOf<String, MutableList<PdfRenderer>>()
    private val mutex = Mutex()

    suspend fun renderPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 400
    ): Bitmap? = semaphore.withPermit {
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            var renderer: PdfRenderer? = null
            var isRendererHealthy = true

            try {
                renderer = mutex.withLock {
                    val list = renderers.getOrPut(uriString) { mutableListOf() }
                    if (list.isNotEmpty()) {
                        list.removeAt(list.size - 1)
                    } else {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                            ?: throw IOException("Cannot open ParcelFileDescriptor for URI: $uri")
                        PdfRenderer(pfd)
                    }
                }

                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    return@withContext null
                }

                renderer.openPage(pageIndex).use { page ->
                    val width = targetWidth.coerceAtLeast(1)
                    val scale = width.toFloat() / page.width.toFloat()
                    val height = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            } catch (e: Exception) {
                isRendererHealthy = false
                try {
                    renderer?.close()
                } catch (_: Exception) {}
                null
            } finally {
                if (isRendererHealthy && renderer != null) {
                    mutex.withLock {
                        renderers[uriString]?.add(renderer)
                    }
                }
            }
        }
    }

    suspend fun closeUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            val listToClose = mutex.withLock {
                renderers.remove(uriString)
            }
            listToClose?.forEach { renderer ->
                try {
                    renderer.close()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun closeAll() {
        withContext(Dispatchers.IO) {
            val allRenderers = mutex.withLock {
                val copy = renderers.values.flatten()
                renderers.clear()
                copy
            }
            allRenderers.forEach { renderer ->
                try {
                    renderer.close()
                } catch (_: Exception) {}
            }
        }
    }
}
