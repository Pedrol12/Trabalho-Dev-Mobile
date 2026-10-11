
package com.example.appdeteccaocachorro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Resultado da análise automática das orientações
data class ResultadoYolo(
    val bitmap: Bitmap,
    val deteccoes: List<Deteccao>,
    val rotacaoUtilizada: Int,
    val maiorConfianca: Float?,
    val candidatos: Int,
    val antesNMS: Int,
    val depoisNMS: Int
)

class YoloDetector(context: Context) : AutoCloseable {

    // Ambiente e sessão ONNX
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    private val inputSize = 640

    // Informações do diagnóstico
    var ultimaConfianca: Float? = null
        private set

    var totalCandidatos = 0
        private set

    var caixasAntesNMS = 0
        private set

    var caixasDepoisNMS = 0
        private set

    // Guarda os dados do letterbox
    private data class ImagemPreparada(
        val tensor: OnnxTensor,
        val escalaX: Float,
        val escalaY: Float,
        val margemX: Float,
        val margemY: Float
    )

    init {
        // Carrega o modelo embarcado
        val bytes = context.assets.open("best.onnx").use {
            it.readBytes()
        }

        ortSession = ortEnvironment.createSession(bytes)
    }

    // Prepara a imagem preservando sua proporção
    private fun preprocessarImagem(bitmap: Bitmap): ImagemPreparada {

        val escala = minOf(
            inputSize.toFloat() / bitmap.width,
            inputSize.toFloat() / bitmap.height
        )

        val novaLargura = (bitmap.width * escala)
            .toInt().coerceIn(1, inputSize)

        val novaAltura = (bitmap.height * escala)
            .toInt().coerceIn(1, inputSize)

        val margemX = (inputSize - novaLargura) / 2
        val margemY = (inputSize - novaAltura) / 2

        // Redimensiona a imagem
        val redimensionada = Bitmap.createScaledBitmap(
            bitmap,
            novaLargura,
            novaAltura,
            true
        )

        // Cria fundo cinza 640x640
        val imagemFinal = Bitmap.createBitmap(
            inputSize,
            inputSize,
            Bitmap.Config.ARGB_8888
        )

        Canvas(imagemFinal).apply {
            drawColor(Color.rgb(114, 114, 114))
            drawBitmap(
                redimensionada,
                margemX.toFloat(),
                margemY.toFloat(),
                Paint()
            )
        }

        val pixels = IntArray(inputSize * inputSize)

        imagemFinal.getPixels(
            pixels, 0, inputSize,
            0, 0, inputSize, inputSize
        )

        // Cria o buffer RGB normalizado
        val buffer = ByteBuffer
            .allocateDirect(3 * inputSize * inputSize * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        for (canal in 0 until 3) {
            for (pixel in pixels) {

                val valor = when (canal) {
                    0 -> (pixel shr 16) and 0xFF
                    1 -> (pixel shr 8) and 0xFF
                    else -> pixel and 0xFF
                }

                buffer.put(valor / 255.0f)
            }
        }

        buffer.rewind()

        // Tensor [1, 3, 640, 640]
        val tensor = OnnxTensor.createTensor(
            ortEnvironment,
            buffer,
            longArrayOf(1, 3, 640, 640)
        )

        return ImagemPreparada(
            tensor = tensor,
            escalaX = novaLargura.toFloat() / bitmap.width,
            escalaY = novaAltura.toFloat() / bitmap.height,
            margemX = margemX.toFloat(),
            margemY = margemY.toFloat()
        )
    }

    // Executa a inferência em uma orientação
    fun detectar(
        bitmap: Bitmap,
        confiancaMinima: Float = 0.5f
    ): List<Deteccao> {

        require(confiancaMinima in 0f..1f) {
            "Confiança inválida"
        }

        require(bitmap.width > 0 && bitmap.height > 0) {
            "Imagem inválida"
        }

        ultimaConfianca = null
        totalCandidatos = 0
        caixasAntesNMS = 0
        caixasDepoisNMS = 0

        val deteccoes = mutableListOf<Deteccao>()
        val preparada = preprocessarImagem(bitmap)

        preparada.tensor.use { inputTensor ->

            val inputName = ortSession.inputNames.first()

            // Executa o modelo ONNX
            ortSession.run(
                mapOf(inputName to inputTensor)
            ).use { resultado ->

                @Suppress("UNCHECKED_CAST")
                val output =
                    resultado[0].value as Array<Array<FloatArray>>

                // Confere a saída [1, 5, 8400]
                require(
                    output.size == 1 &&
                            output[0].size == 5 &&
                            output[0].all {
                                it.size == output[0][0].size
                            }
                ) {
                    "Formato YOLO inesperado"
                }

                totalCandidatos = output[0][0].size

                ultimaConfianca = output[0][4]
                    .filter { it.isFinite() }
                    .maxOrNull()

                // Percorre as previsões
                for (i in 0 until totalCandidatos) {

                    val cx = output[0][0][i]
                    val cy = output[0][1][i]
                    val w = output[0][2][i]
                    val h = output[0][3][i]
                    val conf = output[0][4][i]

                    if (!listOf(cx, cy, w, h, conf).all {
                            it.isFinite()
                        }) continue

                    if (conf < confiancaMinima || conf > 1f) continue
                    if (w <= 0f || h <= 0f) continue

                    // Retira as margens do letterbox
                    val xMin = (
                            (cx - w / 2f - preparada.margemX) /
                                    preparada.escalaX
                            ).coerceIn(0f, bitmap.width.toFloat())

                    val yMin = (
                            (cy - h / 2f - preparada.margemY) /
                                    preparada.escalaY
                            ).coerceIn(0f, bitmap.height.toFloat())

                    val xMax = (
                            (cx + w / 2f - preparada.margemX) /
                                    preparada.escalaX
                            ).coerceIn(0f, bitmap.width.toFloat())

                    val yMax = (
                            (cy + h / 2f - preparada.margemY) /
                                    preparada.escalaY
                            ).coerceIn(0f, bitmap.height.toFloat())

                    // Ignora coordenadas inválidas
                    if (xMax <= xMin || yMax <= yMin) continue

                    deteccoes.add(
                        Deteccao(
                            rotuloClasse = "cachorro",
                            confianca = conf,
                            xMin = xMin,
                            yMin = yMin,
                            xMax = xMax,
                            yMax = yMax
                        )
                    )
                }
            }
        }

        caixasAntesNMS = deteccoes.size

        val caixasFinais = aplicarNMS(deteccoes)

        caixasDepoisNMS = caixasFinais.size

        return caixasFinais
    }

    // Testa as quatro orientações automaticamente
    fun detectarAutomaticamente(
        bitmap: Bitmap,
        confiancaMinima: Float = 0.5f
    ): ResultadoYolo {

        require(confiancaMinima in 0f..1f)

        var melhorResultado: ResultadoYolo? = null
        var melhorPontuacao = -1f
        var maiorConfiancaBruta: Float? = null

        for (angulo in listOf(0, 90, 180, 270)) {

            // Gira a imagem somente para a inferência
            val girada = girarImagem(bitmap, angulo)

            val deteccoes = detectar(
                girada,
                confiancaMinima
            )

            val candidatosAtual = totalCandidatos
            val antesAtual = caixasAntesNMS
            val depoisAtual = caixasDepoisNMS

            val confiancaAtual = ultimaConfianca

            if (confiancaAtual != null &&
                (maiorConfiancaBruta == null ||
                        confiancaAtual > maiorConfiancaBruta)
            ) {
                maiorConfiancaBruta = confiancaAtual
            }

            // Recupera as coordenadas da imagem original
            val caixasOriginais = deteccoes.mapNotNull {
                converterCaixa(it, angulo, bitmap.width, bitmap.height)
            }

            // Pontuação da melhor caixa válida
            val pontuacao = caixasOriginais
                .maxOfOrNull { it.confianca } ?: -1f

            if (pontuacao > melhorPontuacao) {
                melhorPontuacao = pontuacao

                melhorResultado = ResultadoYolo(
                    bitmap = bitmap,
                    deteccoes = caixasOriginais,
                    rotacaoUtilizada = angulo,
                    maiorConfianca = confiancaAtual,
                    candidatos = candidatosAtual,
                    antesNMS = antesAtual,
                    depoisNMS = depoisAtual
                )
            }
        }

        // Se nenhuma orientação detectar, retorna lista vazia
        return melhorResultado ?: ResultadoYolo(
            bitmap = bitmap,
            deteccoes = emptyList(),
            rotacaoUtilizada = 0,
            maiorConfianca = maiorConfiancaBruta,
            candidatos = totalCandidatos,
            antesNMS = 0,
            depoisNMS = 0
        )
    }

    // Gira a imagem no sentido horário
    private fun girarImagem(bitmap: Bitmap, angulo: Int): Bitmap {

        if (angulo == 0) return bitmap

        val matriz = Matrix().apply {
            postRotate(angulo.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matriz, true
        )
    }

    // Converte a caixa girada para a foto original
    private fun converterCaixa(
        caixa: Deteccao,
        angulo: Int,
        largura: Int,
        altura: Int
    ): Deteccao? {

        // Converte os quatro cantos da caixa
        val pontos = listOf(
            caixa.xMin to caixa.yMin,
            caixa.xMax to caixa.yMin,
            caixa.xMin to caixa.yMax,
            caixa.xMax to caixa.yMax
        ).map { (x, y) ->

            when (angulo) {
                90 -> y to (altura - x)
                180 -> (largura - x) to (altura - y)
                270 -> (largura - y) to x
                else -> x to y
            }
        }

        val xMin = pontos.minOf { it.first }
            .coerceIn(0f, largura.toFloat())

        val yMin = pontos.minOf { it.second }
            .coerceIn(0f, altura.toFloat())

        val xMax = pontos.maxOf { it.first }
            .coerceIn(0f, largura.toFloat())

        val yMax = pontos.maxOf { it.second }
            .coerceIn(0f, altura.toFloat())

        if (xMax <= xMin || yMax <= yMin) return null

        // Mantém o ID e a confiança da detecção
        return caixa.copy(
            xMin = xMin,
            yMin = yMin,
            xMax = xMax,
            yMax = yMax
        )
    }

    // Remove caixas muito sobrepostas
    private fun aplicarNMS(
        deteccoes: List<Deteccao>,
        iouLimite: Float = 0.45f
    ): List<Deteccao> {

        val ordenadas = deteccoes
            .sortedByDescending { it.confianca }
            .toMutableList()

        val selecionadas = mutableListOf<Deteccao>()

        while (ordenadas.isNotEmpty()) {

            val melhor = ordenadas.removeAt(0)
            selecionadas.add(melhor)

            ordenadas.removeAll { outra ->
                calcularIoU(melhor, outra) > iouLimite
            }
        }

        return selecionadas
    }

    // Calcula a sobreposição de duas caixas
    private fun calcularIoU(a: Deteccao, b: Deteccao): Float {

        val x1 = maxOf(a.xMin, b.xMin)
        val y1 = maxOf(a.yMin, b.yMin)
        val x2 = minOf(a.xMax, b.xMax)
        val y2 = minOf(a.yMax, b.yMax)

        val largura = maxOf(0f, x2 - x1)
        val altura = maxOf(0f, y2 - y1)

        val intersecao = largura * altura
        val uniao = a.areaPx2 + b.areaPx2 - intersecao

        return if (uniao > 0f) intersecao / uniao else 0f
    }

    // Libera a sessão ONNX
    override fun close() {
        ortSession.close()
    }
}
