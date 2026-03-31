package com.dotmatrix.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.dotmatrix.app.ui.navigation.AppNavigation
import com.dotmatrix.app.ui.theme.DotMatrixAppTheme
import com.dotmatrix.app.viewmodel.SettingsViewModel
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) {
                sharedViewModel?.retryAutoConnect()
            }
        }

    private var sharedViewModel: SharedConnectionViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // 1. Enable Edge-to-Edge to handle status bar correctly
        enableEdgeToEdge()
        
        super.onCreate(savedInstanceState)

        val sharedViewModel   = ViewModelProvider(this)[SharedConnectionViewModel::class.java]
        this.sharedViewModel = sharedViewModel
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        checkPermissions()

        setContent {
            val themeMode  by settingsViewModel.themeMode.collectAsState()

            DotMatrixAppTheme(
                themeMode  = themeMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(sharedViewModel, settingsViewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
