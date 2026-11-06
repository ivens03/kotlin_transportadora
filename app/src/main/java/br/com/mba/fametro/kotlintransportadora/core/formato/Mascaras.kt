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

    private fun Char.isAsciiDigit() = this in '0'..'9'
}
