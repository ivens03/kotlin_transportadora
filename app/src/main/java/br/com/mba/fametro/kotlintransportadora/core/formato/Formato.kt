package br.com.mba.fametro.kotlintransportadora.core.formato

import androidx.compose.ui.graphics.Color
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Textos e formatos da tela (pt-BR, fuso do celular). */
object Formato {
    private val BR = Locale.forLanguageTag("pt-BR")
    private val DATA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm", BR).withZone(ZoneId.systemDefault())
    private val HORA = DateTimeFormatter.ofPattern("HH:mm", BR).withZone(ZoneId.systemDefault())

    fun instante(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }
    fun dataHora(iso: String?): String = instante(iso)?.let(DATA_HORA::format) ?: "—"
    fun hora(iso: String?): String = instante(iso)?.let(HORA::format) ?: "—"

    fun km(valor: Double?): String = valor?.let {
        NumberFormat.getNumberInstance(BR).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }.format(it) + " km"
    } ?: "—"

    fun minutos(m: Int?): String = when {
        m == null -> "—"
        m < 60 -> "$m min"
        else -> "${m / 60} h ${m % 60} min"
    }

    /** "há 12 s", "há 3 min" — para mostrar se o GPS está atualizado. */
    fun haQuanto(iso: String?, agora: Instant = Instant.now()): String {
        val i = instante(iso) ?: return "—"
        val s = Duration.between(i, agora).seconds.coerceAtLeast(0)
        return when {
            s < 60 -> "há $s s"
            s < 3600 -> "há ${s / 60} min"
            else -> "há ${s / 3600} h"
        }
    }
}

/** Rótulo e cor de cada status que vem da API. */
data class Etiqueta(val texto: String, val cor: Color)

object Rotulos {
    fun rota(status: String) = when (status) {
        "PLANEJADA" -> Etiqueta("Planejada", Cores.Azul)
        "EM_ANDAMENTO" -> Etiqueta("Em andamento", Cores.Laranja)
        "CONCLUIDA" -> Etiqueta("Concluída", Cores.Verde)
        "CANCELADA" -> Etiqueta("Cancelada", Cores.Cinza)
        else -> Etiqueta(status, Cores.Cinza)
    }

    fun parada(status: String) = when (status) {
        "PENDENTE" -> Etiqueta("Pendente", Cores.Azul)
        "ENTREGUE" -> Etiqueta("Entregue", Cores.Verde)
        "NAO_ENTREGUE" -> Etiqueta("Não entregue", Cores.Vermelho)
        else -> Etiqueta(status, Cores.Cinza)
    }

    fun encomenda(status: String) = when (status) {
        "AGUARDANDO_ROTA" -> Etiqueta("Preparando envio", Cores.Cinza)
        "ROTEIRIZADA" -> Etiqueta("Separada para entrega", Cores.Azul)
        "EM_TRANSITO" -> Etiqueta("A caminho", Cores.Laranja)
        "ENTREGUE" -> Etiqueta("Entregue", Cores.Verde)
        "CANCELADA" -> Etiqueta("Cancelada", Cores.Vermelho)
        else -> Etiqueta(status, Cores.Cinza)
    }

    fun evento(tipo: String) = when (tipo) {
        "ENTREGUE" -> Cores.Verde
        "NAO_ENTREGUE", "CANCELADA" -> Cores.Vermelho
        "SAIU_PARA_ENTREGA" -> Cores.Laranja
        "AGUARDANDO_NOVA_ROTA" -> Cores.Cinza
        else -> Cores.Azul
    }
}
