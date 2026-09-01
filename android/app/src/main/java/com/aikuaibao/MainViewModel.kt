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

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val resp = Api.service.getArticles()
                _state.value = if (resp.articles.isEmpty())
                    UiState.Error("暂无内容，请稍后刷新")
                else
                    UiState.Success(resp.articles)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }
}
