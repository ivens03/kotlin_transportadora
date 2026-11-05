package br.com.mba.fametro.kotlintransportadora.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Encomenda
import br.com.mba.fametro.kotlintransportadora.core.api.EventoEncomenda
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
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MarcadorMapa
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Rastreio da encomenda: onde está o veículo, quantas entregas antes da sua e a previsão de chegada,
 * mais a linha do tempo. A caminho, atualiza sozinho a cada 15 s (o mesmo conteúdo dos webhooks).
 */
class EncomendaDetalheViewModel(private val c: AppContainer, private val codigo: String) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<Encomenda>>(EstadoTela.Carregando)
        private set

    init {
        viewModelScope.launch {
            while (true) {
                carregar()
                val status = (estado as? EstadoTela.Pronto)?.dado?.encomenda?.status
                if (status != "EM_TRANSITO" && status != "ROTEIRIZADA") break
                delay(15_000)
            }
        }
    }

    suspend fun carregar() {
        try {
            estado = EstadoTela.Pronto(c.chamar { encomenda(codigo) })
        } catch (e: ApiErro) {
            if (estado !is EstadoTela.Pronto) estado = EstadoTela.Erro(e.message)
        }
    }

    fun tentarDeNovo() = viewModelScope.launch { estado = EstadoTela.Carregando; carregar() }
}

@Composable
fun EncomendaDetalheTela(codigo: String, aoVoltar: () -> Unit) {
    val vm = viewModelDe(chave = "enc-$codigo") { EncomendaDetalheViewModel(it, codigo) }
    Scaffold(topBar = { Topo(codigo, "Rastreio", aoVoltar = aoVoltar) }, containerColor = Cores.Fundo) { m ->
        when (val e = vm.estado) {
            EstadoTela.Carregando -> Carregando(Modifier.padding(m))
            is EstadoTela.Erro -> Box(Modifier.padding(m)) { TelaErro(e.mensagem) { vm.tentarDeNovo() } }
            is EstadoTela.Pronto -> Detalhe(e.dado, Modifier.padding(m))
        }
    }
}

@Composable
private fun Detalhe(d: Encomenda, modifier: Modifier) {
    val enc = d.encomenda
    val r = d.rastreio
    val destino = Coordenada(enc.latitude, enc.longitude)
    val veiculo = r?.localizacao?.let { Coordenada(it.latitude, it.longitude) }

    Column(modifier.fillMaxSize()) {
        MapaOsm(
            modifier = Modifier.fillMaxWidth().weight(0.4f),
            linhas = listOfNotNull(veiculo?.let { LinhaMapa(listOf(it, destino), Cores.Azul, 6f, tracejada = true) }),
            marcadores = listOf(MarcadorMapa(destino, "★", Cores.Verde, "Destino")),
            veiculo = veiculo,
            chaveEnquadrar = veiculo != null, // reenquadra quando o veículo aparece
        )
        LazyColumn(Modifier.weight(0.6f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Cartao {
                    EtiquetaStatus(Rotulos.encomenda(enc.status))
                    Spacer(Modifier.height(8.dp))
                    Text(previsao(d), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Cores.Texto)
                    detalhePrevisao(d)?.let { Text(it, color = Cores.AzulEscuro, modifier = Modifier.padding(top = 4.dp)) }
                    r?.localizacao?.rua?.let { rua ->
                        Text("Agora na $rua" + (r.localizacao.bairro?.let { ", $it" } ?: ""),
                            fontWeight = FontWeight.SemiBold, color = Cores.Texto, modifier = Modifier.padding(top = 4.dp))
                    }
                    r?.localizacao?.let {
                        Text("Posição do veículo atualizada ${Formato.haQuanto(it.registradaEm)}",
                            style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item {
                Cartao {
                    Text(enc.descricao, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Info("Destinatário", enc.destinatarioNome)
                    Info("Endereço", "${enc.endereco}, ${enc.bairro} · ${enc.cidade}/${enc.uf}")
                    if (r != null) {
                        Info("Rota", "${r.rotaCodigo} (${Rotulos.rota(r.rotaStatus).texto.lowercase()})")
                        Info("Entregador", r.entregador)
                        Info("Veículo", "${r.veiculoModelo} · ${r.veiculoPlaca}")
                    }
                    if (enc.tentativasEntrega > 0) Info("Tentativas", "${enc.tentativasEntrega} sem sucesso")
                }
            }
            item { Text("Acompanhamento", style = MaterialTheme.typography.titleMedium) }
            item {
                Cartao {
                    if (d.eventos.isEmpty()) Text("Sem eventos ainda.", color = Cores.TextoSuave)
                    d.eventos.asReversed().forEachIndexed { i, ev -> LinhaTempo(ev, primeira = i == 0, ultima = i == d.eventos.lastIndex) }
                }
            }
        }
    }
}

private fun previsao(d: Encomenda): String {
    val r = d.rastreio
    return when (d.encomenda.status) {
        "ENTREGUE" -> "Entregue ${r?.entregueEm?.let { "às " + Formato.hora(it) } ?: ""}".trim()
        "EM_TRANSITO" -> r?.previsaoChegadaEm?.let { "Chega por volta das ${Formato.hora(it)}" } ?: "A caminho"
        "ROTEIRIZADA" -> "Separada para a rota de entrega"
        "AGUARDANDO_ROTA" -> if (d.encomenda.tentativasEntrega > 0) "Voltou ao depósito: nova tentativa em breve" else "Preparando o envio"
        "CANCELADA" -> "Encomenda cancelada"
        else -> d.encomenda.status
    }
}

private fun detalhePrevisao(d: Encomenda): String? {
    val r = d.rastreio ?: return null
    if (r.previsaoMinutos == null) return null
    val antes = when (r.entregasAntes) {
        null -> ""
        0 -> "Você é a próxima entrega! · "
        1 -> "1 entrega antes da sua · "
        else -> "${r.entregasAntes} entregas antes da sua · "
    }
    return antes + "${Formato.km(r.distanciaKm)} · ~${Formato.minutos(r.previsaoMinutos)}"
}

/** Item da linha do tempo: bolinha colorida + traço ligando ao próximo. O mais recente fica em cima. */
@Composable
private fun LinhaTempo(ev: EventoEncomenda, primeira: Boolean, ultima: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(2.dp).height(6.dp).background(if (primeira) Cores.Borda.copy(alpha = 0f) else Cores.Borda))
            Box(Modifier.size(12.dp).background(Rotulos.evento(ev.tipo), CircleShape))
            Box(Modifier.width(2.dp).weight(1f).background(if (ultima) Cores.Borda.copy(alpha = 0f) else Cores.Borda))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.padding(bottom = 12.dp)) {
            Text(ev.descricao, style = MaterialTheme.typography.labelLarge, color = if (primeira) Cores.Texto else Cores.TextoSuave)
            ev.detalhe?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cores.Vermelho) }
            Text(Formato.dataHora(ev.ocorridoEm) + (ev.rotaCodigo?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
        }
    }
}
