package br.com.mba.fametro.kotlintransportadora.desenvolvimento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.acesso.BotaoSair
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Arquitetura as Dados
import br.com.mba.fametro.kotlintransportadora.core.api.Camada
import br.com.mba.fametro.kotlintransportadora.core.api.Passo
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.sessao.Sessao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EstadoTela
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.container
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

class ComoFuncionaViewModel(private val c: AppContainer) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<Dados>>(EstadoTela.Carregando); private set
    var fluxo by mutableStateOf("login"); private set
    var passo by mutableStateOf<Passo?>(null); private set
    var aberta by mutableStateOf<String?>(null); private set

    fun carregar() {
        viewModelScope.launch {
            estado = try { EstadoTela.Pronto(c.chamar { arquitetura() }) } catch (e: ApiErro) { EstadoTela.Erro(e.message) }
        }
    }

    fun escolherFluxo(id: String) { fluxo = id; passo = null }
    fun alternarPasso(p: Passo) { passo = if (passo == p) null else p }
    fun abrir(id: String) { aberta = if (aberta == id) null else id }
}

private fun icone(id: String): ImageVector = when (id) {
    "celular" -> Icons.Filled.PhoneAndroid; "navegador" -> Icons.Filled.Web; "borda" -> Icons.Filled.Security
    "api" -> Icons.Filled.Dns; "banco" -> Icons.Filled.Storage; "externos" -> Icons.Filled.Public
    "cliente" -> Icons.Filled.Webhook; "dev" -> Icons.Filled.Computer; "github" -> Icons.Filled.Commit
    "vps" -> Icons.Filled.Cloud; else -> Icons.Filled.Widgets
}

/**
 * Perfil DESENVOLVIMENTO: como o sistema funciona, do celular à VPS. Desenha GET /arquitetura — a mesma resposta
 * do painel web. Escolher um fluxo acende as caixas por onde o dado passa; tocar num passo acende só a origem e o
 * destino; tocar numa caixa abre os frameworks e as versões dela.
 */
@Composable
fun ComoFuncionaTela(sessao: Sessao) {
    val vm = viewModelDe { ComoFuncionaViewModel(it) }
    val c = container()
    LaunchedEffect(Unit) { vm.carregar() }

    Scaffold(topBar = {
        Topo("Como funciona", "${sessao.primeiroNome} · Desenvolvimento", acoes = {
            IconButton(onClick = vm::carregar) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = Cores.Azul) }
            BotaoSair { c.sessao.sair() }
        })
    }, containerColor = Cores.Fundo) { m ->
        when (val e = vm.estado) {
            EstadoTela.Carregando -> Carregando(Modifier.padding(m))
            is EstadoTela.Erro -> Box(Modifier.padding(m)) { TelaErro(e.mensagem) { vm.carregar() } }
            is EstadoTela.Pronto -> Conteudo(e.dado, vm, Modifier.padding(m))
        }
    }
}

@Composable
private fun Conteudo(d: Dados, vm: ComoFuncionaViewModel, modifier: Modifier) {
    val fluxo = d.fluxos.find { it.id == vm.fluxo }
    val acesas = vm.passo?.let { setOf(it.de, it.para) } ?: Arquitetura.camadasDoFluxo(fluxo)
    fun nome(id: String) = d.camadas.find { it.id == id }?.nome ?: id

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Text("Servidor agora", style = MaterialTheme.typography.titleMedium)
            Text("medido às ${Formato.hora(d.ambiente.agora)} · no ar há ${Arquitetura.tempoNoAr(d.ambiente.noArDesde, d.ambiente.agora)}",
                style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
            Spacer(Modifier.height(8.dp))
            listOf("Perfil" to d.ambiente.perfil, "Java" to d.ambiente.java, "Spring Boot" to d.ambiente.springBoot,
                "Hibernate" to d.ambiente.hibernate, "Banco" to d.ambiente.banco, "Migração (Flyway)" to d.ambiente.migracao,
                "Máquina" to d.ambiente.servidor).forEach { (r, v) ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(r, Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                    Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Cartao {
            Text("Caminho do dado", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.fluxos.forEach { f ->
                    FilterChip(selected = vm.fluxo == f.id, onClick = { vm.escolherFluxo(f.id) }, label = { Text(f.titulo) },
                        leadingIcon = if (f.id == "deploy") { { Icon(Icons.Filled.RocketLaunch, null, Modifier.size(18.dp)) } } else null)
                }
            }
            if (vm.fluxo == "deploy") {
                val entrega = d.camadas.filter { it.grupo == "entrega" }
                entrega.forEachIndexed { i, cam ->
                    Caixa(cam, acesa = true, aberta = vm.aberta == cam.id) { vm.abrir(cam.id) }
                    if (i < entrega.lastIndex) Seta()
                }
            } else {
                val colunas = Arquitetura.colunas(d.camadas)
                colunas.forEachIndexed { i, (titulo, camadas) ->
                    Text(titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = Cores.TextoSuave,
                        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        camadas.forEach { cam -> Caixa(cam, cam.id in acesas, vm.aberta == cam.id) { vm.abrir(cam.id) } }
                    }
                    if (i < colunas.lastIndex) Seta()
                }
            }
            Text("Toque numa caixa para ver os frameworks e as versões.", style = MaterialTheme.typography.bodySmall,
                color = Cores.TextoSuave, modifier = Modifier.padding(top = 8.dp))
        }

        fluxo?.let { f ->
            Cartao {
                Text(f.titulo, style = MaterialTheme.typography.titleMedium)
                Text(f.resumo, style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                Spacer(Modifier.height(8.dp))
                f.passos.forEachIndexed { i, p ->
                    val ativo = vm.passo == p
                    Row(Modifier.fillMaxWidth().background(if (ativo) Cores.AzulClaro else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { vm.alternarPasso(p) }.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(24.dp).background(Cores.Marinho, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(if (p.de == p.para) nome(p.de) else "${nome(p.de)} → ${nome(p.para)}", style = MaterialTheme.typography.labelMedium, color = Cores.AzulEscuro)
                            Text(p.oque, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text("Toque num passo para acender só a origem e o destino no desenho.", style = MaterialTheme.typography.bodySmall,
                    color = Cores.TextoSuave, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Cartao {
            Text("Ferramentas usadas no desenvolvimento", style = MaterialTheme.typography.titleMedium)
            Arquitetura.porCategoria(d.ferramentas).forEach { (categoria, itens) ->
                Text(categoria, style = MaterialTheme.typography.labelLarge, color = Cores.AzulEscuro, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                itens.forEach { f ->
                    Text(f.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(f.para, style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }

        Text("Vem de GET /api/v1/arquitetura — o painel web desenha a mesma resposta.", style = MaterialTheme.typography.bodySmall,
            color = Cores.TextoSuave, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Caixa(c: Camada, acesa: Boolean, aberta: Boolean, aoTocar: () -> Unit) {
    Surface(Modifier.fillMaxWidth().alpha(if (acesa || aberta) 1f else 0.45f).clickable(onClick = aoTocar),
        shape = RoundedCornerShape(12.dp), color = if (acesa) Cores.AzulClaro else Color.White, contentColor = Cores.Texto,
        border = BorderStroke(2.dp, if (acesa) Cores.Azul else Cores.Borda)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone(c.id), null, tint = Cores.Azul, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(c.nome, style = MaterialTheme.typography.titleSmall)
                    Text(c.onde, style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
                }
            }
            if (aberta) {
                Text(c.papel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                c.tecnologias.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (t.versao.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(t.versao, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Cores.AzulEscuro,
                                modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Text(t.papel, style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Seta() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.ArrowDownward, null, tint = Cores.Cinza)
    }
}
