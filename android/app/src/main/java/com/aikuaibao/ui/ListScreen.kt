package com.aikuaibao.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aikuaibao.model.Article
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    articles: List<Article>,
    generatedAt: String?,
    isRefreshing: Boolean,
    refreshError: String?,
    onErrorShown: () -> Unit,
    onItemClick: (Article) -> Unit,
    onRefresh: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(refreshError) {
        refreshError?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    val listState = rememberLazyListState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI快报",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            buildString {
                                append("${articles.size} 条")
                                generatedAt?.let { append(" · 更新于 ${formatRelative(it)}") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(articles, key = { _, a -> a.id }) { index, article ->
                    ArticleCard(article, index + 1, onItemClick)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
fun ArticleCard(article: Article, rank: Int, onItemClick: (Article) -> Unit) {
    val accent = sourceAccent(article.sourceName)
    // HN 条目的 summary 与 title 相同，重复展示没有信息量，直接跳过
    val showSummary = article.summary.isNotBlank() &&
        article.summary.trim() != article.title.trim()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(article) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                Box {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    if (!article.videoUrl.isNullOrBlank()) {
                        Surface(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                "▶ 视频",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        article.sourceName,
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (showSummary) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (article.score > 0) {
                        MetaText("🔥 ${article.score}", heatColor(article.score))
                    }
                    article.comments?.takeIf { it > 0 }?.let { MetaText("💬 $it") }
                    article.author?.takeIf { it.isNotBlank() }?.let {
                        MetaText("@${it.take(14)}")
                    }
                    Spacer(Modifier.weight(1f))
                    article.publishedAt?.let { MetaText(formatRelative(it)) }
                }
            }
        }
    }
}

@Composable
private fun MetaText(text: String, color: Color? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** 首次加载的骨架屏，比一个居中转圈更像「内容马上就到」 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI快报",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        SkeletonList(
            Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun SkeletonList(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerBar(0.28f, 12.dp, shimmer)
                    ShimmerBar(0.95f, 18.dp, shimmer)
                    ShimmerBar(0.66f, 18.dp, shimmer)
                    ShimmerBar(0.42f, 12.dp, shimmer)
                }
            }
        }
    }
}

@Composable
private fun ShimmerBar(widthFraction: Float, barHeight: Dp, shimmer: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(barHeight)
            .alpha(shimmer)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}

/** 按来源给一个识别色，列表里扫一眼就知道是哪家的内容 */
private fun sourceAccent(sourceName: String): Color = when {
    sourceName.contains("Hacker", ignoreCase = true) -> Color(0xFFFF6600)
    sourceName.contains("TechCrunch", ignoreCase = true) -> Color(0xFF1DA362)
    sourceName.contains("Verge", ignoreCase = true) -> Color(0xFF7B2FF2)
    sourceName.contains("Ars", ignoreCase = true) -> Color(0xFFFF4E00)
    sourceName.contains("Wired", ignoreCase = true) -> Color(0xFF0088CC)
    sourceName.contains("Reddit", ignoreCase = true) -> Color(0xFFFF4500)
    sourceName.contains("arXiv", ignoreCase = true) -> Color(0xFFB31B1B)
    sourceName.contains("YouTube", ignoreCase = true) -> Color(0xFFCC0000)
    else -> Color(0xFF3D5AFE)
}

private fun heatColor(score: Int): Color = when {
    score >= 500 -> Color(0xFFD32F2F)
    score >= 200 -> Color(0xFFF57C00)
    else -> Color(0xFF9E9E9E)
}

/** 相对时间：刚刚 / x分钟前 / x小时前 / x天前 / M月d日 */
fun formatRelative(iso: String): String = try {
    val t = OffsetDateTime.parse(iso)
    val minutes = Duration.between(t.toInstant(), Instant.now()).toMinutes()
    when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        minutes < 60 * 24 -> "${minutes / 60}小时前"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}天前"
        else -> t.atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("M月d日"))
    }
} catch (e: Exception) {
    iso.take(10)
}

fun formatDate(iso: String): String = iso.take(10)
