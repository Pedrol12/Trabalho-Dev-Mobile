package com.example.appdeteccaocachorro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.appdeteccaocachorro.ui.screens.Tela1Configuracao
import com.example.appdeteccaocachorro.ui.screens.Tela2Resultado
import com.example.appdeteccaocachorro.ui.screens.Tela3Historico
import com.example.appdeteccaocachorro.ui.screens.Tela4AuditoriaCaixas
import com.example.appdeteccaocachorro.ui.theme.AppDeteccaoCachorroTheme
import com.example.appdeteccaocachorro.ui.theme.BrandPrimary
import com.example.appdeteccaocachorro.ui.theme.TextSecondary
import com.example.appdeteccaocachorro.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDeteccaoCachorroTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val abaSelecionada by viewModel.abaSelecionada.collectAsState()
    val telaAtual by viewModel.telaAtual.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = abaSelecionada == 0,
                    onClick = { viewModel.definirAba(0) },
                    icon = { Text(text = "🎯", fontWeight = FontWeight.Bold) },
                    label = { Text("Detectar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = BrandPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = abaSelecionada == 1,
                    onClick = { viewModel.definirAba(1) },
                    icon = { Text(text = "🕒", fontWeight = FontWeight.Bold) },
                    label = { Text("Histórico") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = BrandPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (telaAtual) {
            "TELA_1" -> Tela1Configuracao(viewModel = viewModel, modifier = modifier)
            "TELA_2" -> Tela2Resultado(viewModel = viewModel, modifier = modifier)
            "TELA_3" -> Tela3Historico(viewModel = viewModel, modifier = modifier)
            "TELA_4" -> Tela4AuditoriaCaixas(viewModel = viewModel, modifier = modifier)
            else -> Tela1Configuracao(viewModel = viewModel, modifier = modifier)
        }
    }
}