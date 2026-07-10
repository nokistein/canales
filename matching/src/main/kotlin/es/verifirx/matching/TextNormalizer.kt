package es.verifirx.matching

import java.text.Normalizer

/**
 * Normalizes OCR'd Spanish pharmacy text so that formatting noise (accents, case,
 * punctuation, stray whitespace from a scan) doesn't cause false mismatches.
 */
object TextNormalizer {

    // Common presentation abbreviations that appear inconsistently between the
    // system printout and the manufacturer's cupón precinto label.
    private val abbreviations = mapOf(
        "COMPRIMIDOS" to "COMP",
        "COMPRIMIDO" to "COMP",
        "CAPSULAS" to "CAPS",
        "CAPSULA" to "CAPS",
        "SOLUCION" to "SOL",
        "INYECTABLE" to "INY",
        "AMPOLLAS" to "AMP",
        "AMPOLLA" to "AMP",
        "ENVASE" to "ENV",
        "UNIDADES" to "UDS",
        "UNIDAD" to "UD",
    )

    fun normalize(text: String): String {
        val stripped = stripAccents(text.uppercase())
        val cleaned = stripped
            .replace(Regex("[^A-Z0-9%. ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return abbreviations.entries.fold(cleaned) { acc, (long, short) ->
            acc.replace(Regex("\\b$long\\b"), short)
        }
    }

    fun tokens(text: String): List<String> =
        normalize(text).split(" ").filter { it.isNotBlank() }

    private fun stripAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "")
    }
}
