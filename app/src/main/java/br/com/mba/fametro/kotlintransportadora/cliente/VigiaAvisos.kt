package br.com.mba.fametro.kotlintransportadora.cliente

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.MainActivity
import br.com.mba.fametro.kotlintransportadora.R
import br.com.mba.fametro.kotlintransportadora.RotaCertaApp
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.AvisoWebhook
import br.com.mba.fametro.kotlintransportadora.core.api.Webhook
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** O que a tela de avisos mostra. [carregado] = já fez a 1ª leitura (antes disso, "carregando"). */
data class EstadoAvisos(
    val carregado: Boolean = false,
    val webhooks: List<Webhook> = emptyList(),
    val avisos: List<AvisoWebhook> = emptyList(),
    val erro: String? = null,
)

/**
 * Acompanha os avisos que a transportadora mandou para o webhook do cliente e solta uma notificação no
 * celular para cada aviso novo que o servidor do cliente **aceitou** (status ENTREGUE = respondeu 2xx).
 *
 * Como funciona: o webhook é servidor → servidor (a API faz POST na URL do cliente); o celular não recebe esse
 * POST. Então o app lê, a cada [INTERVALO_MS], o histórico de envios (`GET /webhooks/{id}/entregas`), que guarda
 * o corpo exato que foi enviado. O que o servidor do cliente recebeu é o que aparece na notificação.
 * Roda enquanto o cliente está logado com o app aberto (inclusive em segundo plano, até o Android fechar o app).
 */
class VigiaAvisos(private val c: AppContainer) {

    private val _estado = MutableStateFlow(EstadoAvisos())
    val estado: StateFlow<EstadoAvisos> = _estado.asStateFlow()

    private var job: Job? = null
    /**
     * Ids que já terminaram (entregues, falhos, descartados). Nulo até a 1ª leitura: o histórico antigo não
     * notifica; um aviso ainda PENDENTE notifica quando o servidor do cliente aceitar.
     */
    private var vistos: Set<Long>? = null

    fun iniciar() {
        if (job?.isActive == true) return
        job = c.escopo.launch {
            while (isActive) {
                ler()
                delay(INTERVALO_MS)
            }
        }
    }

    fun parar() {
        job?.cancel()
        job = null
        vistos = null
        _estado.value = EstadoAvisos()
    }

    /** Leitura na hora (puxar para atualizar na tela). */
    fun atualizar() = c.escopo.launch { ler() }

    private suspend fun ler() {
        try {
            val webhooks = c.chamar { webhooks() }.content
            val avisos = webhooks.flatMap { w -> c.chamar { avisosDoWebhook(w.id) }.content }
                .sortedByDescending { it.criadaEm } // ISO-8601 em UTC: ordem de texto = ordem de tempo
            val anteriores = vistos
            if (anteriores != null) novosEntregues(anteriores, avisos).asReversed().forEach(::notificar)
            vistos = (anteriores ?: emptySet()) + avisos.filter { it.status == "ENTREGUE" || it.status == "DESCARTADA" || it.status == "FALHOU" }.map { it.id }
            _estado.value = EstadoAvisos(true, webhooks, avisos)
        } catch (e: ApiErro) {
            _estado.value = _estado.value.copy(carregado = true, erro = e.message)
        }
    }

    private fun notificar(a: AvisoWebhook) {
        val ctx = c.contexto
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val abrir = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val (titulo, texto) = textoNotificacao(a)
        val n = NotificationCompat.Builder(ctx, RotaCertaApp.CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(ctx).notify(idNotificacao(a), n)
    }

    companion object {
        const val INTERVALO_MS = 5_000L

        /** Avisos que o servidor do cliente aceitou e que ainda não tinham sido vistos (mais recentes primeiro). */
        fun novosEntregues(vistos: Set<Long>, avisos: List<AvisoWebhook>): List<AvisoWebhook> =
            avisos.filter { it.status == "ENTREGUE" && it.id !in vistos && it.evento != "PING" }

        fun textoNotificacao(a: AvisoWebhook): Pair<String, String> {
            val p = a.payload
            val codigo = p?.encomenda?.codigo
            val titulo = listOfNotNull(codigo, p?.descricao ?: a.evento).joinToString(" · ")
            val texto = p?.detalhe ?: p?.encomenda?.let { "Para ${it.destinatarioNome}" } ?: "Aviso ${a.evento}"
            return titulo to texto
        }

        /** Mesmo id para a mesma encomenda: a localização nova substitui a anterior. */
        fun idNotificacao(a: AvisoWebhook): Int = (a.payload?.encomenda?.codigo ?: a.id.toString()).hashCode()
    }
}
