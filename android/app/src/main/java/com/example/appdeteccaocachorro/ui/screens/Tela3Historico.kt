package com.example.appdeteccaocachorro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appdeteccaocachorro.network.HistoricoSessaoDto
import com.example.appdeteccaocachorro.ui.theme.BrandPrimary
import com.example.appdeteccaocachorro.ui.theme.SuccessGreen
import com.example.appdeteccaocachorro.ui.theme.TextPrimary
import com.example.appdeteccaocachorro.ui.theme.TextSecondary
import com.example.appdeteccaocachorro.viewmodel.MainViewModel

@Composable
fun Tela3Historico(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historico by viewModel.historico.collectAsState()
    val carregando by viewModel.carregandoHistorico.collectAsState()
    val filtro by viewModel.filtroHistorico.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.carregarHistorico()
    }

    val listaFiltrada = historico.filter {
        it.idSessao.contains(filtro, ignoreCase = true) ||
        it.nomeModelo.contains(filtro, ignoreCase = true) ||
        it.dataHora.contains(filtro, ignoreCase = true)
    }

    val totalInferencias = historico.size
    val totalObjetos = historico.sumOf { it.totalObjetos }
    val mediaTempo = if (historico.isNotEmpty()) {
        historico.map { it.tempoExecucaoMs }.average().toLong()
    } else 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Cabecalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Histórico de Inferências",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "• Sincronizado com o Servidor Backend",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            TextButton(onClick = { viewModel.carregarHistorico() }) {
                Text("🔄 Atualizar", color = BrandPrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cards de Metricas Resumo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardResumo(titulo = "INFERÊNCIAS", valor = "$totalInferencias", modifier = Modifier.weight(1f))
            CardResumo(titulo = "OBJETOS CONTADOS", valor = "$totalObjetos", modifier = Modifier.weight(1f))
            CardResumo(titulo = "MÉDIA TEMPO", valor = "$mediaTempo ms", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Campo de Filtro
        OutlinedTextField(
            value = filtro,
            onValueChange = { viewModel.setFiltroHistorico(it) },
            placeholder = { Text("🔍 Filtrar por data ou modelo...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Registros Recentes (Toque para detalhes)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (carregando) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (listaFiltrada.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma inferência encontrada.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(listaFiltrada) { item ->
                    CardItemSessao(
                        item = item,
                        onClick = { viewModel.carregarDetalhesSessao(item.idSessao) }
                    )
                }
            }
        }
    }
}

@Composable
fun CardResumo(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = titulo,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CardItemSessao(
    item: HistoricoSessaoDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.idSessao,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = item.dataHora,
                    fontSize = 11.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Modelo:", fontSize = 11.sp, color = TextSecondary)
                    Text(item.nomeModelo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total:", fontSize = 11.sp, color = TextSecondary)
                    Text("${item.totalObjetos} objs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Tempo:", fontSize = 11.sp, color = TextSecondary)
                    Text("${item.tempoExecucaoMs} ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✔ Enviado ao Servidor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )
                }

                Text(
                    text = "Ver detalhes →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
        }
    }
}
