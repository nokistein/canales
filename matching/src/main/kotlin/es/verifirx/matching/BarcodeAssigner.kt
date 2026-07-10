package es.verifirx.matching

/**
 * Matches decoded barcodes (from the cupón precinto's Code128/EAN symbol) to the
 * table row they fall in, by vertical proximity. A barcode-decoded CN is much
 * more reliable than OCR on a small thermal-printed sticker, so callers should
 * prefer it over the OCR'd right-column CN when one is available.
 */
object BarcodeAssigner {

    fun assign(rows: List<SegmentedRow>, barcodes: List<BarcodeHit>): Map<Int, BarcodeHit> {
        if (rows.isEmpty() || barcodes.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, BarcodeHit>()
        for (barcode in barcodes) {
            val closestRow = rows.minByOrNull { row ->
                val rowCenterY = (row.top + row.bottom) / 2.0
                kotlin.math.abs(barcode.centerY - rowCenterY)
            } ?: continue

            val existing = result[closestRow.rowIndex]
            if (existing == null) {
                result[closestRow.rowIndex] = barcode
            } else {
                // Two barcodes claim the same row (rare): keep the vertically closer one.
                val rowCenterY = (closestRow.top + closestRow.bottom) / 2.0
                if (kotlin.math.abs(barcode.centerY - rowCenterY) < kotlin.math.abs(existing.centerY - rowCenterY)) {
                    result[closestRow.rowIndex] = barcode
                }
            }
        }
        return result
    }
}
