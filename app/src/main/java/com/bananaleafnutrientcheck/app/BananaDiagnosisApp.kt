package com.bananaleafnutrientcheck.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bananaleafnutrientcheck.app.navigation.BananaNavHost
import com.bananaleafnutrientcheck.app.ui.theme.BananaLeafNutrientTheme

@Composable
fun BananaDiagnosisApp() {
    BananaLeafNutrientTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            BananaNavHost()
        }
    }
}
