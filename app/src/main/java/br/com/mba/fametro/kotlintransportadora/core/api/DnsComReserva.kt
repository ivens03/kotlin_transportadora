package br.com.mba.fametro.kotlintransportadora.core.api

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * DNS do app: usa o DNS da rede e, se ele falhar ou "mentir", pergunta a um DNS-over-HTTPS.
 *
 * Por quê: redes corporativas bloqueiam domínios devolvendo um endereço falso em vez de erro. A rede do DETRAN
 * responde 127.0.0.1 / 0.0.0.0 para *.sslip.io, e o app tentava conectar no próprio celular ("Sem conexão").
 * O DoH vai por HTTPS (porta 443, como qualquer site) direto para o IP do Cloudflare/Google, então o filtro de
 * DNS da rede não vê nem altera a resposta. O certificado do servidor continua sendo verificado normalmente:
 * só a descoberta do IP muda.
 */
class DnsComReserva(
    private val rede: Dns = Dns.SYSTEM,
    private val reservas: List<Dns> = dohPublicos(),
) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        val daRede = try {
            rede.lookup(hostname)
        } catch (_: UnknownHostException) {
            emptyList()
        }
        if (daRede.isNotEmpty() && daRede.none(::enderecoFalso)) return daRede

        for (doh in reservas) {
            val r = try {
                doh.lookup(hostname)
            } catch (_: UnknownHostException) {
                continue
            }
            if (r.isNotEmpty() && r.none(::enderecoFalso)) return r
        }
        if (daRede.isNotEmpty()) return daRede // ex.: "localhost" no emulador com adb reverse
        throw UnknownHostException("Não foi possível resolver $hostname")
    }

    companion object {
        /** 127.x, ::1 e 0.0.0.0: um servidor público nunca está aí; é bloqueio da rede. */
        fun enderecoFalso(ip: InetAddress) = ip.isLoopbackAddress || ip.isAnyLocalAddress

        /** Cloudflare e Google, pelos IPs fixos (sem depender do DNS da rede para achar o próprio DoH). */
        fun dohPublicos(): List<Dns> {
            val cliente = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
            fun doh(url: String, vararg ips: String): Dns = DnsOverHttps.Builder()
                .client(cliente)
                .url(url.toHttpUrl())
                // IP literal: getByName só converte o texto, não consulta DNS
                .bootstrapDnsHosts(ips.map(InetAddress::getByName))
                .includeIPv6(false)
                .build()
            return listOf(
                doh("https://cloudflare-dns.com/dns-query", "1.1.1.1", "1.0.0.1"),
                doh("https://dns.google/dns-query", "8.8.8.8", "8.8.4.4"),
            )
        }
    }
}
