package com.vani

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vani.data.TranscriptionRepository
import com.vani.ui.ConversationScreen
import com.vani.ui.OnboardingScreen
import com.vani.viewmodel.MainViewModel
import com.vani.viewmodel.OnboardingViewModel

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    val onboardingViewModel = remember { OnboardingViewModel(context) }
    val mainViewModel = remember { MainViewModel(repository, context) }
    
    // Check if setup is completed
    val startDestination = if (onboardingViewModel.isSetupCompleted()) {
        "conversation"
    } else {
        "onboarding"
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onOnboardingComplete = {
                    navController.navigate("conversation") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("conversation") {
            ConversationScreen(viewModel = mainViewModel)
        }
    }
}