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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.acesso.CampoTexto
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.CargaCriada
import br.com.mba.fametro.kotlintransportadora.core.api.Cliente
import br.com.mba.fametro.kotlintransportadora.core.api.Entregador
import br.com.mba.fametro.kotlintransportadora.core.api.IncluirCargas
import br.com.mba.fametro.kotlintransportadora.core.api.NovaCarga
import br.com.mba.fametro.kotlintransportadora.core.api.NovaRota
import br.com.mba.fametro.kotlintransportadora.core.api.RotaResumo
import br.com.mba.fametro.kotlintransportadora.core.api.VeiculoResumo
import br.com.mba.fametro.kotlintransportadora.core.formato.Cnpj
import br.com.mba.fametro.kotlintransportadora.core.formato.Mascaras
import br.com.mba.fametro.kotlintransportadora.core.geo.Coordenada
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Aviso
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoPrincipal
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.BotaoSecundario
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MapaOsm
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.MarcadorMapa
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Opcao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Seletor
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NovaEntregaViewModel(private val c: AppContainer) : ViewModel() {
    var empresas by mutableStateOf<List<Cliente>>(emptyList()); private set
    var entregadores by mutableStateOf<List<Entregador>>(emptyList()); private set
    var veiculos by mutableStateOf<List<VeiculoResumo>>(emptyList()); private set
    /** Rotas PLANEJADA e EM_ANDAMENTO: quem está nelas não pode ganhar rota nova. */
    var rotasAtivas by mutableStateOf<List<RotaResumo>>(emptyList()); private set
    var carregando by mutableStateOf(true); private set
    var erroCarga by mutableStateOf<String?>(null); private set

    var empresa by mutableStateOf<String?>(null)
    var descricao by mutableStateOf("")
    var peso by mutableStateOf("")
    var volume by mutableStateOf("")
    var entregador by mutableStateOf<String?>(null); private set
    var veiculo by mutableStateOf<Long?>(null)

    fun rotaDoEntregador(matricula: String) = rotasAtivas.find { it.entregadorMatricula == matricula }
    fun rotaDoVeiculo(id: Long) = rotasAtivas.find { it.veiculoId == id }

    /** Entregador escolhido já tem rota que ainda não saiu: a entrega entra nela, com o veículo dela. */
    val rotaPlanejada get() = entregador?.let(::rotaDoEntregador)?.takeIf { it.status == "PLANEJADA" }

    fun escolherEntregador(matricula: String?) {
        entregador = matricula
        // com rota planejada, o veículo é o dela; trocando para outro entregador, solta o veículo que estava em uso
        veiculo = rotaPlanejada?.veiculoId ?: veiculo?.takeIf { rotaDoVeiculo(it) == null }
    }

    var salvando by mutableStateOf(false); private set
    var erro by mutableStateOf<String?>(null); private set
    /** Criou a carga mas não gerou rota (sem entregador/veículo): mostra o código e volta. */
    var semRota by mutableStateOf<CargaCriada?>(null); private set

    val escolhida get() = empresas.find { it.matricula == empresa }

    fun carregar(empresaInicial: String?) {
        carregando = true; erroCarga = null
        viewModelScope.launch {
            try {
                // as três listas em paralelo
                val e = async { c.chamar { clientes() }.content }
                val m = async { c.chamar { entregadores() }.content }
                val v = async { c.chamar { veiculos() }.content }
                val p = async { c.chamar { rotas("PLANEJADA", 100) }.content }
                val a = async { c.chamar { rotas("EM_ANDAMENTO", 100) }.content }
                empresas = e.await().filter { it.status != "INATIVO" }.sortedBy { it.nome.lowercase() }
                entregadores = m.await().sortedBy { it.nome.lowercase() }
                veiculos = v.await().sortedBy { it.placa }
                rotasAtivas = p.await() + a.await()
                if (empresa == null && empresas.any { it.matricula == empresaInicial && it.temEndereco }) empresa = empresaInicial
            } catch (ex: ApiErro) { erroCarga = ex.message }
            carregando = false
        }
    }

    /** Salva a carga; com entregador e veículo, já gera a rota e devolve o id dela. */
    fun salvar(aoGerarRota: (Long) -> Unit) {
        val emp = escolhida
        val kg = Mascaras.numero(peso)
        val m3 = Mascaras.numero(volume)
        erro = when {
            emp == null -> "Escolha a empresa que vai receber."
            !emp.temEndereco -> "Essa empresa não tem endereço de entrega cadastrado."
            descricao.isBlank() -> "Diga o que vai ser entregue."
            kg == null || kg <= 0 -> "Informe o peso em kg."
            m3 == null || m3 <= 0 -> "Informe o volume em m³."
            (entregador == null) != (veiculo == null) -> "Para gerar a rota, escolha entregador e veículo (ou nenhum dos dois)."
            else -> null
        }
        if (erro != null) return
        emp!!
        salvando = true
        viewModelScope.launch {
            try {
                val carga = c.chamar {
                    cadastrarCarga(NovaCarga(descricao.trim(), kg!!, m3!!, emp.nome, emp.telefone, emp.endereco!!, emp.bairro.orEmpty(),
                        emp.cidade.orEmpty(), emp.uf.orEmpty(), emp.cep, emp.latitude!!, emp.longitude!!, clienteMatricula = emp.matricula))
                }
                val ent = entregador; val vei = veiculo; val existente = rotaPlanejada
                if (ent != null && vei != null) {
                    try {
                        val rota = c.chamar {
                            if (existente != null) incluirNaRota(existente.id, IncluirCargas(listOf(carga.id)))
                            else gerarRota(NovaRota(vei, ent, listOf(carga.id)))
                        }
                        aoGerarRota(rota.id)
                    } catch (ex: ApiErro) {
                        semRota = carga
                        erro = "A entrega ${carga.codigo} foi criada, mas não entrou em rota: ${ex.message}"
                    }
                } else semRota = carga
            } catch (ex: ApiErro) { erro = ex.message }
            salvando = false
        }
    }
}

/** Nova entrega no celular: para qual empresa, o que vai e (opcional) quem leva — aí a rota já sai pronta. */
@Composable
fun NovaEntregaTela(empresaInicial: String?, aoVoltar: () -> Unit, aoNovaEmpresa: () -> Unit, aoAbrirRota: (Long) -> Unit) {
    val vm = viewModelDe { NovaEntregaViewModel(it) }
    LaunchedEffect(Unit) { vm.carregar(empresaInicial) }

    Scaffold(topBar = { Topo("Nova entrega", "Para uma empresa cliente", aoVoltar = aoVoltar) }, containerColor = Cores.Fundo) { m ->
        if (vm.carregando) { Carregando(Modifier.padding(m)); return@Scaffold }
        vm.erroCarga?.let { TelaErro(it) { vm.carregar(empresaInicial) }; return@Scaffold }

        Column(Modifier.padding(m).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.semRota?.let { carga ->
                Cartao {
                    Text("Entrega ${carga.codigo} cadastrada", style = MaterialTheme.typography.titleMedium, color = Cores.Verde)
                    Text(if (vm.erro == null) "Ela fica aguardando rota: dá para montar a rota depois, no painel." else vm.erro!!,
                        style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                }
                BotaoPrincipal("Voltar", aoVoltar)
                return@Column
            }

            Cartao {
                Text("Para qual empresa", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Seletor("Empresa", vm.empresas.map {
                    Opcao(it.matricula, it.nome, if (it.temEndereco) it.cnpj?.let(Cnpj::formatar) ?: it.matricula else "Sem endereço cadastrado",
                        habilitada = it.temEndereco)
                }, vm.empresa, { vm.empresa = it }, textoVazio = "Nenhuma empresa cadastrada")
                TextButton(onClick = aoNovaEmpresa) { Text("+ Cadastrar outra empresa") }
                vm.escolhida?.takeIf { it.temEndereco }?.let { e ->
                    Text("Entregar em", style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
                    Text("${e.endereco} — ${e.bairro}, ${e.cidade}/${e.uf}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    MapaOsm(Modifier.fillMaxWidth().height(160.dp),
                        marcadores = listOf(MarcadorMapa(Coordenada(e.latitude!!, e.longitude!!), "★", Cores.Verde, e.nome)),
                        chaveEnquadrar = e.matricula)
                }
            }

            Cartao {
                Text("O que vai", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CampoTexto(vm.descricao, { vm.descricao = it.take(200) }, "Descrição")
                Spacer(Modifier.height(8.dp))
                Row {
                    CampoTexto(vm.peso, { vm.peso = Mascaras.decimal(it, 2) }, "Peso (kg)", tipo = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    CampoTexto(vm.volume, { vm.volume = Mascaras.decimal(it, 3) }, "Volume (m³)", tipo = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f), ultimo = true)
                }
            }

            Cartao {
                Text("Quem leva (opcional)", style = MaterialTheme.typography.titleMedium)
                Text("Escolhendo os dois, a rota já sai pronta e aparece no app do entregador. Se ele já tem rota que ainda não saiu, a entrega entra nela.",
                    style = MaterialTheme.typography.bodySmall, color = Cores.TextoSuave)
                Spacer(Modifier.height(8.dp))
                Seletor("Entregador", listOf(Opcao<String?>(null, "Definir depois")) + vm.entregadores.map { e ->
                    val r = vm.rotaDoEntregador(e.matricula)
                    Opcao<String?>(e.matricula, e.nome, when {
                        r == null -> e.matricula
                        r.status == "EM_ANDAMENTO" -> "Na rua agora (${r.codigo})"
                        else -> "Tem a ${r.codigo}, que ainda não saiu: leva junto"
                    }, habilitada = r?.status != "EM_ANDAMENTO")
                }, vm.entregador, vm::escolherEntregador)
                Spacer(Modifier.height(8.dp))
                val planejada = vm.rotaPlanejada
                if (planejada != null) {
                    Text("Vai na rota ${planejada.codigo} · ${planejada.veiculoPlaca} (${planejada.totalParadas} parada(s)); a ordem das paradas é recalculada.",
                        style = MaterialTheme.typography.bodyMedium, color = Cores.Azul)
                } else {
                    Seletor("Veículo", listOf(Opcao<Long?>(null, "Definir depois")) + vm.veiculos.map { v ->
                        val r = vm.rotaDoVeiculo(v.id)
                        Opcao<Long?>(v.id, "${v.placa} · ${v.modelo}",
                            r?.let { "Em uso na ${it.codigo}" } ?: v.capacidadeCargaKg?.let { kg -> "até ${kg.toInt()} kg" }, habilitada = r == null)
                    }, vm.veiculo, { vm.veiculo = it })
                }
            }

            vm.erro?.let { Aviso(it) }
            BotaoPrincipal(when {
                vm.rotaPlanejada != null -> "Cadastrar e incluir na ${vm.rotaPlanejada!!.codigo}"
                vm.entregador != null && vm.veiculo != null -> "Cadastrar e gerar rota"
                else -> "Cadastrar entrega"
            },
                { vm.salvar(aoAbrirRota) }, carregando = vm.salvando)
            BotaoSecundario("Cancelar", aoVoltar)
            Spacer(Modifier.height(24.dp))
        }
    }
}
