package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * End-to-end test simulating a 4-row "Justificante de la Dispensación" page:
 * row 0 matches cleanly, row 1 has a cupón with a different CN (wrong box),
 * row 2 is missing its cupón entirely, row 3 matches via a barcode read that
 * disagrees with noisy right-column OCR text (barcode should win).
 */
class DispensationVerifierTest {

    private val verifier = DispensationVerifier()
    private val pageWidth = 1000

    @Test
    fun `end to end verification produces the expected verdict per row`() {
        val blocks = listOf(
            // Row 0: clean match
            OcrBlock("IBUPROFENO CINFA 600MG", 50, 90, 500, 110),
            OcrBlock("CN 654321", 50, 112, 200, 130),
            OcrBlock("IBUPROFENO CINFA 600MG CN 654321", 600, 95, 950, 125),

            // Row 1: cupón CN does not match the printed CN
            OcrBlock("PARACETAMOL KERN 1G", 50, 290, 500, 310),
            OcrBlock("CN 111222", 50, 312, 200, 330),
            OcrBlock("IBUPROFENO CINFA 600MG CN 999888", 600, 295, 950, 325),

            // Row 2: no cupón stuck down at all
            OcrBlock("AMOXICILINA 500MG", 50, 490, 500, 510),
            OcrBlock("CN 333444", 50, 512, 200, 530),

            // Row 3: right-column OCR is noisy but the barcode confirms the CN
            OcrBlock("OMEPRAZOL NORMON 20MG", 50, 690, 500, 710),
            OcrBlock("CN 777555", 50, 712, 200, 730),
            OcrBlock("0MEPRAZ0L N0RM0N Z0MG", 600, 695, 950, 725),
        )
        val barcodes = listOf(
            BarcodeHit("7775555", left = 600, top = 730, right = 900, bottom = 745),
        )

        val result = verifier.verify(blocks, pageWidth, barcodes)

        assertEquals(4, result.rows.size)
        assertEquals(Verdict.MATCH, result.rows[0].verdict)
        assertEquals(Verdict.MISMATCH, result.rows[1].verdict)
        assertEquals(Verdict.NEEDS_REVIEW, result.rows[2].verdict)
        assertEquals(Verdict.MATCH, result.rows[3].verdict)
        assertEquals(true, result.rows[3].right.cnFromBarcode)

        assertEquals(2, result.matches)
        assertEquals(1, result.mismatches)
        assertEquals(1, result.needsReview)
    }
}
