package br.com.mba.fametro.kotlintransportadora.acesso

import br.com.mba.fametro.kotlintransportadora.core.formato.Mascaras
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoPrincipal
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

/**
 * Primeiro acesso (código de ativação → PIN/senha) e troca de PIN/senha (voluntária ou obrigatória a cada 30 dias).
 * PIN = matrícula ENT-…; senha = demais (funcionário FUN-…, cliente CLI-…). As regras de força ficam no back-end.
 */
class CredencialViewModel(private val c: AppContainer, matriculaInicial: String?) : ViewModel() {
    var matricula by mutableStateOf(matriculaInicial.orEmpty())
    var codigoOuAtual by mutableStateOf("")
    var nova by mutableStateOf("")
    var confirmacao by mutableStateOf("")
    var enviando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    val ehPin get() = matricula.trim().uppercase().startsWith("ENT")
    val nome get() = if (ehPin) "PIN" else "senha"

    fun validoPara(envio: Boolean) = matricula.isNotBlank() && codigoOuAtual.isNotBlank() && nova.isNotBlank() &&
        (!envio || nova == confirmacao)

    fun enviar(ativacao: Boolean, aoConcluir: (String) -> Unit) {
        if (nova != confirmacao) { erro = "A confirmação não bate com o novo $nome."; return }
        enviando = true
        erro = null
        val m = matricula.trim().uppercase()
        viewModelScope.launch {
            try {
                if (ativacao) c.sessao.ativar(m, codigoOuAtual.trim().uppercase(), nova)
                else c.sessao.trocar(m, codigoOuAtual, nova)
                aoConcluir(if (ehPin) "Pronto! Entre com a matrícula $m e o PIN novo." else "Pronto! Entre com seu e-mail e a senha nova.")
            } catch (e: ApiErro) {
                erro = e.completa
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun PrimeiroAcessoTela(aoVoltar: () -> Unit, aoConcluir: (String) -> Unit) =
    FormularioCredencial(ativacao = true, matricula = null, vencida = false, aoVoltar = aoVoltar, aoConcluir = aoConcluir)

@Composable
fun TrocarCredencialTela(matricula: String?, vencida: Boolean, aoVoltar: () -> Unit, aoConcluir: (String) -> Unit) =
    FormularioCredencial(ativacao = false, matricula = matricula, vencida = vencida, aoVoltar = aoVoltar, aoConcluir = aoConcluir)

@Composable
private fun FormularioCredencial(ativacao: Boolean, matricula: String?, vencida: Boolean, aoVoltar: () -> Unit, aoConcluir: (String) -> Unit) {
    val vm = viewModelDe(chave = "cred-$ativacao-$matricula") { CredencialViewModel(it, matricula) }
    val titulo = if (ativacao) "Primeiro acesso" else "Trocar ${vm.nome}"
    val teclado = if (vm.ehPin) KeyboardType.NumberPassword else KeyboardType.Password
    /** PIN: só números, até 6 (Mascaras.pin); senha: livre. */
    val filtro: (String) -> String = { if (vm.ehPin) Mascaras.pin(it) else it }

    Scaffold(topBar = { Topo(titulo, aoVoltar = aoVoltar) }, containerColor = Cores.Fundo) { margem ->
        Column(Modifier.fillMaxSize().padding(margem).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vencida) Aviso("Seu ${vm.nome} venceu (vale 30 dias). Crie um novo para continuar.", erro = false)
            Cartao {
                Text(
                    if (ativacao) "Use o código que o Administrativo passou (vale 72 h) e crie seu PIN (entregador) ou senha."
                    else "Informe o ${vm.nome} atual e o novo.",
                    style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave,
                )
                Spacer(Modifier.height(12.dp))
                CampoTexto(vm.matricula, { vm.matricula = Mascaras.matricula(it) }, "Matrícula", dica = "ENT-000001 / FUN-… / CLI-…", maiusculas = true)
                Spacer(Modifier.height(8.dp))
                if (ativacao) CampoTexto(vm.codigoOuAtual, { vm.codigoOuAtual = Mascaras.codigoAtivacao(it) }, "Código de ativação", dica = "8 letras ou números", maiusculas = true)
                else CampoTexto(vm.codigoOuAtual, { vm.codigoOuAtual = filtro(it) }, "${vm.nome.replaceFirstChar(Char::uppercase)} atual", tipo = teclado, segredo = true)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.nova, { vm.nova = filtro(it) }, "Novo ${vm.nome}", tipo = teclado, segredo = true)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.confirmacao, { vm.confirmacao = filtro(it) }, "Confirme o novo ${vm.nome}", tipo = teclado, segredo = true, ultimo = true)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (vm.ehPin) "PIN: 6 números, sem sequência (123456) nem repetição (111111)."
                    else "Senha: no mínimo 10 caracteres, com letras e números.",
                    style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave,
                )
                Spacer(Modifier.height(12.dp))
                vm.erro?.let { Aviso(it); Spacer(Modifier.height(12.dp)) }
                BotaoPrincipal(if (ativacao) "Ativar" else "Trocar", { vm.enviar(ativacao, aoConcluir) },
                    carregando = vm.enviando, habilitado = vm.validoPara(envio = false))
            }
        }
    }
}
