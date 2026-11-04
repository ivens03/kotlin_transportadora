package br.com.mba.fametro.kotlintransportadora.entregador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FiltroGpsTest {

    private val t0 = Instant.parse("2026-11-04T12:00:00Z")
    private fun leitura(segundos: Long, lat: Double = -3.727171, precisao: Double? = 10.0) =
        Leitura(lat, -38.527151, t0.plusSeconds(segundos), precisaoM = precisao)

    // 0,0002° de latitude ≈ 22 m
    private val andou = -3.727171 + 0.0002

    @Test
    fun `primeira leitura boa e aceita`() {
        assertTrue(FiltroGps().aceitar(leitura(0)))
    }

    @Test
    fun `leitura imprecisa e descartada`() {
        assertFalse(FiltroGps().aceitar(leitura(0, precisao = 150.0)))
    }

    @Test
    fun `menos de 2 s depois e descartada mesmo tendo andado`() {
        val f = FiltroGps()
        f.aceitar(leitura(0))
        assertFalse(f.aceitar(Leitura(andou, -38.527151, t0.plusMillis(1_500), precisaoM = 10.0)))
    }

    @Test
    fun `andou 15 m ou mais e passou o intervalo, aceita`() {
        val f = FiltroGps()
        f.aceitar(leitura(0))
        assertTrue(f.aceitar(leitura(3, lat = andou)))
    }

    @Test
    fun `parado so manda o batimento a cada 30 s`() {
        val f = FiltroGps()
        f.aceitar(leitura(0))
        assertFalse(f.aceitar(leitura(10)))
        assertFalse(f.aceitar(leitura(29)))
        assertTrue(f.aceitar(leitura(30)))
    }

    @Test
    fun `reiniciar aceita a proxima leitura na hora`() {
        val f = FiltroGps()
        f.aceitar(leitura(0))
        f.reiniciar()
        assertTrue(f.aceitar(leitura(1)))
    }

    @Test
    fun `paraEnvio arredonda como o back-end aceita`() {
        val p = Leitura(-3.72717149, -38.52715151, Instant.parse("2026-11-04T12:00:00.123456789Z"), 42.06, 7.25).paraEnvio()
        assertEquals(-3.727171, p.latitude, 0.0)
        assertEquals(-38.527152, p.longitude, 0.0)
        assertEquals("2026-11-04T12:00:00.123Z", p.registradaEm)
        assertEquals(42.1, p.velocidadeKmH!!, 0.0)
        assertEquals(7.3, p.precisaoM!!, 0.0)
    }
}
