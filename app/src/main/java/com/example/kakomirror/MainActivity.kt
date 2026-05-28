package com.example.kakomirror

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kakomirror.billing.BillingManager
import com.example.kakomirror.theme.KakoMirrorTheme
import com.example.kakomirror.ui.main.MainScreen
import com.example.kakomirror.ui.main.MainScreenViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
  private val viewModel: MainScreenViewModel by viewModels()
  private lateinit var billingManager: BillingManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    MobileAds.initialize(this) {}

    billingManager = BillingManager(
      context = this,
      onPurchaseStateChanged = { adsRemoved ->
        viewModel.setAdsRemoved(adsRemoved)
      },
      onError = { message ->
        viewModel.showError(message)
      }
    )

    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    enableEdgeToEdge()
    setContent {
      KakoMirrorTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainScreen(
            viewModel = viewModel,
            onRemoveAdsClick = {
              billingManager.launchPurchaseFlow(this)
            }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (::billingManager.isInitialized) {
      billingManager.queryPurchases()
    }
  }

  override fun onDestroy() {
    if (::billingManager.isInitialized) {
      billingManager.onDestroy()
    }
    super.onDestroy()
  }
}

