package com.carparking.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun `texte simple non modifie`() {
        assertEquals("Rue de la Paix", with(ExportUtils) { "Rue de la Paix".csvEscape() })
    }

    @Test
    fun `texte avec virgule est entoure de guillemets`() {
        assertEquals("\"Paris, France\"", with(ExportUtils) { "Paris, France".csvEscape() })
    }

    @Test
    fun `guillemets internes sont doubles`() {
        assertEquals("\"Devant le \"\"Café\"\"\"", with(ExportUtils) { "Devant le \"Café\"".csvEscape() })
    }

    @Test
    fun `texte avec retour a la ligne est entoure de guillemets`() {
        assertEquals("\"ligne1\nligne2\"", with(ExportUtils) { "ligne1\nligne2".csvEscape() })
    }
}
