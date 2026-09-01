package com.aikuaibao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aikuaibao.model.Article
import com.aikuaibao.ui.DetailScreen
import com.aikuaibao.ui.ListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.state.collectAsState()
                var selected: Article? by remember { mutableStateOf(null) }

                when {
                    selected != null ->
                        DetailScreen(selected!!) { selected = null }
                    state is UiState.Loading ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    state is UiState.Error ->
                        ErrorView((state as UiState.Error).message) { vm.load() }
                    state is UiState.Success ->
                        ListScreen(
                            (state as UiState.Success).articles,
                            { selected = it }
                        ) { vm.load() }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(msg)
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}
