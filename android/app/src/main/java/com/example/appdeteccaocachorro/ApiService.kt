
package com.example.appdeteccaocachorro

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Define as rotas disponíveis no backend
interface ApiService {

    // Envia uma nova inferência
    @POST("api/inferencia")
    suspend fun enviarInferencia(
        @Body inferencia: InferenciaDto
    ): RespostaInferenciaDto

    // Consulta o histórico de sessões
    @GET("api/inferencia/historico")
    suspend fun buscarHistorico(): List<HistoricoDto>

    // Consulta as caixas de uma sessão específica
    @GET("api/inferencia/{id_sessao}/caixas")
    suspend fun buscarCaixas(
        @Path("id_sessao") idSessao: String
    ): DetalhesInferenciaDto
}
