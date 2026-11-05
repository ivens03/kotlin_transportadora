package br.com.mba.fametro.kotlintransportadora.core.api

import br.com.mba.fametro.kotlintransportadora.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Monta o cliente HTTP (OkHttp) e o Retrofit da API. */
object Rede {

    val json = Json {
        ignoreUnknownKeys = true // a API pode ganhar campos sem quebrar o app
        explicitNulls = false     // não manda "campo": null
        coerceInputValues = true
    }

    fun api(token: () -> String?, aoReceber401: () -> Unit): RotaCertaApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // DNS da rede com reserva em DNS-over-HTTPS (redes que bloqueiam o domínio do servidor)
            .dns(DnsComReserva())
            .addInterceptor(AutorizacaoInterceptor(token, aoReceber401))
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RotaCertaApi::class.java)
    }
}

/**
 * Coloca "Authorization: Bearer <token>" em toda chamada (menos login/ativação) e avisa quando o
 * servidor responde 401 a uma chamada autenticada: token vencido ⇒ a sessão é encerrada.
 */
class AutorizacaoInterceptor(
    private val token: () -> String?,
    private val aoReceber401: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val publico = original.url.encodedPath.contains("/auth/") && !original.url.encodedPath.endsWith("/auth/sessao")
        val t = token()
        val requisicao = if (publico || t == null) original
        else original.newBuilder().header("Authorization", "Bearer $t").build()
        val resposta = chain.proceed(requisicao)
        if (resposta.code == 401 && requisicao.header("Authorization") != null) {
            aoReceber401()
        }
        return resposta
    }
}
