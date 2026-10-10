package com.example.appdeteccaocachorro

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileInputStream
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class YoloDetector(private val context: Context) {

    // ambiente do onnx
    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()

    // instancia do modelo carregado
    private var ortSession: OrtSession

    // tamanho esperado pelo YOLOv8
    private val inputSize = 640

    var nomeModeloAtivo: String = "best.onnx (Modelo Embutido)"
        private set

    init {
        // le e carrega como bytes da pasta assets
        val modelBytes = context.assets.open("best.onnx").readBytes()
        ortSession = ortEnvironment.createSession(modelBytes)
    }

    // Permite carregar um arquivo .onnx externo (Requisito RF1)
    fun carregarModeloExterno(caminhoArquivo: String, nomeExibicao: String): Boolean {
        return try {
            val file = File(caminhoArquivo)
            if (!file.exists()) return false
            val bytes = FileInputStream(file).use { it.readBytes() }
            val novaSessao = ortEnvironment.createSession(bytes)
            ortSession.close()
            ortSession = novaSessao
            nomeModeloAtivo = nomeExibicao
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // converte o bitmap para o tensor [1, 3, 640, 640] normalizado (0.0 a 1.0)
    private fun preprocessarImagem(bitmap: Bitmap): OnnxTensor {
        val bitmapRedimensionado = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val pixels = IntArray(inputSize * inputSize)
        bitmapRedimensionado.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val channels = Array(3) { FloatArray(inputSize * inputSize) }
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            channels[0][i] = r
            channels[1][i] = g
            channels[2][i] = b
        }

        for (c in 0 until 3) {
            buffer.put(channels[c])
        }
        buffer.rewind()

        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        return OnnxTensor.createTensor(ortEnvironment, buffer, shape)
    }

    /**
     * Executa a inferencia do modelo ONNX e converte a saida em List<Deteccao>.
     *
     * Saida tipica do YOLOv8: tensor [1, 4 + num_classes, 8400]
     * canais 0..3: cx, cy, w, h (em escala de 640x640)
     * canais 4..N: probabilidades de cada classe
     */
    fun detectar(
        bitmapOriginal: Bitmap,
        limiarConfianca: Float = 0.25f,
        rotuloPadrao: String = "cachorro"
    ): List<Deteccao> {
        val inputTensor = preprocessarImagem(bitmapOriginal)
        val inputName = ortSession.inputNames.iterator().next()

        val resultado = ortSession.run(mapOf(inputName to inputTensor))
        val outputRaw = resultado[0].value

        inputTensor.close()

        val larguraOriginal = bitmapOriginal.width.toFloat()
        val alturaOriginal = bitmapOriginal.height.toFloat()
        val escalaX = larguraOriginal / inputSize
        val escalaY = alturaOriginal / inputSize

        val candidatos = mutableListOf<Deteccao>()

        // YOLOv8 tipicamente retorna Array<Array<FloatArray>> de dimensao [1][canais][8400]
        if (outputRaw is Array<*> && outputRaw.isNotEmpty()) {
            val batch0 = outputRaw[0]
            if (batch0 is Array<*>) {
                val canais = batch0.size
                val firstChannel = batch0[0] as? FloatArray ?: return emptyList()
                val numBoxes = firstChannel.size

                val numClasses = max(1, canais - 4)

                for (boxIdx in 0 until numBoxes) {
                    val cx = (batch0[0] as FloatArray)[boxIdx]
                    val cy = (batch0[1] as FloatArray)[boxIdx]
                    val w = (batch0[2] as FloatArray)[boxIdx]
                    val h = (batch0[3] as FloatArray)[boxIdx]

                    var maxScore = 0f
                    var classeEscolhidaIdx = 0

                    if (canais == 5) {
                        // Modelo com 1 classe
                        maxScore = (batch0[4] as FloatArray)[boxIdx]
                    } else if (canais > 5) {
                        for (c in 0 until numClasses) {
                            val score = (batch0[4 + c] as FloatArray)[boxIdx]
                            if (score > maxScore) {
                                maxScore = score
                                classeEscolhidaIdx = c
                            }
                        }
                    } else {
                        maxScore = 1.0f
                    }

                    if (maxScore >= limiarConfianca) {
                        // Converte cx, cy, w, h para coordenadas de caixa (escala 640)
                        val xMin640 = cx - (w / 2f)
                        val yMin640 = cy - (h / 2f)
                        val xMax640 = cx + (w / 2f)
                        val yMax640 = cy + (h / 2f)

                        // Mapeia para a resolucao original da imagem em pixels
                        val xMin = (xMin640 * escalaX).coerceIn(0f, larguraOriginal)
                        val yMin = (yMin640 * escalaY).coerceIn(0f, alturaOriginal)
                        val xMax = (xMax640 * escalaX).coerceIn(0f, larguraOriginal)
                        val yMax = (yMax640 * escalaY).coerceIn(0f, alturaOriginal)

                        val rotulo = if (rotuloPadrao.isNotBlank()) rotuloPadrao else "classe_$classeEscolhidaIdx"

                        candidatos.add(
                            Deteccao(
                                idCaixa = 0,
                                rotuloClasse = rotulo,
                                confianca = maxScore,
                                xMin = xMin,
                                yMin = yMin,
                                xMax = xMax,
                                yMax = yMax
                            )
                        )
                    }
                }
            }
        }

        // Aplica NMS (Non-Maximum Suppression) com IoU 0.45 para eliminar caixas duplicadas
        val deteccoesFiltradas = aplicarNMS(candidatos, iouThreshold = 0.45f)

        // Atribui IDs sequenciais para exibicao
        return deteccoesFiltradas.mapIndexed { index, det ->
            det.copy(idCaixa = index + 1)
        }
    }

    // Calcula IoU entre duas deteccoes
    private fun calcularIoU(a: Deteccao, b: Deteccao): Float {
        val interXmin = max(a.xMin, b.xMin)
        val interYmin = max(a.yMin, b.yMin)
        val interXmax = min(a.xMax, b.xMax)
        val interYmax = min(a.yMax, b.yMax)

        val interLargura = max(0f, interXmax - interXmin)
        val interAltura = max(0f, interYmax - interYmin)
        val interArea = interLargura * interAltura

        val uniao = a.areaPx2 + b.areaPx2 - interArea
        return if (uniao <= 0f) 0f else interArea / uniao
    }

    // Non-Maximum Suppression para remover sobreposicoes
    private fun aplicarNMS(boxes: List<Deteccao>, iouThreshold: Float): List<Deteccao> {
        val ordenadas = boxes.sortedByDescending { it.confianca }.toMutableList()
        val selecionadas = mutableListOf<Deteccao>()

        while (ordenadas.isNotEmpty()) {
            val melhor = ordenadas.removeAt(0)
            selecionadas.add(melhor)

            val iter = ordenadas.iterator()
            while (iter.hasNext()) {
                val proxima = iter.next()
                if (calcularIoU(melhor, proxima) > iouThreshold) {
                    iter.remove()
                }
            }
        }
        return selecionadas
    }

    fun fechar() {
        try {
            ortSession.close()
            ortEnvironment.close()
        } catch (_: Exception) {}
    }
}