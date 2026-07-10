package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals

class TextNormalizerTest {

    @Test
    fun `strips accents and uppercases`() {
        assertEquals("IBUPROFENO CINFA 600 MG", TextNormalizer.normalize("Ibuprofeno Cinfa 600 mg"))
        assertEquals("OMEPRAZOL", TextNormalizer.normalize("ómeprazol"))
    }

    @Test
    fun `collapses punctuation and whitespace`() {
        assertEquals("PARACETAMOL 1G", TextNormalizer.normalize("  Paracetamol,   1g!! "))
    }

    @Test
    fun `expands common presentation abbreviations consistently`() {
        assertEquals(
            TextNormalizer.normalize("AMOXICILINA 500 MG COMPRIMIDOS"),
            TextNormalizer.normalize("AMOXICILINA 500 MG COMP"),
        )
    }

    @Test
    fun `tokenizes normalized text`() {
        assertEquals(listOf("ENANTYUM", "25", "MG", "COMP"), TextNormalizer.tokens("Enantyum 25 mg comprimidos"))
    }
}
