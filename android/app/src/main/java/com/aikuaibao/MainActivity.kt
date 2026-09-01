package com.aikuaibao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aikuaibao.model.Article
import com.aikuaibao.ui.DetailScreen
import com.aikuaibao.ui.ListScreen
import com.aikuaibao.ui.LoadingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.state.collectAsState()
                val isRefreshing by vm.isRefreshing.collectAsState()
                val generatedAt by vm.generatedAt.collectAsState()
                val refreshError by vm.refreshError.collectAsState()
                var selected: Article? by remember { mutableStateOf(null) }

                val current = state
                when {
                    selected != null ->
                        DetailScreen(selected!!) { selected = null }

                    current is UiState.Success ->
                        ListScreen(
                            articles = current.articles,
                            generatedAt = generatedAt,
                            isRefreshing = isRefreshing,
                            refreshError = refreshError,
                            onErrorShown = vm::clearRefreshError,
                            onItemClick = { selected = it },
                            onRefresh = vm::refresh
                        )

                    current is UiState.Error ->
                        ErrorView(current.message) { vm.load() }

                    else -> LoadingScreen()
                }
            }
        }
    }
}

@Composable
fun ErrorView(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("加载失败", style = MaterialTheme.typography.titleMedium)
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}
