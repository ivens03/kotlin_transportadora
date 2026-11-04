package br.com.mba.fametro.kotlintransportadora.core.ui.componentes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Linha no mapa: trajeto planejado (tracejado) ou rastro percorrido (contínuo). */
data class LinhaMapa(val pontos: List<Coordenada>, val cor: Color, val larguraPx: Float = 8f, val tracejada: Boolean = false)

/** Marcador redondo com texto curto dentro (número da parada, "CD", ...). */
data class MarcadorMapa(val coordenada: Coordenada, val rotulo: String, val cor: Color, val titulo: String? = null)

/**
 * Mapa OpenStreetMap (osmdroid, sem chave de API) dentro do Compose via AndroidView.
 * Enquadra tudo na primeira vez (e quando {@code chaveEnquadrar} muda); depois respeita o zoom do usuário.
 * Com {@code seguirVeiculo}, centraliza no veículo a cada atualização.
 */
@Composable
fun MapaOsm(
    modifier: Modifier = Modifier,
    linhas: List<LinhaMapa> = emptyList(),
    marcadores: List<MarcadorMapa> = emptyList(),
    veiculo: Coordenada? = null,
    seguirVeiculo: Boolean = false,
    chaveEnquadrar: Any? = Unit,
) {
    val contexto = LocalContext.current
    val mapa = remember { criarMapa(contexto) }
    val enquadrado = remember { arrayOf<Any?>(null) }
    val ciclo = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            mapa.onDetach()
        }
    }

    AndroidView(modifier = modifier.clipToBounds(), // sem isso o osmdroid desenha a grade fora da área do mapa
        factory = { mapa }, update = { m ->
        m.overlays.clear()
        linhas.filter { it.pontos.size > 1 }.forEach { l ->
            m.overlays += Polyline(m).apply {
                setPoints(l.pontos.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.color = l.cor.toArgb()
                outlinePaint.strokeWidth = l.larguraPx
                outlinePaint.strokeCap = Paint.Cap.ROUND
                if (l.tracejada) outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 14f), 0f)
                setOnClickListener { _, _, _ -> false }
            }
        }
        marcadores.forEach { mk ->
            m.overlays += Marker(m).apply {
                position = GeoPoint(mk.coordenada.latitude, mk.coordenada.longitude)
                icon = BitmapDrawable(m.resources, bolinha(m.context, mk.rotulo, mk.cor))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = mk.titulo
                setInfoWindow(null)
            }
        }
        veiculo?.let { v ->
            m.overlays += Marker(m).apply {
                position = GeoPoint(v.latitude, v.longitude)
                icon = BitmapDrawable(m.resources, bolinha(m.context, "▲", Cores.Marinho, borda = Cores.Amarelo, tamanhoDp = 30f))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
            }
        }
        m.invalidate()

        val todos = linhas.flatMap { it.pontos } + marcadores.map { it.coordenada } + listOfNotNull(veiculo)
        if (todos.isNotEmpty() && enquadrado[0] != chaveEnquadrar) {
            enquadrado[0] = chaveEnquadrar
            enquadrar(m, todos)
        } else if (seguirVeiculo && veiculo != null) {
            m.controller.animateTo(GeoPoint(veiculo.latitude, veiculo.longitude))
        }
    })
}

private fun criarMapa(contexto: Context) = MapView(contexto).apply {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    isTilesScaledToDpi = true
    minZoomLevel = 4.0
    maxZoomLevel = 19.0
    controller.setZoom(13.0)
    controller.setCenter(GeoPoint(-3.7319, -38.5267)) // Fortaleza, até os dados chegarem
}

private fun enquadrar(m: MapView, pontos: List<Coordenada>) {
    val acao = {
        if (pontos.size == 1) {
            m.controller.setZoom(16.0)
            m.controller.setCenter(GeoPoint(pontos[0].latitude, pontos[0].longitude))
        } else {
            val caixa = BoundingBox.fromGeoPoints(pontos.map { GeoPoint(it.latitude, it.longitude) })
            m.zoomToBoundingBox(caixa.increaseByScale(1.3f), false, 64)
        }
    }
    if (m.width > 0 && m.height > 0) acao() else m.addOnFirstLayoutListener { _, _, _, _, _ -> acao() }
}

/** Desenha um círculo colorido com borda branca e um texto curto no meio. */
private fun bolinha(contexto: Context, texto: String, cor: Color, borda: Color = Color.White, tamanhoDp: Float = 26f): Bitmap {
    val d = contexto.resources.displayMetrics.density
    val tam = (tamanhoDp * d).toInt()
    val bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = borda.toArgb(); c.drawCircle(tam / 2f, tam / 2f, tam / 2f, p)
    p.color = cor.toArgb(); c.drawCircle(tam / 2f, tam / 2f, tam / 2f - 2.5f * d, p)
    p.color = android.graphics.Color.WHITE
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.DEFAULT_BOLD
    p.textSize = (if (texto.length > 2) 9f else 12f) * d
    c.drawText(texto, tam / 2f, tam / 2f - (p.descent() + p.ascent()) / 2, p)
    return bmp
}
