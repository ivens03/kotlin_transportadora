package br.com.mba.fametro.kotlintransportadora.entregador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.acesso.BotaoSair
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.RotaResumo
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.formato.Rotulos
import br.com.mba.fametro.kotlintransportadora.core.sessao.Sessao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BarraProgresso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EstadoTela
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EtiquetaStatus
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Vazio
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.container
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

/** Rotas do entregador (o back só devolve as dele). Em andamento primeiro, depois as planejadas. */
class MinhasRotasViewModel(private val c: AppContainer) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<List<RotaResumo>>>(EstadoTela.Carregando)
        private set
    var atualizando by mutableStateOf(false)
        private set

    private val ordem = listOf("EM_ANDAMENTO", "PLANEJADA", "CONCLUIDA", "CANCELADA")

    fun carregar(puxou: Boolean = false) {
        if (puxou) atualizando = true
        viewModelScope.launch {
            estado = try {
                EstadoTela.Pronto(c.chamar { rotas() }.content.sortedBy { ordem.indexOf(it.status) })
            } catch (e: ApiErro) {
                if (estado is EstadoTela.Pronto && puxou) estado else EstadoTela.Erro(e.message)
            }
            atualizando = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinhasRotasTela(sessao: Sessao, aoAbrir: (Long) -> Unit) {
    val vm = viewModelDe { MinhasRotasViewModel(it) }
    val c = container()
    LaunchedEffect(Unit) { vm.carregar() }

    Scaffold(
        topBar = {
            Topo("Minhas rotas", "Olá, ${sessao.primeiroNome} · ${sessao.matricula}", acoes = {
                BotaoSair {
                    RastreadorService.parar(c.contexto)
                    c.sessao.sair()
                }
            })
        },
        containerColor = Cores.Fundo,
    ) { margem ->
        PullToRefreshBox(isRefreshing = vm.atualizando, onRefresh = { vm.carregar(puxou = true) }, modifier = Modifier.padding(margem).fillMaxSize()) {
            when (val e = vm.estado) {
                EstadoTela.Carregando -> Carregando()
                is EstadoTela.Erro -> TelaErro(e.mensagem) { vm.carregar() }
                is EstadoTela.Pronto -> if (e.dado.isEmpty()) {
                    Vazio("Nenhuma rota para você", "Quando o Suporte montar sua rota ela aparece aqui. Puxe para atualizar.", Icons.Filled.Route)
                } else {
                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(e.dado, key = { it.id }) { r -> CartaoRota(r) { aoAbrir(r.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoRota(r: RotaResumo, aoClicar: () -> Unit) {
    Cartao(Modifier.clickable(onClick = aoClicar)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.codigo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            EtiquetaStatus(Rotulos.rota(r.status))
        }
        Spacer(Modifier.height(8.dp))
        Text("${r.paradasBaixadas} de ${r.totalParadas} entregas · ${Formato.km(r.distanciaEstimadaKm)} · ${Formato.minutos(r.duracaoEstimadaMin)}",
            color = Cores.TextoSuave, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        BarraProgresso(r.paradasBaixadas, r.totalParadas)
        Spacer(Modifier.height(8.dp))
        Column {
            Text("Veículo ${r.veiculoPlaca} · criada ${Formato.dataHora(r.criadaEm)}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
        }
    }
}
