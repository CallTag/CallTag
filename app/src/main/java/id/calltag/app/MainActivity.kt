package id.calltag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            if (!screenedPhoneExtra.isNullOrBlank()) {
                mainViewModel.performPhoneLookup(screenedPhoneExtra)
            }

            val isDarkMode by mainViewModel.isDarkMode.collectAsState()

            CallTagTheme(darkTheme = isDarkMode) {
                MainScaffold(viewModel = mainViewModel)
            }
        }
    }
}
