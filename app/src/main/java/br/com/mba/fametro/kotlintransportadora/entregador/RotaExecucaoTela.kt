package br.com.mba.fametro.kotlintransportadora.entregador

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.BuildConfig
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Parada
import br.com.mba.fametro.kotlintransportadora.core.api.ParadaPatch
import br.com.mba.fametro.kotlintransportadora.core.api.Rota
import br.com.mba.fametro.kotlintransportadora.core.api.RotaPatch
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.formato.Rotulos
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoPrincipal
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EstadoTela
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EtiquetaStatus
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaOsm
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaRota
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.container
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Execução da rota pelo entregador: iniciar (liga o rastreador), seguir a próxima entrega no mapa e
 * dar baixa (entregue / não entregue + motivo). A rota conclui sozinha na última baixa (regra do back).
 * Recarrega a cada 15 s com a rota em andamento: distância e minutos até a próxima parada ficam atualizados.
 */
class RotaExecucaoViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<Rota>>(EstadoTela.Carregando)
        private set
    var enviando by mutableStateOf(false)
        private set
    var erroAcao by mutableStateOf<String?>(null)
    /** Já pediu para ligar o rastreador nesta tela (não pede de novo a cada recarga). */
    var rastreadorPedido = false
        private set

    init {
        carregar()
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                if ((estado as? EstadoTela.Pronto)?.dado?.status == "EM_ANDAMENTO") carregar(silencioso = true)
            }
        }
    }

    fun carregar(silencioso: Boolean = false) = viewModelScope.launch {
        try {
            estado = EstadoTela.Pronto(c.chamar { rota(id) })
        } catch (e: ApiErro) {
            if (!silencioso || estado !is EstadoTela.Pronto) estado = EstadoTela.Erro(e.message)
        }
    }

    fun iniciar(simular: Boolean) = acao {
        rastreadorPedido = true
        val r = c.chamar { alterarRota(id, RotaPatch("EM_ANDAMENTO")) }
        RastreadorService.iniciar(c.contexto, r.id, r.codigo, simular)
        r
    }

    /** App reaberto com a rota em andamento: religa o rastreador. */
    fun garantirRastreador(rota: Rota) {
        rastreadorPedido = true
        val r = c.rastreio.value
        if (rota.status == "EM_ANDAMENTO" && (!r.ativo || r.rotaId != rota.id)) {
            RastreadorService.iniciar(c.contexto, rota.id, rota.codigo)
        }
    }

    fun baixar(ordem: Int, entregue: Boolean, motivo: String?) = acao {
        val agora = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()
        val r = c.chamar { baixarParada(id, ordem, ParadaPatch(if (entregue) "ENTREGUE" else "NAO_ENTREGUE", motivo, agora)) }
        if (r.status == "CONCLUIDA") RastreadorService.parar(c.contexto)
        r
    }

    private fun acao(bloco: suspend () -> Rota) {
        if (enviando) return
        enviando = true
        erroAcao = null
        viewModelScope.launch {
            try {
                estado = EstadoTela.Pronto(bloco())
            } catch (e: ApiErro) {
                erroAcao = e.completa
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun RotaExecucaoTela(id: Long, aoVoltar: () -> Unit) {
    val vm = viewModelDe(chave = "rota-$id") { RotaExecucaoViewModel(it, id) }
    val c = container()
    val rastreio by c.rastreio.collectAsStateWithLifecycle()
    var simular by rememberSaveable { mutableStateOf(BuildConfig.SIMULADOR_GPS) }
    var depoisDaPermissao by remember { mutableStateOf<(() -> Unit)?>(null) }

    // pede localização (e notificação no Android 13+) antes de ligar o rastreador; sem permissão a rota anda, mas sem GPS
    val permissoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        depoisDaPermissao?.invoke()
        depoisDaPermissao = null
    }
    fun comPermissao(acao: () -> Unit) {
        depoisDaPermissao = acao
        permissoes.launch(buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray())
    }

    val rota = (vm.estado as? EstadoTela.Pronto)?.dado
    LaunchedEffect(rota?.status) {
        if (rota?.status == "EM_ANDAMENTO" && !vm.rastreadorPedido && (!rastreio.ativo || rastreio.rotaId != rota.id)) {
            comPermissao { vm.garantirRastreador(rota) }
        }
    }

    Scaffold(
        topBar = { Topo(rota?.codigo ?: "Rota", rota?.let { Rotulos.rota(it.status).texto }, aoVoltar = aoVoltar) },
        containerColor = Cores.Fundo,
    ) { margem ->
        when (val e = vm.estado) {
            EstadoTela.Carregando -> Carregando(Modifier.padding(margem))
            is EstadoTela.Erro -> Box(Modifier.padding(margem)) { TelaErro(e.mensagem) { vm.carregar() } }
            is EstadoTela.Pronto -> {
                val r = e.dado
                val veiculo = rastreio.ultimaPosicao.takeIf { rastreio.rotaId == r.id }
                    ?: r.ultimaPosicao?.let { Coordenada(it.latitude, it.longitude) }
                Column(Modifier.padding(margem).fillMaxSize()) {
                    MapaOsm(
                        modifier = Modifier.fillMaxWidth().weight(0.42f),
                        linhas = listOf(MapaRota.trajeto(r)),
                        marcadores = MapaRota.marcadores(r),
                        veiculo = veiculo,
                        seguirVeiculo = r.status == "EM_ANDAMENTO",
                        chaveEnquadrar = r.id,
                    )
                    LazyColumn(Modifier.weight(0.58f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        vm.erroAcao?.let { item { Aviso(it) } }
                        when (r.status) {
                            "PLANEJADA" -> item {
                                Cartao {
                                    Text("${r.paradas.size} entregas · ${Formato.km(r.distanciaEstimadaKm)} · ${Formato.minutos(r.duracaoEstimadaMin)}",
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("Veículo ${r.veiculoModelo} ${r.veiculoPlaca}", color = Cores.TextoSuave)
                                    Spacer(Modifier.height(12.dp))
                                    if (BuildConfig.SIMULADOR_GPS) LinhaSimulador(simular) { simular = it }
                                    BotaoPrincipal("Iniciar rota", { comPermissao { vm.iniciar(simular) } }, carregando = vm.enviando, cor = Cores.Verde)
                                    Text("Ao iniciar, sua localização é enviada ao Suporte e aos clientes até a última entrega.",
                                        style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                            "EM_ANDAMENTO" -> {
                                item { StatusGps(rastreio, r.id, simular = rastreio.simulando, aoSimular = { RastreadorService.simular(c.contexto, it) }) }
                                r.paradas.firstOrNull { it.ordem == r.proximaParada }?.let { p ->
                                    item { ProximaParada(r, p, vm.enviando, aoEntregar = { vm.baixar(p.ordem, true, null) },
                                        aoNaoEntregar = { motivo -> vm.baixar(p.ordem, false, motivo) }) }
                                }
                            }
                            else -> item { Resumo(r) }
                        }
                        item { Text("Paradas (${r.progresso.entregues + r.progresso.naoEntregues}/${r.progresso.totalParadas})",
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                        items(r.paradas, key = { it.ordem }) { p -> LinhaParada(p, proxima = p.ordem == r.proximaParada) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaSimulador(ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Simular deslocamento", style = MaterialTheme.typography.labelLarge)
            Text("Só no build de teste: o veículo anda sozinho pelo trajeto.", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
        }
        Switch(checked = ligado, onCheckedChange = aoMudar)
    }
}

@Composable
private fun StatusGps(r: EstadoRastreio, rotaId: Long, simular: Boolean, aoSimular: (Boolean) -> Unit) {
    val ativo = r.ativo && r.rotaId == rotaId
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ativo) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, null, tint = if (ativo) Cores.Verde else Cores.Vermelho)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(when {
                    r.semPermissao -> "Sem permissão de localização"
                    !ativo -> "Rastreamento desligado"
                    simular -> "Simulando deslocamento"
                    else -> "Rastreamento ligado"
                }, style = MaterialTheme.typography.labelLarge)
                Text("${r.naFila} na fila · último envio ${r.ultimoEnvioEm?.let { Formato.haQuanto(it.toString()) } ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
            }
        }
        r.erro?.let { Spacer(Modifier.height(8.dp)); Aviso(it, erro = !it.startsWith("Sem sinal")) }
        if (BuildConfig.SIMULADOR_GPS && ativo) {
            Spacer(Modifier.height(8.dp))
            LinhaSimulador(simular, aoSimular)
        }
    }
}

@Composable
private fun ProximaParada(r: Rota, p: Parada, enviando: Boolean, aoEntregar: () -> Unit, aoNaoEntregar: (String) -> Unit) {
    val ctx = LocalContext.current
    var perguntarMotivo by remember { mutableStateOf(false) }
    Cartao {
        Text("PRÓXIMA ENTREGA", style = MaterialTheme.typography.labelLarge, color = Cores.Laranja)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            Numero(p.ordem, Cores.Laranja)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.destinatarioNome, style = MaterialTheme.typography.titleMedium)
                Text(p.endereco, style = MaterialTheme.typography.bodyMedium)
                Text("${p.bairro} · ${p.cidade}/${p.uf}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                Text("${p.cargaCodigo} · ${p.descricao}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
            }
        }
        if (r.distanciaAteProximaParadaKm != null) {
            Spacer(Modifier.height(8.dp))
            Text("${Formato.km(r.distanciaAteProximaParadaKm)} · cerca de ${Formato.minutos(r.minutosAteProximaParada)}",
                fontWeight = FontWeight.SemiBold, color = Cores.AzulEscuro)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                // abre o app de navegação instalado (Google Maps, Waze...) com o destino
                val uri = Uri.parse("geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}(${Uri.encode(p.destinatarioNome)})")
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Directions, null); Spacer(Modifier.width(4.dp)); Text("Navegar")
            }
            p.destinatarioTelefone?.let { tel ->
                OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) } },
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Phone, null); Spacer(Modifier.width(4.dp)); Text("Ligar")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BotaoPrincipal("Entregue", aoEntregar, carregando = enviando, cor = Cores.Verde)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { perguntarMotivo = true }, enabled = !enviando, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cores.Vermelho), shape = RoundedCornerShape(6.dp)) {
            Icon(Icons.Filled.Close, null); Spacer(Modifier.width(4.dp)); Text("Não entregue")
        }
    }
    if (perguntarMotivo) DialogoMotivo(aoConfirmar = { perguntarMotivo = false; aoNaoEntregar(it) }, aoCancelar = { perguntarMotivo = false })
}

private val MOTIVOS = listOf("Destinatário ausente", "Endereço não encontrado", "Recusado pelo destinatário", "Local fechado", "Outro")

@Composable
private fun DialogoMotivo(aoConfirmar: (String) -> Unit, aoCancelar: () -> Unit) {
    var escolhido by remember { mutableStateOf(MOTIVOS.first()) }
    var outro by remember { mutableStateOf("") }
    val motivo = if (escolhido == "Outro") outro.trim() else escolhido
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Por que não entregou?") },
        text = {
            Column {
                MOTIVOS.forEach { m ->
                    Row(Modifier.fillMaxWidth().selectable(selected = escolhido == m, onClick = { escolhido = m }).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = escolhido == m, onClick = { escolhido = m })
                        Text(m)
                    }
                }
                if (escolhido == "Outro") OutlinedTextField(outro, { if (it.length <= 300) outro = it }, label = { Text("Motivo") },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { aoConfirmar(motivo) }, enabled = motivo.isNotBlank()) { Text("Confirmar", color = Cores.Vermelho) } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun Resumo(r: Rota) {
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (r.status == "CONCLUIDA") Icons.Filled.CheckCircle else Icons.Filled.PlayArrow, null,
                tint = Rotulos.rota(r.status).cor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (r.status == "CONCLUIDA") "Rota concluída" else "Rota ${Rotulos.rota(r.status).texto.lowercase()}",
                style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text("${r.progresso.entregues} entregue(s) · ${r.progresso.naoEntregues} não entregue(s)")
        r.concluidaEm?.let { Text("Concluída em ${Formato.dataHora(it)}", color = Cores.TextoSuave) }
    }
}

@Composable
private fun LinhaParada(p: Parada, proxima: Boolean) {
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Numero(p.ordem, if (proxima) Cores.Laranja else Rotulos.parada(p.status).cor)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.destinatarioNome, style = MaterialTheme.typography.labelLarge)
                Text("${p.endereco} · ${p.bairro}", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                p.motivo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cores.Vermelho) }
            }
            EtiquetaStatus(Rotulos.parada(p.status))
        }
    }
}

@Composable
fun Numero(n: Int, cor: Color) {
    Box(Modifier.size(28.dp).background(cor, CircleShape), contentAlignment = Alignment.Center) {
        Text(n.toString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

