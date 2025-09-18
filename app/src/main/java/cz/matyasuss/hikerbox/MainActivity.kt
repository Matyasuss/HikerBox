package cz.matyasuss.hikerbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.matyasuss.hikerbox.ui.theme.HikerBoxTheme
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.CopyrightOverlay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HikerBoxTheme {
                OSMMap(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun OSMMap(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                minZoomLevel = 4.0
                setExpectedCenter(GeoPoint(50.0338394, 15.7903739))
                val copyrightOverlay = CopyrightOverlay(context).apply {
                    setAlignRight(true)
                    setAlignBottom(true)
                }
                overlays.add(copyrightOverlay)            }
        },
        modifier = modifier
    )
}
