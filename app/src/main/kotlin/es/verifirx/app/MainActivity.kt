package es.verifirx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.verifirx.app.ui.navigation.VerifiRxNavHost
import es.verifirx.app.ui.theme.VerifiRxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val services = (application as VerifiRxApplication).services
        setContent {
            VerifiRxTheme {
                VerifiRxNavHost(services = services)
            }
        }
    }
}
