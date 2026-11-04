package br.com.mba.fametro.kotlintransportadora.entregador

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import br.com.mba.fametro.kotlintransportadora.MainActivity
import br.com.mba.fametro.kotlintransportadora.R
import br.com.mba.fametro.kotlintransportadora.RotaCertaApp
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.LotePosicoes
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Serviço em primeiro plano (notificação fixa) que segue rodando com a tela apagada ou o app em segundo plano:
 *  1. lê o GPS (LocationManager, sem Google Play Services) ou o simulador;
 *  2. filtra ([FiltroGps]) e grava na fila em arquivo ([FilaGps]);
 *  3. a cada 5 s envia o que estiver na fila em lote (POST /rotas/{id}/posicoes).
 * Ao parar (rota concluída ou "sair"), tenta esvaziar a fila antes de encerrar.
 */
class RastreadorService : LifecycleService() {

    private val container by lazy { (application as RotaCertaApp).container }
    private val filtro = FiltroGps()
    private var rotaId: Long = -1
    private var codigo: String = ""
    private var envio: Job? = null
    private var simulacao: Job? = null
    private var ouvinte: LocationListener? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACAO_INICIAR -> iniciar(intent.getLongExtra(EXTRA_ROTA, -1), intent.getStringExtra(EXTRA_CODIGO).orEmpty(),
                if (intent.hasExtra(EXTRA_SIMULAR)) intent.getBooleanExtra(EXTRA_SIMULAR, false) else null)
            ACAO_SIMULAR -> if (rotaId > 0) alternarSimulacao(intent.getBooleanExtra(EXTRA_SIMULAR, false)) else stopSelf()
            ACAO_PARAR -> if (rotaId > 0) parar() else stopSelf()
        }
        return START_REDELIVER_INTENT // se o sistema matar, volta com a mesma rota
    }

    /** [simular] nulo = não mexe no simulador (religar o rastreador ao reabrir o app). */
    private fun iniciar(id: Long, cod: String, simular: Boolean?) {
        if (id <= 0) { stopSelf(); return }
        val trocouDeRota = id != rotaId
        rotaId = id
        codigo = cod
        ServiceCompat.startForeground(this, NOTIFICACAO, notificacao(0), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) // ServiceCompat ignora o tipo abaixo do Android 10
        if (trocouDeRota) filtro.reiniciar()
        container.rastreio.update { it.copy(rotaId = id, ativo = true, erro = null) }
        if (envio == null) envio = lifecycleScope.launch {
            atualizarFila()
            while (isActive) {
                enviar()
                delay(INTERVALO_ENVIO_MS)
            }
        }
        if (ouvinte == null) ouvirGps()
        if (simular != null) alternarSimulacao(simular)
    }

    @SuppressLint("MissingPermission") // conferida logo abaixo
    private fun ouvirGps() {
        val permitido = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!permitido) {
            container.rastreio.update { it.copy(semPermissao = true) }
            return
        }
        val lm = getSystemService(LocationManager::class.java)
        val l = LocationListener { loc -> if (simulacao == null) registrar(loc.paraLeitura()) }
        ouvinte = l
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.isProviderEnabled(it) }
            .forEach { lm.requestLocationUpdates(it, 2_000L, 0f, l, Looper.getMainLooper()) }
        container.rastreio.update { it.copy(semPermissao = false) }
    }

    private fun registrar(l: Leitura) {
        if (!filtro.aceitar(l)) return
        val id = rotaId
        lifecycleScope.launch {
            val n = container.filaGps.adicionar(id, l.paraEnvio())
            container.rastreio.update { it.copy(naFila = n, ultimaPosicao = l.coordenada) }
        }
    }

    /** Liga/desliga o simulador (só existe no build debug). Enquanto simula, o GPS real é ignorado. */
    private fun alternarSimulacao(ligar: Boolean) {
        simulacao?.cancel()
        simulacao = null
        container.rastreio.update { it.copy(simulando = ligar) }
        if (!ligar) return
        simulacao = lifecycleScope.launch {
            val sim = SimuladorGps()
            var rota = runCatching { container.chamar { rota(rotaId) } }.getOrNull() ?: return@launch
            sim.preparar(rota, container.rastreio.value.ultimaPosicao)
            var tique = 0
            while (isActive) {
                if (tique++ % 5 == 0) { // de tempos em tempos vê quais paradas já foram baixadas
                    rota = runCatching { container.chamar { rota(rotaId) } }.getOrDefault(rota)
                    if (rota.status != "EM_ANDAMENTO") break
                }
                val pendentes = rota.paradas.filter { it.status == "PENDENTE" }.map { Coordenada(it.latitude, it.longitude) }
                val p = sim.proxima(pendentes) ?: break
                registrar(Leitura(p.latitude, p.longitude, Instant.now(), velocidadeKmH = 32.0, precisaoM = 5.0))
                delay(1_000)
            }
            container.rastreio.update { it.copy(simulando = false) }
            simulacao = null
        }
    }

    private suspend fun enviar() {
        val id = rotaId
        val lote = container.filaGps.lote(id)
        if (lote.isEmpty()) return
        try {
            container.chamar { enviarPosicoes(id, LotePosicoes(lote)) }
            container.filaGps.confirmar(id, lote.size)
            container.rastreio.update { it.copy(ultimoEnvioEm = Instant.now(), erro = null) }
        } catch (e: ApiErro) {
            when {
                e.semConexao -> container.rastreio.update { it.copy(erro = "Sem sinal: guardando posições para enviar depois.") }
                e.status == 404 || e.status == 422 -> {
                    // rota não aceita mais posições (cancelada / não é do entregador): não adianta insistir
                    container.filaGps.limpar(id)
                    container.rastreio.update { it.copy(erro = e.message) }
                }
                e.status == 401 -> parar()
                else -> container.rastreio.update { it.copy(erro = e.message) }
            }
        }
        atualizarFila()
    }

    private suspend fun atualizarFila() {
        val n = container.filaGps.tamanho(rotaId)
        container.rastreio.update { it.copy(naFila = n) }
        getSystemService(android.app.NotificationManager::class.java).notify(NOTIFICACAO, notificacao(n))
    }

    private fun parar() {
        ouvinte?.let { getSystemService(LocationManager::class.java).removeUpdates(it) }
        ouvinte = null
        simulacao?.cancel()
        envio?.cancel()
        lifecycleScope.launch {
            if (rotaId > 0) runCatching { enviar() } // última tentativa de esvaziar a fila
            container.rastreio.update { EstadoRastreio(rotaId = it.rotaId, naFila = it.naFila, ultimoEnvioEm = it.ultimoEnvioEm) }
            ServiceCompat.stopForeground(this@RastreadorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        ouvinte?.let { getSystemService(LocationManager::class.java).removeUpdates(it) }
        container.rastreio.update { it.copy(ativo = false, simulando = false) }
        super.onDestroy()
    }

    private fun notificacao(naFila: Int): Notification {
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, RotaCertaApp.CANAL_RASTREIO)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rota $codigo em andamento")
            .setContentText(if (naFila == 0) "Localização enviada ao Suporte e aos clientes" else "$naFila posição(ões) aguardando sinal")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(abrir)
            .build()
    }

    companion object {
        private const val NOTIFICACAO = 1
        private const val INTERVALO_ENVIO_MS = 5_000L
        private const val ACAO_INICIAR = "iniciar"
        private const val ACAO_PARAR = "parar"
        private const val ACAO_SIMULAR = "simular"
        private const val EXTRA_ROTA = "rota"
        private const val EXTRA_CODIGO = "codigo"
        private const val EXTRA_SIMULAR = "simular"

        fun iniciar(ctx: Context, rotaId: Long, codigo: String, simular: Boolean? = null) {
            val i = Intent(ctx, RastreadorService::class.java).setAction(ACAO_INICIAR)
                .putExtra(EXTRA_ROTA, rotaId).putExtra(EXTRA_CODIGO, codigo)
            if (simular != null) i.putExtra(EXTRA_SIMULAR, simular)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun simular(ctx: Context, ligar: Boolean) {
            ctx.startService(Intent(ctx, RastreadorService::class.java).setAction(ACAO_SIMULAR).putExtra(EXTRA_SIMULAR, ligar))
        }

        fun parar(ctx: Context) {
            ctx.startService(Intent(ctx, RastreadorService::class.java).setAction(ACAO_PARAR))
        }
    }
}

private fun Location.paraLeitura() = Leitura(
    latitude = latitude,
    longitude = longitude,
    em = Instant.ofEpochMilli(time),
    velocidadeKmH = if (hasSpeed()) speed * 3.6 else null,
    precisaoM = if (hasAccuracy()) accuracy.toDouble() else null,
)
