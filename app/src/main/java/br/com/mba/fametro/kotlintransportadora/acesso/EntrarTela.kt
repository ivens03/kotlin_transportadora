package br.com.mba.fametro.kotlintransportadora.acesso

import br.com.mba.fametro.kotlintransportadora.core.formato.Mascaras
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoPrincipal
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoSecundario
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

/**
 * Login com duas abas: Entregador (matrícula + PIN, credencial do app) e Empresa (e-mail + senha:
 * funcionários e clientes). Senha/PIN vencido (403 CREDENCIAL_EXPIRADA) leva à troca obrigatória.
 */
class EntrarViewModel(private val c: AppContainer) : ViewModel() {
    var aba by mutableIntStateOf(0)
    var matricula by mutableStateOf("")
    var pin by mutableStateOf("")
    var email by mutableStateOf("")
    var senha by mutableStateOf("")
    var enviando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    /** Matrícula com credencial vencida (a tela navega para a troca e zera). */
    var vencida by mutableStateOf<String?>(null)

    val entregador get() = aba == 0
    val podeEnviar get() = if (entregador) matricula.isNotBlank() && pin.length == 6 else email.isNotBlank() && senha.isNotBlank()

    fun entrar() {
        if (!podeEnviar || enviando) return
        enviando = true
        erro = null
        viewModelScope.launch {
            try {
                if (entregador) c.sessao.entrarApp(matricula.trim().uppercase(), pin)
                else c.sessao.entrarPainel(email.trim().lowercase(), senha)
                // sucesso: a raiz troca para o módulo do perfil sozinha (observa a sessão)
            } catch (e: ApiErro) {
                if (e.credencialExpirada) {
                    vencida = ApiErro.matriculaDaMensagem(e.message) ?: if (entregador) matricula.trim().uppercase() else ""
                } else {
                    erro = e.completa
                }
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun EntrarTela(
    aviso: String?,
    aoPrimeiroAcesso: () -> Unit,
    aoCredencialVencida: (String?) -> Unit,
    aoTrocarCredencial: () -> Unit,
) {
    val vm = viewModelDe { EntrarViewModel(it) }
    LaunchedEffect(vm.vencida) {
        vm.vencida?.let { aoCredencialVencida(it.ifBlank { null }); vm.vencida = null }
    }

    Column(Modifier.fillMaxSize().background(Cores.Fundo).verticalScroll(rememberScrollState()).imePadding()) {
        // cabeçalho amarelo com a marca
        Box(Modifier.fillMaxWidth().background(Cores.Amarelo).statusBarsPadding().padding(top = 32.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = Cores.Marinho, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("RotaCerta", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Cores.Marinho)
                }
                Text("Entregas no caminho certo", color = Cores.Marinho.copy(alpha = .8f))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offsetCima(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Cartao {
                Text("Como você quer entrar?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Entregador" to Icons.Filled.TwoWheeler, "Empresa" to Icons.Filled.Business).forEachIndexed { i, (rotulo, icone) ->
                        SegmentedButton(
                            selected = vm.aba == i,
                            onClick = { vm.aba = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            icon = { Icon(icone, null, Modifier.size(18.dp)) },
                        ) { Text(rotulo) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (vm.entregador) {
                    CampoTexto(vm.matricula, { vm.matricula = Mascaras.matricula(it) }, "Matrícula", dica = "ENT-000001", maiusculas = true)
                    Spacer(Modifier.height(8.dp))
                    CampoTexto(vm.pin, { vm.pin = Mascaras.pin(it) }, "PIN (6 dígitos)",
                        tipo = KeyboardType.NumberPassword, segredo = true, ultimo = true)
                } else {
                    CampoTexto(vm.email, { vm.email = Mascaras.email(it) }, "E-mail", tipo = KeyboardType.Email)
                    Spacer(Modifier.height(8.dp))
                    CampoTexto(vm.senha, { vm.senha = it }, "Senha", tipo = KeyboardType.Password, segredo = true, ultimo = true)
                    Spacer(Modifier.height(6.dp))
                    Text("Funcionários e clientes (Portal do Cliente) entram por aqui.",
                        style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                }
                Spacer(Modifier.height(16.dp))
                (vm.erro ?: aviso)?.let {
                    Aviso(it, erro = vm.erro != null)
                    Spacer(Modifier.height(12.dp))
                }
                BotaoPrincipal("Entrar", vm::entrar, carregando = vm.enviando, habilitado = vm.podeEnviar)
            }
            BotaoSecundario("Primeiro acesso: tenho um código de ativação", aoPrimeiroAcesso)
            BotaoSecundario("Trocar minha senha ou PIN", aoTrocarCredencial)
        }
    }
}

/** Puxa o cartão para cima, sobre a faixa amarela (detalhe visual do Mercado Livre). */
private fun Modifier.offsetCima() = this.offset(y = (-28).dp)
