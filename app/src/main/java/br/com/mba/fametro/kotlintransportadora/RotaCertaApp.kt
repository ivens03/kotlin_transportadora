package br.com.mba.fametro.kotlintransportadora

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.osmdroid.config.Configuration
import java.io.File

class RotaCertaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // osmdroid: o servidor de tiles do OSM exige User-Agent identificando o app; cache no diretório do app
        Configuration.getInstance().apply {
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(CANAL_RASTREIO, getString(R.string.canal_rastreio), NotificationManager.IMPORTANCE_LOW),
            // HIGH = aparece no topo da tela (heads-up), como mensagem nova
            NotificationChannel(CANAL_AVISOS, getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_HIGH),
        ))
    }

    companion object {
        const val CANAL_RASTREIO = "rastreio"
        const val CANAL_AVISOS = "avisos"
    }
}
