package com.strathmore.predictivedispatch.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color as AndroidColor
import com.strathmore.predictivedispatch.data.BlackSpot
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ─── Kenya GPS Bounds Guard ────────────────────────────────────────────────────
// Prevents the Android emulator's default California GPS (-122.08400, 37.42200)
// from ever being shown. Snaps to Nairobi CBD if coordinates are outside Kenya.
private const val NAIROBI_LAT = -1.286389
private const val NAIROBI_LON = 36.817223

fun resolveNairobiCoordinates(lat: Double, lon: Double): Pair<Double, Double> {
    val isWithinKenya = lat in -4.7..5.0 && lon in 33.9..41.9
    return if (isWithinKenya) Pair(lat, lon) else Pair(NAIROBI_LAT, NAIROBI_LON)
}

// ─── Permissive SSL (Emulator chain validation fix) ────────────────────────────
fun enablePermissiveSsl() {
    try {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        })
        val sslCtx = SSLContext.getInstance("TLS")
        sslCtx.init(null, trustAll, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sslCtx.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    } catch (_: Exception) {}
}

// ─── CartoDB Tile Sources (No API Key, No OSM Policy Restrictions) ─────────────
// CartoDB serves tiles to Android apps freely. Voyager = light street map,
// Dark_All = dark obsidian map. Both confirmed working via HTTPS from the emulator.

private val CartoDarkTiles = object : XYTileSource(
    "CartoDarkMatter",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://b.basemaps.cartocdn.com/rastertiles/dark_all/",
        "https://c.basemaps.cartocdn.com/rastertiles/dark_all/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$z/$x/$y.png"
    }
}

private val CartoVoyagerTiles = object : XYTileSource(
    "CartoVoyager",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$z/$x/$y.png"
    }
}

// ─── Map Theme Application ─────────────────────────────────────────────────────
fun applyThemeToMapView(mapView: MapView, isDark: Boolean) {
    val target = if (isDark) CartoDarkTiles else CartoVoyagerTiles
    if (mapView.tileProvider.tileSource?.name() != target.name()) {
        mapView.setTileSource(target)
    }
    mapView.setBackgroundColor(
        if (isDark) AndroidColor.parseColor("#111318") else AndroidColor.parseColor("#F4F6F9")
    )
    mapView.invalidate()
}

// ─── Black Spot Marker Icon ────────────────────────────────────────────────────
private fun createBlackSpotMarkerIcon(context: Context, isSevere: Boolean): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = if (isSevere) AndroidColor.parseColor("#4DE53935") else AndroidColor.parseColor("#4DFFB300")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    paint.color = if (isSevere) AndroidColor.parseColor("#E53935") else AndroidColor.parseColor("#FFB300")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.7f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * density
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.7f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

// ─── Black Spot Overlay Manager ────────────────────────────────────────────────
fun updateBlackSpotMarkers(
    mapView: MapView,
    blackSpots: List<BlackSpot>,
    show: Boolean,
    onBlackSpotClicked: (BlackSpot) -> Unit
) {
    val existingCount = mapView.overlays.count { (it is Marker) && (it.relatedObject is BlackSpot) }
    if (!show) {
        if (existingCount > 0) {
            mapView.overlays.removeAll { (it is Marker) && (it.relatedObject is BlackSpot) }
            mapView.invalidate()
        }
        return
    }
    if (existingCount == blackSpots.size) return

    mapView.overlays.removeAll { (it is Marker) && (it.relatedObject is BlackSpot) }
    val context = mapView.context
    val severeIcon = createBlackSpotMarkerIcon(context, isSevere = true)
    val moderateIcon = createBlackSpotMarkerIcon(context, isSevere = false)

    for (spot in blackSpots) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(spot.latitude, spot.longitude)
            title = spot.roadName
            snippet = "${spot.locationName} (${spot.totalAccidents} crashes)"
            icon = if (spot.severityLevel == 1) severeIcon else moderateIcon
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            relatedObject = spot
            setOnMarkerClickListener { _, _ ->
                onBlackSpotClicked(spot)
                true
            }
        }
        mapView.overlays.add(marker)
    }
    mapView.invalidate()
}
