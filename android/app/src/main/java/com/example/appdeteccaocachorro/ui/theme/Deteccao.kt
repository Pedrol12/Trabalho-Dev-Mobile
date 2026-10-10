package com.example.appdeteccaocachorro

// guarda os dados de UM objeto detectado pelo modelo
data class Deteccao(
    val idCaixa: Int = 1,
    val rotuloClasse: String = "cachorro",
    val confianca: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
) {
    // calculos geometricos conforme RF5 da especificacao N2
    val larguraPx: Float get() = (xMax - xMin).coerceAtLeast(0f)
    val alturaPx: Float get() = (yMax - yMin).coerceAtLeast(0f)
    val centroideX: Float get() = xMin + (larguraPx / 2f)
    val centroideY: Float get() = yMin + (alturaPx / 2f)
    val areaPx2: Float get() = larguraPx * alturaPx
}