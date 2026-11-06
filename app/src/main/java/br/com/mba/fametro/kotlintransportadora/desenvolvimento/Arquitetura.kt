package br.com.mba.fametro.kotlintransportadora.desenvolvimento

import br.com.mba.fametro.kotlintransportadora.core.api.Camada
import br.com.mba.fametro.kotlintransportadora.core.api.Ferramenta
import br.com.mba.fametro.kotlintransportadora.core.api.Fluxo
import java.time.Duration
import java.time.Instant

/** Regras da tela "Como funciona", sem Android (testáveis na JVM). As mesmas do painel web. */
object Arquitetura {

    /** Colunas do desenho do caminho do dado, de cima para baixo no celular. Id novo no JSON cai na última. */
    val COLUNAS = listOf(
        "Quem usa" to listOf("celular", "navegador"),
        "Entrada" to listOf("borda"),
        "Servidor" to listOf("api"),
        "Dados e serviços" to listOf("banco", "externos", "cliente"),
    )

    fun colunas(camadas: List<Camada>): List<Pair<String, List<Camada>>> {
        val uso = camadas.filter { it.grupo == "uso" }
        val conhecidos = COLUNAS.flatMap { it.second }.toSet()
        return COLUNAS.mapIndexed { i, (titulo, ids) ->
            val extras = if (i == COLUNAS.lastIndex) uso.filter { it.id !in conhecidos } else emptyList()
            titulo to ids.mapNotNull { id -> uso.find { it.id == id } } + extras
        }
    }

    /** Caixas que o fluxo usa (acendem no desenho). */
    fun camadasDoFluxo(f: Fluxo?): Set<String> = f?.passos?.flatMap { listOf(it.de, it.para) }?.toSet().orEmpty()

    /** Ferramentas agrupadas pela categoria, na ordem em que aparecem. */
    fun porCategoria(fs: List<Ferramenta>): List<Pair<String, List<Ferramenta>>> =
        fs.groupBy { it.categoria }.toList()

    /** "42 min", "2 h 5 min", "2 d 3 h". */
    fun tempoNoAr(desde: String, agora: String): String {
        val min = Duration.between(Instant.parse(desde), Instant.parse(agora)).toMinutes().coerceAtLeast(0)
        val d = min / 1440; val h = (min % 1440) / 60; val m = min % 60
        return when { d > 0 -> "$d d $h h"; h > 0 -> "$h h $m min"; else -> "$m min" }
    }

}
