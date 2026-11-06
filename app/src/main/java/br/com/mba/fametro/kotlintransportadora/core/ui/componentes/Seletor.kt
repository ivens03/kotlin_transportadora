package br.com.mba.fametro.kotlintransportadora.core.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores

/** Uma opção da lista: texto principal, linha de apoio e se pode ser escolhida. */
data class Opcao<T>(val valor: T, val texto: String, val apoio: String? = null, val habilitada: Boolean = true)

/**
 * Campo de escolha (parece um campo de texto; o toque abre a lista). Versão simples do "dropdown",
 * sem depender da API experimental do Material 3.
 */
@Composable
fun <T> Seletor(rotulo: String, opcoes: List<Opcao<T>>, escolhido: T?, aoEscolher: (T?) -> Unit,
                modifier: Modifier = Modifier, textoVazio: String? = null) {
    var aberto by remember { mutableStateOf(false) }
    val atual = opcoes.firstOrNull { it.valor == escolhido }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = atual?.texto ?: (textoVazio ?: ""),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            singleLine = true,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // camada por cima: o toque em qualquer parte do campo abre a lista
        Box(Modifier.matchParentSize().clickable { aberto = true })
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            if (textoVazio != null) {
                DropdownMenuItem(text = { Text(textoVazio, color = Cores.TextoSuave) }, onClick = { aberto = false; aoEscolher(null) })
            }
            opcoes.forEach { o ->
                DropdownMenuItem(
                    enabled = o.habilitada,
                    text = {
                        Column {
                            Text(o.texto)
                            o.apoio?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave) }
                        }
                    },
                    onClick = { aberto = false; aoEscolher(o.valor) },
                )
            }
        }
    }
}
