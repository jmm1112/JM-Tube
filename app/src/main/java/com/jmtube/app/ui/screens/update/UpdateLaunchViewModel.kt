package com.jmtube.app.ui.screens.update

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.jmtube.app.BuildConfig
import com.jmtube.app.data.local.LocalDataManager
import com.jmtube.app.data.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class UpdateLaunchViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private var checked = false
        private val _openUpdate = MutableStateFlow(false)
        val openUpdate: StateFlow<Boolean> = _openUpdate.asStateFlow()

        fun check() {
            if (checked || !BuildConfig.UPDATER_ENABLED) return
            checked = true
            viewModelScope.launch {
                val updates = UpdateRepository(OkHttpClient(), LocalDataManager(context))
                val release = updates.fetch()
                if (release != null) {
                    _openUpdate.value = true
                }
            }
        }

        fun consumeOpenUpdate() {
            _openUpdate.value = false
        }
    }

@Composable
fun UpdateLaunchEffect(
    needsOnboarding: Boolean?,
    onOpenUpdate: () -> Unit,
) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    val viewModel: UpdateLaunchViewModel = hiltViewModel(activity)
    val openUpdate by viewModel.openUpdate.collectAsStateWithLifecycle()
    LaunchedEffect(needsOnboarding) {
        if (needsOnboarding == false) viewModel.check()
    }
    LaunchedEffect(openUpdate) {
        if (openUpdate) {
            viewModel.consumeOpenUpdate()
            onOpenUpdate()
        }
    }
}
