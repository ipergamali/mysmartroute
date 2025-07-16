package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.foundation.layout.BringIntoViewRequester
import androidx.compose.foundation.layout.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Modifier που μετακινεί αυτόματα το στοιχείο εντός ορατής περιοχής
 * όταν λαμβάνει το focus.
 */
fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { state ->
            if (state.isFocused) {
                scope.launch { bringIntoViewRequester.bringIntoView() }
            }
        }
}
