package pavansaiajayx.aipdfreadereditor.core.database.mapper

import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.model.Document

fun DocumentEntity.asExternalModel(): Document = Document(
    id = id.toString(),
    fileName = fileName,
    uri = uri,
    sizeBytes = fileSizeBytes,
    pageCount = pageCount,
    lastAccessedEpochMs = lastAccessedAt,
    createdAtEpochMs = createdAt,
    thumbnailUri = thumbnailUri,
    isFavorite = isFavorite,
    isEncrypted = isEncrypted
)

fun Document.asEntity(): DocumentEntity = DocumentEntity(
    id = id.toLongOrNull() ?: 0L,
    fileName = fileName,
    uri = uri,
    pageCount = pageCount,
    fileSizeBytes = sizeBytes,
    thumbnailUri = thumbnailUri,
    isFavorite = isFavorite,
    isEncrypted = isEncrypted,
    createdAt = createdAtEpochMs,
    lastAccessedAt = lastAccessedEpochMs
)
