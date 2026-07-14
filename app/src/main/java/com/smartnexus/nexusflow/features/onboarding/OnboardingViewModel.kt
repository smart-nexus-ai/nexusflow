package com.smartnexus.nexusflow.features.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Onboarding ViewModel.
 *
 * Currently stateless — slide state is managed in Compose with rememberPagerState.
 * Future: Persist that onboarding is completed using DataStore so subsequent launches
 * skip directly to Auth.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel()
