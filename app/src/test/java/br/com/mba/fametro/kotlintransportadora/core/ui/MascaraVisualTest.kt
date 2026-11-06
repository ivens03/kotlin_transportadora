package br.com.mba.fametro.kotlintransportadora.core.ui

import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MascaraVisual
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MascaraVisualTest {

    @Test
    fun `CNPJ completo e pela metade`() {
        assertEquals("12.ABC.345/01DE-35", MascaraVisual.aplicar("##.###.###/####-##", "12ABC34501DE35").first)
        assertEquals("12.ABC.3", MascaraVisual.aplicar("##.###.###/####-##", "12ABC3").first)
    }

    @Test
    fun `cursor pula os símbolos da máscara`() {
        // "60325001" → "60325-001": o 6º dígito (índice 5) fica na posição 6 da tela, depois do hífen
        val (tela, paraTela) = MascaraVisual.aplicar("#####-###", "60325001")
        assertEquals("60325-001", tela)
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4, 6, 7, 8, 9), paraTela)
    }
}
