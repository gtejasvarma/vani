package com.vani

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vani.data.TranscriptionRepository
import com.vani.ui.ConversationScreen
import com.vani.ui.SetupScreen
import com.vani.util.PermissionUtils
import com.vani.viewmodel.MainViewModel
import com.vani.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // Handle permission denial - could show a dialog or disable functionality
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions on startup
        if (!PermissionUtils.hasRecordAudioPermission(this)) {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
        
        setContent {
            MaterialTheme {
                Surface {
                    VaniApp()
                }
            }
        }
    }
}

@Composable
fun VaniApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Create dependencies
    val repository = remember { TranscriptionRepository() }
    val setupViewModel = remember { SetupViewModel(context) }
    val mainViewModel = remember { MainViewModel(repository, context) }
    
    // Check if setup is completed
    val startDestination = if (setupViewModel.isSetupCompleted()) {
        "conversation"
    } else {
        "setup"
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("setup") {
            SetupScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate("conversation") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        
        composable("conversation") {
            ConversationScreen(viewModel = mainViewModel)
        }
    }
}