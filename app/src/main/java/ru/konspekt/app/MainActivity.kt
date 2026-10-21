package ru.konspekt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.konspekt.app.data.AppViewModel
import ru.konspekt.app.ui.KonspektApp
import ru.konspekt.app.ui.theme.KonspektTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KonspektTheme(settings = viewModel.settings) {
                KonspektApp(viewModel)
            }
        }
    }
}
