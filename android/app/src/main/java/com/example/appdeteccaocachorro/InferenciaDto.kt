
package com.example.appdeteccaocachorro

// Dimensões da imagem analisada pelo YOLO.
data class DimensaoImagemDto(
    val largura_px: Int,
    val altura_px: Int
)

// Dados de cada caixa enviados ao backend.
data class CaixaDto(
    val rotulo_classe: String,
    val confianca: Float,
    val largura_px: Float,
    val altura_px: Float,
    val centroide_x: Float,
    val centroide_y: Float,
    val area_px2: Float
)

// Sessão completa da inferência.
data class InferenciaDto(
    val id_sessao: String,
    val data_hora: String,
    val nome_modelo: String,
    val tempo_execucao_ms: Long,
    val total_objetos: Int,
    val confianca_media: Float,
    val caixas_delimitadoras: List<CaixaDto>,

    // Opcional temporariamente para manter
    // compatibilidade com o código atual.
    val dimensao_imagem: DimensaoImagemDto? = null
)

// Resposta do POST.
data class RespostaInferenciaDto(
    val message: String,
    val id_sessao: String,
    val total_caixas_salvas: Int
)

// Dados exibidos no histórico.
data class HistoricoDto(
    val id_sessao: String,
    val data_hora: String,
    val nome_modelo: String,
    val tempo_execucao_ms: Long,
    val total_objetos: Int,
    val confianca_media: Float
)

// Resposta do detalhamento de uma sessão.
data class DetalhesInferenciaDto(
    val sessao: HistoricoDto,
    val caixas_delimitadoras: List<CaixaDetalheDto>
)

// Caixa recuperada do PostgreSQL.
data class CaixaDetalheDto(
    val id_caixa: Int,
    val id_sessao: String,
    val rotulo_classe: String,
    val confianca: Float,
    val largura_px: Float,
    val altura_px: Float,
    val centroide_x: Float,
    val centroide_y: Float,
    val area_px2: Float
)
