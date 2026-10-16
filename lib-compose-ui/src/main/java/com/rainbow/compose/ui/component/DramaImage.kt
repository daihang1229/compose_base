package com.rainbow.compose.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Compose 网络图片入口，统一封装 AsyncImage 的裁剪、占位图和错误图行为。
 */
@Composable
fun DramaImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = androidx.compose.ui.graphics.RectangleShape,
    placeholder: Painter? = null,
    error: Painter? = null,
) {
    AsyncImage(
        model = url?.takeIf { it.isNotEmpty() },
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
    )
}
