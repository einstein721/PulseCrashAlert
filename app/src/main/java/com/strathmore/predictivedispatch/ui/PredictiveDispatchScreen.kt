package com.strathmore.predictivedispatch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.strathmore.predictivedispatch.data.BlackSpot
import com.strathmore.predictivedispatch.data.BlackSpotRepository
import com.strathmore.predictivedispatch.network.DispatchTriageResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

data class TriageOption(val label: String, val code: Int)

@Composable
fun PredictiveDispatchScreen(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    isSubmitting: Boolean,
    triageResult: DispatchTriageResponse?,
    hasPhotoAttached: Boolean = false,
    onCapturePhotoClick: () -> Unit,
    onSubmitReport: (victimCode: Int, causeCode: Int, age: Int, casualtyCount: Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = rememberDispatchColors(isDark)

    val victimOptions = listOf(
        TriageOption("Pedestrian", 4),
        TriageOption("Motorcyclist", 1),
        TriageOption("Passenger", 3),
        TriageOption("Driver", 0)
    )
    val causeOptions = listOf(
        TriageOption("Head-On / Overtaking", 10),
        TriageOption("Speeding / Control Loss", 26),
        TriageOption("Hit & Run", 98),
        TriageOption("Other", 99)
    )

    var selectedVictim by remember { mutableIntStateOf(4) }
    var selectedCause by remember { mutableIntStateOf(26) }
    var isAdult by remember { mutableStateOf(true) }
    var casualtyCount by remember { mutableIntStateOf(1) }
    var isSheetExpanded by remember { mutableStateOf(true) }

    var showBlackSpots by remember { mutableStateOf(false) }
    var selectedBlackSpot by remember { mutableStateOf<BlackSpot?>(null) }
    var mapInstance by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(latitude, longitude) {
        if (!showBlackSpots || selectedBlackSpot == null) {
            mapInstance?.controller?.animateTo(GeoPoint(latitude, longitude))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Theme-Adaptive OSMDroid MapView (Keyless Esri Dark/Light Nairobi Map)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        isHorizontalMapRepetitionEnabled = false
                        isVerticalMapRepetitionEnabled = false
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(latitude, longitude))
                        applyThemeToMapView(this, isDark)
                        mapInstance = this
                        onResume()
                    }
                },
                update = { mapView ->
                    applyThemeToMapView(mapView, isDark)
                    updateBlackSpotMarkers(
                        mapView = mapView,
                        blackSpots = BlackSpotRepository.nairobiBlackSpots,
                        show = showBlackSpots,
                        onBlackSpotClicked = { spot ->
                            selectedBlackSpot = spot
                            mapView.controller.animateTo(GeoPoint(spot.latitude, spot.longitude))
                            mapView.controller.setZoom(16.5)
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            // Centered Marker Pin and Coordinate Information
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceCard.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "Crash Pin",
                        tint = colors.signalCrimson,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceCard.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Nairobi Urban Corridor • NTSA Live Feed",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Top Controls Bar: Floating GNSS Status Pill + Black Spots Button
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceCard.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (accuracyMeters <= 15f) colors.emeraldLock else colors.amberAlert)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS ±${accuracyMeters.toInt()}m",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Black Spot Toggle Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (showBlackSpots) colors.signalCrimson else colors.surfaceCard.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, if (showBlackSpots) colors.signalCrimson else colors.border),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        showBlackSpots = !showBlackSpots
                        if (!showBlackSpots) {
                            selectedBlackSpot = null
                            mapInstance?.controller?.animateTo(GeoPoint(latitude, longitude))
                            mapInstance?.controller?.setZoom(16.0)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Black Spots",
                        tint = if (showBlackSpots) Color.White else colors.signalCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showBlackSpots) "Black Spots (10)" else "Black Spots",
                        color = if (showBlackSpots) Color.White else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Recenter to Current GPS Location Button
        Surface(
            shape = CircleShape,
            color = colors.surfaceCard.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isSheetExpanded) (if (showBlackSpots) 430.dp else 360.dp) else (if (showBlackSpots) 180.dp else 85.dp), end = 16.dp)
                .clip(CircleShape)
                .clickable {
                    selectedBlackSpot = null
                    mapInstance?.controller?.animateTo(GeoPoint(latitude, longitude))
                    mapInstance?.controller?.setZoom(16.0)
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "My Location",
                tint = colors.emeraldLock,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp)
            )
        }

        // 4. Black Spot Carousel (Visible when Black Spots toggle is active)
        AnimatedVisibility(
            visible = showBlackSpots,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isSheetExpanded) 345.dp else 75.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = colors.signalCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NAIROBI ACCIDENT BLACK SPOTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.signalCrimson,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "NTSA Historical Records",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BlackSpotRepository.nairobiBlackSpots) { spot ->
                            val isCritical = spot.severityLevel == 1
                            val badgeColor = if (isCritical) colors.signalCrimson else colors.amberAlert
                            val isSelected = selectedBlackSpot?.id == spot.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) badgeColor.copy(alpha = 0.20f) else colors.background,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) badgeColor else colors.border
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedBlackSpot = spot
                                        mapInstance?.controller?.animateTo(GeoPoint(spot.latitude, spot.longitude))
                                        mapInstance?.controller?.setZoom(16.5)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isCritical) "SEVERITY 1 (CRITICAL)" else "SEVERITY 0 (MODERATE)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = spot.roadName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${spot.totalAccidents} fatal/serious crashes",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Collapsible Bottom Triage Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = colors.surfaceCard,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Drag & Tap Handle (48dp touch target)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isSheetExpanded = !isSheetExpanded }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 12f) isSheetExpanded = false
                                if (dragAmount < -12f) isSheetExpanded = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colors.textSecondary.copy(alpha = 0.45f))
                    )
                }

                AnimatedVisibility(visible = isSheetExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))

                        if (triageResult != null) {
                            val isSevere = triageResult.severity == 1
                            val badgeColor = if (isSevere) colors.signalCrimson else colors.amberAlert
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = badgeColor.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, badgeColor, RoundedCornerShape(18.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (isSevere) "PRIORITY TRAUMA ALERT (SEVERITY 1)"
                                        else "MODERATE INCIDENT LOGGED (SEVERITY 0)",
                                        color = badgeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Incident #${triageResult.incidentId} transmitted to NTSA Command Center.",
                                        color = colors.textPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Status: ${triageResult.dispatchStatus} • Confidence: ${String.format(java.util.Locale.US, "%.1f%%", triageResult.confidence * 100)}",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Primary Victim Involved (VICTIM_CODE)
                        Text(
                            text = "Primary Victim Involved",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(victimOptions) { option ->
                                val active = selectedVictim == option.code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (active) colors.signalCrimson else colors.chipIdle)
                                        .border(
                                            1.dp,
                                            if (active) colors.signalCrimson else colors.border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedVictim = option.code }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (active) Color.White else colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Observed Crash Type (CAUSE CODE)
                        Text(
                            text = "Observed Crash Type",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(causeOptions) { option ->
                                val active = selectedCause == option.code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (active) colors.signalCrimson else colors.chipIdle)
                                        .border(
                                            1.dp,
                                            if (active) colors.signalCrimson else colors.border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedCause = option.code }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (active) Color.White else colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Casualties Stepper + Age Toggle + Photo Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Casualty Stepper (NO.)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.chipIdle)
                                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (casualtyCount > 1) casualtyCount-- },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = colors.textPrimary)
                                }
                                Text(
                                    text = "$casualtyCount Victim${if (casualtyCount > 1) "s" else ""}",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { if (casualtyCount < 10) casualtyCount++ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = colors.textPrimary)
                                }
                            }

                            // Adult (30) vs Juvenile (12) Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.chipIdle)
                                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                    .clickable { isAdult = !isAdult }
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                Text(
                                    text = if (isAdult) "Adult (30)" else "Juvenile (12)",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Scene Photo Button
                            OutlinedButton(
                                onClick = onCapturePhotoClick,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.border),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (hasPhotoAttached) Icons.Rounded.CheckCircle else Icons.Rounded.CameraAlt,
                                    contentDescription = "Photo",
                                    tint = if (hasPhotoAttached) colors.emeraldLock else colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hasPhotoAttached) "Attached" else "Photo",
                                    color = if (hasPhotoAttached) colors.emeraldLock else colors.textPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Emergency CTA Button
                Button(
                    onClick = {
                        onSubmitReport(
                            selectedVictim,
                            selectedCause,
                            if (isAdult) 30 else 12,
                            casualtyCount
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.signalCrimson),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PING NTSA DISPATCH NOW",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }

        // 5. Selected Black Spot Detail Dialog
        if (selectedBlackSpot != null) {
            val spot = selectedBlackSpot!!
            val isCritical = spot.severityLevel == 1
            val badgeColor = if (isCritical) colors.signalCrimson else colors.amberAlert

            AlertDialog(
                onDismissRequest = { selectedBlackSpot = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = colors.surfaceCard,
                title = {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, badgeColor)
                        ) {
                            Text(
                                text = if (isCritical) "SEVERITY 1: CRITICAL BLACK SPOT" else "SEVERITY 0: MODERATE RISK CORRIDOR",
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = spot.roadName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = spot.locationName,
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.background,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "HISTORICAL ACCIDENT DATA (NTSA)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${spot.totalAccidents} Recorded Serious/Fatal Crashes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Primary Cause: ${spot.primaryCause}",
                                    fontSize = 12.sp,
                                    color = badgeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Infrastructure & Environmental Risk:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = spot.riskFactor,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mapInstance?.controller?.animateTo(GeoPoint(spot.latitude, spot.longitude))
                            mapInstance?.controller?.setZoom(16.5)
                            selectedBlackSpot = null
                        }
                    ) {
                        Text("Focus on Map", color = colors.signalCrimson, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedBlackSpot = null }) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PredictiveDispatchScreenPreview() {
    PredictiveDispatchScreen(
        latitude = -1.286389,
        longitude = 36.817223,
        accuracyMeters = 8f,
        isSubmitting = false,
        triageResult = DispatchTriageResponse(
            incidentId = "INC-8921",
            severity = 1,
            confidence = 0.94,
            dispatchStatus = "DISPATCHED_RED_ALERT"
        ),
        hasPhotoAttached = true,
        onCapturePhotoClick = {},
        onSubmitReport = { _, _, _, _ -> }
    )
}
