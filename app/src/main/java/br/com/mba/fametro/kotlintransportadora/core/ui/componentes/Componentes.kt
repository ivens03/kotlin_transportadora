package br.com.mba.fametro.kotlintransportadora.core.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.mba.fametro.kotlintransportadora.core.formato.Etiqueta
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores

/** Estado padrão de uma tela que busca dados na API. */
sealed interface EstadoTela<out T> {
    data object Carregando : EstadoTela<Nothing>
    data class Erro(val mensagem: String) : EstadoTela<Nothing>
    data class Pronto<T>(val dado: T) : EstadoTela<T>
}

/** Barra amarela do topo (marca do app), com voltar opcional e ações à direita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Topo(titulo: String, subtitulo: String? = null, aoVoltar: (() -> Unit)? = null, acoes: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = Cores.Marinho)
                if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Cores.Marinho.copy(alpha = .75f))
            }
        },
        navigationIcon = {
            if (aoVoltar != null) IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Cores.Marinho)
            }
        },
        actions = acoes,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Cores.Amarelo),
    )
}

@Composable
fun Cartao(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), content = conteudo)
    }
}

@Composable
fun EtiquetaStatus(etiqueta: Etiqueta, modifier: Modifier = Modifier) {
    Text(
        etiqueta.texto,
        modifier = modifier
            .background(etiqueta.cor.copy(alpha = .12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = etiqueta.cor,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cores.Azul) }
}

@Composable
fun Vazio(titulo: String, texto: String, icone: ImageVector = Icons.Filled.LocalShipping, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icone, null, tint = Cores.Cinza, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave, textAlign = TextAlign.Center)
    }
}

@Composable
fun TelaErro(mensagem: String, aoTentarDeNovo: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.ErrorOutline, null, tint = Cores.Vermelho, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensagem, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = aoTentarDeNovo) { Text("Tentar de novo") }
    }
}

/** Faixa de aviso (erro em vermelho, informação em azul). */
@Composable
fun Aviso(texto: String, erro: Boolean = true, modifier: Modifier = Modifier) {
    val cor = if (erro) Cores.Vermelho else Cores.Azul
    Text(
        texto,
        modifier = modifier.fillMaxWidth().background(cor.copy(alpha = .1f), RoundedCornerShape(6.dp)).padding(12.dp),
        color = if (erro) Color(0xFFB0202F) else Cores.AzulEscuro,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun BotaoPrincipal(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier, carregando: Boolean = false,
                   habilitado: Boolean = true, cor: Color = Cores.Azul) {
    Button(
        onClick = aoClicar,
        enabled = habilitado && !carregando,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cor),
    ) {
        if (carregando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotaoSecundario(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = aoClicar, modifier = modifier.fillMaxWidth()) { Text(texto, color = Cores.Azul) }
}

/** Linha "rótulo .... valor" usada nos cartões de detalhe. */
@Composable
fun Info(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(rotulo, color = Cores.TextoSuave, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Barra de progresso das entregas (sem o "pontinho" final do Material 3, que parecia progresso com 0%). */
@Composable
fun BarraProgresso(feitas: Int, total: Int, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else feitas.toFloat() / total },
        modifier = modifier.fillMaxWidth(),
        color = Cores.Verde,
        trackColor = Cores.Borda,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}
