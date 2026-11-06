package br.com.mba.fametro.kotlintransportadora.acesso

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.mba.fametro.kotlintransportadora.core.sessao.Sessao
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Topo
import br.com.mba.fametro.kotlintransportadora.core.ui.componentes.Vazio
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.Cores
import br.com.mba.fametro.kotlintransportadora.navegacao.container

/** Administrativo e Financeiro trabalham no painel web: o app só avisa. */
@Composable
fun SemModuloTela(sessao: Sessao) {
    val c = container()
    Scaffold(topBar = { Topo("RotaCerta", sessao.nome, acoes = { BotaoSair { c.sessao.sair() } }) }, containerColor = Cores.Fundo) { m ->
        Column(Modifier.fillMaxSize().padding(m)) {
            Vazio("Use o painel web", "O perfil ${sessao.perfil} (${sessao.matricula}) trabalha pelo navegador. " +
                "No celular há os módulos do entregador, do cliente, do Suporte e o \"Como funciona\" do Desenvolvimento.", Icons.Filled.Computer)
        }
    }
}

@Composable
fun BotaoSair(aoSair: () -> Unit) {
    IconButton(onClick = aoSair) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = Cores.Marinho) }
}
