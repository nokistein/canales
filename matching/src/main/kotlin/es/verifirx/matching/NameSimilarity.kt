package es.verifirx.matching

import kotlin.math.max

/**
 * Fuzzy comparison of medication names as read from the two columns. OCR noise,
 * abbreviations ("COMP" vs "COMPRIMIDOS") and minor formatting differences are
 * expected even when both sides describe the same product, so this is a
 * supporting signal, not the primary one (the CN is authoritative).
 */
object NameSimilarity {

    /** Combined similarity score in [0.0, 1.0]; 1.0 means the names are effectively identical. */
    fun score(a: String, b: String): Double {
        val normA = TextNormalizer.normalize(a)
        val normB = TextNormalizer.normalize(b)
        if (normA.isEmpty() || normB.isEmpty()) return 0.0
        if (normA == normB) return 1.0

        val levenshteinRatio = 1.0 - levenshtein(normA, normB).toDouble() / max(normA.length, normB.length)
        val jaccard = tokenJaccard(TextNormalizer.tokens(a), TextNormalizer.tokens(b))

        // Character-level similarity is weighted higher than token overlap: a single
        // misread character (e.g. "0" vs "O") should not tank the score just because
        // it makes one whole token fail an exact-match Jaccard comparison.
        return (levenshteinRatio * 0.6) + (jaccard * 0.4)
    }

    private fun tokenJaccard(a: List<String>, b: List<String>): Double {
        val setA = a.toSet()
        val setB = b.toSet()
        if (setA.isEmpty() && setB.isEmpty()) return 1.0
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return intersection.toDouble() / union
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
            }
            val tmp = previous
            previous = current
            current = tmp
        }
        return previous[b.length]
    }
}
