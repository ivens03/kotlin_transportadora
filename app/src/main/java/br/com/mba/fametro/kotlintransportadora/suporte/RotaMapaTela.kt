package br.com.mba.fametro.kotlintransportadora.suporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Rota
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.formato.Rotulos
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EstadoTela
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EtiquetaStatus
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Info
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.LinhaMapa
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaOsm
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaRota
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.entregador.Numero
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Acompanhamento ao vivo: a cada 5 s busca a rota e só as posições NOVAS do rastro (?desde=última),
 * sem baixar o rastro inteiro de novo. Para quando a rota sai de EM_ANDAMENTO.
 */
class RotaMapaViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<Rota>>(EstadoTela.Carregando)
        private set
    var rastro by mutableStateOf<List<Coordenada>>(emptyList())
        private set
    private var desde: String? = null

    init {
        viewModelScope.launch {
            while (true) {
                atualizar()
                if ((estado as? EstadoTela.Pronto)?.dado?.status != "EM_ANDAMENTO") break
                delay(5_000)
            }
        }
    }

    private suspend fun atualizar() {
        try {
            val rota = c.chamar { rota(id) }
            val novas = if (rota.status == "PLANEJADA") emptyList() else c.chamar { posicoes(id, desde) }
            if (novas.isNotEmpty()) {
                rastro = rastro + novas.map { Coordenada(it.latitude, it.longitude) }
                desde = novas.last().registradaEm
            }
            estado = EstadoTela.Pronto(rota)
        } catch (e: ApiErro) {
            if (estado !is EstadoTela.Pronto) estado = EstadoTela.Erro(e.message)
        }
    }

    fun tentarDeNovo() = viewModelScope.launch { estado = EstadoTela.Carregando; atualizar() }
}

@Composable
fun RotaMapaTela(id: Long, aoVoltar: () -> Unit) {
    val vm = viewModelDe(chave = "mapa-$id") { RotaMapaViewModel(it, id) }
    val rota = (vm.estado as? EstadoTela.Pronto)?.dado
    Scaffold(topBar = { Topo(rota?.codigo ?: "Rota", rota?.let { "${it.entregadorNome} · ${it.veiculoPlaca}" }, aoVoltar = aoVoltar) },
        containerColor = Cores.Fundo) { m ->
        when (val e = vm.estado) {
            EstadoTela.Carregando -> Carregando(Modifier.padding(m))
            is EstadoTela.Erro -> Box(Modifier.padding(m)) { TelaErro(e.mensagem) { vm.tentarDeNovo() } }
            is EstadoTela.Pronto -> {
                val r = e.dado
                val veiculo = vm.rastro.lastOrNull() ?: r.ultimaPosicao?.let { Coordenada(it.latitude, it.longitude) }
                Column(Modifier.padding(m).fillMaxSize()) {
                    MapaOsm(
                        modifier = Modifier.fillMaxWidth().weight(0.5f),
                        linhas = listOf(MapaRota.trajeto(r), LinhaMapa(vm.rastro, Cores.Verde, 9f)),
                        marcadores = MapaRota.marcadores(r),
                        veiculo = veiculo,
                        chaveEnquadrar = r.id,
                    )
                    LazyColumn(Modifier.weight(0.5f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Cartao {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${r.progresso.percentual}% concluída", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    EtiquetaStatus(Rotulos.rota(r.status))
                                }
                                Spacer(Modifier.height(6.dp))
                                Info("Entregas", "${r.progresso.entregues} ok · ${r.progresso.naoEntregues} sem sucesso · ${r.progresso.pendentes} pendentes")
                                Info("Veículo", "${r.veiculoModelo} · ${r.veiculoPlaca}")
                                r.ultimaPosicao?.let { Info("Último GPS", "${Formato.haQuanto(it.registradaEm)} · ${it.velocidadeKmH?.let { v -> "${v.toInt()} km/h" } ?: ""}") }
                                r.proximaParada?.let { Info("Próxima", "parada $it · ${Formato.km(r.distanciaAteProximaParadaKm)} · ~${Formato.minutos(r.minutosAteProximaParada)}") }
                            }
                        }
                        items(r.paradas, key = { it.ordem }) { p ->
                            Cartao {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Numero(p.ordem, if (p.ordem == r.proximaParada) Cores.Laranja else Rotulos.parada(p.status).cor)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.destinatarioNome, style = MaterialTheme.typography.labelLarge)
                                        Text("${p.bairro} · ${p.cargaCodigo}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                                        p.motivo?.let { Text(it, color = Cores.Vermelho, style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    EtiquetaStatus(Rotulos.parada(p.status))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
