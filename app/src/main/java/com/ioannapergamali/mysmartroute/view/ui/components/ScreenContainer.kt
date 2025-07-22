package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.KeyboardBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import com.ioannapergamali.mysmartroute.view.ui.util.rememberKeyboardBubbleState

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val bubbleState = rememberKeyboardBubbleState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.primary,
        LocalKeyboardBubbleState provides bubbleState
    ) {
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
                        .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                        .imePadding(),
                    content = content
                )
                KeyboardBubble(
                    text = bubbleState.text,
                    visible = imeVisible && bubbleState.activeFieldId != null,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

