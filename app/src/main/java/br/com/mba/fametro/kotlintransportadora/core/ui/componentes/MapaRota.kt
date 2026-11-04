package br.com.mba.fametro.kotlintransportadora.core.ui.componentes

import br.com.mba.fametro.kotlintransportadora.core.api.Rota
import br.com.mba.fametro.kotlintransportadora.core.formato.Rotulos
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores

/** Camadas de mapa de uma rota, usadas pelo entregador e pelo Suporte (mesmo desenho nas duas telas). */
object MapaRota {

    /** Trajeto planejado (depósito → paradas → retorno), tracejado em azul. */
    fun trajeto(rota: Rota) = LinhaMapa(rota.trajeto.map { Coordenada(it.latitude, it.longitude) }, Cores.Azul, 7f, tracejada = true)

    /** Depósito + uma bolinha numerada por parada, na cor do status (a próxima fica laranja). */
    fun marcadores(rota: Rota): List<MarcadorMapa> =
        listOf(MarcadorMapa(Coordenada(rota.origemLatitude, rota.origemLongitude), "CD", Cores.Marinho, "Depósito")) +
            rota.paradas.map { p ->
                val cor = if (p.ordem == rota.proximaParada) Cores.Laranja else Rotulos.parada(p.status).cor
                MarcadorMapa(Coordenada(p.latitude, p.longitude), p.ordem.toString(), cor, p.destinatarioNome)
            }
}
