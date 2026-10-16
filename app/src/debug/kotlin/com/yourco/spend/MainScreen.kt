package com.yourco.spend

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import com.yourco.spend.core.designsystem.ComponentGalleryScreen
import com.yourco.spend.navigation.AppRoot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showGallery by rememberSaveable { mutableStateOf(false) }

    if (showGallery) {
        BackHandler { showGallery = false }
        ComponentGalleryScreen()
    } else {
        AppRoot(onNavigateToSettings = { showGallery = true })
    }
}
