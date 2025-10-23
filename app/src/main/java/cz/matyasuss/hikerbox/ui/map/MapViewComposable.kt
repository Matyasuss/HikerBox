package cz.matyasuss.hikerbox.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import cz.matyasuss.hikerbox.model.Charger
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    chargers: List<Charger>,
    onChargerClick: (String) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(7.0)
                minZoomLevel = 4.0
                controller.setCenter(GeoPoint(49.8, 15.5))

                val copyrightOverlay = CopyrightOverlay(context).apply {
                    setAlignRight(true)
                    setAlignBottom(true)
                }
                overlays.add(copyrightOverlay)
            }
        },
        update = { mapView ->
            // Remove existing markers
            mapView.overlays.removeAll { it is Marker }

            // Add markers for each charger
            chargers.forEach { charger ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(charger.latitude, charger.longitude)
                    title = charger.name
                    snippet = charger.description

                    // Set marker color based on type
                    val color = when (charger.typeSpec) {
                        "charge_lock" -> Color.BLUE
                        "charge_unloc" -> Color.GREEN
                        else -> Color.RED
                    }

                    icon = createColoredMarker(mapView.context, color)

                    // Handle marker click
                    setOnMarkerClickListener { _, _ ->
                        onChargerClick(charger.id)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }

            mapView.invalidate()
        },
        modifier = modifier
    )
}

private fun createColoredMarker(context: Context, color: Int): BitmapDrawable {
    val size = 40
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Draw circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    // Draw white border
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    return bitmap.toDrawable(context.resources)
}