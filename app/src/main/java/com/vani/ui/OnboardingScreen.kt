package com.vani.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vani.ui.onboarding.*
import com.vani.viewmodel.OnboardingStep
import com.vani.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    
    when (uiState.currentStep) {
        OnboardingStep.WELCOME -> {
            WelcomeScreen(
                onSetupClick = viewModel::nextStep,
                modifier = modifier
            )
        }
        
        OnboardingStep.LANGUAGE_SELECTION -> {
            LanguageSelectionScreen(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageChange = viewModel::updateLanguage,
                onNextClick = viewModel::nextStep,
                onBackClick = viewModel::previousStep,
                progress = uiState.setupProgress,
                modifier = modifier
            )
        }
        
        OnboardingStep.PERMISSIONS -> {
            PermissionsScreen(
                hasPermission = uiState.hasPermission,
                onPermissionGranted = viewModel::onPermissionGranted,
                onPermissionDenied = viewModel::onPermissionDenied,
                onNextClick = {
                    viewModel.completeSetupAndFinish()
                    onOnboardingComplete()
                },
                onBackClick = viewModel::previousStep,
                onRetryClick = { /* Permission launcher will be triggered from within the screen */ },
                errorMessage = uiState.errorMessage,
                progress = uiState.setupProgress,
                modifier = modifier
            )
        }
        
        OnboardingStep.SETUP_COMPLETE -> {
            SetupCompleteScreen(
                isLoading = uiState.isLoading,
                onComplete = onOnboardingComplete,
                modifier = modifier
            )
        }
    }
}