package com.example.appdeteccaocachorro

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class YoloDetector (context: Context){

    //ambiente do onnx
    private val ortEnvironment : OrtEnvironment = OrtEnvironment.getEnvironment()

    //instancia do modelo carregado
    private val ortSession: OrtSession

    //tamnaho que o modelo espera
    private val inputSize = 640


    init{
        //le e carrega como bytes
        val modelBytes = context.assets.open("best.onnx").readBytes()

        //cria a sessao com esses bytes
        ortSession = ortEnvironment.createSession(modelBytes)
    }

    //converte a foto pro formato que o modelo espera
    private fun preprocessarImagem(bitmap: Bitmap): OnnxTensor {

        //redimensiona a imagem pro tamanho que o modelo foi treinado
        val bitmapRedimensionado = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        //buffer q vai guardar todos os valores float
        val buffer = FloatBuffer.allocate(3 * inputSize * inputSize)

        //array temporario com os pixels da imagem
        val pixels = IntArray(inputSize * inputSize)
        bitmapRedimensionado.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        //separa os canais R, G, B
        val channels = Array(3) { FloatArray(inputSize * inputSize) }

        for (i in pixels.indices) {
            val pixel = pixels[i]

            //extrai cada cor do pixel e normaliza
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            channels[0][i] = r
            channels[1][i] = g
            channels[2][i] = b
        }

        //junta os 3 canais no buffer, na ordem certa
        for (c in 0 until 3) {
            buffer.put(channels[c])
        }
        buffer.rewind() //volta o cursor pro inicio, pra poder ser lido

        //cria o tensor final [1, 3, 640, 640]
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        return OnnxTensor.createTensor(ortEnvironment, buffer, shape)
    }

    //recebe uma foto, roda o modelo, e devolve a lista de deteccoes encontradas
    fun detectar(bitmap: Bitmap): List<Deteccao> {

        //pre-processa a imagem pro formato certo
        val inputTensor = preprocessarImagem(bitmap)

        //nome da entrada do modelo
        val inputName = ortSession.inputNames.iterator().next()

        //roda o modelo passando o tensor de entrada
        val resultado = ortSession.run(mapOf(inputName to inputTensor))

        //pega a saida bruta do modelo
        val outputTensor = resultado[0].value as Array<Array<FloatArray>>

        //fecha o tensor de entrada pra liberar memoria
        inputTensor.close()


        return emptyList()
    }

}