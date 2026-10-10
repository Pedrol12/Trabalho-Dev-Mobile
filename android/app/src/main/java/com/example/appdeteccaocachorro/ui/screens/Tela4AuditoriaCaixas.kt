package com.example.appdeteccaocachorro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appdeteccaocachorro.network.CaixaDelimitadoraDto
import com.example.appdeteccaocachorro.ui.theme.BrandPrimary
import com.example.appdeteccaocachorro.ui.theme.SuccessGreen
import com.example.appdeteccaocachorro.ui.theme.TextPrimary
import com.example.appdeteccaocachorro.ui.theme.TextSecondary
import com.example.appdeteccaocachorro.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Tela4AuditoriaCaixas(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val detalhes by viewModel.detalhesSessao.collectAsState()
    val carregando by viewModel.carregandoDetalhes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Botao Voltar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.navegarPara("TELA_3") },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("← Voltar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (carregando) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (detalhes == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum detalhe disponível para esta sessão.", color = TextSecondary)
            }
        } else {
            val sessao = detalhes!!.sessao
            val caixas = detalhes!!.caixasDelimitadoras

            // Card Resumo da Sessao
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Detecção #${sessao.idSessao}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = sessao.dataHora,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MODELO", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(sessao.nomeModelo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL CAIXAS", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${caixas.size} caixas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TEMPO INFERÊNCIA", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${sessao.tempoExecucaoMs} ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✔ BD Servidor Conectado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Titulo da Secao de Telemetria
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📐 Telemetria das Caixas Delimitadoras",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Exibindo ${caixas.size}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Cartoes de Auditoria (RF5 / RF8)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(caixas) { caixa ->
                    CardAuditoriaCaixa(caixa = caixa)
                }
            }
        }
    }
}

@Composable
fun CardAuditoriaCaixa(caixa: CaixaDelimitadoraDto) {
    val confPorcento = (caixa.confianca * 100).roundToInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecalho do Card da Caixa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Caixa #${caixa.idCaixa} (${caixa.rotuloClasse})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )

                Box(
                    modifier = Modifier
                        .background(BrandPrimary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Confiança: $confPorcento%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dimensoes Wpx e Hpx
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("LARGURA (W_px)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f px", caixa.larguraPx),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("ALTURA (H_px)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f px", caixa.alturaPx),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Centroide (Xc, Yc) e Area Apx2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CENTROIDE (X_c, Y_c)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "(%.2f, %.2f) px", caixa.centroideX, caixa.centroideY),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("ÁREA (A_px²)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f px²", caixa.areaPx2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
