package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun LogoImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
