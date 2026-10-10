package com.example.appdeteccaocachorro.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InferenceApiService {

    @POST("/api/inferencia")
    suspend fun enviarInferencia(@Body request: InferenciaRequest): Response<InferenciaResponse>

    @GET("/api/inferencia/historico")
    suspend fun obterHistorico(): Response<List<HistoricoSessaoDto>>

    @GET("/api/inferencia/{id_sessao}/caixas")
    suspend fun obterDetalhesSessao(@Path("id_sessao") idSessao: String): Response<DetalhesSessaoDto>
}

object RetrofitClient {
    // 10.0.2.2 eh o loopback para o host da máquina no Emulador Android.
    // Pode ser alterado para o IP local da máquina na rede Wi-Fi se rodar em celular físico.
    var baseUrl: String = "http://10.0.2.2:3000/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            instance = buildService(field)
        }

    private fun buildService(url: String): InferenceApiService {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InferenceApiService::class.java)
    }

    var instance: InferenceApiService = buildService(baseUrl)
        private set
}
