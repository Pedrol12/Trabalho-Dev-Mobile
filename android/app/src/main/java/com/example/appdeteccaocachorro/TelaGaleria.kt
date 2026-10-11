
package com.example.appdeteccaocachorro

import android.graphics.Bitmap
import android.graphics.BitmapFactory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.FilterList

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaGaleria(
    processando: Boolean,
    erroInferencia: String?,
    onAnalisarImagem: (
        Bitmap,
        Float,
        String
    ) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado local usado apenas para ler a foto.
    var carregandoImagem by remember {
        mutableStateOf(false)
    }

    var mensagem by remember {
        mutableStateOf("")
    }

    // Configuração de confiança.
    var confianca by remember {
        mutableFloatStateOf(0.5f)
    }

    // Filtro de classe.
    var classeSelecionada by remember {
        mutableStateOf("Todas as classes")
    }

    var menuAberto by remember {
        mutableStateOf(false)
    }

    val ocupado = carregandoImagem || processando

    // Seletor de imagens do Android.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null && !ocupado) {

            carregandoImagem = true
            mensagem = ""

            // Guarda as configurações selecionadas
            // antes da abertura da imagem.
            val confiancaEscolhida = confianca
            val filtroEscolhido = classeSelecionada

            scope.launch {

                try {

                    // Faz a leitura fora da thread principal.
                    val bitmap = withContext(Dispatchers.IO) {

                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                            ?: error(
                                "Não foi possível abrir a imagem."
                            )
                    }

                    // A Galeria apenas entrega os dados.
                    // O ViewModel executará a inferência.
                    onAnalisarImagem(
                        bitmap,
                        confiancaEscolhida,
                        filtroEscolhido
                    )

                } catch (e: Exception) {

                    mensagem =
                        "Erro ao abrir imagem: ${e.message}"

                } finally {

                    carregandoImagem = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Detectar Cachorros",
            style = MaterialTheme.typography.headlineSmall
        )

        // Configurações da detecção.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "Configurações da detecção",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text("Confiança mínima")

                    Text(
                        text = "${(confianca * 100).toInt()}%"
                    )
                }

                Slider(
                    value = confianca,
                    onValueChange = {
                        confianca = it
                    },
                    valueRange = 0.1f..1f,
                    enabled = !ocupado
                )

                HorizontalDivider()

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null
                    )

                    Text(
                        text = "Filtro por classe",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = menuAberto,
                    onExpandedChange = {
                        if (!ocupado) {
                            menuAberto = !menuAberto
                        }
                    }
                ) {

                    OutlinedTextField(
                        value = classeSelecionada,
                        onValueChange = {},
                        readOnly = true,

                        label = {
                            Text("Classe")
                        },

                        trailingIcon = {
                            ExposedDropdownMenuDefaults
                                .TrailingIcon(
                                    expanded = menuAberto
                                )
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                ExposedDropdownMenuAnchorType
                                    .PrimaryNotEditable
                            ),

                        enabled = !ocupado
                    )

                    ExposedDropdownMenu(
                        expanded = menuAberto,
                        onDismissRequest = {
                            menuAberto = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Todas as classes")
                            },
                            onClick = {
                                classeSelecionada =
                                    "Todas as classes"

                                menuAberto = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Cachorro")
                            },
                            onClick = {
                                classeSelecionada = "Cachorro"
                                menuAberto = false
                            }
                        )
                    }
                }

                Text(
                    text =
                        "O modelo atual identifica a classe cachorro.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Escolher imagem.
        Button(
            onClick = {
                launcher.launch(arrayOf("image/*"))
            },
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {

            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text("Selecionar imagem da galeria")
        }

        // Leitura local da imagem.
        if (carregandoImagem) {

            CircularProgressIndicator()

            Text("Carregando imagem...")
        }

        // Processamento feito pelo ViewModel.
        if (processando) {

            CircularProgressIndicator()

            Text(
                "Executando YOLO e salvando análise..."
            )
        }

        if (mensagem.isNotEmpty()) {

            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!erroInferencia.isNullOrEmpty()) {

            Text(
                text = "Erro na análise: $erroInferencia",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
