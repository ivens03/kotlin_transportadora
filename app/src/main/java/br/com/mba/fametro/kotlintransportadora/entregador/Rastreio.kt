package br.com.mba.fametro.kotlintransportadora.entregador

import br.com.mba.fametro.kotlintransportadora.core.api.NovaPosicao
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.geo.Geo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit

/** O que o rastreador está fazendo (a tela da rota mostra). */
data class EstadoRastreio(
    val rotaId: Long? = null,
    val ativo: Boolean = false,
    val simulando: Boolean = false,
    val naFila: Int = 0,
    val ultimoEnvioEm: Instant? = null,
    val ultimaPosicao: Coordenada? = null,
    val semPermissao: Boolean = false,
    val erro: String? = null,
)

/** Uma leitura crua do GPS (o serviço converte do android.location.Location). */
data class Leitura(
    val latitude: Double,
    val longitude: Double,
    val em: Instant,
    val velocidadeKmH: Double? = null,
    val precisaoM: Double? = null,
) {
    val coordenada get() = Coordenada(latitude, longitude)

    /** Formato aceito pelo back: 6 casas na coordenada, 1 na velocidade/precisão, hora em ISO-8601. */
    fun paraEnvio() = NovaPosicao(
        latitude = arredondar(latitude, 6),
        longitude = arredondar(longitude, 6),
        registradaEm = em.truncatedTo(ChronoUnit.MILLIS).toString(),
        velocidadeKmH = velocidadeKmH?.let { arredondar(it.coerceIn(0.0, 999.0), 1) },
        precisaoM = precisaoM?.let { arredondar(it.coerceIn(0.0, 99_999.0), 1) },
    )

    private fun arredondar(v: Double, casas: Int) = BigDecimal.valueOf(v).setScale(casas, RoundingMode.HALF_UP).toDouble()
}

/**
 * Economiza bateria e dados: só guarda a leitura se passou o intervalo mínimo E andou a distância mínima.
 * Parado (no sinal, na entrega) ainda manda uma a cada [batimentoMs] para o Suporte saber que o celular está vivo.
 * Mesmas regras do app web (environment.gps).
 */
class FiltroGps(
    private val intervaloMinimoMs: Long = 2_000,
    private val distanciaMinimaM: Double = 15.0,
    private val batimentoMs: Long = 30_000,
    private val precisaoMaximaM: Double = 100.0,
) {
    private var ultima: Leitura? = null

    fun aceitar(l: Leitura): Boolean {
        if ((l.precisaoM ?: 0.0) > precisaoMaximaM) return false // leitura ruim (dentro de prédio, túnel)
        val u = ultima
        if (u != null) {
            val dt = l.em.toEpochMilli() - u.em.toEpochMilli()
            if (dt < intervaloMinimoMs) return false
            if (Geo.distanciaM(u.coordenada, l.coordenada) < distanciaMinimaM && dt < batimentoMs) return false
        }
        ultima = l
        return true
    }

    fun reiniciar() {
        ultima = null
    }
}
