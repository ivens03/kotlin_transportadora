package br.com.mba.fametro.kotlintransportadora.navegacao

import kotlinx.serialization.Serializable

// Navegação "type-safe": cada tela é uma classe serializável; os argumentos são as propriedades.

// acesso (deslogado)
@Serializable object Entrar
@Serializable object PrimeiroAcesso
@Serializable data class TrocarCredencial(val matricula: String? = null, val vencida: Boolean = false)

// entregador
@Serializable object MinhasRotas
@Serializable data class RotaExecucao(val id: Long)

// cliente
@Serializable object Encomendas
@Serializable data class EncomendaDetalhe(val codigo: String)

// suporte (mapa:acompanhar)
@Serializable object RotasAcompanhar
@Serializable data class RotaMapa(val id: Long)

// perfis do painel sem módulo no app (Administrativo, Financeiro, Desenvolvimento)
@Serializable object SemModulo
