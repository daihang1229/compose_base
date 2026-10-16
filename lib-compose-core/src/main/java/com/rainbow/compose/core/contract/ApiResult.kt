package com.rainbow.compose.core.contract

/**
 * 网络请求结果的通用包装，用于 [UiStateExtKt.toUiStateFlow] 映射到 [UiState]。
 *
 * 业务层（Repository）可返回该类型，也可通过 Mapper 将各自网络库的结果转换成它，
 * 从而与具体网络实现解耦。
 */
sealed interface ApiResult<out T> {
    data object Loading : ApiResult<Nothing>
    data class Success<T>(val data: T?) : ApiResult<T>
    data class Error(val isServerError: Boolean, val message: String) : ApiResult<Nothing>
}
