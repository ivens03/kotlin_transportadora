package br.com.mba.fametro.kotlintransportadora.suporte

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
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

/** Suporte no celular: só acompanhar as rotas (montar rota e cadastrar carga continuam no painel web). */
class RotasAcompanharViewModel(private val c: AppContainer) : ViewModel() {
    var filtro by mutableStateOf<String?>("EM_ANDAMENTO")
        private set
    var estado by mutableStateOf<EstadoTela<List<RotaResumo>>>(EstadoTela.Carregando)
        private set
    var atualizando by mutableStateOf(false)
        private set

    fun filtrar(status: String?) { filtro = status; estado = EstadoTela.Carregando; carregar() }

    fun carregar(puxou: Boolean = false) {
        if (puxou) atualizando = true
        viewModelScope.launch {
            estado = try { EstadoTela.Pronto(c.chamar { rotas(filtro) }.content) } catch (e: ApiErro) { EstadoTela.Erro(e.message) }
            atualizando = false
        }
    }
}

private val FILTROS = listOf("EM_ANDAMENTO" to "Em andamento", "PLANEJADA" to "Planejadas", "CONCLUIDA" to "Concluídas", null to "Todas")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotasAcompanharTela(sessao: Sessao, aoAbrir: (Long) -> Unit, aoEmpresas: () -> Unit, aoNovaEntrega: () -> Unit) {
    val vm = viewModelDe { RotasAcompanharViewModel(it) }
    val c = container()
    LaunchedEffect(Unit) { vm.carregar() }

    Scaffold(topBar = { Topo("Acompanhar rotas", "${sessao.primeiroNome} · Suporte", acoes = {
            IconButton(onClick = aoEmpresas) { Icon(Icons.Filled.Storefront, contentDescription = "Empresas", tint = Cores.Azul) }
            BotaoSair { c.sessao.sair() }
        }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = aoNovaEntrega, containerColor = Cores.Laranja, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) }, text = { Text("Nova entrega") })
        },
        containerColor = Cores.Fundo) { m ->
        Column(Modifier.padding(m).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FILTROS.forEach { (s, rotulo) -> FilterChip(selected = vm.filtro == s, onClick = { vm.filtrar(s) }, label = { Text(rotulo) }) }
            }
            PullToRefreshBox(isRefreshing = vm.atualizando, onRefresh = { vm.carregar(true) }, modifier = Modifier.fillMaxSize()) {
                when (val e = vm.estado) {
                    EstadoTela.Carregando -> Carregando()
                    is EstadoTela.Erro -> TelaErro(e.mensagem) { vm.carregar() }
                    is EstadoTela.Pronto -> if (e.dado.isEmpty()) Vazio("Nenhuma rota", "Não há rotas com esse status agora.", Icons.Filled.Map)
                    else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(e.dado, key = { it.id }) { r ->
                            Cartao(Modifier.clickable { aoAbrir(r.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.codigo, style = MaterialTheme.typography.titleMedium)
                                        Text("${r.entregadorNome} · ${r.veiculoPlaca}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                                    }
                                    EtiquetaStatus(Rotulos.rota(r.status))
                                }
                                Spacer(Modifier.height(8.dp))
                                BarraProgresso(r.paradasBaixadas, r.totalParadas)
                                Text("${r.paradasBaixadas}/${r.totalParadas} paradas · ${Formato.km(r.distanciaEstimadaKm)}" +
                                    (r.iniciadaEm?.let { " · saiu ${Formato.hora(it)}" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
