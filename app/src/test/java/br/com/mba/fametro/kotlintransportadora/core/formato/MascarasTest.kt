package br.com.mba.fametro.kotlintransportadora.core.formato

import org.junit.Assert.assertEquals
import org.junit.Test

class MascarasTest {

    @Test
    fun `matrícula em maiúsculas, sem espaço nem símbolo`() {
        assertEquals("ENT-000001", Mascaras.matricula("ent-000001"))
        assertEquals("CLI-TESTE-01", Mascaras.matricula("cli-teste-01 "))
        assertEquals("FUN-000002", Mascaras.matricula("fun--0000@02"))
    }

    @Test
    fun `código de ativação tem 8 letras ou números`() {
        assertEquals("AB12CD34", Mascaras.codigoAtivacao("ab-12 cd34ef"))
    }

    @Test
    fun `e-mail sem espaços e minúsculo`() {
        assertEquals("ana.silva@empresa.com", Mascaras.email(" Ana.Silva @Empresa.com "))
    }

    @Test
    fun `PIN só com 6 números`() {
        assertEquals("123456", Mascaras.pin("12a3456789"))
        assertEquals("", Mascaras.pin("abc"))
    }
}
