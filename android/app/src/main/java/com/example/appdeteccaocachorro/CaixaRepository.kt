
package com.example.appdeteccaocachorro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Gerencia as caixas delimitadoras dos cachorros
class CaixaRepository {

    // Lista que armazena as caixas detectadas
    private val _caixas = MutableStateFlow<List<Deteccao>>(emptyList())

    // Permite que as telas observem a lista
    val caixas = _caixas.asStateFlow()

    // CREATE - Adiciona uma caixa
    fun adicionar(deteccao: Deteccao) {
        require(coordenadasValidas(deteccao)) {
            "Coordenadas ou confiança inválidas"
        }

        require(buscarPorId(deteccao.id) == null) {
            "Já existe uma caixa com esse ID"
        }

        _caixas.value = _caixas.value + deteccao
    }

    // READ - Lista todas as caixas
    fun listar(): List<Deteccao> {
        return _caixas.value
    }

    // READ - Busca uma caixa pelo ID
    fun buscarPorId(id: String): Deteccao? {
        return _caixas.value.find { it.id == id }
    }

    // UPDATE - Atualiza uma caixa existente
    fun atualizar(deteccao: Deteccao): Boolean {
        require(coordenadasValidas(deteccao)) {
            "Coordenadas ou confiança inválidas"
        }

        if (buscarPorId(deteccao.id) == null) {
            return false
        }

        _caixas.value = _caixas.value.map { caixa ->
            if (caixa.id == deteccao.id) deteccao else caixa
        }

        return true
    }

    // DELETE - Exclui uma caixa pelo ID
    fun excluir(id: String): Boolean {
        if (buscarPorId(id) == null) {
            return false
        }

        _caixas.value = _caixas.value.filterNot {
            it.id == id
        }

        return true
    }

    // Adiciona as caixas encontradas pelo YOLO
    fun adicionarDeteccoes(deteccoes: List<Deteccao>) {
        deteccoes.forEach { adicionar(it) }
    }

    // Verifica se as coordenadas são válidas
    private fun coordenadasValidas(d: Deteccao): Boolean {
        return listOf(
            d.xMin, d.yMin, d.xMax, d.yMax,
            d.confianca
        ).all { it.isFinite() } &&
                d.xMin >= 0f &&
                d.yMin >= 0f &&
                d.xMax > d.xMin &&
                d.yMax > d.yMin &&
                d.confianca in 0f..1f
    }
}
