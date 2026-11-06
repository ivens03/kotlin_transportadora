package br.com.mba.fametro.kotlintransportadora.core.formato

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CnpjTest {

    @Test
    fun `CNPJ numérico de sempre`() {
        assertTrue(Cnpj.valido("11222333000181"))
        assertTrue(Cnpj.valido("11.222.333/0001-81"))
        assertFalse(Cnpj.valido("11222333000182"))
    }

    @Test
    fun `CNPJ alfanumérico (exemplo oficial da Receita)`() {
        assertTrue(Cnpj.valido("12ABC34501DE35"))
        assertTrue(Cnpj.valido("12.abc.345/01de-35"))
        assertFalse(Cnpj.valido("12ABC34501DE36"))
    }

    @Test
    fun `tamanho errado ou tudo igual é inválido`() {
        assertFalse(Cnpj.valido("1122233300018"))
        assertFalse(Cnpj.valido("00000000000000"))
        assertFalse(Cnpj.valido(null))
    }

    @Test
    fun formatar() {
        assertEquals("12.ABC.345/01DE-35", Cnpj.formatar("12ABC34501DE35"))
        assertEquals("123", Cnpj.formatar("123"))
    }
}
