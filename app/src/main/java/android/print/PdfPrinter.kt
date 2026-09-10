package android.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.File

object PdfPrinter {
    fun print(
        adapter: PrintDocumentAdapter,
        path: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        adapter.onLayout(null, attributes, CancellationSignal(), object : PrintDocumentAdapter.LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                try {
                    val descriptor = ParcelFileDescriptor.open(
                        path, 
                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                    )
                    adapter.onWrite(
                        arrayOf(PageRange.ALL_PAGES), 
                        descriptor, 
                        CancellationSignal(), 
                        object : PrintDocumentAdapter.WriteResultCallback() {
                            override fun onWriteFinished(pages: Array<out PageRange>?) {
                                super.onWriteFinished(pages)
                                try { descriptor.close() } catch (e: Exception) {}
                                onSuccess()
                            }

                            override fun onWriteFailed(error: CharSequence?) {
                                super.onWriteFailed(error)
                                try { descriptor.close() } catch (e: Exception) {}
                                onError(error?.toString() ?: "Write failed")
                            }
                        }
                    )
                } catch (e: Exception) {
                    onError(e.message ?: "Unknown error")
                }
            }

            override fun onLayoutFailed(error: CharSequence?) {
                super.onLayoutFailed(error)
                onError(error?.toString() ?: "Layout failed")
            }
        }, null)
    }
}
