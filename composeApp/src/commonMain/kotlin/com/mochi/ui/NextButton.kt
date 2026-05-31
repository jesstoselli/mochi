package com.mochi.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Simple "advance" button built on the reusable springy [BouncyButton]. */
@Composable
fun NextButton(
    onClick: () -> Unit,
    text: String = "Next",
    modifier: Modifier = Modifier,
) {
    BouncyButton(onClick = onClick, modifier = modifier) {
        Text(text)
    }
}
