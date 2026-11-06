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

class MascarasEmpresaTest {

    @Test
    fun `CNPJ - 12 letras ou números e 2 dígitos no fim`() {
        assertEquals("12ABC34501DE35", Mascaras.cnpj("12.abc.345/01de-35"))
        assertEquals("12ABC34501DE", Mascaras.cnpj("12ABC34501DEXY")) // letra não entra nos dígitos verificadores
        assertEquals("11222333000181", Mascaras.cnpj("11.222.333/0001-8199"))
    }

    @Test
    fun `telefone, CEP e UF`() {
        assertEquals("85999998888", Mascaras.telefone("(85) 99999-8888 1"))
        assertEquals("60325001", Mascaras.cep("60325-0019"))
        assertEquals("CE", Mascaras.uf("c-e1x"))
    }

    @Test
    fun `decimal com uma vírgula e casas limitadas`() {
        assertEquals("1250,50", Mascaras.decimal("1250.505", 2))
        assertEquals("0,125", Mascaras.decimal("0,12,59", 3))
        assertEquals(1250.5, Mascaras.numero("1250,50")!!, 0.0001)
        assertEquals(null, Mascaras.numero(","))
    }
}
