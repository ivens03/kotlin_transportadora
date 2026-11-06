package br.com.mba.fametro.kotlintransportadora.acesso

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/** Campo de formulário padrão; [segredo] = senha/PIN com o "olho" para mostrar. */
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    tipo: KeyboardType = KeyboardType.Text,
    segredo: Boolean = false,
    maiusculas: Boolean = false,
    dica: String? = null,
    ultimo: Boolean = false,
    /** Formata só na tela (ex.: MascaraVisual.CNPJ); o valor continua limpo. */
    mascara: VisualTransformation? = null,
    habilitado: Boolean = true,
) {
    var mostrar by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        placeholder = dica?.let { { Text(it) } },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = tipo,
            capitalization = if (maiusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
            imeAction = if (ultimo) ImeAction.Done else ImeAction.Next,
            autoCorrectEnabled = false,
        ),
        enabled = habilitado,
        visualTransformation = when {
            segredo && !mostrar -> PasswordVisualTransformation()
            mascara != null -> mascara
            else -> VisualTransformation.None
        },
        trailingIcon = if (!segredo) null else {
            {
                IconButton(onClick = { mostrar = !mostrar }) {
                    Icon(if (mostrar) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrar) "Esconder" else "Mostrar")
                }
            }
        },
    )
}
