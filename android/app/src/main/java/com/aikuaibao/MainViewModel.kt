package com.aikuaibao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aikuaibao.model.Article
import com.aikuaibao.network.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Success(val articles: List<Article>) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    /** 下拉刷新指示器状态：只控制转圈，不清空列表 */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    /** 数据生成时间（articles.json 的 generated_at） */
    private val _generatedAt = MutableStateFlow<String?>(null)
    val generatedAt: StateFlow<String?> = _generatedAt

    /** 刷新失败但已有旧列表时的轻提示，用 Snackbar 展示，不覆盖内容 */
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError

    init { load() }

    /** 首次加载 / 错误页重试：会切到骨架屏 */
    fun load() {
        _state.value = UiState.Loading
        fetch(isRefresh = false)
    }

    /** 下拉刷新：保留当前列表，失败也不清屏 */
    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        fetch(isRefresh = true)
    }

    private fun fetch(isRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val resp = Api.service.getArticles()
                _generatedAt.value = resp.generatedAt.ifBlank { null }
                when {
                    resp.articles.isNotEmpty() -> {
                        _state.value = UiState.Success(resp.articles)
                        _refreshError.value = null
                    }
                    isRefresh -> _refreshError.value = "刷新结果为空，已保留上次内容"
                    else -> _state.value = UiState.Error("暂无内容，请稍后刷新")
                }
            } catch (e: Exception) {
                val msg = e.message ?: "加载失败"
                if (isRefresh && _state.value is UiState.Success) {
                    _refreshError.value = "刷新失败：$msg"
                } else {
                    _state.value = UiState.Error(msg)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearRefreshError() { _refreshError.value = null }
}
