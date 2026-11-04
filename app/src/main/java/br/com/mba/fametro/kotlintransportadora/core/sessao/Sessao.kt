package br.com.mba.fametro.kotlintransportadora.core.sessao

import java.time.Instant

/** Quem está logado (vem do token + GET /auth/sessao). Define o "módulo" que o app abre. */
data class Sessao(
    val token: String,
    val matricula: String,
    val nome: String,
    val perfil: String,
    val tipoCredencial: String,
    val permissoes: Set<String>,
    val expiraEm: Instant,
) {
    val entregador get() = tipoCredencial == "APP_ENTREGADOR"
    val cliente get() = "encomenda:acompanhar" in permissoes
    val acompanhaMapa get() = "mapa:acompanhar" in permissoes
    val primeiroNome get() = nome.trim().substringBefore(' ')

    fun expirada(agora: Instant = Instant.now()) = !expiraEm.isAfter(agora)

    /** Para qual área o app vai depois do login. */
    val modulo: Modulo
        get() = when {
            entregador -> Modulo.ENTREGADOR
            cliente -> Modulo.CLIENTE
            acompanhaMapa -> Modulo.SUPORTE
            else -> Modulo.SEM_MODULO
        }
}

enum class Modulo { ENTREGADOR, CLIENTE, SUPORTE, SEM_MODULO }

/** Estado da sessão observado pela raiz da navegação. */
sealed interface EstadoSessao {
    data object Carregando : EstadoSessao
    data class Deslogado(val aviso: String? = null) : EstadoSessao
    data class Logado(val sessao: Sessao) : EstadoSessao
}
