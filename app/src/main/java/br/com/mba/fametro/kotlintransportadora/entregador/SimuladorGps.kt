package br.com.mba.fametro.kotlintransportadora.entregador

import br.com.mba.fametro.kotlintransportadora.core.api.Rota
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.geo.Geo

/**
 * Só para demonstração (build debug): "dirige" pelo trajeto planejado em linha reta entre os pontos.
 * Para em cada parada pendente até o entregador dar baixa, como na vida real.
 */
class SimuladorGps(private val passoM: Double = 40.0) {

    private var caminho: List<Coordenada> = emptyList()
    private var indice = 0

    /** Monta o caminho a partir de onde o veículo está (ou do depósito) passando pelas paradas pendentes. */
    fun preparar(rota: Rota, atual: Coordenada?) {
        val pendentes = rota.paradas.filter { it.status == "PENDENTE" }.sortedBy { it.ordem }
            .map { Coordenada(it.latitude, it.longitude) }
        val origem = Coordenada(rota.origemLatitude, rota.origemLongitude)
        val pontos = buildList {
            add(atual ?: origem)
            addAll(pendentes)
            if (rota.retornaOrigem) add(origem)
        }
        caminho = Geo.interpolar(pontos, passoM)
        indice = 0
    }

    /**
     * Próxima posição. Fica parado enquanto estiver a menos de 30 m de uma parada ainda pendente.
     * Devolve null quando chegou ao fim do caminho.
     */
    fun proxima(paradasPendentes: List<Coordenada>): Coordenada? {
        if (caminho.isEmpty() || indice >= caminho.size) return null
        val aqui = caminho[indice]
        val segurar = paradasPendentes.any { Geo.distanciaM(it, aqui) < 30.0 }
        if (!segurar) indice++
        return aqui
    }
}
