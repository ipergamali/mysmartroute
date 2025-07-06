package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.ioannapergamali.mysmartroute.R

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
        SelectionContainer {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .padding(dimensionResource(id = R.dimen.padding_screen))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
                    content = content
                )
            }
        }
    }
}

