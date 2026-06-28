package com.photocleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.photocleaner.ui.navigation.NavGraph
import com.photocleaner.ui.theme.PhotoCleanerTheme
import com.photocleaner.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PhotoCleanerTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Triggered after the user interacts with the permission dialog.
                }

                LaunchedEffect(Unit) {
                    if (!PermissionHelper.hasStoragePermission(this@MainActivity)) {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                }

                NavGraph(
                    snackbarHostState = snackbarHostState,
                    onMissingPermission = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                )
            }
        }
    }
}
