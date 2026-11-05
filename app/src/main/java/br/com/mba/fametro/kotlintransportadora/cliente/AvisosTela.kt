package br.com.mba.fametro.kotlintransportadora.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mba.fametro.kotlintransportadora.core.api.AvisoWebhook
import br.com.mba.fametro.kotlintransportadora.core.formato.Etiqueta
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EtiquetaStatus
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Vazio
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.container

/**
 * Avisos do webhook: tudo que a transportadora mandou para o servidor do cliente, o mais novo em cima,
 * com o resultado (o servidor aceitou? qual HTTP?). Atualiza sozinho a cada 5 s ([VigiaAvisos]).
 */
@Composable
fun AvisosTela(aoVoltar: () -> Unit) {
    val vigia = container().avisos
    val e by vigia.estado.collectAsStateWithLifecycle()

    Scaffold(topBar = { Topo("Avisos do webhook", "atualiza a cada 5 s", aoVoltar = aoVoltar) }, containerColor = Cores.Fundo) { m ->
        when {
            !e.carregado -> Carregando(Modifier.padding(m))
            e.webhooks.isEmpty() && e.erro == null -> Vazio("Nenhum webhook cadastrado",
                "Cadastre a URL do seu sistema em POST /api/v1/webhooks para receber os avisos das encomendas.",
                Icons.Filled.NotificationsNone, Modifier.padding(m))
            else -> LazyColumn(Modifier.padding(m).fillMaxSize(), contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                e.erro?.let { item { Aviso(it) } }
                items(e.webhooks, key = { "w${it.id}" }) { w ->
                    Cartao {
                        Text("Seu servidor", style = MaterialTheme.typography.labelLarge, color = Cores.TextoSuave)
                        Text(w.url, style = MaterialTheme.typography.bodyMedium, color = Cores.Texto)
                        Text((if (w.ativo) "Ativo" else "Pausado") + " · " + w.eventos.sorted().joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                    }
                }
                if (e.avisos.isEmpty()) item { Text("Nenhum aviso enviado ainda.", color = Cores.TextoSuave) }
                items(e.avisos, key = { it.id }) { CartaoAviso(it) }
            }
        }
    }
}

@Composable
private fun CartaoAviso(a: AvisoWebhook) {
    val p = a.payload
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(listOfNotNull(p?.encomenda?.codigo, p?.descricao ?: a.evento).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            EtiquetaStatus(situacao(a))
        }
        p?.detalhe?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, fontWeight = FontWeight.SemiBold, color = if (a.evento == "NAO_ENTREGUE") Cores.Vermelho else Cores.AzulEscuro)
        }
        Spacer(Modifier.height(4.dp))
        val resultado = when {
            a.status == "ENTREGUE" -> "Seu servidor respondeu HTTP ${a.ultimoStatusHttp ?: "2xx"} às ${Formato.hora(a.entregueEm)}"
            a.status == "DESCARTADA" -> "Substituído por uma localização mais nova"
            a.ultimoErro != null -> "Tentativa ${a.tentativas}: ${a.ultimoErro}"
            else -> "Na fila de envio"
        }
        Text("${Formato.dataHora(a.criadaEm)} · $resultado", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
    }
}

private fun situacao(a: AvisoWebhook) = when (a.status) {
    "ENTREGUE" -> Etiqueta("Recebido", Cores.Verde)
    "FALHOU" -> Etiqueta("Falhou", Cores.Vermelho)
    "DESCARTADA" -> Etiqueta("Substituído", Cores.Cinza)
    else -> Etiqueta("Enviando", Cores.Laranja)
}
