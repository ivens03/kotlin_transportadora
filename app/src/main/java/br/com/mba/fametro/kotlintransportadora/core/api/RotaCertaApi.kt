package br.com.mba.fametro.kotlintransportadora.core.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints da API RotaCerta usados pelo app (Retrofit gera a implementação).
 * Caminhos relativos a BuildConfig.API_URL (.../api/v1/). O token vai no AutorizacaoInterceptor.
 */
interface RotaCertaApi {

    // acesso (públicos)
    @POST("auth/app/tokens")
    suspend fun entrarApp(@Body corpo: LoginAppRequest): TokenResponse

    @POST("auth/painel/tokens")
    suspend fun entrarPainel(@Body corpo: LoginPainelRequest): TokenResponse

    @POST("auth/credenciais")
    suspend fun ativar(@Body corpo: AtivacaoRequest): CredencialResponse

    @PATCH("auth/credenciais/{matricula}")
    suspend fun trocarCredencial(@Path("matricula") matricula: String, @Body corpo: TrocaCredencialRequest): CredencialResponse

    @GET("auth/sessao")
    suspend fun sessao(): SessaoResponse

    // rotas: o entregador só recebe as dele; o suporte vê todas
    @GET("rotas")
    suspend fun rotas(
        @Query("status") status: String? = null,
        @Query("size") tamanho: Int = 50,
        @Query("sort") ordem: String = "criadaEm,desc",
    ): Pagina<RotaResumo>

    @GET("rotas/{id}")
    suspend fun rota(@Path("id") id: Long): Rota

    @PATCH("rotas/{id}")
    suspend fun alterarRota(@Path("id") id: Long, @Body corpo: RotaPatch): Rota

    @PATCH("rotas/{id}/paradas/{ordem}")
    suspend fun baixarParada(@Path("id") id: Long, @Path("ordem") ordem: Int, @Body corpo: ParadaPatch): Rota

    @POST("rotas/{id}/posicoes")
    suspend fun enviarPosicoes(@Path("id") id: Long, @Body corpo: LotePosicoes): LoteResposta

    @GET("rotas/{id}/posicoes")
    suspend fun posicoes(@Path("id") id: Long, @Query("desde") desde: String? = null): List<Posicao>

    // Portal do Cliente
    @GET("encomendas")
    suspend fun encomendas(
        @Query("status") status: String? = null,
        @Query("size") tamanho: Int = 50,
    ): Pagina<EncomendaResumo>

    @GET("encomendas/{codigo}")
    suspend fun encomenda(@Path("codigo") codigo: String): Encomenda

    @GET("webhooks")
    suspend fun webhooks(@Query("size") tamanho: Int = 10): Pagina<Webhook>

    /** Mais recentes primeiro (a API ordena por criadaEm desc). */
    @GET("webhooks/{id}/entregas")
    suspend fun avisosDoWebhook(@Path("id") id: Long, @Query("size") tamanho: Int = 30): Pagina<AvisoWebhook>

    // Suporte: empresas clientes e nova entrega
    @GET("clientes")
    suspend fun clientes(@Query("size") tamanho: Int = 500): Pagina<Cliente>

    /** Cadastra a empresa e devolve o código de ativação do login dela no Portal do Cliente. */
    @POST("clientes")
    suspend fun cadastrarEmpresa(@Body corpo: NovaEmpresa): CodigoAtivacao

    /** Desenvolvimento: arquitetura (camadas, fluxos, ferramentas) + ambiente medido no servidor. */
    @GET("arquitetura")
    suspend fun arquitetura(): Arquitetura

    @GET("entregadores")
    suspend fun entregadores(@Query("status") status: String = "ATIVO", @Query("size") tamanho: Int = 100): Pagina<Entregador>

    @GET("veiculos")
    suspend fun veiculos(@Query("status") status: String = "ATIVO", @Query("size") tamanho: Int = 100): Pagina<VeiculoResumo>

    @POST("cargas")
    suspend fun cadastrarCarga(@Body corpo: NovaCarga): CargaCriada

    /** Rota só com as cargas escolhidas, já com entregador e veículo (o entregador vê em "Minhas rotas"). */
    @POST("rotas")
    suspend fun gerarRota(@Body corpo: NovaRota): Rota

    /** Entregador já tem rota que ainda não saiu: a carga entra nela e a ordem das paradas é recalculada. */
    @POST("rotas/{id}/cargas")
    suspend fun incluirNaRota(@Path("id") id: Long, @Body corpo: IncluirCargas): Rota
}
