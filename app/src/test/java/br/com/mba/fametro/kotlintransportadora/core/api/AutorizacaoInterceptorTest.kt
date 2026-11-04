package br.com.mba.fametro.kotlintransportadora.core.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AutorizacaoInterceptorTest {

    private val servidor = MockWebServer()
    private var avisos401 = 0
    private var token: String? = "abc"
    private val http = OkHttpClient.Builder().addInterceptor(AutorizacaoInterceptor({ token }, { avisos401++ })).build()

    @Before fun subir() = servidor.start()
    @After fun derrubar() = servidor.shutdown()

    private fun get(caminho: String, status: Int = 200): String? {
        servidor.enqueue(MockResponse().setResponseCode(status))
        http.newCall(Request.Builder().url(servidor.url(caminho)).build()).execute().close()
        return servidor.takeRequest().getHeader("Authorization")
    }

    @Test
    fun `chamada autenticada leva o Bearer`() {
        assertEquals("Bearer abc", get("/api/v1/rotas"))
    }

    @Test
    fun `login e ativacao vao sem token, mas a sessao vai com`() {
        assertNull(get("/api/v1/auth/app/tokens"))
        assertNull(get("/api/v1/auth/credenciais/ENT-1"))
        assertEquals("Bearer abc", get("/api/v1/auth/sessao"))
    }

    @Test
    fun `401 de chamada autenticada encerra a sessao`() {
        get("/api/v1/rotas", 401)
        assertEquals(1, avisos401)
    }

    @Test
    fun `401 do login (senha errada) nao encerra sessao nenhuma`() {
        get("/api/v1/auth/painel/tokens", 401)
        token = null
        get("/api/v1/rotas", 401)
        assertEquals(0, avisos401)
    }
}
