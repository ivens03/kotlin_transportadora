package br.com.mba.fametro.kotlintransportadora.cliente

import br.com.mba.fametro.kotlintransportadora.core.api.AvisoPayload
import br.com.mba.fametro.kotlintransportadora.core.api.AvisoWebhook
import br.com.mba.fametro.kotlintransportadora.core.api.EncomendaResumo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Quais avisos do webhook viram notificação e com que texto. */
class VigiaAvisosTest {

    private val enc = EncomendaResumo("CRG-000001", "Kit de treino", "EM_TRANSITO", "Academia Gaviões", "Av. Bezerra de Menezes, 1496",
        "São Gerardo", "Fortaleza", "CE", -3.73, -38.55, 0, "2026-11-05T12:00:00Z", "2026-11-05T12:00:00Z")

    private fun aviso(id: Long, status: String, evento: String = "LOCALIZACAO", detalhe: String? = null) = AvisoWebhook(
        id = id, evento = evento, status = status, criadaEm = "2026-11-05T12:00:%02dZ".format(id),
        payload = AvisoPayload(evento, "Localização do veículo atualizada", "2026-11-05T12:00:00Z", enc, detalhe = detalhe),
    )

    @Test
    fun `so notifica o que o servidor do cliente aceitou e ainda nao tinha visto`() {
        val lista = listOf(aviso(4, "ENTREGUE"), aviso(3, "PENDENTE"), aviso(2, "DESCARTADA"), aviso(1, "ENTREGUE"),
            aviso(5, "ENTREGUE", evento = "PING"))
        assertEquals(listOf(4L), VigiaAvisos.novosEntregues(setOf(1L), lista).map { it.id })
    }

    @Test
    fun `texto da notificacao usa a frase do webhook com rua e quanto falta`() {
        val (titulo, texto) = VigiaAvisos.textoNotificacao(
            aviso(1, "ENTREGUE", detalhe = "Na Avenida Bezerra de Menezes, São Gerardo · faltam ~4 min (1,2 km)"))
        assertEquals("CRG-000001 · Localização do veículo atualizada", titulo)
        assertEquals("Na Avenida Bezerra de Menezes, São Gerardo · faltam ~4 min (1,2 km)", texto)
    }

    @Test
    fun `sem frase mostra o destinatario e a mesma encomenda reaproveita a notificacao`() {
        val a = aviso(1, "ENTREGUE", evento = "SAIU_PARA_ENTREGA")
        assertEquals("Para Academia Gaviões", VigiaAvisos.textoNotificacao(a).second)
        assertEquals(VigiaAvisos.idNotificacao(a), VigiaAvisos.idNotificacao(aviso(9, "ENTREGUE")))
    }
}
