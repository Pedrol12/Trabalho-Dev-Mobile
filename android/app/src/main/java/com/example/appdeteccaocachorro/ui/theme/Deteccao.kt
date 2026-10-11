
package com.example.appdeteccaocachorro

import java.util.UUID

// Guarda os dados de UM objeto detectado pelo modelo
data class Deteccao(
    val rotuloClasse: String,
    val confianca: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val id: String = UUID.randomUUID().toString()
) {
    // Calculos geometricos
    val larguraPx: Float get() = xMax - xMin
    val alturaPx: Float get() = yMax - yMin
    val centroideX: Float get() = xMin + larguraPx / 2
    val centroideY: Float get() = yMin + alturaPx / 2
    val areaPx2: Float get() = larguraPx * alturaPx
}
