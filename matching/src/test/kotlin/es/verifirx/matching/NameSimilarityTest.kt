package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NameSimilarityTest {

    @Test
    fun `identical names score 1`() {
        assertEquals(1.0, NameSimilarity.score("Ibuprofeno Cinfa 600mg", "IBUPROFENO CINFA 600MG"))
    }

    @Test
    fun `minor OCR noise still scores high`() {
        // Single digit misread as a letter ("0" -> "O") inside one token.
        val score = NameSimilarity.score("Ibuprofeno Cinfa 600 mg", "Ibuprofeno Cinfa 60O mg")
        assertTrue(score > 0.75, "expected high similarity, got $score")
    }

    @Test
    fun `different medications score low`() {
        val score = NameSimilarity.score("Ibuprofeno Cinfa 600mg", "Paracetamol Kern 1g")
        assertTrue(score < 0.4, "expected low similarity, got $score")
    }

    @Test
    fun `blank input scores zero`() {
        assertEquals(0.0, NameSimilarity.score("", "Ibuprofeno 600mg"))
    }
}
