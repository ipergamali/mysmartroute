package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.ioannapergamali.mysmartroute.view.ui.util.rememberAdaptiveLogoSize

@Composable
fun LogoImage(
    @DrawableRes drawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val targetSize = rememberAdaptiveLogoSize()

    Image(
        painter = painterResource(drawableRes),
        contentDescription = contentDescription,
        modifier = modifier.size(targetSize),
        contentScale = ContentScale.Fit
    )
}
