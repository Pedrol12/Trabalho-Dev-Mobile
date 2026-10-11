

package com.example.appdeteccaocachorro

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.util.Locale

// Exibe a foto e as caixas detectadas
@Composable
fun TelaResultado(
    bitmap: Bitmap,
    deteccoes: List<Deteccao>,
    tempoExecucaoMs: Long
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Resultado da Detecção",
            style = MaterialTheme.typography.headlineSmall
        )

        // Resumo da inferência
        Text("Cachorros detectados: ${deteccoes.size}")
        Text("Tempo de execução: $tempoExecucaoMs ms")

        HorizontalDivider()

        // Mantém a proporção original da fotografia
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    bitmap.width.toFloat() / bitmap.height
                )
        ) {

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Imagem analisada",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // Desenha as caixas nas coordenadas originais
            Canvas(modifier = Modifier.fillMaxSize()) {

                val escalaX = size.width / bitmap.width
                val escalaY = size.height / bitmap.height

                deteccoes.forEach { caixa ->

                    val x = caixa.xMin * escalaX
                    val y = caixa.yMin * escalaY

                    val largura = caixa.larguraPx * escalaX
                    val altura = caixa.alturaPx * escalaY

                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(x, y),
                        size = Size(largura, altura),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        HorizontalDivider()

        // Informações individuais de cada caixa
        deteccoes.forEachIndexed { indice, caixa ->

            val percentual = String.format(
                Locale("pt", "BR"),
                "%.2f",
                caixa.confianca * 100
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        "Caixa #${indice + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Classe: ${caixa.rotuloClasse}")
                    Text("Confiança: $percentual%")
                    Text("Largura: ${caixa.larguraPx} px")
                    Text("Altura: ${caixa.alturaPx} px")
                    Text("Centro X: ${caixa.centroideX} px")
                    Text("Centro Y: ${caixa.centroideY} px")
                    Text("Área: ${caixa.areaPx2} px²")
                }
            }
        }
    }
}
