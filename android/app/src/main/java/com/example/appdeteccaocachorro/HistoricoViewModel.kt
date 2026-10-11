
package com.example.appdeteccaocachorro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Estado da tela de histórico.
data class HistoricoUiState(
    val sessoes: List<HistoricoDto> = emptyList(),
    val detalhes: DetalhesInferenciaDto? = null,
    val carregando: Boolean = false,
    val erro: String? = null
)

class HistoricoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HistoricoUiState()
    )

    // A tela observa este StateFlow.
    val uiState: StateFlow<HistoricoUiState> =
        _uiState.asStateFlow()

    // Busca as sessões salvas no PostgreSQL.
    fun carregarHistorico() {

        if (_uiState.value.carregando) return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                carregando = true,
                erro = null
            )

            try {

                val sessoes = withContext(Dispatchers.IO) {
                    RetrofitClient.api.buscarHistorico()
                }

                _uiState.value = _uiState.value.copy(
                    sessoes = sessoes,
                    carregando = false,
                    erro = null
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    carregando = false,
                    erro = "Erro ao buscar histórico: ${e.message}"
                )
            }
        }
    }

    // Busca as caixas de uma sessão selecionada.
    fun carregarDetalhes(idSessao: String) {

        if (_uiState.value.carregando) return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                carregando = true,
                erro = null
            )

            try {

                val detalhes = withContext(Dispatchers.IO) {
                    RetrofitClient.api.buscarCaixas(idSessao)
                }

                _uiState.value = _uiState.value.copy(
                    detalhes = detalhes,
                    carregando = false,
                    erro = null
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    carregando = false,
                    erro = "Erro ao buscar detalhes: ${e.message}"
                )
            }
        }
    }

    // Volta à lista sem fazer outra consulta.
    fun voltarAoHistorico() {

        if (_uiState.value.carregando) return

        _uiState.value = _uiState.value.copy(
            detalhes = null,
            erro = null
        )
    }
}
