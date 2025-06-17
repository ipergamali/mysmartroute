package com.ioannapergamali.mysmartroute.view.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionsMapScreen(
    navController: NavController,
    start: String,
    end: String,
    openDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Directions",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    val encodedStart = URLEncoder.encode(start, StandardCharsets.UTF_8.toString())
                    val encodedEnd = URLEncoder.encode(end, StandardCharsets.UTF_8.toString())
                    loadUrl("https://www.google.com/maps/dir/$encodedStart/$encodedEnd")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .border(2.dp, MaterialTheme.colorScheme.primary)
        )
    }
}
