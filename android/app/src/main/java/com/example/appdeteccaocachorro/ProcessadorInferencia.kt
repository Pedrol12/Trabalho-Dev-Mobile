
package com.example.appdeteccaocachorro

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

// Resultado da análise e do envio ao banco.
data class ProcessamentoInferencia(
    val analise: ResultadoYolo,
    val tempoMs: Long,
    val mensagemServidor: String,
    val salvoNoBanco: Boolean
)

object ProcessadorInferencia {

    // Utilizado tanto pela Galeria quanto pela Câmera.
    suspend fun analisarESalvar(
        context: Context,
        bitmap: Bitmap,
        confiancaMinima: Float = 0.5f
    ): ProcessamentoInferencia {

        // Executa o YOLO fora da thread principal.
        val processamento = withContext(Dispatchers.Default) {

            val inicio = SystemClock.elapsedRealtime()

            val resultado = YoloDetector(context).use { detector ->
                detector.detectarAutomaticamente(
                    bitmap,
                    confiancaMinima = confiancaMinima
                )
            }

            val tempo =
                SystemClock.elapsedRealtime() - inicio

            Pair(resultado, tempo)
        }

        val resultado = processamento.first
        val tempoMs = processamento.second

        // Converte as caixas para o formato da API.
        val caixasDto = resultado.deteccoes.map { caixa ->

            CaixaDto(
                rotulo_classe = caixa.rotuloClasse,
                confianca = caixa.confianca,
                largura_px = caixa.larguraPx,
                altura_px = caixa.alturaPx,
                centroide_x = caixa.centroideX,
                centroide_y = caixa.centroideY,
                area_px2 = caixa.areaPx2
            )
        }

        // Calcula a confiança média das detecções.
        val confiancaMedia =
            if (resultado.deteccoes.isNotEmpty()) {

                resultado.deteccoes
                    .map { it.confianca }
                    .average()
                    .toFloat()

            } else {
                0f
            }

        // Data e hora da inferência em UTC.
        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Monta o JSON completo da análise.
        val inferencia = InferenciaDto(
            id_sessao = UUID.randomUUID().toString(),
            data_hora = formato.format(Date()),
            nome_modelo = "best.onnx",
            tempo_execucao_ms = tempoMs,
            total_objetos = resultado.deteccoes.size,
            confianca_media = confiancaMedia,

            // NOVO: envia as dimensões da imagem
            // efetivamente utilizada pelo detector.
            dimensao_imagem = DimensaoImagemDto(
                largura_px = resultado.bitmap.width,
                altura_px = resultado.bitmap.height
            ),

            caixas_delimitadoras = caixasDto
        )

        // Tenta salvar no PostgreSQL.
        // Se a API falhar, a detecção local continua disponível.
        return try {

            val resposta = withContext(Dispatchers.IO) {
                RetrofitClient.api.enviarInferencia(inferencia)
            }

            ProcessamentoInferencia(
                analise = resultado,
                tempoMs = tempoMs,
                mensagemServidor =
                    "Inferência salva! Sessão: ${resposta.id_sessao}",
                salvoNoBanco = true
            )

        } catch (e: Exception) {

            ProcessamentoInferencia(
                analise = resultado,
                tempoMs = tempoMs,
                mensagemServidor =
                    "Não foi possível salvar no servidor: ${e.message}",
                salvoNoBanco = false
            )
        }
    }
}
