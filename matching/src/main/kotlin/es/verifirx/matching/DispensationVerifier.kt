package es.verifirx.matching

/**
 * End-to-end pipeline: raw OCR blocks + decoded barcodes for one "Justificante de
 * la Dispensación" sheet in, a per-row verdict out.
 *
 * The CN is treated as the authoritative identifier (it's a precise 6-7 digit
 * code with essentially no chance of an accidental match), so it drives
 * MATCH/MISMATCH. The medication name is a supporting signal used to catch
 * cases that need a human look — e.g. a coincidentally-parsed CN next to a
 * wildly different name usually means the row segmentation slipped, not that
 * the pharmacy actually dispensed the wrong box.
 */
class DispensationVerifier(
    private val segmenter: RowSegmenter = RowSegmenter(),
    private val nameReviewThreshold: Double = 0.5,
) {

    fun verify(blocks: List<OcrBlock>, pageWidth: Int, barcodes: List<BarcodeHit> = emptyList()): VerificationResult {
        val rows = segmenter.segment(blocks, pageWidth)
        val barcodeByRow = BarcodeAssigner.assign(rows, barcodes)

        val comparisons = rows.map { row ->
            val left = FieldParser.parse(Side.LEFT, row.leftText)
            val right = FieldParser.parse(Side.RIGHT, row.rightText, barcode = barcodeByRow[row.rowIndex])
            compareRow(row.rowIndex, left, right)
        }

        return VerificationResult(comparisons)
    }

    private fun compareRow(rowIndex: Int, left: ParsedField, right: ParsedField): RowComparison {
        if (left.cn == null || left.name == null) {
            return RowComparison(
                rowIndex, left, right, cnMatch = null, nameSimilarity = null,
                verdict = Verdict.NEEDS_REVIEW,
                reason = "No se pudo leer con claridad la columna del sistema (izquierda).",
            )
        }
        if (right.cn == null && right.name == null) {
            return RowComparison(
                rowIndex, left, right, cnMatch = null, nameSimilarity = null,
                verdict = Verdict.NEEDS_REVIEW,
                reason = "No se detectó el cupón precinto en esta fila.",
            )
        }

        val cnMatch = right.cn?.let { CnExtractor.matches(left.cn, it) }
        val nameSimilarity = right.name?.let { NameSimilarity.score(left.name, it) }

        return when {
            cnMatch == false -> RowComparison(
                rowIndex, left, right, cnMatch, nameSimilarity,
                verdict = Verdict.MISMATCH,
                reason = "El código nacional del cupón no coincide con el impreso.",
            )
            cnMatch == null -> RowComparison(
                rowIndex, left, right, cnMatch, nameSimilarity,
                verdict = Verdict.NEEDS_REVIEW,
                reason = "No se pudo leer el código nacional del cupón; verificar manualmente.",
            )
            // A barcode-decoded CN is trustworthy on its own (barcode symbologies
            // carry a check digit), so garbled OCR on the sticker's printed name
            // shouldn't force a manual review when the barcode already confirmed it.
            nameSimilarity != null && nameSimilarity < nameReviewThreshold && !right.cnFromBarcode -> RowComparison(
                rowIndex, left, right, cnMatch, nameSimilarity,
                verdict = Verdict.NEEDS_REVIEW,
                reason = "El código nacional coincide pero el nombre leído difiere notablemente; revisar manualmente.",
            )
            else -> RowComparison(
                rowIndex, left, right, cnMatch, nameSimilarity,
                verdict = Verdict.MATCH,
                reason = "Código nacional y nombre coinciden.",
            )
        }
    }
}
