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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
    /**
     * 중심관광지 rank — lower is more central. Decides which pin represents a stack of overlapping
     * pins (see [clusterPins]). MY 지도 places carry no ranking, so they default to last.
     */
    val rank: Int = Int.MAX_VALUE,
) {
    /** The color mission is done — the pin shows its photo in full color instead of grayscale. */
    val verified: Boolean get() = fillColor != null
}

/** One marker actually drawn: [head] is the pin shown, [extra] how many it stands in for. */
private data class PinCluster(val head: MapPinData, val extra: Int)

private const val EARTH_RADIUS_M = 6_371_000.0

private fun metersBetween(a: MapPinData, b: MapPinData): Double {
    val latMid = Math.toRadians((a.lat + b.lat) / 2.0)
    val dx = Math.toRadians(a.lng - b.lng) * cos(latMid) * EARTH_RADIUS_M
    val dy = Math.toRadians(a.lat - b.lat) * EARTH_RADIUS_M
    return sqrt(dx * dx + dy * dy)
}

/** Web-Mercator ground resolution — how many metres one screen pixel covers at this zoom. */
private fun metersPerPixel(latitude: Double, zoom: Double): Double =
    156_543.03392 * cos(Math.toRadians(latitude)) / 2.0.pow(zoom)

/**
 * Collapses pins that would overlap on screen into one marker each, the way a map app stacks
 * crowded places: walk the pins best-rank-first, and let each one swallow every un-taken pin
 * closer than [minMeters]. The survivor keeps its photo and gets a `+N` badge.
 *
 * Screen distance between two pins depends only on the zoom level, never on where the map is
 * panned — so this is recomputed on zoom changes alone, not on every scroll.
 */
private fun clusterPins(pins: List<MapPinData>, minMeters: Double): List<PinCluster> {
    if (pins.size < 2) return pins.map { PinCluster(it, 0) }
    val remaining = pins.sortedBy { it.rank }.toMutableList()
    val clusters = mutableListOf<PinCluster>()
    while (remaining.isNotEmpty()) {
        val head = remaining.removeAt(0)
        val swallowed = remaining.filter { metersBetween(head, it) < minMeters }
        remaining.removeAll(swallowed)
        clusters += PinCluster(head, swallowed.size)
    }
    return clusters
}

private val JejuCenter = GeoPoint(33.38, 126.55)

/**
 * How far the map may be dragged — 제주 island with a margin wide enough to hold 우도 (126.95E),
 * 마라도 (33.11N) and 비양도 (126.23E) plus breathing room, so every pin can be centred but the map
 * can't be panned off to the mainland. `BoundingBox(north, east, south, west)`.
 */
private val JejuScrollBounds = BoundingBox(33.70, 127.10, 33.05, 126.00)
private const val MIN_ZOOM = 11.0
private const val DEFAULT_ZOOM = MIN_ZOOM
private const val MAX_FIT_ZOOM = 13.0
private const val BOUNDS_PADDING_PX = 40
// 추천 지도 fits all of Jeju on screen, where the closest recommended places sit ~3 km apart — big
// photo pins overlap each other at that zoom. 36dp keeps the photo readable while leaving the
// island legible; panning/zooming in separates the rest.
private const val PIN_SIZE_DP = 36
private const val PIN_BORDER_DP = 2.5f
/** Two pins closer than this many pin-widths are drawn as one stacked marker. */
private const val PIN_CLUSTER_GAP = 1.15

/** Remembers which pin set the camera was last fitted to, so loading a photo doesn't re-zoom the map. */
private class CameraFit {
    var key: String? = null
}

/** Holds the current marker-drawing pass so the map's zoom listener can re-run it. */
private class MarkerRenderer {
    var draw: (MapView) -> Unit = {}
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
            // This is a 제주 map and nothing else — panning off into open sea (or to the mainland)
            // only ever means the traveler has lost the island. The box wraps 제주 plus 우도·비양도
            // ·마라도 with a margin, so every pin stays reachable while the map can't be dragged
            // away from the place it is about.
            setScrollableAreaLimitDouble(JejuScrollBounds)
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

    // Markers are rebuilt from two places — Compose's `update` and the map's own zoom events — so
    // the drawing pass is stashed here for the zoom listener to re-run. It is (re)built inside
    // `update` rather than at composition, so that `update` keeps capturing `pins`/`icons` itself
    // and therefore re-runs when a photo finishes loading.
    val renderer = remember { MarkerRenderer() }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            // Panning never changes the pixel gap between two pins, so only zoom re-clusters.
            override fun onScroll(event: ScrollEvent?): Boolean = false
            override fun onZoom(event: ZoomEvent?): Boolean {
                renderer.draw(mapView)
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(320.dp).clip(shape),
        factory = { mapView },
        update = { view ->
            renderer.draw = { target ->
                drawMarkers(target, context, pins, icons, pinSizePx, density, onPinClick)
            }
            renderer.draw(view)

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
 * Lays every marker down for the current zoom: pins that would overlap are collapsed into one
 * badged marker (see [clusterPins]), and tapping such a marker zooms in instead of opening a sheet,
 * since a stack can't say which of its places you meant.
 */
private fun drawMarkers(
    view: MapView,
    context: Context,
    pins: List<MapPinData>,
    icons: Map<String, Bitmap>,
    pinSizePx: Int,
    density: Float,
    onPinClick: (MapPinData) -> Unit,
) {
    view.overlays.removeAll { it is Marker }
    val minMeters = pinSizePx * PIN_CLUSTER_GAP * metersPerPixel(view.mapCenter.latitude, view.zoomLevelDouble)
    clusterPins(pins, minMeters).forEach { cluster ->
        val pin = cluster.head
        val photo = pin.imageUrl?.let { icons[iconKey(it, pin.verified)] }
        val marker = Marker(view).apply {
            position = GeoPoint(pin.lat, pin.lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = if (photo != null) {
                photoPinDrawable(context, photo, pin.verified, pinSizePx, density, cluster.extra)
            } else {
                pinDrawable(context, pin, cluster.extra)
            }
            title = pin.label
            setOnMarkerClickListener { _, _ ->
                if (cluster.extra > 0) {
                    view.controller.animateTo(GeoPoint(pin.lat, pin.lng), view.zoomLevelDouble + 2.0, 300L)
                } else {
                    onPinClick(pin)
                }
                true
            }
        }
        view.overlays.add(marker)
    }
    view.invalidate()
}

/**
 * Crops [photo] into a circular [Marker] icon with the app's pin border. An unverified pin
 * ([verified] false) is drawn desaturated, so the map reads at a glance as "the colors I've
 * earned, and the ones still waiting". [extra] > 0 adds the `+N` badge for the places this pin is
 * standing in for.
 */
private fun photoPinDrawable(
    context: Context,
    photo: Bitmap,
    verified: Boolean,
    sizePx: Int,
    density: Float,
    extra: Int = 0,
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
    canvas.drawStackBadge(extra, sizePx, density)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * `+N` in the top-right corner — this marker is standing in for [extra] more places hidden under
 * it at this zoom. Drawn over the photo rather than beside it so the marker keeps one anchor point
 * and stays centred on its own coordinates.
 */
private fun Canvas.drawStackBadge(extra: Int, sizePx: Int, density: Float) {
    if (extra <= 0) return
    val radius = sizePx * 0.30f
    val cx = sizePx - radius
    val cy = radius
    drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BrandForest.toArgb() })
    drawCircle(
        cx, cy, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            color = SurfaceWhite.toArgb()
        },
    )
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SurfaceWhite.toArgb()
        textSize = radius * 1.0f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawText("+$extra", cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
}

/**
 * Fallback [Marker] icon — the original filled-or-outlined circle, used while a pin's photo is
 * still loading and for the places TourAPI has no photo for.
 */
private fun pinDrawable(context: Context, pin: MapPinData, extra: Int = 0): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (PIN_SIZE_DP * density).toInt()
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
    canvas.drawStackBadge(extra, sizePx, density)

    return BitmapDrawable(context.resources, bitmap)
}
