package br.com.mba.fametro.kotlintransportadora.core.api

import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

/**
 * Serviços públicos de endereço (sem chave), os mesmos do site:
 *  - ViaCEP: CEP → rua, bairro, cidade, UF
 *  - Nominatim (OpenStreetMap): endereço → ponto no mapa (a política pede um User-Agent que identifique o app)
 */
object Enderecos {

    data class EnderecoCep(val endereco: String, val bairro: String, val cidade: String, val uf: String)

    private val http = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

    private fun get(url: String): String? = http.newCall(
        Request.Builder().url(url).header("User-Agent", "RotaCerta/1.0 (MBA Fametro - transportadora)").build()
    ).execute().use { r -> if (r.isSuccessful) r.body.string() else null }

    /** Nulo se o CEP não existe. Lança exceção se o serviço estiver fora. */
    suspend fun porCep(cep: String): EnderecoCep? = withContext(Dispatchers.IO) {
        if (cep.length != 8) return@withContext null
        val corpo = get("https://viacep.com.br/ws/$cep/json/") ?: return@withContext null
        val o = Rede.json.parseToJsonElement(corpo).jsonObject
        if (o["erro"] != null) return@withContext null
        fun campo(nome: String) = o[nome]?.jsonPrimitive?.content.orEmpty()
        EnderecoCep(campo("logradouro"), campo("bairro"), campo("localidade"), campo("uf"))
    }

    /** Primeiro resultado do OpenStreetMap, arredondado a 6 casas (o que a API aceita). Nulo se não achar. */
    suspend fun coordenadas(endereco: String, bairro: String, cidade: String, uf: String): Coordenada? = withContext(Dispatchers.IO) {
        val q = listOf(endereco, bairro, cidade, uf, "Brasil").filter { it.isNotBlank() }.joinToString(", ")
        val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", q).addQueryParameter("format", "json")
            .addQueryParameter("limit", "1").addQueryParameter("countrycodes", "br").build()
        val corpo = get(url.toString()) ?: return@withContext null
        val primeiro = Rede.json.parseToJsonElement(corpo).jsonArray.firstOrNull()?.jsonObject ?: return@withContext null
        fun seis(v: String) = BigDecimal(v).setScale(6, RoundingMode.HALF_UP).toDouble()
        Coordenada(seis(primeiro["lat"]!!.jsonPrimitive.content), seis(primeiro["lon"]!!.jsonPrimitive.content))
    }
}
