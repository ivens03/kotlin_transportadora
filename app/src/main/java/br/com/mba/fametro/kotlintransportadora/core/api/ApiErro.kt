package br.com.mba.fametro.kotlintransportadora.core.api

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Erro da API já traduzido para a tela (mensagem em português vinda do ErroResponse do back). */
class ApiErro(
    val status: Int,
    val codigo: String?,
    override val message: String,
    val campos: List<CampoErro> = emptyList(),
) : Exception(message) {

    val semConexao get() = status == 0
    val credencialExpirada get() = codigo == CREDENCIAL_EXPIRADA

    /** Mensagem + erros de campo, para mostrar num aviso só. */
    val completa: String
        get() = if (campos.isEmpty()) message else message + "\n" + campos.joinToString("\n") { "• ${it.mensagem}" }

    companion object {
        const val CREDENCIAL_EXPIRADA = "CREDENCIAL_EXPIRADA"

        /** A mensagem do 403 CREDENCIAL_EXPIRADA aponta para /auth/credenciais/{matricula}: tira a matrícula de lá. */
        private val MATRICULA_NA_MENSAGEM = Regex("credenciais/([A-Z]+-[A-Z0-9-]+)")

        fun matriculaDaMensagem(mensagem: String?): String? =
            mensagem?.let { MATRICULA_NA_MENSAGEM.find(it)?.groupValues?.get(1) }

        /** Cancelamento de corrotina não é erro de API: tem de seguir adiante (por isso o rethrow). */
        fun de(e: Throwable, json: Json): ApiErro = when (e) {
            is kotlinx.coroutines.CancellationException -> throw e
            is ApiErro -> e
            is HttpException -> {
                val corpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                val erro = corpo?.let { runCatching { json.decodeFromString<ErroResponse>(it) }.getOrNull() }
                ApiErro(e.code(), erro?.codigo, erro?.mensagem ?: padrao(e.code()), erro?.campos.orEmpty())
            }
            is IOException -> ApiErro(0, null, "Sem conexão com o servidor. Confira a internet e tente de novo.")
            else -> ApiErro(-1, null, e.message ?: "Erro inesperado.")
        }

        private fun padrao(status: Int) = when (status) {
            401 -> "Sessão expirada: entre de novo."
            403 -> "Seu perfil não tem acesso a isto."
            404 -> "Não encontrado."
            in 500..599 -> "O servidor teve um problema. Tente de novo em instantes."
            else -> "Erro $status."
        }
    }
}
