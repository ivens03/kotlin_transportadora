package br.com.mba.fametro.kotlintransportadora.core.api

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class DnsComReservaTest {

    private val vps = InetAddress.getByName("137.131.162.187")
    private val host = "rotacerta.137-131-162-187.sslip.io"

    private fun dns(vararg ips: String) = Dns { ips.map(InetAddress::getByName) }
    private val semResposta = Dns { throw UnknownHostException(it) }
    private val naoDeveSerChamado = Dns { error("não deveria consultar a reserva") }

    @Test
    fun `rede responde certo - usa a rede e nem consulta a reserva`() {
        val r = DnsComReserva(dns("137.131.162.187"), listOf(naoDeveSerChamado)).lookup(host)
        assertEquals(listOf(vps), r)
    }

    @Test
    fun `rede devolve 127_0_0_1 (bloqueio) - usa a reserva`() {
        val r = DnsComReserva(dns("127.0.0.1"), listOf(dns("137.131.162.187"))).lookup(host)
        assertEquals(listOf(vps), r)
    }

    @Test
    fun `rede devolve 0_0_0_0 (bloqueio) - usa a reserva`() {
        val r = DnsComReserva(dns("0.0.0.0"), listOf(dns("137.131.162.187"))).lookup(host)
        assertEquals(listOf(vps), r)
    }

    @Test
    fun `rede falha e a primeira reserva também - tenta a segunda`() {
        val r = DnsComReserva(semResposta, listOf(semResposta, dns("137.131.162.187"))).lookup(host)
        assertEquals(listOf(vps), r)
    }

    @Test
    fun `localhost de verdade (emulador com adb reverse) continua funcionando`() {
        val local = InetAddress.getByName("127.0.0.1")
        val r = DnsComReserva(dns("127.0.0.1"), listOf(semResposta)).lookup("localhost")
        assertEquals(listOf(local), r)
    }

    @Test(expected = UnknownHostException::class)
    fun `ninguém resolve - erro de host desconhecido (o app mostra sem conexão)`() {
        DnsComReserva(semResposta, listOf(semResposta)).lookup(host)
    }
}
