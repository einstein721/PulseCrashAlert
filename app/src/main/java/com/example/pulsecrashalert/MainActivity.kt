package com.example.pulsecrashalert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.strathmore.predictivedispatch.location.GpsLocationTracker
import com.strathmore.predictivedispatch.network.CrashReportRequest
import com.strathmore.predictivedispatch.network.DispatchTriageResponse
import com.strathmore.predictivedispatch.network.PredictiveDispatchApi
import com.strathmore.predictivedispatch.ui.DarkDispatchColors
import com.strathmore.predictivedispatch.ui.LightDispatchColors
import com.strathmore.predictivedispatch.ui.PredictiveDispatchScreen
import com.strathmore.predictivedispatch.ui.enablePermissiveSsl
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var gpsTracker: GpsLocationTracker
    private val apiService by lazy { PredictiveDispatchApi.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trust all SSL certificates for map tiles in Android development
        enablePermissiveSsl()

        // MUST run before MapView is instantiated
        val osmConfig = Configuration.getInstance()
        osmConfig.load(applicationContext, applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        osmConfig.userAgentValue = "PulseCrashAlert/1.0 (NTSA-Crash-Alert; contact@strathmore.edu)"

        gpsTracker = GpsLocationTracker(applicationContext)
        enableEdgeToEdge()

        setContent {
            val isDark = isSystemInDarkTheme()
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
            }

            val colorScheme = if (isDark) {
                darkColorScheme(
                    background = DarkDispatchColors.background,
                    surface = DarkDispatchColors.surfaceCard
                )
            } else {
                lightColorScheme(
                    background = LightDispatchColors.background,
                    surface = LightDispatchColors.surfaceCard
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
                    PredictiveDispatchApp(gpsTracker, apiService)
                }
            }
        }
    }
}

@Composable
fun PredictiveDispatchApp(
    gpsTracker: GpsLocationTracker,
    apiService: PredictiveDispatchApi
) {
    val coroutineScope = rememberCoroutineScope()

    // Default to Nairobi Central Coordinates until GPS lock
    // resolveNairobiCoordinates() guards against the emulator's California default
    var latitude by remember { mutableDoubleStateOf(-1.286389) }   // Nairobi CBD
    var longitude by remember { mutableDoubleStateOf(36.817223) }  // Nairobi CBD
    var accuracyMeters by remember { mutableFloatStateOf(8f) }
    var isSubmitting by remember { mutableStateOf(false) }
    var triageResult by remember { mutableStateOf<DispatchTriageResponse?>(null) }
    var capturedPhotoBase64 by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun updateCoordinates() {
        coroutineScope.launch {
            val location = gpsTracker.getCurrentCoordinates()
            if (location != null) {
                // Apply Kenya bounds guard: emulator defaults to California (37.4, -122.0)
                val (safeLat, safeLon) = com.strathmore.predictivedispatch.ui.resolveNairobiCoordinates(
                    location.latitude, location.longitude
                )
                latitude = safeLat
                longitude = safeLon
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5f
            }
        }
    }

    // Live continuous GPS + Wi-Fi location updates
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        updateCoordinates()
        val callback = gpsTracker.startLocationUpdates { location ->
            val (safeLat, safeLon) = com.strathmore.predictivedispatch.ui.resolveNairobiCoordinates(
                location.latitude, location.longitude
            )
            latitude = safeLat
            longitude = safeLon
            if (location.hasAccuracy()) {
                accuracyMeters = location.accuracy
            }
        }

        onDispose {
            gpsTracker.stopLocationUpdates(callback)
        }
    }

    // Permission launcher for Location and Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            hasLocationPermission = true
            updateCoordinates()
        }
    }

    // Camera launcher for scene photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            capturedPhotoBase64 = base64
            Toast.makeText(context, "Scene photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    PredictiveDispatchScreen(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        isSubmitting = isSubmitting,
        triageResult = triageResult,
        hasPhotoAttached = capturedPhotoBase64 != null,
        onCapturePhotoClick = {
            val cameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        },
        onSubmitReport = { victimCode, causeCode, age, casualtyCount ->
            coroutineScope.launch {
                isSubmitting = true
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val requestPayload = CrashReportRequest(
                    latitude = latitude,
                    longitude = longitude,
                    gpsAccuracyMeters = accuracyMeters,
                    hour = currentHour,
                    age = age,
                    causeCode = causeCode,
                    victimCode = victimCode,
                    genderCode = if (casualtyCount > 1) 2 else 0, // 0 = Male, 2 = Multiple (clean_data.csv)
                    countyCode = 33, // Nairobi County code in clean_data.csv (County 33)
                    casualtyCount = casualtyCount,
                    imageBase64 = capturedPhotoBase64
                )

                try {
                    val response = apiService.submitCrashReport(requestPayload)
                    triageResult = response
                    Toast.makeText(
                        context,
                        "Incident #${response.incidentId} transmitted to NTSA Command Center",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.e("PredictiveDispatch", "API backend unreachable, running on-device DecisionTreeEngine", e)
                    // On-device ML Decision Tree evaluation (trained from Kenya Accidents Database)
                    val localResponse = com.strathmore.predictivedispatch.ml.DecisionTreeEngine.predictSeverity(requestPayload)
                    triageResult = localResponse
                    Toast.makeText(
                        context,
                        "On-device Decision Tree active: Incident #${localResponse.incidentId} evaluated.",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isSubmitting = false
                }
            }
        }
    )
}
