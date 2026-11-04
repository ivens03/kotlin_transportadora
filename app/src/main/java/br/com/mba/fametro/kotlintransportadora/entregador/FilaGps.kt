package br.com.mba.fametro.kotlintransportadora.entregador

import br.com.mba.fametro.kotlintransportadora.core.api.NovaPosicao
import br.com.mba.fametro.kotlintransportadora.core.api.Rede
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Fila "offline-first" das posições: grava em arquivo (um por rota) antes de tentar enviar.
 * Sem sinal, as leituras acumulam; quando volta, saem em lotes de até 500 (limite do back-end).
 * O back ignora repetidas (mesma rota + mesmo instante), então reenviar é seguro.
 */
class FilaGps(private val pasta: File, private val maximo: Int = 5_000) {

    private val trava = Mutex()
    private val memoria = mutableMapOf<Long, MutableList<NovaPosicao>>()
    private val serializador = ListSerializer(NovaPosicao.serializer())

    suspend fun adicionar(rotaId: Long, p: NovaPosicao): Int = trava.withLock {
        val fila = carregar(rotaId)
        fila += p
        if (fila.size > maximo) fila.subList(0, fila.size - maximo).clear() // guarda as mais recentes
        salvar(rotaId, fila)
        fila.size
    }

    suspend fun lote(rotaId: Long, tamanho: Int = 500): List<NovaPosicao> = trava.withLock {
        carregar(rotaId).take(tamanho).toList()
    }

    /** Tira da frente as [quantidade] que o servidor confirmou. */
    suspend fun confirmar(rotaId: Long, quantidade: Int): Int = trava.withLock {
        val fila = carregar(rotaId)
        fila.subList(0, minOf(quantidade, fila.size)).clear()
        salvar(rotaId, fila)
        fila.size
    }

    suspend fun tamanho(rotaId: Long): Int = trava.withLock { carregar(rotaId).size }

    suspend fun limpar(rotaId: Long) = trava.withLock {
        memoria.remove(rotaId)
        withContext(Dispatchers.IO) { arquivo(rotaId).delete() }
    }

    private suspend fun carregar(rotaId: Long): MutableList<NovaPosicao> = memoria.getOrPut(rotaId) {
        withContext(Dispatchers.IO) {
            val a = arquivo(rotaId)
            if (!a.exists()) mutableListOf()
            else runCatching { Rede.json.decodeFromString(serializador, a.readText()).toMutableList() }.getOrElse { mutableListOf() }
        }
    }

    private suspend fun salvar(rotaId: Long, fila: List<NovaPosicao>) = withContext(Dispatchers.IO) {
        pasta.mkdirs()
        val a = arquivo(rotaId)
        if (fila.isEmpty()) a.delete()
        else {
            val tmp = File(pasta, "rota-$rotaId.tmp")
            tmp.writeText(Rede.json.encodeToString(serializador, fila))
            tmp.renameTo(a) // troca atômica: nunca fica arquivo pela metade
        }
    }

    private fun arquivo(rotaId: Long) = File(pasta, "rota-$rotaId.json")
}
