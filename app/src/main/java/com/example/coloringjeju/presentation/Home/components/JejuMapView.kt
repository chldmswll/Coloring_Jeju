package com.example.coloringjeju.presentation.Home.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.coloringjeju.ui.theme.BrandForest
import com.example.coloringjeju.ui.theme.ColoringTheme
import com.example.coloringjeju.ui.theme.SurfaceBorder
import com.example.coloringjeju.ui.theme.SurfaceWhite
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

/**
 * A single collectible place pin on the map. `fillColor = null` marks it unvisited (color
 * mission not yet verified) — unrelated to [saved], which marks whether the traveler has added
 * this place to MY 지도; MY 지도 only ever shows pins where [saved] is true. [tag]/[headline]/
 * [description] back the detail sheet shown when the pin is tapped (see [PlaceDetailContent]).
 */
data class MapPinData(
    val label: String,
    val emoji: String,
    val fillColor: Color?,
    val lat: Double,
    val lng: Double,
    val tag: String,
    val headline: String,
    val description: String,
    val saved: Boolean,
)

private val JejuCenter = GeoPoint(33.38, 126.55)
private const val MIN_ZOOM = 11.0
private const val DEFAULT_ZOOM = MIN_ZOOM
private const val MAX_FIT_ZOOM = 13.0
private const val BOUNDS_PADDING_PX = 40

/**
 * The real "제주 지도" — OpenStreetMap tiles via osmdroid (`TileSourceFactory.MAPNIK`, no API
 * key), one [Marker] per [pins] entry. Zooms to fit every visible pin (capped at zoom
 * [MAX_FIT_ZOOM]) whenever [pins] changes — e.g. switching 추천 지도 ↔ MY 지도, or
 * saving/removing a place — and falls back to Jeju's center at zoom [DEFAULT_ZOOM] when [pins]
 * is empty. Tapping a marker reports it via [onPinClick]; the caller shows its detail as a
 * bottom sheet.
 */
@Composable
fun JejuMapView(pins: List<MapPinData>, onPinClick: (MapPinData) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shape = ColoringTheme.shapes.xl

    // Configuration (user agent, cache paths) is set once in MainActivity.onCreate, before any
    // Compose content — not re-loaded here, since Configuration.load() would overwrite it with
    // whatever (empty, on a fresh install) SharedPreferences holds.
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Zooming out too far was repeatedly hitting OSM's tile-usage-policy block (each
            // zoom-out step requests a whole new, previously-uncached set of tiles) — restricting
            // the zoom-out range further cuts how many distinct tile sets a pinch gesture can
            // pull at once.
            minZoomLevel = MIN_ZOOM
            maxZoomLevel = 19.0
            overlays.add(CopyrightOverlay(context))
            controller.setCenter(JejuCenter)
            controller.setZoom(DEFAULT_ZOOM)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(320.dp).clip(shape),
        factory = { mapView },
        update = { view ->
            view.overlays.removeAll { it is Marker }
            pins.forEach { pin ->
                val marker = Marker(view).apply {
                    position = GeoPoint(pin.lat, pin.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = pinDrawable(context, pin)
                    title = pin.label
                    setOnMarkerClickListener { _, _ ->
                        onPinClick(pin)
                        true
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()

            if (pins.isEmpty()) {
                view.controller.setCenter(JejuCenter)
                view.controller.setZoom(DEFAULT_ZOOM)
            } else if (pins.size == 1) {
                view.controller.setCenter(GeoPoint(pins.first().lat, pins.first().lng))
                view.controller.setZoom(MAX_FIT_ZOOM)
            } else {
                view.post {
                    val box = BoundingBox.fromGeoPoints(pins.map { GeoPoint(it.lat, it.lng) })
                    view.zoomToBoundingBox(box, false, BOUNDS_PADDING_PX)
                    if (view.zoomLevelDouble > MAX_FIT_ZOOM) {
                        view.controller.setZoom(MAX_FIT_ZOOM)
                    }
                }
            }
        },
    )
}

/** Draws a small filled-or-outlined circle bitmap for a [Marker] icon, matching the app's pin style. */
private fun pinDrawable(context: Context, pin: MapPinData): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (36 * density).toInt()
    val strokePx = (if (pin.fillColor != null) 3f else 2.5f) * density

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = center - strokePx

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = (pin.fillColor ?: SurfaceWhite).toArgb()
    }
    canvas.drawCircle(center, center, radius, fillPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = (if (pin.fillColor != null) BrandForest else SurfaceBorder).toArgb()
    }
    canvas.drawCircle(center, center, radius, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}
