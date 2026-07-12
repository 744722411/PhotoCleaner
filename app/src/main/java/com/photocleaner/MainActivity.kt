package com.photocleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                var mediaAccessLevel by remember {
                    mutableStateOf(PermissionHelper.getMediaAccessLevel(this@MainActivity))
                }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            mediaAccessLevel = PermissionHelper.getMediaAccessLevel(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    mediaAccessLevel = PermissionHelper.getMediaAccessLevel(this@MainActivity)
                }

                LaunchedEffect(Unit) {
                    if (!PermissionHelper.hasStoragePermission(this@MainActivity)) {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                }

                NavGraph(
                    snackbarHostState = snackbarHostState,
                    mediaAccessLevel = mediaAccessLevel,
                    onMissingPermission = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                )
            }
        }
    }
}
