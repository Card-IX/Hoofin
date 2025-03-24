package com.example.gettahoofin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gettahoofin.ui.theme.GettaHoofinTheme
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream
// Add this import for splash screen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Security manager
    private lateinit var securityManager: DeviceSecurityManager

    // App preferences
    private lateinit var appPreferences: AppPreferences

    // Sound player
    private lateinit var soundPlayer: SoundPlayer

    // App settings
    private lateinit var appSettings: AppSettings

    // Resource manager
    private val resourceManager = ResourceManager()

    // Gson instance for JSON parsing
    private val gson = Gson()

    // Program cache for faster loading
    private val programCache = mutableMapOf<String, Program>()

    /// ViewModel for workout with factory for dependencies
    private val workoutViewModel: WorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
                    return WorkoutViewModel(soundPlayer, appSettings, appPreferences) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // Development mode flag - set to true during development to bypass security check
    // IMPORTANT: Set to false before releasing the app
    private val developmentMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize logger - disable for release builds
        AppLogger.setDebugMode(true)  // Set to false for release builds

        // Install splash screen before anything else
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize app preferences
        appPreferences = AppPreferences(this)

        // Initialize app settings
        appSettings = AppSettings(this)

        // Initialize sound player
        soundPlayer = SoundPlayer(this, appSettings)
        resourceManager.register(soundPlayer)

        // Initialize the security manager and check if device is authorized
        securityManager = DeviceSecurityManager(this)

        // For development purposes, reset the device authorization to allow the current device
        if (developmentMode) {
            securityManager.resetDeviceAuthorization()
        }

        // Only check authorization if not in development mode
        if (!developmentMode && !securityManager.isAuthorizedDevice()) {
            showUnauthorizedDeviceDialog()
            return
        }

        // Preload program data in background
        preloadProgramData()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            // Create a program data provider function with caching
            val programDataProvider = remember {
                { programName: String ->
                    // Check cache first
                    programCache[programName] ?: run {
                        // Map program name to file name
                        val fileName = when (programName) {
                            "Start Hoofin': Couch to Walker" -> "start_hoofin.json"
                            "Progress Hoofin': Walker to Jogger" -> "progress_hoofin.json"
                            "Lively Hoofin': Jogger to Runner" -> "lively_hoofin.json"
                            "Advanced Hoofin': Runner to Racer" -> "advanced_hoofin.json"
                            else -> null
                        }

                        // If file name is valid, load it
                        if (fileName != null) {
                            // Load synchronously but can be optimized further if needed
                            val jsonString = getJsonDataFromAsset(this@MainActivity, fileName)
                            if (jsonString != null) {
                                val program = gson.fromJson(jsonString, Program::class.java)
                                // Store in cache for future use
                                programCache[programName] = program
                                program
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }

            // Preload all programs in background for faster navigation
            LaunchedEffect(Unit) {
                val programNames = listOf(
                    "Start Hoofin': Couch to Walker",
                    "Progress Hoofin': Walker to Jogger",
                    "Lively Hoofin': Jogger to Runner",
                    "Advanced Hoofin': Runner to Racer"
                )

                programNames.forEach { name ->
                    programDataProvider(name)  // This will populate the cache
                }
            }

            GettaHoofinTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Use the navigation component
                    AppNavigation(
                        viewModel = workoutViewModel,
                        programDataProvider = programDataProvider,
                        appPreferences = appPreferences,
                        appSettings = appSettings
                    )

                    // Snackbar host
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        snackbar = { data ->
                            Snackbar(
                                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium)),
                                content = {
                                    Text(data.visuals.message)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    /**
     * Preload program data in the background to improve app responsiveness
     */
    private fun preloadProgramData() {
        CoroutineScope(Dispatchers.IO).launch {
            val programNames = listOf(
                "start_hoofin.json",
                "progress_hoofin.json",
                "lively_hoofin.json",
                "advanced_hoofin.json"
            )

            try {
                for (fileName in programNames) {
                    val jsonString = getJsonDataFromAsset(this@MainActivity, fileName)
                    if (jsonString != null) {
                        val formattedName = when (fileName) {
                            "start_hoofin.json" -> "Start Hoofin': Couch to Walker"
                            "progress_hoofin.json" -> "Progress Hoofin': Walker to Jogger"
                            "lively_hoofin.json" -> "Lively Hoofin': Jogger to Runner"
                            "advanced_hoofin.json" -> "Advanced Hoofin': Runner to Racer"
                            else -> null
                        }

                        if (formattedName != null) {
                            val program = gson.fromJson(jsonString, Program::class.java)
                            programCache[formattedName] = program
                            AppLogger.d("MainActivity", "Preloaded program: $formattedName")
                        }
                    }
                }
                AppLogger.d("MainActivity", "Program preloading complete")
            } catch (e: Exception) {
                AppLogger.e("MainActivity", "Error preloading programs", e)
            }
        }
    }

    /**
     * Shows an error dialog when unauthorized device is detected
     */
    private fun showUnauthorizedDeviceDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Unauthorized Device")
        builder.setMessage("This application is not authorized to run on this device.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setCancelable(false)
        builder.setPositiveButton("Exit") { _, _ ->
            finishAffinity() // Close the app completely
        }

        // Show dialog on UI thread
        runOnUiThread {
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleHelper.getSupportedLocale()
        val context = LocaleHelper.updateLocale(newBase, locale)
        super.attachBaseContext(context)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up all resources using ResourceManager
        resourceManager.cleanup()
    }

    override fun onPause() {
        super.onPause()

        // Save workout state when app is paused
        workoutViewModel.saveCurrentState()

        // Force immediate preference save
        appPreferences.forceSave()
    }

    override fun onStop() {
        super.onStop()

        // Additional safety to ensure preferences are saved
        appPreferences.forceSave()
    }

    /**
     * Helper function to load JSON data from assets
     */
    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            jsonString = String(buffer, Charsets.UTF_8)
        } catch (ioException: IOException) {
            AppLogger.e("MainActivity", "Error reading JSON file: $fileName", ioException)
            return null
        }
        return jsonString
    }
}