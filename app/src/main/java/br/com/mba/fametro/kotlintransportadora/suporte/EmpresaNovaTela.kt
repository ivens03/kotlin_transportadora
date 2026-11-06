package br.com.mba.fametro.kotlintransportadora.suporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.acesso.CampoTexto
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.CodigoAtivacao
import br.com.mba.fametro.kotlintransportadora.core.api.Enderecos
import br.com.mba.fametro.kotlintransportadora.core.api.NovaEmpresa
import br.com.mba.fametro.kotlintransportadora.core.formato.Cnpj
import br.com.mba.fametro.kotlintransportadora.core.formato.Formato
import br.com.mba.fametro.kotlintransportadora.core.formato.Mascaras
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoPrincipal
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoSecundario
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaOsm
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MarcadorMapa
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MascaraVisual
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

class EmpresaNovaViewModel(private val c: AppContainer) : ViewModel() {
    var nome by mutableStateOf("")
    var cnpj by mutableStateOf("")
    var email by mutableStateOf("")
    var telefone by mutableStateOf("")
    var cep by mutableStateOf("")
    var endereco by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("Fortaleza")
    var uf by mutableStateOf("CE")
    var ponto by mutableStateOf<Coordenada?>(null)
    var buscandoCep by mutableStateOf(false)
        private set
    var localizando by mutableStateOf(false)
        private set
    var salvando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var criada by mutableStateOf<CodigoAtivacao?>(null)
        private set

    val cnpjInvalido get() = cnpj.length == 14 && !Cnpj.valido(cnpj)

    /** CEP → rua/bairro/cidade (ViaCEP) e já procura o ponto no mapa. */
    fun buscarCep() {
        erro = null
        buscandoCep = true
        viewModelScope.launch {
            try {
                val r = Enderecos.porCep(cep)
                if (r == null) erro = "CEP não encontrado."
                else { endereco = if (r.endereco.isBlank()) "" else "${r.endereco}, "; bairro = r.bairro; cidade = r.cidade; uf = r.uf }
            } catch (_: Exception) { erro = "Não deu para consultar o CEP agora." }
            buscandoCep = false
        }
    }

    fun localizar() {
        if (endereco.isBlank() || cidade.isBlank()) { erro = "Preencha endereço e cidade para localizar."; return }
        erro = null
        localizando = true
        viewModelScope.launch {
            try {
                ponto = Enderecos.coordenadas(endereco.trim().trimEnd(','), bairro, cidade, uf)
                    ?: Enderecos.coordenadas("", bairro, cidade, uf) // sem achar o número, ao menos o bairro
                if (ponto == null) erro = "Endereço não encontrado no mapa: confira rua e bairro."
            } catch (_: Exception) { erro = "Serviço de mapas indisponível agora." }
            localizando = false
        }
    }

    fun salvar() {
        erro = when {
            nome.isBlank() -> "Informe o nome da empresa."
            !Cnpj.valido(cnpj) -> "CNPJ inválido: confira os números."
            !email.contains('@') || !email.substringAfter('@').contains('.') -> "Informe um e-mail válido."
            telefone.isNotEmpty() && telefone.length < 10 -> "Telefone com DDD (10 ou 11 números)."
            endereco.isBlank() || bairro.isBlank() || cidade.isBlank() || uf.length != 2 -> "Preencha o endereço completo."
            ponto == null -> "Toque em \"Localizar no mapa\" para marcar onde fica a empresa."
            else -> null
        }
        if (erro != null) return
        salvando = true
        viewModelScope.launch {
            try {
                criada = c.chamar {
                    cadastrarEmpresa(NovaEmpresa(nome.trim(), cnpj, email, telefone.ifBlank { null }, cep.ifBlank { null },
                        endereco.trim().trimEnd(',').trim(), bairro.trim(), cidade.trim(), uf, ponto!!.latitude, ponto!!.longitude))
                }
            } catch (e: ApiErro) { erro = e.message }
            salvando = false
        }
    }
}

/** Cadastro de empresa cliente no celular: dados, login do portal (e-mail) e endereço de entrega com o ponto no mapa. */
@Composable
fun EmpresaNovaTela(aoVoltar: () -> Unit, aoEntregar: (String) -> Unit) {
    val vm = viewModelDe { EmpresaNovaViewModel(it) }
    Scaffold(topBar = { Topo("Nova empresa", "A entrega vai para ela", aoVoltar = aoVoltar) }, containerColor = Cores.Fundo) { m ->
        Column(Modifier.padding(m).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val criada = vm.criada
            if (criada != null) {
                Cartao {
                    Text("Empresa cadastrada", style = MaterialTheme.typography.titleMedium, color = Cores.Verde)
                    Text("${criada.nome} · ${criada.matricula}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Código de ativação do Portal do Cliente", style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                    SelectionContainer {
                        Text(criada.codigoAtivacao, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                            letterSpacing = 3.sp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                    Text("Vale até ${Formato.dataHora(criada.codigoExpiraEm)}. Anote agora: ele não aparece de novo. " +
                        "No app ou no site, a empresa entra em \"Primeiro acesso\" com a matrícula e o código e cria a senha.",
                        style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                }
                BotaoPrincipal("Nova entrega para ${criada.nome}", { aoEntregar(criada.matricula) })
                BotaoSecundario("Voltar para empresas", aoVoltar)
                return@Column
            }

            Cartao {
                Text("Empresa", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.nome, { vm.nome = it.take(150) }, "Nome da empresa")
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.cnpj, { vm.cnpj = Mascaras.cnpj(it) }, "CNPJ", mascara = MascaraVisual.CNPJ,
                    maiusculas = true, dica = "12.345.678/0001-95")
                if (vm.cnpjInvalido) Text("CNPJ inválido: confira os números.", color = Cores.Vermelho, style = MaterialTheme.typography.bodySmall)
                else Text("Aceita o CNPJ novo, com letras.", color = Cores.TextoSuave, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.email, { vm.email = Mascaras.email(it) }, "E-mail (login do portal)", tipo = KeyboardType.Email)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.telefone, { vm.telefone = Mascaras.telefone(it) }, "Telefone (opcional)", tipo = KeyboardType.Phone,
                    mascara = MascaraVisual.TELEFONE)
            }

            Cartao {
                Text("Endereço de entrega", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CampoTexto(vm.cep, { vm.cep = Mascaras.cep(it); if (vm.cep.length == 8) vm.buscarCep() }, "CEP",
                        tipo = KeyboardType.Number, mascara = MascaraVisual.CEP, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = vm::buscarCep, enabled = vm.cep.length == 8 && !vm.buscandoCep) {
                        Text(if (vm.buscandoCep) "…" else "Buscar")
                    }
                }
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.endereco, { vm.endereco = it.take(200) }, "Endereço e número")
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.bairro, { vm.bairro = it.take(80) }, "Bairro")
                Spacer(Modifier.height(8.dp))
                Row {
                    CampoTexto(vm.cidade, { vm.cidade = it.take(80) }, "Cidade", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    CampoTexto(vm.uf, { vm.uf = Mascaras.uf(it) }, "UF", maiusculas = true, modifier = Modifier.width(80.dp), ultimo = true)
                }
                Spacer(Modifier.height(12.dp))
                BotaoPrincipal(if (vm.ponto == null) "Localizar no mapa" else "Localizar de novo", vm::localizar,
                    carregando = vm.localizando, cor = Cores.AzulEscuro)
                vm.ponto?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    MapaOsm(Modifier.fillMaxWidth().height(200.dp),
                        marcadores = listOf(MarcadorMapa(p, "★", Cores.Verde, vm.nome.ifBlank { "Empresa" })), chaveEnquadrar = p)
                    Text("%.6f, %.6f".format(p.latitude, p.longitude), style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
                }
            }

            vm.erro?.let { Aviso(it) }
            BotaoPrincipal("Cadastrar empresa", vm::salvar, carregando = vm.salvando)
            Spacer(Modifier.height(24.dp))
        }
    }
}
