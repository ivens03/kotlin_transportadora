package br.com.mba.fametro.kotlintransportadora.core.api

import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErroTest {

    private fun http(status: Int, corpo: String) =
        HttpException(Response.error<Any>(status, corpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun `tira a matricula da mensagem de credencial expirada`() {
        val msg = "Credencial expirada: troque em PATCH /api/v1/auth/credenciais/ENT-000123 antes de entrar."
        assertEquals("ENT-000123", ApiErro.matriculaDaMensagem(msg))
        assertEquals("CLI-TESTE-01", ApiErro.matriculaDaMensagem("... /auth/credenciais/CLI-TESTE-01"))
        assertNull(ApiErro.matriculaDaMensagem("Matrícula ou PIN inválidos."))
        assertNull(ApiErro.matriculaDaMensagem(null))
    }

    @Test
    fun `usa a mensagem e os campos do ErroResponse do back`() {
        val corpo = """{"status":422,"codigo":"REGRA_NEGOCIO","mensagem":"Rota já iniciada.",
            "campos":[{"campo":"status","mensagem":"transição inválida"}],"extra":"ignorado"}"""
        val e = ApiErro.de(http(422, corpo), Rede.json)
        assertEquals(422, e.status)
        assertEquals("REGRA_NEGOCIO", e.codigo)
        assertEquals("Rota já iniciada.\n• transição inválida", e.completa)
    }

    @Test
    fun `reconhece credencial expirada`() {
        val e = ApiErro.de(http(403, """{"status":403,"codigo":"CREDENCIAL_EXPIRADA","mensagem":"x"}"""), Rede.json)
        assertTrue(e.credencialExpirada)
    }

    @Test
    fun `corpo que nao e JSON cai na mensagem padrao`() {
        assertEquals("O servidor teve um problema. Tente de novo em instantes.", ApiErro.de(http(502, "<html>"), Rede.json).message)
    }

    @Test
    fun `falha de rede vira sem conexao`() {
        assertTrue(ApiErro.de(IOException("timeout"), Rede.json).semConexao)
    }

    @Test(expected = CancellationException::class)
    fun `cancelamento de corrotina nao e engolido`() {
        ApiErro.de(CancellationException("saiu da tela"), Rede.json)
    }
}
