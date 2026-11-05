package br.com.mba.fametro.kotlintransportadora.core.api

import kotlinx.serialization.Serializable

// Espelho dos DTOs da API (spring.transportadora). Datas chegam em ISO-8601 (String) e são convertidas
// na tela (core/formato). Números decimais (BigDecimal no back) viram Double.

// ---------- acesso ----------
@Serializable data class LoginAppRequest(val matricula: String, val pin: String)
@Serializable data class LoginPainelRequest(val email: String, val senha: String)
@Serializable data class AtivacaoRequest(val matricula: String, val codigoAtivacao: String, val credencial: String)
@Serializable data class TrocaCredencialRequest(val credencialAtual: String, val novaCredencial: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val expiraEm: String,
    val matricula: String,
    val perfil: String,
    val tipoCredencial: String,
    val credencialExpiraEm: String? = null,
)

@Serializable
data class SessaoResponse(
    val matricula: String,
    val nome: String,
    val perfil: String,
    val tipoCredencial: String,
    val modulo: String,
    val permissoes: List<String> = emptyList(),
    val expiraEm: String,
)

@Serializable data class CredencialResponse(val matricula: String, val tipoCredencial: String, val expiraEm: String)

// ---------- erro padrão (ErroResponse do back) ----------
@Serializable data class CampoErro(val campo: String, val mensagem: String)

@Serializable
data class ErroResponse(
    val status: Int,
    val erro: String? = null,
    val codigo: String? = null,
    val mensagem: String? = null,
    val campos: List<CampoErro> = emptyList(),
)

// ---------- paginação (PagedModel do Spring) ----------
@Serializable data class PaginaInfo(val size: Int, val number: Int, val totalElements: Long, val totalPages: Int)
@Serializable data class Pagina<T>(val content: List<T> = emptyList(), val page: PaginaInfo)

// ---------- rotas (entregador e suporte) ----------
@Serializable
data class RotaResumo(
    val id: Long,
    val codigo: String,
    val status: String,
    val veiculoPlaca: String,
    val entregadorMatricula: String,
    val entregadorNome: String,
    val totalParadas: Int,
    val paradasBaixadas: Int,
    val distanciaEstimadaKm: Double? = null,
    val duracaoEstimadaMin: Int? = null,
    val criadaEm: String,
    val iniciadaEm: String? = null,
    val concluidaEm: String? = null,
)

@Serializable
data class Parada(
    val ordem: Int,
    val cargaId: Long,
    val cargaCodigo: String,
    val descricao: String,
    val destinatarioNome: String,
    val destinatarioTelefone: String? = null,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val latitude: Double,
    val longitude: Double,
    val distanciaDoAnteriorKm: Double? = null,
    val status: String,
    val motivo: String? = null,
    val finalizadaEm: String? = null,
)

/** Ponto do trajeto planejado: ORIGEM, PARADA (com ordem) ou RETORNO. */
@Serializable data class Ponto(val tipo: String, val ordem: Int? = null, val latitude: Double, val longitude: Double)

@Serializable
data class Progresso(val totalParadas: Int, val entregues: Int, val naoEntregues: Int, val pendentes: Int, val percentual: Int)

@Serializable
data class Posicao(
    val latitude: Double,
    val longitude: Double,
    val registradaEm: String,
    val velocidadeKmH: Double? = null,
    val precisaoM: Double? = null,
    val recebidaEm: String? = null,
)

@Serializable
data class Rota(
    val id: Long,
    val codigo: String,
    val status: String,
    val veiculoPlaca: String,
    val veiculoModelo: String,
    val entregadorMatricula: String,
    val entregadorNome: String,
    val origemLatitude: Double,
    val origemLongitude: Double,
    val retornaOrigem: Boolean,
    val distanciaEstimadaKm: Double? = null,
    val duracaoEstimadaMin: Int? = null,
    val paradas: List<Parada> = emptyList(),
    val trajeto: List<Ponto> = emptyList(),
    val progresso: Progresso,
    val proximaParada: Int? = null,
    val ultimaPosicao: Posicao? = null,
    val distanciaAteProximaParadaKm: Double? = null,
    val minutosAteProximaParada: Int? = null,
    val criadaEm: String,
    val iniciadaEm: String? = null,
    val concluidaEm: String? = null,
)

@Serializable data class RotaPatch(val status: String)
@Serializable data class ParadaPatch(val status: String, val motivo: String? = null, val finalizadaEm: String? = null)

/** Leitura de GPS a enviar (o back aceita até 6 casas na coordenada e 1 na velocidade/precisão). */
@Serializable
data class NovaPosicao(
    val latitude: Double,
    val longitude: Double,
    val registradaEm: String,
    val velocidadeKmH: Double? = null,
    val precisaoM: Double? = null,
)

@Serializable data class LotePosicoes(val posicoes: List<NovaPosicao>)
@Serializable data class LoteResposta(val recebidas: Int, val gravadas: Int, val duplicadas: Int)

// ---------- Portal do Cliente ----------
@Serializable
data class EncomendaResumo(
    val codigo: String,
    val descricao: String,
    val status: String,
    val destinatarioNome: String,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val latitude: Double,
    val longitude: Double,
    val tentativasEntrega: Int = 0,
    val criadaEm: String,
    val atualizadaEm: String,
)

@Serializable
data class Localizacao(
    val latitude: Double,
    val longitude: Double,
    val velocidadeKmH: Double? = null,
    val registradaEm: String,
    /** Rua e bairro onde o veículo está (OpenStreetMap, calculado na API); nulos se o mapa não souber. */
    val rua: String? = null,
    val bairro: String? = null,
)

@Serializable
data class Rastreio(
    val rotaCodigo: String,
    val rotaStatus: String,
    val entregador: String,
    val veiculoModelo: String,
    val veiculoPlaca: String,
    val ordemParada: Int,
    val entregasAntes: Int? = null,
    val localizacao: Localizacao? = null,
    val distanciaKm: Double? = null,
    val previsaoMinutos: Int? = null,
    val previsaoChegadaEm: String? = null,
    val entregueEm: String? = null,
)

@Serializable
data class EventoEncomenda(
    val tipo: String,
    val descricao: String,
    val rotaCodigo: String? = null,
    val detalhe: String? = null,
    val ocorridoEm: String,
)

@Serializable
data class Encomenda(val encomenda: EncomendaResumo, val rastreio: Rastreio? = null, val eventos: List<EventoEncomenda> = emptyList())

// ---------- webhooks do cliente (avisos que a transportadora manda para o servidor dele) ----------
@Serializable
data class Webhook(val id: Long, val url: String, val eventos: List<String> = emptyList(), val ativo: Boolean = true)

/** Corpo que o servidor do cliente recebeu (o mesmo JSON do POST do webhook). */
@Serializable
data class AvisoPayload(
    val evento: String,
    val descricao: String,
    val ocorridoEm: String,
    val encomenda: EncomendaResumo? = null,
    val rastreio: Rastreio? = null,
    /** Na LOCALIZACAO: "Na Av. ..., bairro · faltam ~12 min (3,4 km) · ..."; na falha: o motivo. */
    val detalhe: String? = null,
)

/** Um aviso enviado ao webhook: o que foi, se o servidor do cliente aceitou (HTTP) e o corpo enviado. */
@Serializable
data class AvisoWebhook(
    val id: Long,
    val evento: String,
    /** PENDENTE, ENTREGUE (servidor respondeu 2xx), FALHOU ou DESCARTADA (localização substituída por outra mais nova). */
    val status: String,
    val tentativas: Int = 0,
    val ultimoStatusHttp: Int? = null,
    val ultimoErro: String? = null,
    val criadaEm: String,
    val entregueEm: String? = null,
    val payload: AvisoPayload? = null,
)
