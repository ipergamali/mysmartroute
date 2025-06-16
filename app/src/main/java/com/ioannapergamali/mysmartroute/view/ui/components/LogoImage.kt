package com.ioannapergamali.mysmartroute.view.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LogoImage(
    base64Data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    baseSize: Dp = 40.dp,
    referenceWidth: Dp = 600.dp
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val scale = screenWidth.value / referenceWidth.value
    val targetSize = baseSize * scale

    val painter = remember(base64Data) {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        BitmapPainter(bitmap.asImageBitmap())
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(targetSize),
        contentScale = ContentScale.Fit
    )
}
