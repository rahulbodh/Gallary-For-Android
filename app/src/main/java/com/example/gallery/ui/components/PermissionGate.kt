package com.example.gallery.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Requests the correct media permissions for the running API level and shows
 * [content] once granted. Shows a simple rationale + request button otherwise.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val state = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(state.allPermissionsGranted) {
        if (state.allPermissionsGranted) onGranted() else onDenied()
    }

    if (state.allPermissionsGranted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Gallery needs access to your photos and videos to show them here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (state.shouldShowRationale) {
                Text(
                    text = "You previously denied this. Please allow access to continue.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Button(
                onClick = { state.launchMultiplePermissionRequest() },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Grant access")
            }
        }
    }
}
