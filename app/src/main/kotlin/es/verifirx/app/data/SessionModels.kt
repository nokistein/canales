package es.verifirx.app.data

import es.verifirx.matching.Verdict
import kotlinx.serialization.Serializable

@Serializable
enum class StoredVerdict { MATCH, MISMATCH, NEEDS_REVIEW }

fun Verdict.toStored(): StoredVerdict = when (this) {
    Verdict.MATCH -> StoredVerdict.MATCH
    Verdict.MISMATCH -> StoredVerdict.MISMATCH
    Verdict.NEEDS_REVIEW -> StoredVerdict.NEEDS_REVIEW
}

@Serializable
data class RowRecord(
    val rowIndex: Int,
    val leftName: String?,
    val leftCn: String?,
    val rightName: String?,
    val rightCn: String?,
    val cnFromBarcode: Boolean,
    val nameSimilarity: Double?,
    val verdict: StoredVerdict,
    val reason: String,
    /** Set once a pharmacist has manually reviewed/overridden the automatic verdict. */
    val manualVerdict: StoredVerdict? = null,
    val manualNote: String? = null,
)

@Serializable
data class SessionRecord(
    val id: String,
    val createdAtEpochMillis: Long,
    val imagePath: String,
    val rows: List<RowRecord>,
) {
    val effectiveVerdicts: List<StoredVerdict> get() = rows.map { it.manualVerdict ?: it.verdict }
    val matches: Int get() = effectiveVerdicts.count { it == StoredVerdict.MATCH }
    val mismatches: Int get() = effectiveVerdicts.count { it == StoredVerdict.MISMATCH }
    val needsReview: Int get() = effectiveVerdicts.count { it == StoredVerdict.NEEDS_REVIEW }
}
