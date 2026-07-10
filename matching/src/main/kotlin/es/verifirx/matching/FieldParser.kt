package es.verifirx.matching

/** Splits a column's raw OCR text into a medication name and a CN, for one side of a row. */
object FieldParser {

    private val labeledCn = Regex("C\\.?N\\.?\\s*[:.]?\\s*\\d[\\d .-]{4,9}\\d")
    private val anyDigitRun = Regex("\\d[\\d .-]{4,9}\\d|\\d{6,7}")

    fun parse(side: Side, rawText: String, barcode: BarcodeHit? = null): ParsedField {
        val ocrCn = CnExtractor.extract(rawText)
        val cn = barcode?.value?.let { CnExtractor.extract(it) ?: it } ?: ocrCn

        val withoutCn = if (labeledCn.containsMatchIn(rawText)) {
            labeledCn.replace(rawText, " ")
        } else {
            anyDigitRun.replace(rawText, " ")
        }
        val name = TextNormalizer.normalize(withoutCn).takeIf { it.isNotBlank() }

        return ParsedField(
            side = side,
            rawText = rawText,
            name = name,
            cn = cn,
            cnFromBarcode = barcode != null && cn == (barcode.value.let { CnExtractor.extract(it) ?: it }),
        )
    }
}
