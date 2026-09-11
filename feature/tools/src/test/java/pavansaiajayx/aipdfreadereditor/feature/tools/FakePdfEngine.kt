package pavansaiajayx.aipdfreadereditor.feature.tools

import android.net.Uri
import pavansaiajayx.aipdfreadereditor.core.pdf.edit.PdfEdit
import pavansaiajayx.aipdfreadereditor.core.pdf.engine.PdfEngine
import java.io.File

internal class FakePdfEngine(private val tempDir: File) : PdfEngine {

    override val cacheDir: File get() = tempDir

    var mergeCalled = false
    var splitCalled = false
    var compressCalled = false
    var encryptCalled = false
    var decryptCalled = false
    var watermarkCalled = false
    var rotateCalled = false
    var imagesToPdfCalled = false
    var pdfToImagesCalled = false
    var extractTextCalled = false
    var shouldFail = false

    override suspend fun mergePdfUris(
        uriStrings: List<String>,
        outputCacheFile: File,
        onProgress: ((Int) -> Unit)?
    ): Result<File> {
        mergeCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Merge failed"))
        outputCacheFile.writeText("merged content")
        onProgress?.invoke(100)
        return Result.success(outputCacheFile)
    }

    override suspend fun splitPdfUri(uriString: String, outputCacheFolder: File): Result<List<File>> {
        splitCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Split failed"))
        outputCacheFolder.mkdirs()
        val file1 = File(outputCacheFolder, "page_1.pdf").apply { writeText("p1") }
        val file2 = File(outputCacheFolder, "page_2.pdf").apply { writeText("p2") }
        return Result.success(listOf(file1, file2))
    }

    override suspend fun compressPdfUri(uriString: String, outputCacheFile: File): Result<File> {
        compressCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Compress failed"))
        outputCacheFile.writeText("compressed content")
        return Result.success(outputCacheFile)
    }

    override suspend fun encryptPdfUri(uriString: String, password: String, outputCacheFile: File): Result<File> {
        encryptCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Encrypt failed"))
        outputCacheFile.writeText("encrypted content")
        return Result.success(outputCacheFile)
    }

    override suspend fun decryptPdfUri(uriString: String, password: String, outputCacheFile: File): Result<File> {
        decryptCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Decrypt failed"))
        outputCacheFile.writeText("decrypted content")
        return Result.success(outputCacheFile)
    }

    override suspend fun imagesToPdfUris(uriStrings: List<String>, outputCacheFile: File): Result<File> {
        imagesToPdfCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Images to PDF failed"))
        outputCacheFile.writeText("images pdf content")
        return Result.success(outputCacheFile)
    }

    override suspend fun pdfToImagesUri(uriString: String, outputCacheFolder: File): Result<List<File>> {
        pdfToImagesCalled = true
        if (shouldFail) return Result.failure(RuntimeException("PDF to Images failed"))
        outputCacheFolder.mkdirs()
        val file1 = File(outputCacheFolder, "page_1.jpg").apply { writeText("img1") }
        return Result.success(listOf(file1))
    }

    override suspend fun addWatermarkUri(uriString: String, watermarkText: String, outputCacheFile: File): Result<File> {
        watermarkCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Watermark failed"))
        outputCacheFile.writeText("watermarked content")
        return Result.success(outputCacheFile)
    }

    override suspend fun rotatePagesUri(uriString: String, rotationDegrees: Int, outputCacheFile: File): Result<File> {
        rotateCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Rotate failed"))
        outputCacheFile.writeText("rotated content")
        return Result.success(outputCacheFile)
    }

    override suspend fun extractTextUri(uriString: String, outputCacheFile: File): Result<File> {
        extractTextCalled = true
        if (shouldFail) return Result.failure(RuntimeException("Extract text failed"))
        outputCacheFile.writeText("sample extracted text")
        return Result.success(outputCacheFile)
    }

    override suspend fun mergePdfs(uris: List<Uri>, outputCacheFile: File, onProgress: ((Int) -> Unit)?): Result<File> {
        return mergePdfUris(emptyList(), outputCacheFile, onProgress)
    }

    override suspend fun splitPdf(uri: Uri, outputCacheFolder: File): Result<List<File>> {
        return splitPdfUri("", outputCacheFolder)
    }

    override suspend fun compressPdf(uri: Uri, outputCacheFile: File): Result<File> {
        return compressPdfUri("", outputCacheFile)
    }

    override suspend fun encryptPdf(uri: Uri, password: String, outputCacheFile: File): Result<File> {
        return encryptPdfUri("", password, outputCacheFile)
    }

    override suspend fun decryptPdf(uri: Uri, password: String, outputCacheFile: File): Result<File> {
        return decryptPdfUri("", password, outputCacheFile)
    }

    override suspend fun imagesToPdf(uris: List<Uri>, outputCacheFile: File): Result<File> {
        return imagesToPdfUris(emptyList(), outputCacheFile)
    }

    override suspend fun pdfToImages(uri: Uri, outputCacheFolder: File): Result<List<File>> {
        return pdfToImagesUri("", outputCacheFolder)
    }

    override suspend fun addWatermark(uri: Uri, watermarkText: String, outputCacheFile: File): Result<File> {
        return addWatermarkUri("", watermarkText, outputCacheFile)
    }

    override suspend fun rotatePages(uri: Uri, rotationDegrees: Int, outputCacheFile: File): Result<File> {
        return rotatePagesUri("", rotationDegrees, outputCacheFile)
    }

    override suspend fun extractText(uri: Uri, outputCacheFile: File): Result<File> {
        return extractTextUri("", outputCacheFile)
    }

    override suspend fun extractTextToString(uri: Uri): Result<String> = Result.success("sample")
    override suspend fun copyToUri(sourceFile: File, destinationUri: Uri): Result<Unit> = Result.success(Unit)
    override suspend fun copyToFolder(sourceFiles: List<File>, folderUri: Uri): Result<List<Pair<String, Uri>>> = Result.success(emptyList())
    override suspend fun deletePages(uri: Uri, pagesToRemove: List<Int>, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
    override suspend fun reorderPages(uri: Uri, newOrder: List<Int>, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
    override suspend fun extractSinglePage(uri: Uri, pageNumber: Int, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
    override suspend fun flattenPdf(uri: Uri, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
    override suspend fun searchInPdf(uri: Uri, query: String): Result<List<Int>> = Result.success(emptyList())
    override suspend fun getPageCount(uri: Uri): Result<Int> = Result.success(1)
    override suspend fun splitSelectedPages(uri: Uri, selectedPages: List<Int>, outputCacheFolder: File): Result<List<File>> = Result.success(emptyList())
    override suspend fun extractPages(uri: Uri, pages: List<Int>, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
    override suspend fun applyAnnotations(uri: Uri, edits: List<PdfEdit>, pageIndex: Int, outputCacheFile: File): Result<File> = Result.success(outputCacheFile)
}
