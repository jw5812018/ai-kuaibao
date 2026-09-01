package com.aikuaibao.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.aikuaibao.model.Article
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(article: Article, onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = sourceAccent(article.sourceName)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // HN 条目的 summary 与 title 相同，正文再显示一遍就是重复，直接跳过
    val showSummary = article.summary.isNotBlank() &&
        article.summary.trim() != article.title.trim()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        article.sourceName,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { shareArticle(context, article) }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 封面（无图则整块不渲染，不留空白）
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }

            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Spacer(Modifier.height(12.dp))

                // 元信息行：来源色标 + 时间 + 热度 + 评论 + 作者
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    article.publishedAt?.let {
                        Text(
                            formatRelative(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (article.score > 0) {
                        Text(
                            "🔥 ${article.score}",
                            style = MaterialTheme.typography.labelMedium,
                            color = heatColor(article.score)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    article.comments?.takeIf { it > 0 }?.let {
                        Text(
                            "💬 $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                article.author?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "作者 @$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                // 视频（启用 JS，否则 YouTube embed 播不了）
                if (!article.videoUrl.isNullOrBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                loadUrl(toEmbed(article.videoUrl!!))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (showSummary) {
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp
                    )
                } else {
                    // 只有标题、没有实质摘要（HN 多为此形态），明确告知而非留白
                    Text(
                        "该条目来源未提供摘要，点击下方按钮阅读原文。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (article.tags.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        article.tags.joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { openUrl(context, article.url) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("阅读原文 ↗") }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, article.url)
                        scope.launch { snackbarHostState.showSnackbar("链接已复制") }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("复制链接") }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun shareArticle(context: Context, article: Article) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.url}")
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享到")) }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("url", text))
}

fun toEmbed(url: String): String {
    Regex("youtube\\.com/watch\\?v=([^&]+)").find(url)?.let {
        return "https://www.youtube.com/embed/${it.groupValues[1]}"
    }
    Regex("youtu\\.be/([^?&]+)").find(url)?.let {
        return "https://www.youtube.com/embed/${it.groupValues[1]}"
    }
    return url
}
