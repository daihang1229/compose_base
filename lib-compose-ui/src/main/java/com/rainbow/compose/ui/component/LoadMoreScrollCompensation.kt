package com.rainbow.compose.ui.component

/** 单次底部回弹中，将逐帧释放的位移交给列表消费；实例不跨展示会话复用。 */
internal class LoadMoreScrollCompensation(initialOffsetPx: Float) {
    private var previousOffsetPx = initialOffsetPx.coerceAtLeast(0f)

    var consumedOffsetPx: Float = 0f
        private set

    fun consume(offsetPx: Float, consumeOffset: (Float) -> Float): Float {
        val currentOffsetPx = offsetPx.coerceIn(0f, previousOffsetPx)
        val releasedOffsetPx = previousOffsetPx - currentOffsetPx
        previousOffsetPx = currentOffsetPx
        if (releasedOffsetPx <= 0f) return 0f

        val consumed = consumeOffset(releasedOffsetPx).takeIf { it.isFinite() }
            ?.coerceIn(0f, releasedOffsetPx) ?: 0f
        consumedOffsetPx += consumed
        return consumed
    }
}
