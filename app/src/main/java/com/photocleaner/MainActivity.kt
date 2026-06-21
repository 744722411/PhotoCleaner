package com.photocleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.photocleaner.ui.navigation.NavGraph
import com.photocleaner.ui.theme.PhotoCleanerTheme
import com.photocleaner.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled - app will function with whatever was granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestStoragePermissions()

        setContent {
            PhotoCleanerTheme {
                NavGraph()
            }
        }
    }

    private fun requestStoragePermissions() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }
    }
}
