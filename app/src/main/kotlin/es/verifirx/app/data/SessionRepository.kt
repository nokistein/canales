package es.verifirx.app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import es.verifirx.matching.VerificationResult
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local-only persistence for verification sessions: the captured image plus the
 * per-row comparison, so a pharmacist can reopen a past check for an audit trail.
 * Everything lives under the app's private storage (excluded from backups, see
 * data_extraction_rules.xml) — nothing is uploaded anywhere.
 */
class SessionRepository(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val sessionsDir: File by lazy {
        File(context.filesDir, "sessions").apply { mkdirs() }
    }
    private val imagesDir: File by lazy {
        File(context.filesDir, "captures").apply { mkdirs() }
    }

    suspend fun save(bitmap: Bitmap, result: VerificationResult): SessionRecord = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val imageFile = File(imagesDir, "$id.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val record = SessionRecord(
            id = id,
            createdAtEpochMillis = System.currentTimeMillis(),
            imagePath = imageFile.absolutePath,
            rows = result.rows.map { row ->
                RowRecord(
                    rowIndex = row.rowIndex,
                    leftName = row.left.name,
                    leftCn = row.left.cn,
                    rightName = row.right.name,
                    rightCn = row.right.cn,
                    cnFromBarcode = row.right.cnFromBarcode,
                    nameSimilarity = row.nameSimilarity,
                    verdict = row.verdict.toStored(),
                    reason = row.reason,
                )
            },
        )
        writeRecord(record)
        record
    }

    suspend fun update(record: SessionRecord): Unit = withContext(Dispatchers.IO) {
        writeRecord(record)
    }

    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        recordFile(id).delete()
        File(imagesDir, "$id.jpg").delete()
    }

    suspend fun list(): List<SessionRecord> = withContext(Dispatchers.IO) {
        sessionsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString<SessionRecord>(it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.createdAtEpochMillis }
            ?: emptyList()
    }

    suspend fun get(id: String): SessionRecord? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<SessionRecord>(recordFile(id).readText()) }.getOrNull()
    }

    fun imageUriFor(record: SessionRecord) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(record.imagePath))

    private fun writeRecord(record: SessionRecord) {
        recordFile(record.id).writeText(json.encodeToString(record))
    }

    private fun recordFile(id: String) = File(sessionsDir, "$id.json")
}
