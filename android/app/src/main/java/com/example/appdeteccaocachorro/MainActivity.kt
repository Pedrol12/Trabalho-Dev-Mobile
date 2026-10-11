package com.example.appdeteccaocachorro

import android.graphics.Bitmap
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.lifecycle.ViewModelProvider

import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.appdeteccaocachorro.ui.theme.AppDeteccaoCachorroTheme

import kotlinx.coroutines.launch

private const val TELA_INICIO = 0
private const val TELA_CAIXAS = 1
private const val TELA_RESULTADO = 2
private const val TELA_HISTORICO = 3
private const val TELA_CAMERA = 4

private const val ORIGEM_NENHUMA = -1

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Uma instância do ViewModel para a Activity.
        val inferenciaViewModel =
            ViewModelProvider(this)[
                InferenciaViewModel::class.java
            ]

        setContent {

            AppDeteccaoCachorroTheme {

                val repository = remember {
                    CaixaRepository()
                }

                val scope = rememberCoroutineScope()

                val snackbarHostState = remember {
                    SnackbarHostState()
                }

                // Observa o StateFlow do ViewModel.
                val estadoInferencia by
                inferenciaViewModel.uiState.collectAsState()

                val processando =
                    estadoInferencia.processando

                var telaAtual by remember {
                    mutableIntStateOf(TELA_INICIO)
                }

                val pilhaTelas = remember {
                    mutableStateListOf<Int>()
                }

                var imagemResultado by remember {
                    mutableStateOf<Bitmap?>(null)
                }

                var deteccoesResultado by remember {
                    mutableStateOf<List<Deteccao>>(emptyList())
                }

                var tempoExecucaoMs by remember {
                    mutableLongStateOf(0L)
                }

                var erroCamera by remember {
                    mutableStateOf<String?>(null)
                }

                var erroGaleria by remember {
                    mutableStateOf<String?>(null)
                }

                // Identifica qual tela solicitou a análise.
                var origemInferencia by remember {
                    mutableIntStateOf(ORIGEM_NENHUMA)
                }

                // Filtro escolhido para a imagem da galeria.
                var filtroGaleria by remember {
                    mutableStateOf("Todas as classes")
                }

                fun navegarPara(destino: Int) {

                    if (
                        processando ||
                        destino == telaAtual
                    ) {
                        return
                    }

                    pilhaTelas.add(telaAtual)

                    telaAtual = destino
                }

                fun voltar() {

                    if (processando) return

                    if (pilhaTelas.isNotEmpty()) {

                        telaAtual = pilhaTelas.removeAt(
                            pilhaTelas.lastIndex
                        )

                    } else {

                        telaAtual = TELA_INICIO
                    }
                }

                val podeVoltar =
                    pilhaTelas.isNotEmpty() ||
                            telaAtual != TELA_INICIO

                val tituloTela = when (telaAtual) {

                    TELA_INICIO ->
                        "Início"

                    TELA_CAIXAS ->
                        "Caixas delimitadoras"

                    TELA_RESULTADO ->
                        "Resultado da detecção"

                    TELA_HISTORICO ->
                        "Histórico"

                    TELA_CAMERA ->
                        "Câmera"

                    else ->
                        "Detector de Cachorros"
                }

                // Recebe o resultado de qualquer origem.
                LaunchedEffect(
                    estadoInferencia.resultado,
                    estadoInferencia.erro
                ) {

                    val processamento =
                        estadoInferencia.resultado

                    val erro = estadoInferencia.erro

                    if (processamento != null) {

                        val resultado = processamento.analise

                        // Aplica o filtro apenas quando
                        // a origem foi a Galeria.
                        val deteccoesExibidas =
                            if (
                                origemInferencia == TELA_INICIO &&
                                filtroGaleria != "Todas as classes"
                            ) {

                                resultado.deteccoes.filter { deteccao ->

                                    deteccao.rotuloClasse.equals(
                                        filtroGaleria,
                                        ignoreCase = true
                                    )
                                }

                            } else {

                                resultado.deteccoes
                            }

                        // Mantém o CRUD local atualizado.
                        repository.adicionarDeteccoes(
                            deteccoesExibidas
                        )

                        // Prepara a tela de resultado.
                        imagemResultado = resultado.bitmap

                        deteccoesResultado =
                            deteccoesExibidas

                        tempoExecucaoMs =
                            processamento.tempoMs

                        erroCamera = null
                        erroGaleria = null

                        // Limpa o estado do ViewModel
                        // antes de navegar.
                        inferenciaViewModel.limparEstado()

                        origemInferencia = ORIGEM_NENHUMA

                        navegarPara(TELA_RESULTADO)

                        // Mostra o resultado do envio à API.
                        scope.launch {

                            snackbarHostState.showSnackbar(
                                message =
                                    processamento.mensagemServidor,
                                duration =
                                    SnackbarDuration.Long
                            )
                        }

                    } else if (erro != null) {

                        if (origemInferencia == TELA_CAMERA) {

                            erroCamera = erro

                        } else {

                            erroGaleria = erro
                        }

                        inferenciaViewModel.limparEstado()

                        origemInferencia = ORIGEM_NENHUMA
                    }
                }

                // Botão Voltar do Android.
                BackHandler(
                    enabled = podeVoltar && !processando
                ) {
                    voltar()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),

                    topBar = {

                        Surface(
                            color = MaterialTheme.colorScheme.surface
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .height(64.dp)
                                    .padding(horizontal = 12.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                if (podeVoltar) {

                                    IconButton(
                                        onClick = {
                                            voltar()
                                        },
                                        enabled = !processando
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.AutoMirrored
                                                    .Rounded
                                                    .ArrowBack,

                                            contentDescription = "Voltar"
                                        )
                                    }

                                } else {

                                    Icon(
                                        imageVector =
                                            Icons.Rounded.Home,

                                        contentDescription = null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        modifier =
                                            Modifier.padding(
                                                horizontal = 12.dp
                                            )
                                    )
                                }

                                Text(
                                    text = tituloTela,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge
                                )
                            }
                        }
                    },

                    bottomBar = {

                        NavigationBar(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainer
                        ) {

                            NavigationBarItem(
                                selected =
                                    telaAtual == TELA_INICIO,

                                onClick = {
                                    navegarPara(TELA_INICIO)
                                },

                                enabled = !processando,

                                icon = {
                                    Icon(
                                        Icons.Rounded.Home,
                                        contentDescription = null
                                    )
                                },

                                label = {
                                    Text("Início")
                                }
                            )

                            NavigationBarItem(
                                selected =
                                    telaAtual == TELA_CAMERA,

                                onClick = {
                                    navegarPara(TELA_CAMERA)
                                },

                                enabled = !processando,

                                icon = {
                                    Icon(
                                        Icons.Rounded.PhotoCamera,
                                        contentDescription = null
                                    )
                                },

                                label = {
                                    Text("Câmera")
                                }
                            )

                            NavigationBarItem(
                                selected =
                                    telaAtual == TELA_HISTORICO,

                                onClick = {
                                    navegarPara(TELA_HISTORICO)
                                },

                                enabled = !processando,

                                icon = {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null
                                    )
                                },

                                label = {
                                    Text("Histórico")
                                }
                            )

                            NavigationBarItem(
                                selected =
                                    telaAtual == TELA_CAIXAS,

                                onClick = {
                                    navegarPara(TELA_CAIXAS)
                                },

                                enabled = !processando,

                                icon = {
                                    Icon(
                                        Icons.Rounded.Inventory2,
                                        contentDescription = null
                                    )
                                },

                                label = {
                                    Text("Caixas")
                                }
                            )
                        }
                    },

                    snackbarHost = {

                        SnackbarHost(
                            hostState = snackbarHostState
                        )
                    }

                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        when (telaAtual) {

                            // Galeria integrada ao ViewModel.
                            TELA_INICIO -> {

                                TelaGaleria(
                                    processando = processando,

                                    erroInferencia = erroGaleria,

                                    onAnalisarImagem = {
                                            bitmap,
                                            confiancaEscolhida,
                                            classeEscolhida ->

                                        if (!processando) {

                                            erroGaleria = null

                                            filtroGaleria =
                                                classeEscolhida

                                            origemInferencia =
                                                TELA_INICIO

                                            inferenciaViewModel
                                                .analisarImagem(
                                                    bitmap = bitmap,

                                                    confiancaMinima =
                                                        confiancaEscolhida
                                                )
                                        }
                                    }
                                )
                            }

                            // CRUD de caixas.
                            TELA_CAIXAS -> {

                                TelaCrudCaixas(
                                    repository = repository
                                )
                            }

                            // Resultado.
                            TELA_RESULTADO -> {

                                imagemResultado?.let { bitmap ->

                                    TelaResultado(
                                        bitmap = bitmap,

                                        deteccoes =
                                            deteccoesResultado,

                                        tempoExecucaoMs =
                                            tempoExecucaoMs
                                    )
                                }
                            }

                            // Histórico do servidor.
                            TELA_HISTORICO -> {

                                TelaHistorico()
                            }

                            // Câmera integrada ao ViewModel.
                            TELA_CAMERA -> {

                                if (processando) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),

                                        verticalArrangement =
                                            Arrangement.spacedBy(16.dp)
                                    ) {

                                        CircularProgressIndicator()

                                        Text(
                                            "Analisando foto da câmera..."
                                        )

                                        Text(
                                            "Executando o YOLO e " +
                                                    "salvando os resultados " +
                                                    "no servidor."
                                        )
                                    }

                                } else {

                                    Column(
                                        modifier =
                                            Modifier.fillMaxSize()
                                    ) {

                                        erroCamera?.let { erro ->

                                            Text(
                                                text =
                                                    "Erro ao analisar foto: $erro",

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .error,

                                                modifier =
                                                    Modifier.padding(
                                                        12.dp
                                                    )
                                            )
                                        }

                                        Box(
                                            modifier =
                                                Modifier.weight(1f)
                                        ) {

                                            TelaCamera(
                                                onFotoCapturada = {
                                                        bitmap,
                                                        confiancaEscolhida ->

                                                    if (!processando) {

                                                        erroCamera = null

                                                        origemInferencia =
                                                            TELA_CAMERA

                                                        inferenciaViewModel
                                                            .analisarImagem(
                                                                bitmap = bitmap,

                                                                confiancaMinima =
                                                                    confiancaEscolhida
                                                            )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
