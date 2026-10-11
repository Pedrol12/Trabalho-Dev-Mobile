
package com.example.appdeteccaocachorro

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado observado pela interface Compose.
data class InferenciaUiState(
    val processando: Boolean = false,
    val resultado: ProcessamentoInferencia? = null,
    val erro: String? = null
)

class InferenciaViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState =
        MutableStateFlow(InferenciaUiState())

    val uiState: StateFlow<InferenciaUiState> =
        _uiState.asStateFlow()

    // Executa a análise sem bloquear a interface.
    fun analisarImagem(
        bitmap: Bitmap,
        confiancaMinima: Float = 0.5f
    ) {

        // Evita iniciar duas análises simultâneas.
        if (_uiState.value.processando) return

        _uiState.value = InferenciaUiState(
            processando = true
        )

        viewModelScope.launch {

            try {

                val processamento =
                    ProcessadorInferencia.analisarESalvar(
                        context = getApplication(),
                        bitmap = bitmap,
                        confiancaMinima = confiancaMinima
                    )

                _uiState.value = InferenciaUiState(
                    processando = false,
                    resultado = processamento
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value = InferenciaUiState(
                    processando = false,
                    erro = e.message
                        ?: "Erro desconhecido na inferência."
                )
            }
        }
    }

    // Limpa o resultado e as mensagens anteriores.
    fun limparEstado() {
        if (!_uiState.value.processando) {
            _uiState.value = InferenciaUiState()
        }
    }
}
