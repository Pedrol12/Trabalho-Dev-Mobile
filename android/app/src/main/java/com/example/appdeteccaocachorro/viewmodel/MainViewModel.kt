package com.example.appdeteccaocachorro.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdeteccaocachorro.Deteccao
import com.example.appdeteccaocachorro.YoloDetector
import com.example.appdeteccaocachorro.network.CaixaDelimitadoraDto
import com.example.appdeteccaocachorro.network.DetalhesSessaoDto
import com.example.appdeteccaocachorro.network.DimensaoImagemDto
import com.example.appdeteccaocachorro.network.HistoricoSessaoDto
import com.example.appdeteccaocachorro.network.InferenciaRequest
import com.example.appdeteccaocachorro.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = YoloDetector(application.applicationContext)

    // Estado da Tela 1: Parametros e selecao
    val nomeModeloAtivo: String get() = detector.nomeModeloAtivo

    private val _imagemSelecionada = MutableStateFlow<Bitmap?>(null)
    val imagemSelecionada: StateFlow<Bitmap?> = _imagemSelecionada.asStateFlow()

    private val _limiarConfianca = MutableStateFlow(0.25f)
    val limiarConfianca: StateFlow<Float> = _limiarConfianca.asStateFlow()

    private val _rotuloPersonalizado = MutableStateFlow("cachorro")
    val rotuloPersonalizado: StateFlow<String> = _rotuloPersonalizado.asStateFlow()

    // Estado da Tela 2: Resultados da Inferencia
    private val _deteccoes = MutableStateFlow<List<Deteccao>>(emptyList())
    val deteccoes: StateFlow<List<Deteccao>> = _deteccoes.asStateFlow()

    private val _tempoExecucaoMs = MutableStateFlow(0L)
    val tempoExecucaoMs: StateFlow<Long> = _tempoExecucaoMs.asStateFlow()

    private val _idSessaoAtual = MutableStateFlow("")
    val idSessaoAtual: StateFlow<String> = _idSessaoAtual.asStateFlow()

    private val _estaProcessando = MutableStateFlow(false)
    val estaProcessando: StateFlow<Boolean> = _estaProcessando.asStateFlow()

    private val _statusEnvioBackend = MutableStateFlow<String?>(null)
    val statusEnvioBackend: StateFlow<String?> = _statusEnvioBackend.asStateFlow()

    // Estado da Tela 3: Historico de Sessoes
    private val _historico = MutableStateFlow<List<HistoricoSessaoDto>>(emptyList())
    val historico: StateFlow<List<HistoricoSessaoDto>> = _historico.asStateFlow()

    private val _carregandoHistorico = MutableStateFlow(false)
    val carregandoHistorico: StateFlow<Boolean> = _carregandoHistorico.asStateFlow()

    private val _filtroHistorico = MutableStateFlow("")
    val filtroHistorico: StateFlow<String> = _filtroHistorico.asStateFlow()

    // Estado da Tela 4: Detalhamento de Sessao
    private val _detalhesSessao = MutableStateFlow<DetalhesSessaoDto?>(null)
    val detalhesSessao: StateFlow<DetalhesSessaoDto?> = _detalhesSessao.asStateFlow()

    private val _carregandoDetalhes = MutableStateFlow(false)
    val carregandoDetalhes: StateFlow<Boolean> = _carregandoDetalhes.asStateFlow()

    // Aba de navegacao ativa: 0 = Detectar, 1 = Historico
    private val _abaSelecionada = MutableStateFlow(0)
    val abaSelecionada: StateFlow<Int> = _abaSelecionada.asStateFlow()

    // Sub-tela no fluxo: "TELA_1", "TELA_2", "TELA_3", "TELA_4"
    private val _telaAtual = MutableStateFlow("TELA_1")
    val telaAtual: StateFlow<String> = _telaAtual.asStateFlow()

    fun definirAba(aba: Int) {
        _abaSelecionada.value = aba
        if (aba == 0) {
            _telaAtual.value = if (_deteccoes.value.isNotEmpty() && _imagemSelecionada.value != null) "TELA_2" else "TELA_1"
        } else {
            _telaAtual.value = "TELA_3"
            carregarHistorico()
        }
    }

    fun navegarPara(tela: String) {
        _telaAtual.value = tela
    }

    fun setImagem(bitmap: Bitmap) {
        _imagemSelecionada.value = bitmap
        _deteccoes.value = emptyList()
        _statusEnvioBackend.value = null
    }

    fun setLimiarConfianca(valor: Float) {
        _limiarConfianca.value = valor
    }

    fun setRotuloPersonalizado(rotulo: String) {
        _rotuloPersonalizado.value = rotulo
    }

    fun setFiltroHistorico(termo: String) {
        _filtroHistorico.value = termo
    }

    fun carregarModeloExterno(caminho: String, nome: String): Boolean {
        return detector.carregarModeloExterno(caminho, nome)
    }

    // Executa a inferencia local com o modelo ONNX (RF4)
    fun executarInferencia() {
        val bitmap = _imagemSelecionada.value ?: return
        _estaProcessando.value = true
        _statusEnvioBackend.value = null

        viewModelScope.launch(Dispatchers.Default) {
            val inicio = System.currentTimeMillis()
            val resultados = detector.detectar(
                bitmapOriginal = bitmap,
                limiarConfianca = _limiarConfianca.value,
                rotuloPadrao = _rotuloPersonalizado.value
            )
            val duracao = System.currentTimeMillis() - inicio

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val idGerado = "sessao_${sdf.format(Date())}"

            withContext(Dispatchers.Main) {
                _deteccoes.value = resultados
                _tempoExecucaoMs.value = duracao
                _idSessaoAtual.value = idGerado
                _estaProcessando.value = false
                _telaAtual.value = "TELA_2"
            }
        }
    }

    // Envia o payload da inferencia para o backend do Aluno 1 (RF6)
    fun enviarParaBackend() {
        val bitmap = _imagemSelecionada.value ?: return
        val caixas = _deteccoes.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _statusEnvioBackend.value = "Enviando..."
                val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                val dataHora = sdfIso.format(Date())

                val mediaConf = if (caixas.isNotEmpty()) {
                    caixas.map { it.confianca }.average().toFloat()
                } else {
                    0f
                }

                val payload = InferenciaRequest(
                    idSessao = _idSessaoAtual.value,
                    dataHora = dataHora,
                    nomeModelo = detector.nomeModeloAtivo,
                    tempoExecucaoMs = _tempoExecucaoMs.value,
                    totalObjetos = caixas.size,
                    confiancaMedia = mediaConf,
                    dimensaoImagem = DimensaoImagemDto(
                        larguraPx = bitmap.width,
                        alturaPx = bitmap.height
                    ),
                    caixasDelimitadoras = caixas.map { c ->
                        CaixaDelimitadoraDto(
                            idCaixa = c.idCaixa,
                            rotuloClasse = c.rotuloClasse,
                            confianca = c.confianca,
                            larguraPx = c.larguraPx,
                            alturaPx = c.alturaPx,
                            centroideX = c.centroideX,
                            centroideY = c.centroideY,
                            areaPx2 = c.areaPx2
                        )
                    }
                )

                val resp = RetrofitClient.instance.enviarInferencia(payload)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        _statusEnvioBackend.value = "Sucesso: Salvo no Banco!"
                    } else {
                        _statusEnvioBackend.value = "Erro HTTP: ${resp.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _statusEnvioBackend.value = "Falha de conexão: ${e.message}"
                }
            }
        }
    }

    // Carrega o historico cronologico de analises (Tela 3 / RF8)
    fun carregarHistorico() {
        _carregandoHistorico.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.instance.obterHistorico()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body() != null) {
                        _historico.value = resp.body()!!
                    }
                    _carregandoHistorico.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _carregandoHistorico.value = false
                }
            }
        }
    }

    // Carrega a auditoria detalhada de uma sessao e suas caixas (Tela 4 / RF8)
    fun carregarDetalhesSessao(idSessao: String) {
        _carregandoDetalhes.value = true
        _telaAtual.value = "TELA_4"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.instance.obterDetalhesSessao(idSessao)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body() != null) {
                        _detalhesSessao.value = resp.body()
                    }
                    _carregandoDetalhes.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _carregandoDetalhes.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.fechar()
    }
}
