package br.com.mba.fametro.kotlintransportadora.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.mba.fametro.kotlintransportadora.AppContainer
import br.com.mba.fametro.kotlintransportadora.RotaCertaApp
import br.com.mba.fametro.kotlintransportadora.acesso.EntrarTela
import br.com.mba.fametro.kotlintransportadora.acesso.PrimeiroAcessoTela
import br.com.mba.fametro.kotlintransportadora.acesso.SemModuloTela
import br.com.mba.fametro.kotlintransportadora.desenvolvimento.ComoFuncionaTela
import br.com.mba.fametro.kotlintransportadora.acesso.TrocarCredencialTela
import br.com.mba.fametro.kotlintransportadora.cliente.AvisosTela
import br.com.mba.fametro.kotlintransportadora.cliente.EncomendaDetalheTela
import br.com.mba.fametro.kotlintransportadora.cliente.EncomendasTela
import br.com.mba.fametro.kotlintransportadora.core.sessao.EstadoSessao
import br.com.mba.fametro.kotlintransportadora.core.sessao.Modulo
import br.com.mba.fametro.kotlintransportadora.core.sessao.Sessao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Carregando
import br.com.mba.fametro.kotlintransportadora.entregador.MinhasRotasTela
import br.com.mba.fametro.kotlintransportadora.entregador.RotaExecucaoTela
import br.com.mba.fametro.kotlintransportadora.suporte.EmpresaNovaTela
import br.com.mba.fametro.kotlintransportadora.suporte.EmpresasTela
import br.com.mba.fametro.kotlintransportadora.suporte.NovaEntregaTela
import br.com.mba.fametro.kotlintransportadora.suporte.RotaMapaTela
import br.com.mba.fametro.kotlintransportadora.suporte.RotasAcompanharTela

/**
 * Raiz: decide o que mostrar pela sessão. Deslogado → telas de acesso; logado → o módulo do perfil.
 * Trocar de estado troca o NavHost inteiro (o "voltar" nunca leva de uma área para a outra).
 */
@Composable
fun RaizApp(container: AppContainer) {
    val estado by container.sessao.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoSessao.Carregando -> Carregando()
        is EstadoSessao.Deslogado -> key("acesso") { NavegacaoAcesso(e.aviso) }
        is EstadoSessao.Logado -> key(e.sessao.token) { NavegacaoModulo(e.sessao) }
    }
}

@Composable
private fun NavegacaoAcesso(aviso: String?) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Entrar) {
        composable<Entrar> { entrada ->
            val avisoVolta = entrada.savedStateHandle.get<String>("aviso")
            EntrarTela(
                aviso = avisoVolta ?: aviso,
                aoPrimeiroAcesso = { nav.navigate(PrimeiroAcesso) },
                aoCredencialVencida = { matricula -> nav.navigate(TrocarCredencial(matricula, vencida = true)) },
                aoTrocarCredencial = { nav.navigate(TrocarCredencial()) },
            )
        }
        composable<PrimeiroAcesso> {
            PrimeiroAcessoTela(aoVoltar = { nav.popBackStack() }, aoConcluir = { msg -> voltarComAviso(nav, msg) })
        }
        composable<TrocarCredencial> { entrada ->
            val destino = entrada.toRoute<TrocarCredencial>()
            TrocarCredencialTela(destino.matricula, destino.vencida, aoVoltar = { nav.popBackStack() },
                aoConcluir = { msg -> voltarComAviso(nav, msg) })
        }
    }
}

private fun voltarComAviso(nav: androidx.navigation.NavController, mensagem: String) {
    nav.previousBackStackEntry?.savedStateHandle?.set("aviso", mensagem)
    nav.popBackStack()
}

@Composable
private fun NavegacaoModulo(sessao: Sessao) {
    val nav = rememberNavController()
    val inicio: Any = when (sessao.modulo) {
        Modulo.ENTREGADOR -> MinhasRotas
        Modulo.CLIENTE -> Encomendas
        Modulo.SUPORTE -> RotasAcompanhar
        Modulo.DESENVOLVIMENTO -> ComoFunciona
        Modulo.SEM_MODULO -> SemModulo
    }
    // cliente: acompanha os avisos do webhook enquanto estiver logado (notificação a cada aviso novo)
    if (sessao.modulo == Modulo.CLIENTE) {
        val vigia = container().avisos
        DisposableEffect(sessao.token) {
            vigia.iniciar()
            onDispose { vigia.parar() }
        }
    }
    NavHost(nav, startDestination = inicio) {
        composable<MinhasRotas> { MinhasRotasTela(sessao, aoAbrir = { nav.navigate(RotaExecucao(it)) }) }
        composable<RotaExecucao> { RotaExecucaoTela(it.toRoute<RotaExecucao>().id, aoVoltar = { nav.popBackStack() }) }
        composable<Encomendas> {
            EncomendasTela(sessao, aoAbrir = { nav.navigate(EncomendaDetalhe(it)) }, aoVerAvisos = { nav.navigate(Avisos) })
        }
        composable<Avisos> { AvisosTela(aoVoltar = { nav.popBackStack() }) }
        composable<EncomendaDetalhe> { EncomendaDetalheTela(it.toRoute<EncomendaDetalhe>().codigo, aoVoltar = { nav.popBackStack() }) }
        composable<RotasAcompanhar> {
            RotasAcompanharTela(sessao, aoAbrir = { nav.navigate(RotaMapa(it)) },
                aoEmpresas = { nav.navigate(Empresas) }, aoNovaEntrega = { nav.navigate(NovaEntrega()) })
        }
        composable<Empresas> {
            EmpresasTela(aoVoltar = { nav.popBackStack() }, aoNova = { nav.navigate(EmpresaNova) },
                aoEntregar = { nav.navigate(NovaEntrega(it)) })
        }
        composable<EmpresaNova> {
            // "Nova entrega para X" troca o cadastro pela entrega: o voltar cai na lista de empresas
            EmpresaNovaTela(aoVoltar = { nav.popBackStack() },
                aoEntregar = { nav.navigate(NovaEntrega(it)) { popUpTo<EmpresaNova> { inclusive = true } } })
        }
        composable<NovaEntrega> {
            NovaEntregaTela(it.toRoute<NovaEntrega>().empresa, aoVoltar = { nav.popBackStack() },
                aoNovaEmpresa = { nav.navigate(EmpresaNova) },
                // rota gerada: abre o mapa dela no lugar do formulário
                aoAbrirRota = { id -> nav.navigate(RotaMapa(id)) { popUpTo<NovaEntrega> { inclusive = true } } })
        }
        composable<RotaMapa> { RotaMapaTela(it.toRoute<RotaMapa>().id, aoVoltar = { nav.popBackStack() }) }
        composable<ComoFunciona> { ComoFuncionaTela(sessao) }
        composable<SemModulo> { SemModuloTela(sessao) }
    }
}

/** Container da aplicação a partir de um Composable. */
@Composable
fun container(): AppContainer = (LocalContext.current.applicationContext as RotaCertaApp).container

/** ViewModel criado com o container (fábrica simples, sem framework de DI). */
@Composable
inline fun <reified VM : ViewModel> viewModelDe(chave: String? = null, crossinline criar: (AppContainer) -> VM): VM {
    val c = container()
    return viewModel(key = chave) { criar(c) }
}
