package br.com.mba.fametro.kotlintransportadora.cliente

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.acesso.BotaoSair
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.EncomendaResumo
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.formato.Rotulos
import br.com.mba.fametro.kotlintransportadora.core.sessao.Sessao
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

/** Portal do Cliente: as encomendas da empresa (o back só devolve as do cliente do token). */
class EncomendasViewModel(private val c: AppContainer) : ViewModel() {
    var filtro by mutableStateOf<String?>(null)
        private set
    var estado by mutableStateOf<EstadoTela<List<EncomendaResumo>>>(EstadoTela.Carregando)
        private set
    var atualizando by mutableStateOf(false)
        private set

    fun filtrar(status: String?) {
        filtro = status
        estado = EstadoTela.Carregando
        carregar()
    }

    fun carregar(puxou: Boolean = false) {
        if (puxou) atualizando = true
        viewModelScope.launch {
            estado = try {
                EstadoTela.Pronto(c.chamar { encomendas(filtro) }.content)
            } catch (e: ApiErro) {
                EstadoTela.Erro(e.message)
            }
            atualizando = false
        }
    }
}

private val FILTROS = listOf(null to "Todas", "EM_TRANSITO" to "A caminho", "ROTEIRIZADA" to "Separadas",
    "AGUARDANDO_ROTA" to "Preparando", "ENTREGUE" to "Entregues")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendasTela(sessao: Sessao, aoAbrir: (String) -> Unit, aoVerAvisos: () -> Unit) {
    val vm = viewModelDe { EncomendasViewModel(it) }
    val c = container()
    LaunchedEffect(Unit) { vm.carregar() }

    // Android 13+: notificação precisa de permissão em tempo de execução (os avisos do webhook viram notificação)
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(topBar = {
        Topo("Minhas encomendas", sessao.nome, acoes = {
            IconButton(onClick = aoVerAvisos) { Icon(Icons.Filled.Notifications, contentDescription = "Avisos do webhook", tint = Cores.Marinho) }
            BotaoSair { c.sessao.sair() }
        })
    }, containerColor = Cores.Fundo) { m ->
        Column(Modifier.padding(m).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FILTROS.forEach { (status, rotulo) ->
                    FilterChip(selected = vm.filtro == status, onClick = { vm.filtrar(status) }, label = { Text(rotulo) })
                }
            }
            PullToRefreshBox(isRefreshing = vm.atualizando, onRefresh = { vm.carregar(puxou = true) }, modifier = Modifier.fillMaxSize()) {
                when (val e = vm.estado) {
                    EstadoTela.Carregando -> Carregando()
                    is EstadoTela.Erro -> TelaErro(e.mensagem) { vm.carregar() }
                    is EstadoTela.Pronto -> if (e.dado.isEmpty()) {
                        Vazio("Nenhuma encomenda aqui", "Quando a transportadora receber suas cargas elas aparecem nesta lista.", Icons.Filled.Inventory2)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(e.dado, key = { it.codigo }) { enc -> CartaoEncomenda(enc) { aoAbrir(enc.codigo) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoEncomenda(e: EncomendaResumo, aoClicar: () -> Unit) {
    Cartao(Modifier.clickable(onClick = aoClicar)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.descricao, style = MaterialTheme.typography.titleMedium)
                Text(e.codigo, style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
            }
            EtiquetaStatus(Rotulos.encomenda(e.status))
        }
        Spacer(Modifier.height(8.dp))
        Text("Para ${e.destinatarioNome} · ${e.bairro}, ${e.cidade}/${e.uf}", style = MaterialTheme.typography.bodyMedium)
        Text("Atualizada ${Formato.dataHora(e.atualizadaEm)}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
    }
}
