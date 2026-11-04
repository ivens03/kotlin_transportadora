package br.com.mba.fametro.kotlintransportadora.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordenada(val latitude: Double, val longitude: Double)

/** Geometria na esfera (mesma fórmula de Haversine do back-end). */
object Geo {
    private const val RAIO_TERRA_M = 6_371_008.8

    fun distanciaM(a: Coordenada, b: Coordenada): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * RAIO_TERRA_M * asin(min(1.0, sqrt(h)))
    }

    /** Pontos a cada ~passoM metros ao longo do caminho (para o simulador andar suave). */
    fun interpolar(caminho: List<Coordenada>, passoM: Double): List<Coordenada> {
        if (caminho.size < 2) return caminho
        val saida = mutableListOf(caminho.first())
        for (k in 0 until caminho.size - 1) {
            val a = caminho[k]
            val b = caminho[k + 1]
            val n = max(1, (distanciaM(a, b) / passoM).roundToInt())
            for (i in 1..n) {
                val f = i.toDouble() / n
                saida += Coordenada(a.latitude + (b.latitude - a.latitude) * f, a.longitude + (b.longitude - a.longitude) * f)
            }
        }
        return saida
    }
}
