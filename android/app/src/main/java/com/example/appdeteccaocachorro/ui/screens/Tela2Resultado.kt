package com.example.appdeteccaocachorro.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appdeteccaocachorro.Deteccao
import com.example.appdeteccaocachorro.ui.theme.BrandPrimary
import com.example.appdeteccaocachorro.ui.theme.BrandSecondary
import com.example.appdeteccaocachorro.ui.theme.TextPrimary
import com.example.appdeteccaocachorro.ui.theme.TextSecondary
import com.example.appdeteccaocachorro.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun Tela2Resultado(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val imagem by viewModel.imagemSelecionada.collectAsState()
    val deteccoes by viewModel.deteccoes.collectAsState()
    val tempoMs by viewModel.tempoExecucaoMs.collectAsState()
    val statusEnvio by viewModel.statusEnvioBackend.collectAsState()

    val mediaConfianca = if (deteccoes.isNotEmpty()) {
        (deteccoes.map { it.confianca }.average() * 100).roundToInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Cabecalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Detecção de Cachorro",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Resultado da Inferência On-Device",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⚡ Local",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards de Estatisticas (Total de Objetos e Confianca Media)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${deteccoes.size}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Objetos Detectados",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$mediaConfianca%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary
                    )
                    Text(
                        text = "Confiança Média",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info de Tempo de Execucao
        Text(
            text = "Tempo de execução: $tempoMs ms (Local NNAPI/CPU)",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Imagem Anotada com Canvas
        Text(
            text = "Imagem Anotada (Bounding Boxes)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            if (imagem != null) {
                DesenharImagemAnotada(
                    bitmap = imagem!!,
                    deteccoes = deteccoes,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma imagem para exibir", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botao Enviar ao Backend
        Button(
            onClick = { viewModel.enviarParaBackend() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
        ) {
            Text("📤 Enviar ao Servidor Backend", fontWeight = FontWeight.SemiBold)
        }

        if (statusEnvio != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusEnvio!!,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (statusEnvio!!.startsWith("Sucesso")) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.navegarPara("TELA_1") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Nova Detecção")
        }
    }
}

@Composable
fun DesenharImagemAnotada(
    bitmap: Bitmap,
    deteccoes: List<Deteccao>,
    modifier: Modifier = Modifier
) {
    val imageBitmap = bitmap.asImageBitmap()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Desenha a imagem original preenchendo o canvas
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(canvasWidth.toInt(), canvasHeight.toInt())
        )

        val escalaX = canvasWidth / bitmap.width.toFloat()
        val escalaY = canvasHeight / bitmap.height.toFloat()

        // Pincel nativo para desenhar os rotulos com texto
        val paintTexto = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintFundoTag = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 0, 119, 182)
            style = android.graphics.Paint.Style.FILL
        }

        for (det in deteccoes) {
            val left = det.xMin * escalaX
            val top = det.yMin * escalaY
            val width = det.larguraPx * escalaX
            val height = det.alturaPx * escalaY

            // Desenha a Bounding Box
            drawRect(
                color = Color(0xFF00B4D8),
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )

            // Desenha etiqueta de texto (classe + % confianca)
            val textoTag = "#${det.idCaixa} ${det.rotuloClasse} ${(det.confianca * 100).roundToInt()}%"
            val textBounds = android.graphics.Rect()
            paintTexto.getTextBounds(textoTag, 0, textoTag.length, textBounds)

            val tagPadding = 8f
            val tagTop = (top - textBounds.height() - tagPadding * 2).coerceAtLeast(0f)
            val tagBottom = tagTop + textBounds.height() + tagPadding * 2
            val tagRight = left + textBounds.width() + tagPadding * 2

            drawContext.canvas.nativeCanvas.drawRoundRect(
                left, tagTop, tagRight, tagBottom,
                8f, 8f,
                paintFundoTag
            )

            drawContext.canvas.nativeCanvas.drawText(
                textoTag,
                left + tagPadding,
                tagBottom - tagPadding,
                paintTexto
            )
        }
    }
}
