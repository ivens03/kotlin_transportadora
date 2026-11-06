package br.com.mba.fametro.kotlintransportadora.core.formato

/**
 * Filtros dos campos de texto: recebem o que a pessoa digitou (ou colou) e devolvem só o que cabe no formato.
 * Mesmas regras do site (angular core/mascaras.ts). Não inserem enfeites (hífen etc.) porque, no Compose,
 * mudar o tamanho do texto enquanto a pessoa digita faz o cursor pular para o lugar errado.
 */
object Mascaras {

    /** ENT-000001 / FUN-… / CLI-…: maiúsculas, letras, números e hífen (sem hífen repetido), até 20. */
    fun matricula(s: String): String =
        s.uppercase().filter { it in 'A'..'Z' || it.isAsciiDigit() || it == '-' }.replace(Regex("-{2,}"), "-").take(20)

    /** Código de ativação: 8 letras ou números. */
    fun codigoAtivacao(s: String): String =
        s.uppercase().filter { it in 'A'..'Z' || it.isAsciiDigit() }.take(8)

    /** E-mail sem espaços e em minúsculas. */
    fun email(s: String): String = s.filterNot(Char::isWhitespace).lowercase().take(200)

    /** PIN: só números, até 6. */
    fun pin(s: String): String = s.filter { it.isAsciiDigit() }.take(6)

    /** CNPJ (também o alfanumérico): 12 letras/números + 2 dígitos verificadores (sempre números). */
    fun cnpj(s: String): String {
        val r = StringBuilder()
        for (ch in s.uppercase()) {
            if (r.length >= 14) break
            if (r.length < 12 && (ch in 'A'..'Z' || ch.isAsciiDigit())) r.append(ch)
            else if (r.length >= 12 && ch.isAsciiDigit()) r.append(ch)
        }
        return r.toString()
    }

    /** Telefone com DDD: só números, até 11. */
    fun telefone(s: String): String = s.filter { it.isAsciiDigit() }.take(11)

    /** CEP: só números, 8. */
    fun cep(s: String): String = s.filter { it.isAsciiDigit() }.take(8)

    /** UF: 2 letras maiúsculas. */
    fun uf(s: String): String = s.uppercase().filter { it in 'A'..'Z' }.take(2)

    /** Número com vírgula: só dígitos e UMA vírgula (o ponto do teclado vira vírgula), até [casas] decimais. */
    fun decimal(s: String, casas: Int): String {
        val t = s.replace('.', ',').filter { it.isAsciiDigit() || it == ',' }
        val i = t.indexOf(',')
        if (i < 0) return t.take(9)
        return t.take(i).take(9) + "," + t.substring(i + 1).replace(",", "").take(casas)
    }

    /** "18,5" → 18.5; vazio ou só vírgula → null. */
    fun numero(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

    private fun Char.isAsciiDigit() = this in '0'..'9'
}
