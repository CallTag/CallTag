package id.calltag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import id.calltag.app.ui.theme.CallTagDesign
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import id.calltag.app.ui.MainScaffold
import id.calltag.app.ui.theme.CallTagTheme
import id.calltag.app.ui.viewmodel.CallTagMainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CallTagApplication
        val screenedPhoneExtra = intent?.getStringExtra("screened_phone")

        setContent {
            val mainViewModel: CallTagMainViewModel = viewModel(
                factory = CallTagMainViewModel.provideFactory(
                    repository = app.repository,
                    preferences = app.preferences
                )
            )
            LaunchedEffect(screenedPhoneExtra) {
                if (!screenedPhoneExtra.isNullOrBlank()) {
                    mainViewModel.performPhoneLookup(screenedPhoneExtra)
                }
            }

            val isDarkMode by mainViewModel.isDarkMode.collectAsState()

            CallTagTheme(darkTheme = isDarkMode) {
                val colors = CallTagDesign.colors
                SideEffect {
                    window.statusBarColor = colors.backgroundPrimary.toArgb()
                    window.navigationBarColor = colors.surfaceCard.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkMode
                        isAppearanceLightNavigationBars = !isDarkMode
                    }
                }
                MainScaffold(viewModel = mainViewModel)
            }
        }
    }
}
