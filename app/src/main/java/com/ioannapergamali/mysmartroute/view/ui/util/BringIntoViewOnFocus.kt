package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// Συμβατική υλοποίηση της bringIntoView όταν η βιβλιοθήκη Compose δεν την παρέχει.
@OptIn(ExperimentalFoundationApi::class)
private suspend fun BringIntoViewRequester.bringIntoView() {
    runCatching {
        val method = BringIntoViewRequester::class.java.getDeclaredMethod("bringIntoView")
        method.isAccessible = true
        method.invoke(this)
    }
}

/**
 * Modifier που μετακινεί αυτόματα το στοιχείο εντός ορατής περιοχής
 * όταν λαμβάνει το focus.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { state: FocusState ->
            if (state.isFocused) {
                scope.launch {
                    bringIntoViewRequester.safeBringIntoView()
                }
            }
        }
}
