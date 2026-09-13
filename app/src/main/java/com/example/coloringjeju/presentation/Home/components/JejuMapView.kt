package com.example.coloringjeju.presentation.Home.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
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
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
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
import kotlin.math.min

/**
 * A single collectible place pin on the map. `fillColor = null` marks it unverified (the color
 * mission hasn't been completed yet) — unrelated to [saved], which marks whether the traveler has
 * added this place to MY 지도; MY 지도 only ever shows pins where [saved] is true.
 *
 * [imageUrl] is the place's TourAPI photo: it fills the marker itself (grayscale until
 * [verified], see [photoPinDrawable]) and the detail sheet's hero image. [contentId] records which
 * TourAPI entry that photo came from.
 *
 * [tag]/[headline]/[description] back the detail sheet shown when the pin is tapped (see
 * [PlaceDetailContent]).
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
    val contentId: String? = null,
    val imageUrl: String? = null,
) {
    /** The color mission is done — the pin shows its photo in full color instead of grayscale. */
    val verified: Boolean get() = fillColor != null
}

private val JejuCenter = GeoPoint(33.38, 126.55)
private const val MIN_ZOOM = 11.0
private const val DEFAULT_ZOOM = MIN_ZOOM
private const val MAX_FIT_ZOOM = 13.0
private const val BOUNDS_PADDING_PX = 40
// 추천 지도 fits all of Jeju on screen, where the closest recommended places sit ~3 km apart — big
// photo pins overlap each other at that zoom. 36dp keeps the photo readable while leaving the
// island legible; panning/zooming in separates the rest.
private const val PIN_SIZE_DP = 36
private const val PIN_BORDER_DP = 2.5f

/** Remembers which pin set the camera was last fitted to, so loading a photo doesn't re-zoom the map. */
private class CameraFit {
    var key: String? = null
}

/**
 * The real "제주 지도" — OpenStreetMap tiles via osmdroid (`TileSourceFactory.MAPNIK`, no API
 * key), one [Marker] per [pins] entry.
 *
 * Each marker is the place's TourAPI photo cropped into a circle, drawn desaturated until the pin
 * is [MapPinData.verified]. Photos come off the Coil singleton (sharing its memory/disk cache with
 * the sheet's hero images) and are kept here as ready-made bitmaps keyed by URL + verified state;
 * until one arrives — or for a place TourAPI has no photo for — the marker falls back to the
 * original filled-or-outlined dot.
 *
 * Zooms to fit every visible pin (capped at zoom [MAX_FIT_ZOOM]) whenever the *set of pins*
 * changes — e.g. switching 추천 지도 ↔ MY 지도 — and falls back to Jeju's center at zoom
 * [DEFAULT_ZOOM] when [pins] is empty. Tapping a marker reports it via [onPinClick]; the caller
 * shows its detail as a bottom sheet.
 */
@Composable
fun JejuMapView(pins: List<MapPinData>, onPinClick: (MapPinData) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shape = ColoringTheme.shapes.xl
    val density = context.resources.displayMetrics.density
    val pinSizePx = (PIN_SIZE_DP * density).toInt()

    // Marker icons keyed by photo URL + verified state (the same photo needs its own bitmap in
    // color and in grayscale). Filled by the loader below and read back in `update`, so a photo
    // landing rebuilds the markers without disturbing the camera.
    val pinIcons = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(pins) {
        pins.forEach { pin ->
            val url = pin.imageUrl ?: return@forEach
            val key = iconKey(url, pin.verified)
            if (pinIcons.containsKey(key)) return@forEach
            // allowHardware(false): the result is re-drawn through a BitmapShader onto a software
            // Canvas below, and a hardware bitmap can't be read back that way.
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(pinSizePx)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val image = result.image
                pinIcons[key] = image.toBitmap(image.width, image.height)
            }
        }
    }

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
    val cameraFit = remember { CameraFit() }

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

    // Read the icon map during composition so a newly loaded photo actually re-runs `update`.
    val icons = pinIcons.toMap()

    AndroidView(
        modifier = modifier.fillMaxWidth().height(320.dp).clip(shape),
        factory = { mapView },
        update = { view ->
            view.overlays.removeAll { it is Marker }
            pins.forEach { pin ->
                val photo = pin.imageUrl?.let { icons[iconKey(it, pin.verified)] }
                val marker = Marker(view).apply {
                    position = GeoPoint(pin.lat, pin.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = if (photo != null) {
                        photoPinDrawable(context, photo, pin.verified, pinSizePx, density)
                    } else {
                        pinDrawable(context, pin)
                    }
                    title = pin.label
                    setOnMarkerClickListener { _, _ ->
                        onPinClick(pin)
                        true
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()

            // Only re-fit when the pin set itself changed — otherwise every photo that finishes
            // loading would yank the camera back while the traveler is panning around.
            val fitKey = pins.joinToString("|") { it.label }
            if (cameraFit.key != fitKey) {
                cameraFit.key = fitKey
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
            }
        },
    )
}

private fun iconKey(url: String, verified: Boolean) = url + if (verified) "@color" else "@gray"

/**
 * Crops [photo] into a circular [Marker] icon with the app's pin border. An unverified pin
 * ([verified] false) is drawn desaturated, so the map reads at a glance as "the colors I've
 * earned, and the ones still waiting".
 */
private fun photoPinDrawable(
    context: Context,
    photo: Bitmap,
    verified: Boolean,
    sizePx: Int,
    density: Float,
): Drawable {
    val borderPx = PIN_BORDER_DP * density
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = center - borderPx / 2f

    // Center-crop: scale the photo's shorter side up to the pin, then centre the overflow.
    val scale = sizePx.toFloat() / min(photo.width, photo.height)
    val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(
            Matrix().apply {
                setScale(scale, scale)
                postTranslate(
                    (sizePx - photo.width * scale) / 2f,
                    (sizePx - photo.height * scale) / 2f,
                )
            },
        )
    }

    val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
        if (!verified) {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
    }
    canvas.drawCircle(center, center, radius, photoPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderPx
        color = (if (verified) BrandForest else SurfaceBorder).toArgb()
    }
    canvas.drawCircle(center, center, radius, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Fallback [Marker] icon — the original filled-or-outlined circle, used while a pin's photo is
 * still loading and for the places TourAPI has no photo for.
 */
private fun pinDrawable(context: Context, pin: MapPinData): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (36 * density).toInt()
    val strokePx = (if (pin.verified) 3f else 2.5f) * density

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
        color = (if (pin.verified) BrandForest else SurfaceBorder).toArgb()
    }
    canvas.drawCircle(center, center, radius, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}
