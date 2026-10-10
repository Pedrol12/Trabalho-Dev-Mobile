package com.example.appdeteccaocachorro.network

import com.google.gson.annotations.SerializedName

// Payload enviado no POST /api/inferencia (Conforme Secao 4 do Enunciado N2)
data class InferenciaRequest(
    @SerializedName("id_sessao")
    val idSessao: String,

    @SerializedName("data_hora")
    val dataHora: String,

    @SerializedName("nome_modelo")
    val nomeModelo: String,

    @SerializedName("tempo_execucao_ms")
    val tempoExecucaoMs: Long,

    @SerializedName("total_objetos")
    val totalObjetos: Int,

    @SerializedName("confianca_media")
    val confiancaMedia: Float,

    @SerializedName("dimensao_imagem")
    val dimensaoImagem: DimensaoImagemDto,

    @SerializedName("caixas_delimitadoras")
    val caixasDelimitadoras: List<CaixaDelimitadoraDto>
)

data class DimensaoImagemDto(
    @SerializedName("largura_px")
    val larguraPx: Int,

    @SerializedName("altura_px")
    val alturaPx: Int
)

data class CaixaDelimitadoraDto(
    @SerializedName("id_caixa")
    val idCaixa: Int,

    @SerializedName("rotulo_classe")
    val rotuloClasse: String,

    @SerializedName("confianca")
    val confianca: Float,

    @SerializedName("largura_px")
    val larguraPx: Float,

    @SerializedName("altura_px")
    val alturaPx: Float,

    @SerializedName("centroide_x")
    val centroideX: Float,

    @SerializedName("centroide_y")
    val centroideY: Float,

    @SerializedName("area_px2")
    val areaPx2: Float
)

// Resposta de POST /api/inferencia
data class InferenciaResponse(
    @SerializedName("message")
    val message: String?,

    @SerializedName("id_sessao")
    val idSessao: String?,

    @SerializedName("total_caixas_salvas")
    val totalCaixasSalvas: Int?
)

// Item retornado no GET /api/inferencia/historico
data class HistoricoSessaoDto(
    @SerializedName("id_sessao")
    val idSessao: String,

    @SerializedName("data_hora")
    val dataHora: String,

    @SerializedName("nome_modelo")
    val nomeModelo: String,

    @SerializedName("tempo_execucao_ms")
    val tempoExecucaoMs: Long,

    @SerializedName("total_objetos")
    val totalObjetos: Int,

    @SerializedName("confianca_media")
    val confiancaMedia: Float
)

// Resposta retornada no GET /api/inferencia/:id_sessao/caixas
data class DetalhesSessaoDto(
    @SerializedName("sessao")
    val sessao: HistoricoSessaoDto,

    @SerializedName("caixas_delimitadoras")
    val caixasDelimitadoras: List<CaixaDelimitadoraDto>
)
