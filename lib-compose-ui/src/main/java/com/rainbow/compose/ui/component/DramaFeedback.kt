package com.rainbow.compose.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 项目统一 Snackbar 外观。 */
@Composable
fun DramaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color.Black.copy(alpha = 0.88f),
                contentColor = Color.White,
                actionColor = Color.White,
                shape = RoundedCornerShape(8.dp),
            )
        },
    )
}

/** 页面级 Scaffold，统一承载 Snackbar。 */
@Composable
fun DramaScaffold(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        snackbarHost = { DramaSnackbarHost(snackbarHostState) },
        content = content,
    )
}

/** 需要旧版居中短提示样式时使用；页面内普通反馈优先使用 Snackbar。 */
@Composable
fun DramaToastHost(
    message: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2_000L,
) {
    if (message.isNullOrEmpty()) return

    LaunchedEffect(message, durationMillis) {
        delay(durationMillis)
        onDismissRequest()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.88f),
            shape = RoundedCornerShape(8.dp),
        ) {
            androidx.compose.material3.Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
