package br.com.mba.fametro.kotlintransportadora.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {

    private val praçaFerreira = Coordenada(-3.727171, -38.527151)
    private val dragaoDoMar = Coordenada(-3.721530, -38.520410)

    @Test
    fun `distancia entre dois pontos de Fortaleza bate com Haversine`() {
        // ~975 m em linha reta (conferido no back-end, que usa a mesma fórmula)
        assertEquals(975.0, Geo.distanciaM(praçaFerreira, dragaoDoMar), 10.0)
    }

    @Test
    fun `distancia de um ponto para ele mesmo e zero`() {
        assertEquals(0.0, Geo.distanciaM(praçaFerreira, praçaFerreira), 0.0)
    }

    @Test
    fun `interpolar mantem as pontas e respeita o passo`() {
        val pontos = Geo.interpolar(listOf(praçaFerreira, dragaoDoMar), 40.0)
        assertEquals(praçaFerreira, pontos.first())
        assertEquals(dragaoDoMar.latitude, pontos.last().latitude, 1e-9)
        assertEquals(dragaoDoMar.longitude, pontos.last().longitude, 1e-9)
        pontos.zipWithNext().forEach { (a, b) -> assertTrue(Geo.distanciaM(a, b) in 30.0..50.0) }
    }

    @Test
    fun `interpolar com menos de dois pontos devolve o mesmo caminho`() {
        assertEquals(listOf(praçaFerreira), Geo.interpolar(listOf(praçaFerreira), 40.0))
    }
}
