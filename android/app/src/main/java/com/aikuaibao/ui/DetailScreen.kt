package com.aikuaibao.ui

import android.content.Intent
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.aikuaibao.model.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(article: Article, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(article.title, style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(article.sourceName) })
                article.publishedAt?.let {
                    Text(it.take(10), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!article.videoUrl.isNullOrBlank()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply { loadUrl(toEmbed(article.videoUrl!!)) }
                    },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
            if (article.summary.isNotBlank()) {
                Text(article.summary, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(article.url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("打开原文链接 ↗") }
        }
    }
}

fun toEmbed(url: String): String {
    val yt = Regex("youtube\\.com/watch\\?v=([^&]+)").find(url)
    if (yt != null) return "https://www.youtube.com/embed/${yt.groupValues[1]}"
    return url
}
