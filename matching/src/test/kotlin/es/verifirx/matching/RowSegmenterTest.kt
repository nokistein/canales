package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals

class RowSegmenterTest {

    private val segmenter = RowSegmenter()

    @Test
    fun `empty input yields no rows`() {
        assertEquals(emptyList(), segmenter.segment(emptyList(), pageWidth = 1000))
    }

    @Test
    fun `clusters blocks into rows by vertical position`() {
        val blocks = listOf(
            // Row 0 (~y 100), left + right
            OcrBlock("IBUPROFENO CINFA 600MG", left = 50, top = 90, right = 500, bottom = 110),
            OcrBlock("CN 654321", left = 50, top = 112, right = 200, bottom = 130),
            OcrBlock("IBUPROFENO CINFA 600MG 654321", left = 600, top = 95, right = 950, bottom = 125),
            // Row 1 (~y 300), left + right
            OcrBlock("PARACETAMOL KERN 1G", left = 50, top = 290, right = 500, bottom = 310),
            OcrBlock("CN 111222", left = 50, top = 312, right = 200, bottom = 330),
            OcrBlock("PARACETAMOL KERN 1G 111222", left = 600, top = 295, right = 950, bottom = 325),
        )

        val rows = segmenter.segment(blocks, pageWidth = 1000)

        assertEquals(2, rows.size)
        assertEquals(2, rows[0].leftBlocks.size)
        assertEquals(1, rows[0].rightBlocks.size)
        assertEquals(2, rows[1].leftBlocks.size)
        assertEquals(1, rows[1].rightBlocks.size)
    }

    @Test
    fun `row missing a cupon still segments with an empty right column`() {
        val blocks = listOf(
            OcrBlock("AMOXICILINA 500MG", left = 50, top = 90, right = 500, bottom = 110),
            OcrBlock("CN 333444", left = 50, top = 112, right = 200, bottom = 130),
        )

        val rows = segmenter.segment(blocks, pageWidth = 1000)

        assertEquals(1, rows.size)
        assertEquals(2, rows[0].leftBlocks.size)
        assertEquals(0, rows[0].rightBlocks.size)
    }

    @Test
    fun `joins row text in reading order`() {
        val blocks = listOf(
            OcrBlock("CINFA 600MG", left = 50, top = 112, right = 200, bottom = 130),
            OcrBlock("IBUPROFENO", left = 50, top = 90, right = 500, bottom = 110),
        )

        val rows = segmenter.segment(blocks, pageWidth = 1000)

        assertEquals("IBUPROFENO CINFA 600MG", rows[0].leftText)
    }
}
