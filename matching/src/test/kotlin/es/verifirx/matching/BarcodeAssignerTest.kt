package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarcodeAssignerTest {

    @Test
    fun `assigns each barcode to the vertically closest row`() {
        val rows = listOf(
            SegmentedRow(0, top = 90, bottom = 130, leftBlocks = emptyList(), rightBlocks = emptyList()),
            SegmentedRow(1, top = 290, bottom = 330, leftBlocks = emptyList(), rightBlocks = emptyList()),
        )
        val barcodes = listOf(
            BarcodeHit("6543215", left = 600, top = 100, right = 900, bottom = 120),
            BarcodeHit("1112225", left = 600, top = 300, right = 900, bottom = 320),
        )

        val assignment = BarcodeAssigner.assign(rows, barcodes)

        assertEquals("6543215", assignment[0]?.value)
        assertEquals("1112225", assignment[1]?.value)
    }

    @Test
    fun `no barcodes yields empty assignment`() {
        val rows = listOf(SegmentedRow(0, top = 0, bottom = 10, leftBlocks = emptyList(), rightBlocks = emptyList()))
        assertTrue(BarcodeAssigner.assign(rows, emptyList()).isEmpty())
    }

    @Test
    fun `two barcodes near the same row keep the closer one`() {
        val rows = listOf(SegmentedRow(0, top = 100, bottom = 100, leftBlocks = emptyList(), rightBlocks = emptyList()))
        val barcodes = listOf(
            BarcodeHit("111111", left = 600, top = 80, right = 900, bottom = 80),
            BarcodeHit("222222", left = 600, top = 101, right = 900, bottom = 101),
        )

        val assignment = BarcodeAssigner.assign(rows, barcodes)

        assertEquals("222222", assignment[0]?.value)
    }
}
