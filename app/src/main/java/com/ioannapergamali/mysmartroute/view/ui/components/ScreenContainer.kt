package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
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
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
        SelectionContainer {
            Column(
                modifier = modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .padding(dimensionResource(id = R.dimen.padding_screen)),
                content = content
            )
        }
    }
}

