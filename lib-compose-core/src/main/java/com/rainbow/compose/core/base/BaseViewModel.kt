package com.rainbow.compose.core.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 所有 ViewModel 的基类。
 */
open class BaseViewModel : ViewModel() {

    /**
     * 是否 loading
     */
    val isLoadingLD by lazy { MutableLiveData<Boolean>() }

    fun launchUI(responseBlock: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main, block = responseBlock)
    }

    fun launchWith(
        coroutine: CoroutineDispatcher = Dispatchers.Main,
        responseBlock: suspend () -> Unit
    ) {
        viewModelScope.launch(coroutine) {
            responseBlock()
        }
    }
}
