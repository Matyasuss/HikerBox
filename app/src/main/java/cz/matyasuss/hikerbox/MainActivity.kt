package cz.matyasuss.hikerbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.matyasuss.hikerbox.ui.screen.MapScreen
import cz.matyasuss.hikerbox.ui.theme.HikerBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HikerBoxTheme {
                MapScreen()
            }
        }
    }
}