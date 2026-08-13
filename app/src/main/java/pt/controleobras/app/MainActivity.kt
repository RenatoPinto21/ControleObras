package pt.controleobras.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import pt.controleobras.app.core.designsystem.theme.ControleObrasTheme
import pt.controleobras.app.core.navigation.ControleObrasNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen — deve ser chamado ANTES de super.onCreate().
        // Mostra o ícone da app sobre fundo IndustrialDeep (#0F1419)
        // durante o arranque, substituindo o ecrã branco genérico.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControleObrasTheme {
                ControleObrasNavHost()
            }
        }
    }
}
