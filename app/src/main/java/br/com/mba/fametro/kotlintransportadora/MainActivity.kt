package br.com.mba.fametro.kotlintransportadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.mba.fametro.kotlintransportadora.core.ui.tema.RotaCertaTema
import br.com.mba.fametro.kotlintransportadora.navegacao.RaizApp

/** Uma Activity só: todas as telas são Composables trocadas pela navegação. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RotaCertaApp).container
        setContent {
            RotaCertaTema { RaizApp(container) }
        }
    }
}
