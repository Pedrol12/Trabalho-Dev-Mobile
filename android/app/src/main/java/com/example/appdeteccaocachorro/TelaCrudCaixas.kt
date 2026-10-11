
package com.example.appdeteccaocachorro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Tela para gerenciar as caixas delimitadoras
@Composable
fun TelaCrudCaixas(repository: CaixaRepository) {

    // Observa as caixas cadastradas
    val caixas by repository.caixas.collectAsState()

    // Caixa selecionada para edição
    var caixaEditando by remember {
        mutableStateOf<Deteccao?>(null)
    }

    var rotulo by remember { mutableStateOf("cachorro") }
    var confianca by remember { mutableStateOf("0.90") }
    var xMin by remember { mutableStateOf("") }
    var yMin by remember { mutableStateOf("") }
    var xMax by remember { mutableStateOf("") }
    var yMax by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    // Limpa os campos do formulário
    fun limparCampos() {
        caixaEditando = null
        rotulo = "cachorro"
        confianca = "0.90"
        xMin = ""
        yMin = ""
        xMax = ""
        yMax = ""
    }

    // Preenche os campos com os dados da caixa
    fun editar(caixa: Deteccao) {
        caixaEditando = caixa
        rotulo = caixa.rotuloClasse
        confianca = caixa.confianca.toString()
        xMin = caixa.xMin.toString()
        yMin = caixa.yMin.toString()
        xMax = caixa.xMax.toString()
        yMax = caixa.yMax.toString()
        mensagem = ""
    }

    // Salva uma caixa nova ou atualiza uma existente
    fun salvar() {
        val conf = confianca.toFloatOrNull()
        val x1 = xMin.toFloatOrNull()
        val y1 = yMin.toFloatOrNull()
        val x2 = xMax.toFloatOrNull()
        val y2 = yMax.toFloatOrNull()

        if (conf == null || x1 == null || y1 == null ||
            x2 == null || y2 == null || rotulo.isBlank()
        ) {
            mensagem = "Preencha os campos corretamente"
            return
        }

        if (!listOf(conf, x1, y1, x2, y2).all { it.isFinite() } ||
            conf !in 0f..1f || x1 < 0f || y1 < 0f ||
            x2 <= x1 || y2 <= y1
        ) {
            mensagem = "Confiança ou coordenadas inválidas"
            return
        }

        val deteccao = Deteccao(
            rotuloClasse = rotulo,
            confianca = conf,
            xMin = x1,
            yMin = y1,
            xMax = x2,
            yMax = y2,
            id = caixaEditando?.id
                ?: java.util.UUID.randomUUID().toString()
        )

        if (caixaEditando == null) {
            repository.adicionar(deteccao)
            mensagem = "Caixa adicionada!"
        } else {
            val atualizada = repository.atualizar(deteccao)
            mensagem = if (atualizada) {
                "Caixa atualizada!"
            } else {
                "Caixa não encontrada"
            }
        }

        limparCampos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "CRUD das Caixas",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Formulário de cadastro e edição
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {

            item {
                OutlinedTextField(
                    value = rotulo,
                    onValueChange = { rotulo = it },
                    label = { Text("Classe") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                CampoNumero(
                    "Confiança (0 a 1)",
                    confianca
                ) { confianca = it }
            }

            item {
                CampoNumero("X mínimo", xMin) { xMin = it }
            }

            item {
                CampoNumero("Y mínimo", yMin) { yMin = it }
            }

            item {
                CampoNumero("X máximo", xMax) { xMax = it }
            }

            item {
                CampoNumero("Y máximo", yMax) { yMax = it }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { salvar() }) {
                        Text(
                            if (caixaEditando == null)
                                "Adicionar"
                            else "Salvar edição"
                        )
                    }

                    OutlinedButton(onClick = {
                        limparCampos()
                        mensagem = ""
                    }) {
                        Text("Limpar")
                    }
                }
            }

            item {
                if (mensagem.isNotEmpty()) {
                    Text(mensagem)
                }
            }

            item {
                HorizontalDivider()

                Text(
                    text = "Caixas cadastradas: ${caixas.size}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Mostra as caixas cadastradas
            items(caixas, key = { it.id }) { caixa ->

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text("Classe: ${caixa.rotuloClasse}")
                        Text("Confiança: ${caixa.confianca}")

                        Text("X mínimo: ${caixa.xMin}")
                        Text("Y mínimo: ${caixa.yMin}")
                        Text("X máximo: ${caixa.xMax}")
                        Text("Y máximo: ${caixa.yMax}")

                        HorizontalDivider()

                        // Cálculos da Deteccao.kt
                        Text("Largura: ${caixa.larguraPx} px")
                        Text("Altura: ${caixa.alturaPx} px")
                        Text("Centro X: ${caixa.centroideX}")
                        Text("Centro Y: ${caixa.centroideY}")
                        Text("Área: ${caixa.areaPx2} px²")

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                editar(caixa)
                            }) {
                                Text("Editar")
                            }

                            TextButton(onClick = {
                                repository.excluir(caixa.id)
                                if (caixaEditando?.id == caixa.id) {
                                    limparCampos()
                                }
                                mensagem = "Caixa excluída!"
                            }) {
                                Text("Excluir")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Campo reutilizável para coordenadas e confiança
@Composable
private fun CampoNumero(
    titulo: String,
    valor: String,
    aoAlterar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoAlterar,
        label = { Text(titulo) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
