package br.com.mba.fametro.kotlintransportadora.core.ui.componentes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Formata o campo SÓ NA TELA (VisualTransformation do Compose): o estado guarda "11222333000181" e o campo
 * mostra "11.222.333/0001-81". Como o texto de verdade não muda de tamanho, o cursor não pula.
 * No padrão, '#' é um caractere digitado; o resto é enfeite.
 */
class MascaraVisual(private val padrao: (Int) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val (exibir, paraTela) = aplicar(padrao(text.length), text.text)
        // da tela para o texto: quantos caracteres digitados existem antes da posição
        val paraTexto = IntArray(exibir.length + 1)
        var digitados = 0
        for (t in 0..exibir.length) {
            paraTexto[t] = digitados
            if (t < exibir.length && digitados < text.length && paraTela[digitados] == t) digitados++
        }
        return TransformedText(AnnotatedString(exibir), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = paraTela[offset.coerceIn(0, text.length)]
            override fun transformedToOriginal(offset: Int) = paraTexto[offset.coerceIn(0, exibir.length)]
        })
    }

    companion object {
        /** Monta o texto da tela e, para cada posição do texto digitado, a posição correspondente na tela. */
        fun aplicar(padrao: String, bruto: String): Pair<String, IntArray> {
            val sb = StringBuilder()
            val paraTela = IntArray(bruto.length + 1)
            var i = 0
            for (ch in padrao) {
                if (i >= bruto.length) break
                if (ch == '#') { paraTela[i] = sb.length; sb.append(bruto[i]); i++ } else sb.append(ch)
            }
            while (i < bruto.length) { paraTela[i] = sb.length; sb.append(bruto[i]); i++ }
            paraTela[bruto.length] = sb.length
            return sb.toString() to paraTela
        }

        val CNPJ = MascaraVisual { "##.###.###/####-##" }
        val CEP = MascaraVisual { "#####-###" }
        /** Fixo (10 dígitos) ou celular (11). */
        val TELEFONE = MascaraVisual { n -> if (n <= 10) "(##) ####-####" else "(##) #####-####" }
    }
}
