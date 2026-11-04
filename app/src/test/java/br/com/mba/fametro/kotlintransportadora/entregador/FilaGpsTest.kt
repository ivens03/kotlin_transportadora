package br.com.mba.fametro.kotlintransportadora.entregador

import br.com.mba.fametro.kotlintransportadora.core.api.NovaPosicao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FilaGpsTest {

    @get:Rule val pasta = TemporaryFolder()

    private fun posicao(i: Int) = NovaPosicao(-3.7 + i / 1e5, -38.5, "2026-11-04T12:00:${"%02d".format(i % 60)}Z")

    @Test
    fun `acumula, entrega em lote e tira so o que o servidor confirmou`() = runTest {
        val fila = FilaGps(pasta.root)
        repeat(7) { fila.adicionar(1, posicao(it)) }

        assertEquals(listOf(posicao(0), posicao(1), posicao(2)), fila.lote(1, 3))
        assertEquals(4, fila.confirmar(1, 3))
        assertEquals(posicao(3), fila.lote(1, 3).first())
    }

    @Test
    fun `sobrevive ao app ser fechado (le do arquivo)`() = runTest {
        FilaGps(pasta.root).apply { repeat(3) { adicionar(9, posicao(it)) } }

        val reaberta = FilaGps(pasta.root)
        assertEquals(3, reaberta.tamanho(9))
        assertEquals(posicao(0), reaberta.lote(9).first())
    }

    @Test
    fun `passando do maximo descarta as mais antigas`() = runTest {
        val fila = FilaGps(pasta.root, maximo = 5)
        repeat(8) { fila.adicionar(1, posicao(it)) }
        assertEquals((3..7).map(::posicao), fila.lote(1))
    }

    @Test
    fun `confirmar tudo ou limpar apaga o arquivo`() = runTest {
        val fila = FilaGps(pasta.root)
        fila.adicionar(1, posicao(0))
        fila.adicionar(2, posicao(0))
        fila.confirmar(1, 10)
        fila.limpar(2)
        assertFalse(File(pasta.root, "rota-1.json").exists())
        assertFalse(File(pasta.root, "rota-2.json").exists())
        assertEquals(0, fila.tamanho(2))
    }
}
