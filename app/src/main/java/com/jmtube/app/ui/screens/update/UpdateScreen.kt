package com.jmtube.app.ui.screens.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jmtube.app.BuildConfig
import com.jmtube.app.R
import com.jmtube.app.data.update.AppRelease
import com.jmtube.app.data.update.UpdateRepository
import com.jmtube.app.ui.components.layout.topbar.FlowTopBar

const val UPDATE_ROUTE = "update"

@Composable
fun UpdateLaunchEffect(
    needsOnboarding: Boolean? = false,
    onOpenUpdate: Boolean = false,
    onNavigateToUpdate: () -> Unit = {},
) {
    LaunchedEffect(needsOnboarding, onOpenUpdate) {
        if (needsOnboarding == false && onOpenUpdate) {
            onNavigateToUpdate()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit = {},
    onClose: () -> Unit = onNavigateBack,
) {
    val handleBack = {
        onClose()
    }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isChecking by remember { mutableStateOf(true) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var isUpToDate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val updates = UpdateRepository(okhttp3.OkHttpClient(), com.jmtube.app.data.local.LocalDataManager(context))
        val result = runCatching { updates.fetch() }.getOrNull()
        if (result != null) {
            release = result
        } else {
            isUpToDate = true
        }
        isChecking = false
    }

    Scaffold(
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.check_for_updates),
                onBack = onNavigateBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "JM Tube " + BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isChecking) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Checking for updates...")
            } else if (release != null) {
                val item = release!!
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "New Update Available: v${item.versionName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (item.releaseNotes.isNotBlank()) {
                            Text(
                                text = item.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { runCatching { uriHandler.openUri(item.downloadUrl) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Update (v${item.versionName})")
                }
            } else {
                Text(
                    text = "JM Tube is up to date!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onNavigateBack) {
                    Text("OK")
                }
            }
        }
    }
}
