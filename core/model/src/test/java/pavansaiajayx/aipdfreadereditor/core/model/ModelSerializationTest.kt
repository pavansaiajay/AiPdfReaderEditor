package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSerializationTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun documentSerializationRoundTrip() {
        val doc = Document(
            id = "doc-1",
            fileName = "sample.pdf",
            uri = "content://media/external/files/100",
            sizeBytes = 2048576L,
            pageCount = 12,
            lastAccessedEpochMs = 1700000000000L,
            createdAtEpochMs = 1690000000000L,
            thumbnailUri = "file:///thumbnails/doc-1.png",
            isFavorite = true,
            isEncrypted = false
        )

        val encoded = json.encodeToString(doc)
        val decoded = json.decodeFromString<Document>(encoded)

        assertEquals(doc, decoded)
        assertEquals("sample.pdf", decoded.fileName)
        assertTrue(decoded.isFavorite)
    }

    @Test
    fun pdfPageGeometrySerialization() {
        val page = PdfPage(
            pageIndex = 0,
            widthPoints = 595.0f,
            heightPoints = 842.0f,
            aspectRatio = 842.0f / 595.0f,
            rotationDegrees = 90
        )

        val encoded = json.encodeToString(page)
        val decoded = json.decodeFromString<PdfPage>(encoded)

        assertEquals(page, decoded)
        assertEquals(90, decoded.rotationDegrees)
    }

    @Test
    fun pdfMetadataSerialization() {
        val metadata = PdfMetadata(
            title = "Annual Report 2026",
            author = "Engineering Team",
            subject = "Architecture",
            keywords = listOf("PDF", "AI", "Mobile"),
            creator = "AiPdfReaderEditor",
            producer = "PDFBox Android",
            creationDateEpochMs = 1700000000000L,
            modificationDateEpochMs = 1700005000000L,
            pageCount = 42,
            isEncrypted = false,
            pdfVersion = "1.7"
        )

        val encoded = json.encodeToString(metadata)
        val decoded = json.decodeFromString<PdfMetadata>(encoded)

        assertEquals(metadata, decoded)
        assertEquals("Annual Report 2026", decoded.title)
    }

    @Test
    fun viewerToolEnumValues() {
        val tools = listOf(
            ViewerTool.None,
            ViewerTool.Hand,
            ViewerTool.Pen,
            ViewerTool.Highlighter,
            ViewerTool.TextStamp,
            ViewerTool.Signature,
            ViewerTool.Eraser
        )

        for (tool in tools) {
            val encoded = json.encodeToString(tool)
            val decoded = json.decodeFromString<ViewerTool>(encoded)
            assertEquals(tool, decoded)
        }
    }

    @Test
    fun pdfAnnotationPolymorphicSerialization() {
        val freehand = PdfAnnotation.Freehand(
            id = "ann-1",
            pageIndex = 0,
            points = listOf(Point(10f, 20f), Point(30f, 40f)),
            colorArgb = 0xFFFF0000.toInt(),
            strokeWidthDp = 3.0f,
            isHighlighter = false
        )

        val textStamp = PdfAnnotation.TextStamp(
            id = "ann-2",
            pageIndex = 1,
            text = "APPROVED",
            xRatio = 0.5f,
            yRatio = 0.8f,
            colorArgb = 0xFF00FF00.toInt(),
            fontSizeSp = 16f
        )

        val encodedFreehand = json.encodeToString<PdfAnnotation>(freehand)
        val decodedFreehand = json.decodeFromString<PdfAnnotation>(encodedFreehand)
        assertEquals(freehand, decodedFreehand)

        val encodedText = json.encodeToString<PdfAnnotation>(textStamp)
        val decodedText = json.decodeFromString<PdfAnnotation>(encodedText)
        assertEquals(textStamp, decodedText)
    }

    @Test
    fun creditTransactionAndBalanceSerialization() {
        val balance = CreditBalance(
            currentCredits = 25,
            lastBonusEpochMs = 1700000000000L,
            totalCreditsEarned = 50,
            totalCreditsSpent = 25
        )

        val tx = CreditTransaction(
            id = "tx-1",
            timestampEpochMs = 1700001000000L,
            amountDelta = -2,
            reason = CreditReason.AiChatQuery,
            inputTokens = 1200,
            outputTokens = 450,
            resultingBalance = 23
        )

        val encodedBalance = json.encodeToString(balance)
        val decodedBalance = json.decodeFromString<CreditBalance>(encodedBalance)
        assertEquals(balance, decodedBalance)

        val encodedTx = json.encodeToString(tx)
        val decodedTx = json.decodeFromString<CreditTransaction>(encodedTx)
        assertEquals(tx, decodedTx)
        assertEquals(CreditReason.AiChatQuery, decodedTx.reason)
    }

    @Test
    fun aiMessageWithCitationsSerialization() {
        val citation = AiCitation(
            pageNumber = 3,
            snippetText = "Revenue increased by 24% year-over-year.",
            confidence = 0.95f
        )

        val message = AiMessage(
            id = "msg-1",
            role = AiRole.Model,
            content = "The document indicates a 24% revenue increase on page 3.",
            timestampEpochMs = 1700002000000L,
            tokenUsage = TokenUsage(promptTokens = 850, candidateTokens = 120, creditCost = 1),
            citations = listOf(citation),
            isPending = false
        )

        val encoded = json.encodeToString(message)
        val decoded = json.decodeFromString<AiMessage>(encoded)

        assertEquals(message, decoded)
        assertEquals(1, decoded.citations.size)
        assertEquals(3, decoded.citations.first().pageNumber)
    }
}
