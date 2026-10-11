
package com.example.appdeteccaocachorro

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.lifecycle.ViewModelProvider

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TelaHistorico() {

    val context = LocalContext.current

    // Obtém o ViewModel associado à Activity.
    val viewModel = remember(context) {
        ViewModelProvider(
            context as ComponentActivity
        )[HistoricoViewModel::class.java]
    }

    // Observa o StateFlow do ViewModel.
    val estado by viewModel.uiState.collectAsState()

    // Busca as sessões quando a tela é aberta.
    LaunchedEffect(viewModel) {
        viewModel.carregarHistorico()
    }

    // O botão físico/gesto Voltar retorna
    // aos registros quando os detalhes estão abertos.
    BackHandler(
        enabled = estado.detalhes != null &&
                !estado.carregando
    ) {
        viewModel.voltarAoHistorico()
    }

    fun formatarData(data: String): String {
        return try {

            val entrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val saida = SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.forLanguageTag("pt-BR")
            )

            val dataConvertida = entrada.parse(data)

            if (dataConvertida != null) {
                saida.format(dataConvertida)
            } else {
                data
            }

        } catch (_: Exception) {
            data
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        if (estado.detalhes == null) {

            // LISTA DO HISTÓRICO

            Text(
                text = "Histórico de Detecções",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    viewModel.carregarHistorico()
                },
                enabled = !estado.carregando
            ) {
                Text("Atualizar histórico")
            }

            if (estado.carregando) {
                CircularProgressIndicator()
            }

            estado.erro?.let { erro ->
                Text(
                    text = erro,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (
                !estado.carregando &&
                estado.sessoes.isEmpty() &&
                estado.erro == null
            ) {
                Text("Nenhuma inferência encontrada.")
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = estado.sessoes,
                    key = { it.id_sessao }
                ) { sessao ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !estado.carregando
                            ) {
                                viewModel.carregarDetalhes(
                                    sessao.id_sessao
                                )
                            }
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            Text(
                                text = formatarData(
                                    sessao.data_hora
                                ),
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Modelo: ${sessao.nome_modelo}"
                            )

                            Text(
                                "Cachorros detectados: " +
                                        "${sessao.total_objetos}"
                            )

                            val confiancaFormatada =
                                String.format(
                                    Locale.forLanguageTag("pt-BR"),
                                    "%.2f",
                                    sessao.confianca_media * 100
                                )

                            Text(
                                "Confiança média: " +
                                        "$confiancaFormatada%"
                            )

                            Text(
                                "Tempo: " +
                                        "${sessao.tempo_execucao_ms} ms"
                            )

                            TextButton(
                                onClick = {
                                    viewModel.carregarDetalhes(
                                        sessao.id_sessao
                                    )
                                },
                                enabled = !estado.carregando
                            ) {
                                Text("Ver detalhes")
                            }
                        }
                    }
                }
            }

        } else {

            // DETALHES DA INFERÊNCIA

            val resultado = estado.detalhes!!

            Text(
                text = "Detalhes da Inferência",
                style = MaterialTheme.typography.headlineSmall
            )

            TextButton(
                onClick = {
                    viewModel.voltarAoHistorico()
                },
                enabled = !estado.carregando
            ) {
                Text("← Voltar ao histórico")
            }

            HorizontalDivider()

            Text(
                "Data: ${formatarData(resultado.sessao.data_hora)}"
            )

            Text(
                "Modelo: ${resultado.sessao.nome_modelo}"
            )

            Text(
                "Total de objetos: " +
                        "${resultado.sessao.total_objetos}"
            )

            Text(
                "Tempo: " +
                        "${resultado.sessao.tempo_execucao_ms} ms"
            )

            Text(
                "Sessão: ${resultado.sessao.id_sessao}"
            )

            HorizontalDivider()

            Text(
                text = "Caixas Delimitadoras",
                style = MaterialTheme.typography.titleLarge
            )

            if (estado.carregando) {
                CircularProgressIndicator()
            }

            estado.erro?.let { erro ->
                Text(
                    text = erro,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (resultado.caixas_delimitadoras.isEmpty()) {
                Text("Nenhuma caixa nesta inferência.")
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = resultado.caixas_delimitadoras,
                    key = { it.id_caixa }
                ) { caixa ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            Text(
                                text = "Caixa #${caixa.id_caixa}",
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Classe: ${caixa.rotulo_classe}"
                            )

                            val confiancaFormatada =
                                String.format(
                                    Locale.forLanguageTag("pt-BR"),
                                    "%.2f",
                                    caixa.confianca * 100
                                )

                            Text(
                                "Confiança: $confiancaFormatada%"
                            )

                            Text(
                                "Largura: ${caixa.largura_px} px"
                            )

                            Text(
                                "Altura: ${caixa.altura_px} px"
                            )

                            Text(
                                "Centroide X: " +
                                        "${caixa.centroide_x} px"
                            )

                            Text(
                                "Centroide Y: " +
                                        "${caixa.centroide_y} px"
                            )

                            Text(
                                "Área: ${caixa.area_px2} px²"
                            )
                        }
                    }
                }
            }
        }
    }
}
