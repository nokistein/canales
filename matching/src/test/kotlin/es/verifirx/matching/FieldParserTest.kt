package es.verifirx.matching

import kotlin.test.Test
import kotlin.test.assertEquals

class FieldParserTest {

    @Test
    fun `splits name and labeled CN`() {
        val field = FieldParser.parse(Side.LEFT, "Ibuprofeno Cinfa 600mg CN: 654321")
        assertEquals("654321", field.cn)
        assertEquals("IBUPROFENO CINFA 600MG", field.name)
    }

    @Test
    fun `barcode value overrides OCR CN and is flagged`() {
        val field = FieldParser.parse(
            Side.RIGHT,
            "Ibuprofeno Cinfa 600mg",
            barcode = BarcodeHit("6543215", 0, 0, 1, 1),
        )
        assertEquals("6543215", field.cn)
        assertEquals(true, field.cnFromBarcode)
    }
}
