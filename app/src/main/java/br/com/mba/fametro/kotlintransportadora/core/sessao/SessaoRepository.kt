package br.com.mba.fametro.kotlintransportadora.core.sessao

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.AtivacaoRequest
import br.com.mba.fametro.kotlintransportadora.core.api.LoginAppRequest
import br.com.mba.fametro.kotlintransportadora.core.api.LoginPainelRequest
import br.com.mba.fametro.kotlintransportadora.core.api.Rede
import br.com.mba.fametro.kotlintransportadora.core.api.RotaCertaApi
import br.com.mba.fametro.kotlintransportadora.core.api.TokenResponse
import br.com.mba.fametro.kotlintransportadora.core.api.TrocaCredencialRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

private val Context.dataStore by preferencesDataStore(name = "sessao")

/**
 * Guarda a sessão no DataStore (sobrevive a fechar o app) e expõe o estado como StateFlow.
 * O token também fica num campo em memória para o interceptor ler sem suspender.
 */
class SessaoRepository(private val context: Context, private val escopo: CoroutineScope) {

    private val _estado = MutableStateFlow<EstadoSessao>(EstadoSessao.Carregando)
    val estado: StateFlow<EstadoSessao> = _estado.asStateFlow()

    @Volatile
    var token: String? = null
        private set

    lateinit var api: RotaCertaApi

    init {
        escopo.launch { carregar() }
    }

    private suspend fun carregar() {
        val p = context.dataStore.data.first()
        val t = p[TOKEN]
        val sessao = if (t == null) null else Sessao(
            token = t,
            matricula = p[MATRICULA].orEmpty(),
            nome = p[NOME].orEmpty(),
            perfil = p[PERFIL].orEmpty(),
            tipoCredencial = p[TIPO].orEmpty(),
            permissoes = p[PERMISSOES].orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
            expiraEm = Instant.ofEpochMilli(p[EXPIRA] ?: 0),
        )
        if (sessao == null || sessao.expirada()) {
            if (sessao != null) limpar()
            _estado.value = EstadoSessao.Deslogado(if (sessao != null) "Sua sessão expirou: entre de novo." else null)
        } else {
            token = sessao.token
            _estado.value = EstadoSessao.Logado(sessao)
        }
    }

    suspend fun entrarApp(matricula: String, pin: String) =
        concluirLogin(chamar { api.entrarApp(LoginAppRequest(matricula, pin)) })

    suspend fun entrarPainel(email: String, senha: String) =
        concluirLogin(chamar { api.entrarPainel(LoginPainelRequest(email, senha)) })

    suspend fun ativar(matricula: String, codigo: String, credencial: String) =
        chamar { api.ativar(AtivacaoRequest(matricula, codigo, credencial)) }

    suspend fun trocar(matricula: String, atual: String, nova: String) =
        chamar { api.trocarCredencial(matricula, TrocaCredencialRequest(atual, nova)) }

    /** Token em mãos: busca nome e permissões (GET /auth/sessao) e grava tudo. */
    private suspend fun concluirLogin(t: TokenResponse): Sessao {
        token = t.accessToken
        val dados = try {
            chamar { api.sessao() }
        } catch (e: ApiErro) {
            token = null
            throw e
        }
        val sessao = Sessao(t.accessToken, dados.matricula, dados.nome, dados.perfil, dados.tipoCredencial,
            dados.permissoes.toSet(), Instant.parse(t.expiraEm))
        context.dataStore.edit {
            it[TOKEN] = sessao.token
            it[MATRICULA] = sessao.matricula
            it[NOME] = sessao.nome
            it[PERFIL] = sessao.perfil
            it[TIPO] = sessao.tipoCredencial
            it[PERMISSOES] = sessao.permissoes.joinToString(",")
            it[EXPIRA] = sessao.expiraEm.toEpochMilli()
        }
        _estado.value = EstadoSessao.Logado(sessao)
        return sessao
    }

    fun sair(aviso: String? = null) {
        if (_estado.value is EstadoSessao.Deslogado) return
        token = null
        _estado.value = EstadoSessao.Deslogado(aviso)
        escopo.launch { limpar() }
    }

    private suspend fun limpar() = context.dataStore.edit { it.clear() }

    private suspend fun <T> chamar(bloco: suspend () -> T): T =
        try { bloco() } catch (e: Throwable) { throw ApiErro.de(e, Rede.json) }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val MATRICULA = stringPreferencesKey("matricula")
        val NOME = stringPreferencesKey("nome")
        val PERFIL = stringPreferencesKey("perfil")
        val TIPO = stringPreferencesKey("tipo_credencial")
        val PERMISSOES = stringPreferencesKey("permissoes")
        val EXPIRA = longPreferencesKey("expira_em")
    }
}
