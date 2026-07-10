package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CnExtractorTest {

    @Test
    fun `extracts labeled CN`() {
        assertEquals("654321", CnExtractor.extract("IBUPROFENO 600MG CN: 654321"))
        assertEquals("654321", CnExtractor.extract("C.N. 654321"))
    }

    @Test
    fun `extracts bare six or seven digit run`() {
        assertEquals("654321", CnExtractor.extract("654321"))
        assertEquals("6543215", CnExtractor.extract("EAN 6543215 UDS 1"))
    }

    @Test
    fun `ignores runs that are the wrong length`() {
        assertNull(CnExtractor.extract("Caja de 30 comprimidos"))
    }

    @Test
    fun `six digit and seven digit with check digit are considered the same code`() {
        assertTrue(CnExtractor.matches("654321", "6543215"))
    }

    @Test
    fun `leading zeros do not break equality`() {
        assertTrue(CnExtractor.matches("012345", "12345"))
    }

    @Test
    fun `different codes do not match`() {
        assertFalse(CnExtractor.matches("654321", "654322"))
    }
}
