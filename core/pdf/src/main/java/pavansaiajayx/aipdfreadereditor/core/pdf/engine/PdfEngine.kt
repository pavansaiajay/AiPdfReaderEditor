package pavansaiajayx.aipdfreadereditor.core.pdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PathMeasure
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSFloat
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationUnknown
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pavansaiajayx.aipdfreadereditor.core.pdf.edit.PdfEdit
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production PDF manipulation engine powered by PDFBox Android.
 * Invariant: Offline operations are strictly 100% free with zero CreditManager coupling.
 */
@Singleton
class PdfEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val cacheDir: File get() = context.cacheDir

    suspend fun mergePdfs(
        uris: List<Uri>,
        outputCacheFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            val merger = PDFMergerUtility()
            val inputStreams = mutableListOf<InputStream>()
            try {
                val total = uris.size
                for ((index, uri) in uris.withIndex()) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
                    inputStreams.add(inputStream)
                    merger.addSource(inputStream)
                    onProgress?.invoke(((index + 1) * 50) / total)
                }
                outputCacheFile.outputStream().use { outputStream ->
                    merger.destinationStream = outputStream
                    merger.mergeDocuments(null)
                }
                onProgress?.invoke(100)
            } finally {
                inputStreams.forEach { stream ->
                    try {
                        stream.close()
                    } catch (_: Exception) {}
                }
            }
            outputCacheFile
        }
    }

    suspend fun splitPdf(uri: Uri, outputCacheFolder: File): Result<List<File>> = runCatching {
        withContext(Dispatchers.IO) {
            outputCacheFolder.mkdirs()
            val splitFiles = mutableListOf<File>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val splitter = Splitter()
                    val splitDocs = splitter.split(document)
                    splitDocs.forEachIndexed { index, splitDoc ->
                        splitDoc.use { doc ->
                            val file = File(outputCacheFolder, "page_${index + 1}.pdf")
                            doc.save(file)
                            splitFiles.add(file)
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            splitFiles
        }
    }

    suspend fun compressPdf(uri: Uri, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    for (page in document.pages) {
                        val resources = page.resources ?: continue
                        for (name in resources.xObjectNames) {
                            val xObject = resources.getXObject(name)
                            if (xObject is PDImageXObject) {
                                val originalBitmap = xObject.image
                                val scaledWidth = (originalBitmap.width * 0.5f).toInt().coerceAtLeast(1)
                                val scaledHeight = (originalBitmap.height * 0.5f).toInt().coerceAtLeast(1)
                                val scaledBitmap = Bitmap.createScaledBitmap(
                                    originalBitmap, scaledWidth, scaledHeight, true
                                )
                                originalBitmap.recycle()

                                val compressedImage = JPEGFactory.createFromImage(document, scaledBitmap, 0.5f)
                                scaledBitmap.recycle()

                                resources.put(name, compressedImage)
                            }
                        }
                    }
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun copyToUri(sourceFile: File, destinationUri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Unable to save file to URI: $destinationUri")
        }
    }

    suspend fun copyToFolder(sourceFiles: List<File>, folderUri: Uri): Result<List<Pair<String, Uri>>> = runCatching {
        withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IllegalStateException("Unable to access folder at URI: $folderUri")
            val copiedFiles = mutableListOf<Pair<String, Uri>>()
            for (file in sourceFiles) {
                val mimeType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "txt" -> "text/plain"
                    else -> "application/pdf"
                }
                val newDoc = folder.createFile(mimeType, file.nameWithoutExtension)
                    ?: throw IllegalStateException("Unable to create file in folder")
                context.contentResolver.openOutputStream(newDoc.uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                copiedFiles.add(Pair(newDoc.name ?: file.name, newDoc.uri))
            }
            copiedFiles
        }
    }

    suspend fun encryptPdf(uri: Uri, password: String, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val ap = AccessPermission()
                    val spp = StandardProtectionPolicy(password, password, ap)
                    spp.encryptionKeyLength = 128
                    document.protect(spp)
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun decryptPdf(uri: Uri, password: String, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream, password).use { document ->
                    document.setAllSecurityToBeRemoved(true)
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int = 2048, reqHeight: Int = 2048): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
            while ((width / inSampleSize) > reqWidth || (height / inSampleSize) > reqHeight) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    suspend fun imagesToPdf(uris: List<Uri>, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            PDDocument().use { document ->
                for (uri in uris) {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }

                    val sampleSize = calculateInSampleSize(boundsOptions, 2048, 2048)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: continue

                    try {
                        val page = PDPage(PDRectangle.A4)
                        document.addPage(page)

                        val pdImage = JPEGFactory.createFromImage(document, bitmap)

                        val pageWidth = page.mediaBox.width
                        val pageHeight = page.mediaBox.height
                        val imageWidth = pdImage.width.toFloat()
                        val imageHeight = pdImage.height.toFloat()

                        val scale = minOf(pageWidth / imageWidth, pageHeight / imageHeight)
                        val scaledWidth = imageWidth * scale
                        val scaledHeight = imageHeight * scale

                        val startX = (pageWidth - scaledWidth) / 2
                        val startY = (pageHeight - scaledHeight) / 2

                        PDPageContentStream(document, page).use { contentStream ->
                            contentStream.drawImage(pdImage, startX, startY, scaledWidth, scaledHeight)
                        }
                    } finally {
                        try {
                            if (!bitmap.isRecycled) {
                                bitmap.recycle()
                            }
                        } catch (_: Exception) {}
                    }
                }
                document.save(outputCacheFile)
            }
            outputCacheFile
        }
    }

    suspend fun pdfToImages(uri: Uri, outputCacheFolder: File): Result<List<File>> = runCatching {
        withContext(Dispatchers.IO) {
            outputCacheFolder.mkdirs()
            val outputFiles = mutableListOf<File>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val renderer = PDFRenderer(document)
                    for (i in 0 until document.numberOfPages) {
                        val bitmap = renderer.renderImageWithDPI(i, 150f)
                        val outputFile = File(outputCacheFolder, "page_${i + 1}.jpg")
                        outputFile.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }
                        outputFiles.add(outputFile)
                        bitmap.recycle()
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")

            outputFiles
        }
    }

    suspend fun addWatermark(uri: Uri, watermarkText: String, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    for (page in document.pages) {
                        PDPageContentStream(document, page, AppendMode.APPEND, true, true).use { contentStream ->
                            val font = PDType1Font.HELVETICA_BOLD
                            val fontSize = 60f
                            contentStream.setFont(font, fontSize)
                            contentStream.setNonStrokingColor(200, 200, 200)

                            val textWidth = font.getStringWidth(watermarkText) / 1000 * fontSize
                            val textHeight = font.fontDescriptor.fontBoundingBox.height / 1000 * fontSize

                            val cx = page.mediaBox.width / 2f
                            val cy = page.mediaBox.height / 2f

                            val matrix = Matrix.getRotateInstance(Math.toRadians(45.0), cx, cy)
                            contentStream.transform(matrix)

                            contentStream.beginText()
                            contentStream.newLineAtOffset(-textWidth / 2f, -textHeight / 2f)
                            contentStream.showText(watermarkText)
                            contentStream.endText()
                        }
                    }
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun extractText(uri: Uri, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(document)
                    outputCacheFile.writeText(text)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun extractTextToString(uri: Uri): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.getText(document)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
        }
    }

    suspend fun deletePages(uri: Uri, pagesToRemove: List<Int>, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val totalPages = document.numberOfPages
                    val validPagesToRemove = pagesToRemove
                        .filter { it in 0 until totalPages }
                        .sortedDescending()

                    for (index in validPagesToRemove) {
                        document.removePage(index)
                    }
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun rotatePages(uri: Uri, rotationDegrees: Int, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    for (page in document.pages) {
                        page.rotation = (page.rotation + rotationDegrees) % 360
                    }
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun reorderPages(uri: Uri, newOrder: List<Int>, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { oldDoc ->
                    PDDocument().use { newDoc ->
                        for (index in newOrder) {
                            if (index in 0 until oldDoc.numberOfPages) {
                                newDoc.importPage(oldDoc.getPage(index))
                            }
                        }
                        newDoc.save(outputCacheFile)
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun extractSinglePage(uri: Uri, pageNumber: Int, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { oldDoc ->
                    PDDocument().use { newDoc ->
                        val zeroBasedIndex = pageNumber - 1
                        if (zeroBasedIndex in 0 until oldDoc.numberOfPages) {
                            newDoc.importPage(oldDoc.getPage(zeroBasedIndex))
                            newDoc.save(outputCacheFile)
                        } else {
                            throw IllegalArgumentException("Invalid page number: $pageNumber")
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun flattenPdf(uri: Uri, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    document.documentCatalog.acroForm?.flatten()
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }

    suspend fun searchInPdf(uri: Uri, query: String): Result<List<Int>> = runCatching {
        withContext(Dispatchers.IO) {
            val matchedPages = mutableListOf<Int>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    for (pageNumber in 1..document.numberOfPages) {
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        val pageText = stripper.getText(document)
                        if (pageText.contains(query, ignoreCase = true)) {
                            matchedPages.add(pageNumber)
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            matchedPages
        }
    }

    suspend fun getPageCount(uri: Uri): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            } ?: throw IllegalStateException("Unable to open PDF for page count: $uri")
        }
    }

    suspend fun splitSelectedPages(uri: Uri, selectedPages: List<Int>, outputCacheFolder: File): Result<List<File>> = runCatching {
        withContext(Dispatchers.IO) {
            outputCacheFolder.mkdirs()
            val splitFiles = mutableListOf<File>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    for (index in selectedPages) {
                        if (index in 0 until document.numberOfPages) {
                            PDDocument().use { singlePageDoc ->
                                singlePageDoc.importPage(document.getPage(index))
                                val file = File(outputCacheFolder, "page_${index + 1}.pdf")
                                singlePageDoc.save(file)
                                splitFiles.add(file)
                            }
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            splitFiles
        }
    }

    suspend fun extractPages(uri: Uri, pages: List<Int>, outputCacheFile: File): Result<File> =
        reorderPages(uri, pages, outputCacheFile)

    suspend fun applyAnnotations(uri: Uri, edits: List<PdfEdit>, pageIndex: Int, outputCacheFile: File): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val page = document.getPage(pageIndex)
                    val pageHeight = page.mediaBox.height

                    for (edit in edits) {
                        when (edit) {
                            is PdfEdit.Draw -> {
                                val annotationDict = COSDictionary()
                                annotationDict.setName(COSName.TYPE, "Annot")
                                annotationDict.setName(COSName.SUBTYPE, "Ink")

                                val pathMeasure = PathMeasure(edit.path, false)
                                val coords = mutableListOf<Float>()
                                val pos = FloatArray(2)
                                var distance = 0f
                                while (distance < pathMeasure.length) {
                                    pathMeasure.getPosTan(distance, pos, null)
                                    coords.add(pos[0])
                                    coords.add(pageHeight - pos[1])
                                    distance += 5f
                                }
                                val pathsArray = COSArray()
                                val pointArray = COSArray()
                                coords.forEach { pointArray.add(COSFloat(it)) }
                                pathsArray.add(pointArray)
                                annotationDict.setItem(COSName.getPDFName("InkList"), pathsArray)

                                val colorArray = COSArray()
                                colorArray.add(COSFloat(Color.red(edit.color) / 255f))
                                colorArray.add(COSFloat(Color.green(edit.color) / 255f))
                                colorArray.add(COSFloat(Color.blue(edit.color) / 255f))
                                annotationDict.setItem(COSName.C, colorArray)

                                val bsDict = COSDictionary()
                                bsDict.setFloat(COSName.W, edit.strokeWidth)
                                annotationDict.setItem(COSName.BS, bsDict)

                                val rect = PDRectangle()
                                var minX = Float.MAX_VALUE
                                var minY = Float.MAX_VALUE
                                var maxX = Float.MIN_VALUE
                                var maxY = Float.MIN_VALUE
                                for (i in 0 until coords.size step 2) {
                                    if (coords[i] < minX) minX = coords[i]
                                    if (coords[i + 1] < minY) minY = coords[i + 1]
                                    if (coords[i] > maxX) maxX = coords[i]
                                    if (coords[i + 1] > maxY) maxY = coords[i + 1]
                                }
                                if (coords.isEmpty()) {
                                    minX = 0f; minY = 0f; maxX = 10f; maxY = 10f
                                }
                                rect.lowerLeftX = minX - edit.strokeWidth
                                rect.lowerLeftY = minY - edit.strokeWidth
                                rect.upperRightX = maxX + edit.strokeWidth
                                rect.upperRightY = maxY + edit.strokeWidth
                                annotationDict.setItem(COSName.RECT, rect.cosArray)

                                val annotation = PDAnnotationUnknown(annotationDict)
                                page.annotations.add(annotation)
                            }
                            is PdfEdit.Highlight -> {
                                val annotation = PDAnnotationTextMarkup(
                                    PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
                                )
                                val rect = PDRectangle(
                                    edit.rect.left,
                                    pageHeight - edit.rect.bottom,
                                    edit.rect.width(),
                                    edit.rect.height()
                                )
                                annotation.rectangle = rect
                                val color = PDColor(
                                    floatArrayOf(
                                        Color.red(edit.color) / 255f,
                                        Color.green(edit.color) / 255f,
                                        Color.blue(edit.color) / 255f
                                    ),
                                    PDDeviceRGB.INSTANCE
                                )
                                annotation.color = color

                                val blX = edit.rect.left
                                val blY = pageHeight - edit.rect.bottom
                                val trX = edit.rect.right
                                val trY = pageHeight - edit.rect.top
                                annotation.quadPoints = floatArrayOf(
                                    blX, blY, trX, blY, blX, trY, trX, trY
                                )

                                page.annotations.add(annotation)
                            }
                            is PdfEdit.Text -> {
                                PDPageContentStream(
                                    document, page, AppendMode.APPEND, true, true
                                ).use { stream ->
                                    stream.beginText()
                                    stream.setFont(PDType1Font.HELVETICA, edit.size)
                                    stream.setNonStrokingColor(
                                        Color.red(edit.color),
                                        Color.green(edit.color),
                                        Color.blue(edit.color)
                                    )
                                    stream.newLineAtOffset(edit.x, pageHeight - edit.y)
                                    stream.showText(edit.text)
                                    stream.endText()
                                }
                            }
                        }
                    }
                    document.save(outputCacheFile)
                }
            } ?: throw IllegalStateException("Unable to open PDF at URI: $uri")
            outputCacheFile
        }
    }
}
