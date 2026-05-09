package com.bananaleafnutrientcheck.app.navigation

import com.bananaleafnutrientcheck.app.R

enum class AppDestination(
    val route: String,
    val labelResId: Int,
) {
    Home("home", R.string.destination_home),
    Scan("scan", R.string.destination_scan),
    About("about", R.string.destination_about),
}
