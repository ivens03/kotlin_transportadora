package br.com.mba.fametro.kotlintransportadora.core.formato

/**
 * CNPJ com dígitos verificadores, já no formato alfanumérico (Receita, julho/2026): 12 letras/números + 2 dígitos.
 * Cada caractere vale o código ASCII − 48, então o CNPJ só com números continua igual. Mesma regra da API.
 */
object Cnpj {
    private val PESOS_DV1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    private val PESOS_DV2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

    fun valido(cnpj: String?): Boolean {
        val c = (cnpj ?: "").replace(Regex("[.\\-/\\s]"), "").uppercase()
        if (!c.matches(Regex("[0-9A-Z]{12}[0-9]{2}")) || c.all { it == c[0] }) return false
        val dv1 = digito(c.take(12), PESOS_DV1)
        val dv2 = digito(c.take(12) + dv1, PESOS_DV2)
        return c[12] - '0' == dv1 && c[13] - '0' == dv2
    }

    private fun digito(base: String, pesos: IntArray): Int {
        val resto = base.indices.sumOf { (base[it] - '0') * pesos[it] } % 11
        return if (resto < 2) 0 else 11 - resto
    }

    /** "11222333000181" → "11.222.333/0001-81" */
    fun formatar(cnpj: String?): String {
        val c = cnpj ?: return ""
        return if (c.length != 14) c else "${c.take(2)}.${c.substring(2, 5)}.${c.substring(5, 8)}/${c.substring(8, 12)}-${c.substring(12)}"
    }
}
