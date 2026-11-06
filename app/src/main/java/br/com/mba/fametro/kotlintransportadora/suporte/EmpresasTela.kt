package br.com.mba.fametro.kotlintransportadora.suporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.core.api.ApiErro
import br.com.mba.fametro.kotlintransportadora.core.api.Cliente
import br.com.mba.fametro.kotlintransportadora.core.formato.Cnpj
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Cartao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.EstadoTela
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.TelaErro
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Vazio
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.viewModelDe
import kotlinx.coroutines.launch

class EmpresasViewModel(private val c: AppContainer) : ViewModel() {
    var estado by mutableStateOf<EstadoTela<List<Cliente>>>(EstadoTela.Carregando)
        private set

    fun carregar() {
        viewModelScope.launch {
            estado = try { EstadoTela.Pronto(c.chamar { clientes() }.content) } catch (e: ApiErro) { EstadoTela.Erro(e.message) }
        }
    }
}

/** Empresas clientes: a entrega é "para a empresa X" e ela acompanha pelo Portal do Cliente. */
@Composable
fun EmpresasTela(aoVoltar: () -> Unit, aoNova: () -> Unit, aoEntregar: (String) -> Unit) {
    val vm = viewModelDe { EmpresasViewModel(it) }
    LaunchedEffect(Unit) { vm.carregar() }

    Scaffold(
        topBar = { Topo("Empresas", "Clientes da transportadora", aoVoltar = aoVoltar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = aoNova, containerColor = Cores.Azul, contentColor = androidx.compose.ui.graphics.Color.White,
                icon = { Icon(Icons.Filled.AddBusiness, null) }, text = { Text("Nova empresa") })
        },
        containerColor = Cores.Fundo,
    ) { m ->
        Column(Modifier.padding(m).fillMaxSize()) {
            when (val e = vm.estado) {
                EstadoTela.Carregando -> Carregando()
                is EstadoTela.Erro -> TelaErro(e.mensagem) { vm.carregar() }
                is EstadoTela.Pronto -> if (e.dado.isEmpty()) Vazio("Nenhuma empresa", "Cadastre a primeira empresa cliente.", Icons.Filled.Storefront)
                else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(e.dado, key = { it.matricula }) { c ->
                        Cartao {
                            Text(c.nome, style = MaterialTheme.typography.titleMedium)
                            Text(listOfNotNull(c.cnpj?.let(Cnpj::formatar) ?: "sem CNPJ", c.matricula).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium, color = Cores.TextoSuave)
                            Text(if (c.temEndereco) "${c.endereco} — ${c.bairro}, ${c.cidade}" else "Sem endereço cadastrado",
                                style = MaterialTheme.typography.bodyMedium, color = if (c.temEndereco) Cores.Texto else Cores.Laranja)
                            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { aoEntregar(c.matricula) }, enabled = c.temEndereco) {
                                    Icon(Icons.Filled.LocalShipping, null, Modifier.padding(end = 6.dp)); Text("Nova entrega")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
