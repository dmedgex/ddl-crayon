package com.trickcal.crayon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trickcal.crayon.model.ThemeMode
import com.trickcal.crayon.navigation.CrayonApp
import com.trickcal.crayon.ui.theme.TrickcalCrayonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CrayonApplication
        val appContainer = app.appContainer
        val viewModelFactory = CrayonViewModelFactory(app.appContainer)

        setContent {
            val themeMode = appContainer.settingsRepository.observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)

            TrickcalCrayonTheme(themeMode = themeMode.value) {
                CrayonApp(
                    appContainer = appContainer,
                    viewModelFactory = viewModelFactory,
                )
            }
        }
    }
}
