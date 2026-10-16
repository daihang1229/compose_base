package com.rainbow.compose.ui.keyboard

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/** Compose 键盘控制器，不持有 Activity、View 或 InputMethodManager。 */
class DramaKeyboardController internal constructor(
    private val focusManager: FocusManager,
    private val keyboardController: SoftwareKeyboardController?,
) {
    fun hide(clearFocus: Boolean = true) {
        if (clearFocus) focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun show(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun rememberDramaKeyboardController(): DramaKeyboardController {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        DramaKeyboardController(focusManager, keyboardController)
    }
}

@Composable
fun rememberDramaKeyboardVisible(): State<Boolean> {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    return remember(density, imeInsets) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
}

fun Modifier.dramaImePadding(): Modifier = imePadding()
