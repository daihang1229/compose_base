package com.rainbow.compose.ui.state

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rainbow.compose.core.contract.ErrorType
import com.rainbow.compose.ui.R
import com.rainbow.compose.ui.theme.commonColors

/**
 * 默认错误页面。
 */
@Composable
fun DefaultErrorPage(
    type: ErrorType = ErrorType.UNKNOWN,
    message: String = "",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    retryText: String = stringResource(R.string.str_retry),
    action: @Composable (() -> Unit)? = null
) {
    val (iconRes, subtitle) = when (type) {
        ErrorType.NO_NET -> R.drawable.net_error to stringResource(R.string.str_net_error_try_again)
        ErrorType.SERVER_ERROR ->
            R.drawable.net_error_v2 to stringResource(R.string.str_the_network_seems_unstable)
        ErrorType.UNKNOWN -> R.drawable.net_error to message
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = commonColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(retryText)
            }
        }

        if (action != null) {
            Spacer(Modifier.height(12.dp))
            action()
        }
    }
}
