package br.com.mba.fametro.kotlintransportadora

import android.app.Application
import br.com.mba.fametro.kotlintransportadora.cliente.VigiaAvisos
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Rede
import br.com.mba.fametro.kotlintransportadora.core.api.RotaCertaApi
import br.com.mba.fametro.kotlintransportadora.core.sessao.SessaoRepository
import br.com.mba.fametro.kotlintransportadora.entregador.EstadoRastreio
import br.com.mba.fametro.kotlintransportadora.entregador.FilaGps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Injeção de dependência "na mão": um objeto por app com tudo que as telas e o serviço usam.
 * (Em projeto maior entraria Hilt/Koin; aqui deixa explícito quem depende de quem.)
 */
class AppContainer(app: Application) {

    /** Contexto da aplicação (para iniciar/parar o serviço de rastreio). */
    val contexto: android.content.Context = app

    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessao = SessaoRepository(app, escopo)

    val api: RotaCertaApi = Rede.api(token = { sessao.token }) {
        sessao.sair("Sua sessão expirou: entre de novo.")
    }

    /** Posições de GPS ainda não enviadas (arquivo por rota: sobrevive a fechar o app e a ficar sem sinal). */
    val filaGps = FilaGps(File(app.filesDir, "fila-gps"))

    /** O que o rastreador está fazendo agora (a tela da rota observa). */
    val rastreio = MutableStateFlow(EstadoRastreio())

    /** Cliente logado: acompanha os avisos do webhook e notifica os novos. */
    val avisos by lazy { VigiaAvisos(this) }

    init {
        sessao.api = api
    }

    /** Chama a API convertendo qualquer falha em [ApiErro] (mensagem pronta para a tela). */
    suspend fun <T> chamar(bloco: suspend RotaCertaApi.() -> T): T =
        try { api.bloco() } catch (e: Throwable) { throw ApiErro.de(e, Rede.json) }
}
