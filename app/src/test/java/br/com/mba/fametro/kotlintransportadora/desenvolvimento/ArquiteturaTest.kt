package br.com.mba.fametro.kotlintransportadora.desenvolvimento

import br.com.mba.fametro.kotlintransportadora.core.api.Arquitetura as Dados
import br.com.mba.fametro.kotlintransportadora.core.api.Camada
import br.com.mba.fametro.kotlintransportadora.core.api.Ferramenta
import br.com.mba.fametro.kotlintransportadora.core.api.Fluxo
import br.com.mba.fametro.kotlintransportadora.core.api.Passo
import br.com.mba.fametro.kotlintransportadora.core.api.Rede
import org.junit.Assert.assertEquals
import org.junit.Test

class ArquiteturaTest {

    private fun camada(id: String, grupo: String = "uso") = Camada(id, grupo, id, "", "")

    @Test
    fun `colunas na ordem do desenho, id desconhecido vai para a última`() {
        val cols = Arquitetura.colunas(listOf(camada("banco"), camada("api"), camada("novo"), camada("celular"), camada("github", "entrega")))
        assertEquals(listOf("Quem usa", "Entrada", "Servidor", "Dados e serviços"), cols.map { it.first })
        assertEquals(listOf("celular"), cols[0].second.map { it.id })
        assertEquals(emptyList<String>(), cols[1].second.map { it.id })
        assertEquals(listOf("banco", "novo"), cols[3].second.map { it.id })
    }

    @Test
    fun `fluxo acende as caixas citadas nos passos`() {
        val f = Fluxo("x", "", "", listOf(Passo("celular", "api", ""), Passo("api", "banco", "")))
        assertEquals(setOf("celular", "api", "banco"), Arquitetura.camadasDoFluxo(f))
        assertEquals(emptySet<String>(), Arquitetura.camadasDoFluxo(null))
    }

    @Test
    fun `ferramentas por categoria mantendo a ordem`() {
        val g = Arquitetura.porCategoria(listOf(Ferramenta("A", "Build", ""), Ferramenta("B", "Infra", ""), Ferramenta("C", "Build", "")))
        assertEquals(listOf("Build", "Infra"), g.map { it.first })
        assertEquals(listOf("A", "C"), g[0].second.map { it.nome })
    }

    @Test
    fun `tempo no ar`() {
        assertEquals("42 min", Arquitetura.tempoNoAr("2026-11-06T10:00:00Z", "2026-11-06T10:42:00Z"))
        assertEquals("2 h 5 min", Arquitetura.tempoNoAr("2026-11-06T10:00:00Z", "2026-11-06T12:05:00Z"))
        assertEquals("2 d 3 h", Arquitetura.tempoNoAr("2026-11-04T10:00:00Z", "2026-11-06T13:00:00Z"))
    }

    @Test
    fun `lê a resposta da API (versão vazia vira padrão)`() {
        val json = """{"ambiente":{"perfil":"homolog","java":"25","springBoot":"4.1.2","hibernate":"7","banco":"PostgreSQL 17",
            "migracao":"6 — x","servidor":"Linux","noArDesde":"2026-11-06T10:00:00Z","agora":"2026-11-06T10:01:00.123456789Z","extra":1},
            "camadas":[{"id":"api","grupo":"uso","nome":"API","onde":"VPS","papel":"p","tecnologias":[{"nome":"Java","papel":"x"}]}],
            "fluxos":[],"ferramentas":[]}"""
        val d = Rede.json.decodeFromString(Dados.serializer(), json)
        assertEquals("", d.camadas[0].tecnologias[0].versao)
        assertEquals("1 min", Arquitetura.tempoNoAr(d.ambiente.noArDesde, d.ambiente.agora))
    }
}
