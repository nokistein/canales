package es.verifirx.matching

/** One detected table row: the OCR blocks belonging to it, already split by column. */
data class SegmentedRow(
    val rowIndex: Int,
    val top: Int,
    val bottom: Int,
    val leftBlocks: List<OcrBlock>,
    val rightBlocks: List<OcrBlock>,
) {
    val leftText: String get() = joinReadingOrder(leftBlocks)
    val rightText: String get() = joinReadingOrder(rightBlocks)

    private fun joinReadingOrder(blocks: List<OcrBlock>): String =
        blocks.sortedWith(compareBy({ it.top }, { it.left }))
            .joinToString(" ") { it.text }
}

/**
 * Groups raw OCR text blocks (one per recognized line) into table rows, then
 * splits each row into a left column (system printout) and right column
 * (cupón precinto) based on horizontal position.
 *
 * This works directly on OCR output geometry rather than trying to detect table
 * gridlines in the image, which is far more robust across scan quality, skew and
 * printer variations for a form that doesn't reliably have visible ruling lines.
 */
class RowSegmenter(
    /** Fraction of the page width (0..1) that separates the left/right columns. */
    private val columnSplitFraction: Double = 0.55,
    /**
     * Two blocks belong to the same row if their vertical centers are within
     * this many multiples of the median block height of each other.
     */
    private val rowGapFactor: Double = 0.6,
) {

    fun segment(blocks: List<OcrBlock>, pageWidth: Int): List<SegmentedRow> {
        if (blocks.isEmpty()) return emptyList()

        val splitX = pageWidth * columnSplitFraction
        val medianHeight = blocks.map { it.height }.sorted().let { it[it.size / 2] }.coerceAtLeast(1)
        val gapThreshold = medianHeight * rowGapFactor

        val sorted = blocks.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<OcrBlock>>()
        var rowTop = Double.NaN
        var rowBottom = Double.NaN

        for (block in sorted) {
            val fitsCurrentRow = rows.isNotEmpty() &&
                block.centerY >= rowTop - gapThreshold &&
                block.centerY <= rowBottom + gapThreshold

            if (fitsCurrentRow) {
                rows.last().add(block)
                rowTop = minOf(rowTop, block.top.toDouble())
                rowBottom = maxOf(rowBottom, block.bottom.toDouble())
            } else {
                rows.add(mutableListOf(block))
                rowTop = block.top.toDouble()
                rowBottom = block.bottom.toDouble()
            }
        }

        return rows.mapIndexed { index, rowBlocks ->
            val (left, right) = rowBlocks.partition { it.centerX < splitX }
            SegmentedRow(
                rowIndex = index,
                top = rowBlocks.minOf { it.top },
                bottom = rowBlocks.maxOf { it.bottom },
                leftBlocks = left,
                rightBlocks = right,
            )
        }
    }
}
