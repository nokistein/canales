package es.verifirx.matching

/**
 * A single piece of OCR output: recognized text plus its bounding box in image
 * pixel coordinates (origin top-left, same convention as ML Kit's Text.Element).
 */
data class OcrBlock(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Double get() = (top + bottom) / 2.0
    val centerX: Double get() = (left + right) / 2.0
    val height: Int get() = bottom - top

    init {
        require(right >= left) { "right must be >= left" }
        require(bottom >= top) { "bottom must be >= top" }
    }
}

/** A decoded barcode/data-matrix value with its bounding box, from the cupón precinto. */
data class BarcodeHit(
    val value: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Double get() = (top + bottom) / 2.0
    val centerX: Double get() = (left + right) / 2.0
}

/** Which side of the sheet a field was read from. */
enum class Side { LEFT, RIGHT }

/** Fields parsed out of one column (left = system printout, right = cupón precinto). */
data class ParsedField(
    val side: Side,
    val rawText: String,
    val name: String?,
    val cn: String?,
    /** True when a barcode-decoded CN was used instead of (or to confirm) OCR text. */
    val cnFromBarcode: Boolean = false,
)

enum class Verdict {
    /** CN and name both agree: the cupón precinto matches the printed line. */
    MATCH,

    /** CN or name clearly disagree: likely wrong medication dispensed. */
    MISMATCH,

    /** Not enough reliable data (missing OCR text, ambiguous name score) to decide automatically. */
    NEEDS_REVIEW,
}

data class RowComparison(
    val rowIndex: Int,
    val left: ParsedField,
    val right: ParsedField,
    val cnMatch: Boolean?,
    val nameSimilarity: Double?,
    val verdict: Verdict,
    val reason: String,
)

data class VerificationResult(
    val rows: List<RowComparison>,
) {
    val matches: Int get() = rows.count { it.verdict == Verdict.MATCH }
    val mismatches: Int get() = rows.count { it.verdict == Verdict.MISMATCH }
    val needsReview: Int get() = rows.count { it.verdict == Verdict.NEEDS_REVIEW }
}
