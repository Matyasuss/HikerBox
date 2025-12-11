package cz.matyasuss.hikerbox.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
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
import org.osmdroid.views.overlay.Overlay
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import kotlin.math.sqrt

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
            // Remove existing overlays except copyright
            mapView.overlays.removeAll { it !is CopyrightOverlay }

            // Create clustering overlay
            val clusteringOverlay = ClusteringOverlay(
                context = mapView.context,
                chargers = chargers,
                onChargerClick = onChargerClick
            )

            mapView.overlays.add(clusteringOverlay)
            mapView.invalidate()
        },
        modifier = modifier
    )
}

private class ClusteringOverlay(
    private val context: Context,
    private val chargers: List<Charger>,
    private val onChargerClick: (String) -> Unit
) : Overlay() {

    private val clusterRadius = 80 // pixels
    private val markerIcons = mutableMapOf<Int, BitmapDrawable>()
    private val clusterIcon = createClusterIcon(context, 60)

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    init {
        // Pre-create marker icons
        markerIcons[Color.BLUE] = createColoredMarker(context, Color.BLUE)
        markerIcons[Color.GREEN] = createColoredMarker(context, Color.GREEN)
        markerIcons[Color.RED] = createColoredMarker(context, Color.RED)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val projection = mapView.projection
        val clusters = clusterChargers(chargers, mapView)

        clusters.forEach { cluster ->
            val point = projection.toPixels(cluster.position, null)

            if (cluster.chargers.size == 1) {
                // Draw single marker
                val charger = cluster.chargers.first()
                val color = getChargerColor(charger.typeSpec)
                val icon = markerIcons[color] ?: markerIcons[Color.RED]!!

                icon.setBounds(
                    point.x - 20,
                    point.y - 40,
                    point.x + 20,
                    point.y
                )
                icon.draw(canvas)
            } else {
                // Draw cluster
                clusterIcon.setBounds(
                    point.x - 30,
                    point.y - 30,
                    point.x + 30,
                    point.y + 30
                )
                clusterIcon.draw(canvas)

                // Draw count
                val count = cluster.chargers.size.toString()
                val textY = point.y + (textPaint.textSize / 3)
                canvas.drawText(count, point.x.toFloat(), textY, textPaint)
            }
        }
    }

    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
        val projection = mapView.projection
        val clusters = clusterChargers(chargers, mapView)

        val tapPoint = Point(e.x.toInt(), e.y.toInt())

        clusters.forEach { cluster ->
            val point = projection.toPixels(cluster.position, null)
            val distance = sqrt(
                ((tapPoint.x - point.x) * (tapPoint.x - point.x) +
                        (tapPoint.y - point.y) * (tapPoint.y - point.y)).toDouble()
            ).toFloat()

            if (distance < 40) { // Touch radius
                if (cluster.chargers.size == 1) {
                    // Single marker clicked
                    onChargerClick(cluster.chargers.first().id)
                    return true
                } else {
                    // Cluster clicked - zoom in
                    mapView.controller.animateTo(cluster.position)
                    mapView.controller.setZoom(mapView.zoomLevelDouble + 2)
                    return true
                }
            }
        }

        return false
    }

    private fun clusterChargers(chargers: List<Charger>, mapView: MapView): List<Cluster> {
        val projection = mapView.projection
        val unclustered = chargers.toMutableList()
        val clusters = mutableListOf<Cluster>()

        while (unclustered.isNotEmpty()) {
            val first = unclustered.removeAt(0)
            val firstPoint = projection.toPixels(
                GeoPoint(first.latitude, first.longitude),
                null
            )

            val cluster = mutableListOf(first)

            val iterator = unclustered.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                val otherPoint = projection.toPixels(
                    GeoPoint(other.latitude, other.longitude),
                    null
                )

                val distance = sqrt(
                    ((firstPoint.x - otherPoint.x) * (firstPoint.x - otherPoint.x) +
                            (firstPoint.y - otherPoint.y) * (firstPoint.y - otherPoint.y)).toDouble()
                ).toFloat()

                if (distance < clusterRadius) {
                    cluster.add(other)
                    iterator.remove()
                }
            }

            // Calculate cluster center
            val avgLat = cluster.map { it.latitude }.average()
            val avgLon = cluster.map { it.longitude }.average()

            clusters.add(
                Cluster(
                    position = GeoPoint(avgLat, avgLon),
                    chargers = cluster
                )
            )
        }

        return clusters
    }

    private fun getChargerColor(typeSpec: String): Int {
        return when (typeSpec) {
            "charge_lock" -> Color.BLUE
            "charge_unloc" -> Color.GREEN
            else -> Color.RED
        }
    }
}

private data class Cluster(
    val position: GeoPoint,
    val chargers: List<Charger>
)

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

private fun createClusterIcon(context: Context, size: Int): BitmapDrawable {
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)

    // Create paint for outer circle (darker)
    val outerPaint = Paint().apply {
        color = Color.parseColor("#2196F3") // Material Blue
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Create paint for inner circle (lighter)
    val innerPaint = Paint().apply {
        color = Color.parseColor("#64B5F6") // Lighter Blue
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Create paint for border
    val borderPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val centerX = size / 2f
    val centerY = size / 2f
    val outerRadius = size / 2f - 2
    val innerRadius = size / 3f

    // Draw outer circle
    canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)

    // Draw border
    canvas.drawCircle(centerX, centerY, outerRadius, borderPaint)

    // Draw inner circle
    canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)

    return bitmap.toDrawable(context.resources)
}