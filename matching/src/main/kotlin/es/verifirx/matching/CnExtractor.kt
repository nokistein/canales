package es.verifirx.matching

/**
 * Extracts and compares Spanish "Código Nacional" (CN) drug codes from OCR text.
 *
 * The CN is a 6-digit code. Printouts sometimes show it with a 7th trailing
 * "dígito de control" (check digit), e.g. as encoded in the cupón precinto's
 * barcode. We treat a 7-digit read as "6-digit CN + check digit" and compare
 * both forms so a 6-digit read on one side still matches a 7-digit read on the
 * other.
 */
object CnExtractor {

    private val labeledCn = Regex("C\\.?N\\.?\\s*[:.]?\\s*(\\d[\\d .-]{4,9}\\d)")
    private val anyDigitRun = Regex("\\d[\\d .-]{4,9}\\d|\\d{6,7}")

    /** Best-effort extraction of a CN-looking digit run from free OCR text. */
    fun extract(text: String): String? {
        val labeled = labeledCn.find(text)?.groupValues?.get(1)
        val candidate = labeled ?: anyDigitRun.findAll(text)
            .map { it.value }
            .map { onlyDigits(it) }
            .firstOrNull { it.length == 6 || it.length == 7 }
        return candidate?.let { onlyDigits(it) }?.takeIf { it.length == 6 || it.length == 7 }
    }

    /** True if [a] and [b] plausibly refer to the same national code. */
    fun matches(a: String, b: String): Boolean {
        val candidatesA = canonicalForms(a)
        val candidatesB = canonicalForms(b)
        return candidatesA.intersect(candidatesB).isNotEmpty()
    }

    private fun canonicalForms(raw: String): Set<String> {
        val digits = onlyDigits(raw)
        if (digits.isEmpty()) return emptySet()
        val stripped = digits.trimStart('0').ifEmpty { "0" }
        val forms = mutableSetOf(stripped)
        if (digits.length == 7) {
            forms += digits.dropLast(1).trimStart('0').ifEmpty { "0" }
        }
        return forms
    }

    private fun onlyDigits(s: String): String = s.filter { it.isDigit() }
}
