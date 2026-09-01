package com.aikuaibao.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aikuaibao.model.Article
import java.time.OffsetDateTime

/*
 * Compose Preview —— 不用启动模拟器就能在 Android Studio 里看卡片渲染效果。
 * 用法：在 AS 打开本文件，点右上角 "Split" / "Design" 视图即可。
 *
 * 注意：Coil 的 AsyncImage 在 Preview 中不加载网络图片（会留白），
 * 所以带图卡片的封面区在预览里是空的，真机上正常。
 * 实际数据里 HN 条目（占一半）本身就没有图，无图形态才是主要形态。
 */

internal fun hoursAgo(h: Long): String =
    OffsetDateTime.now().minusHours(h).toString()

internal val sampleHn = Article(
    id = "p1",
    title = "Don't paste the AI, please",
    summary = "Don't paste the AI, please",   // 与标题相同 → 卡片会自动隐藏摘要
    source = "hn",
    sourceName = "Hacker News",
    url = "https://dontpastetheai.com/",
    author = "pjerem",
    publishedAt = hoursAgo(5),
    score = 1060,
    comments = 581,
    tags = listOf("hackernews")
)

internal val sampleTechCrunch = Article(
    id = "p2",
    title = "Apple shares 'shocking evidence' against former employee accused of stealing company data for OpenAI",
    summary = "Apple says it has evidence that a former employee destroyed evidence of data theft after learning he was under investigation.",
    source = "rss",
    sourceName = "TechCrunch AI",
    url = "https://techcrunch.com/",
    imageUrl = "https://techcrunch.com/wp-content/uploads/2026/08/sample.jpg",
    author = "Amanda Silberling",
    publishedAt = hoursAgo(2),
    score = 0,
    comments = null,
    tags = listOf("news")
)

internal val sampleVerge = Article(
    id = "p3",
    title = "谷歌发布新一代推理模型，声称在数学基准上超越前代 40%",
    summary = "该模型采用了新的训练策略，在 AIME 与 GPQA 上均有明显提升，但推理成本同步上升。",
    source = "rss",
    sourceName = "The Verge AI",
    url = "https://theverge.com/",
    author = "Nilay Patel",
    publishedAt = hoursAgo(26),
    score = 240,
    comments = 88,
    tags = listOf("news")
)

internal val sampleWired = Article(
    id = "p4",
    title = "You Know Who Really Hates AI? Business",
    summary = "Enterprises are quietly discovering the gap between AI demos and AI deployments.",
    source = "rss",
    sourceName = "Wired AI",
    url = "https://wired.com/",
    author = null,
    publishedAt = hoursAgo(72),
    score = 95,
    comments = null,
    tags = listOf("news")
)

@Preview(name = "卡片 · HN 高热度（无图）", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardHn() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(12.dp)) {
                ArticleCard(sampleHn, rank = 1, onItemClick = {})
            }
        }
    }
}

@Preview(name = "卡片 · RSS 带摘要", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardRss() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(12.dp)) {
                ArticleCard(sampleVerge, rank = 7, onItemClick = {})
            }
        }
    }
}

@Preview(name = "卡片 · 各来源色标对比", showBackground = true, widthDp = 400, heightDp = 760)
@Composable
private fun PreviewCardVariants() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArticleCard(sampleHn, 1, {})            // 橙 · 🔥1060 红
                ArticleCard(sampleVerge, 2, {})         // 紫 · 🔥240 橙
                ArticleCard(sampleWired, 3, {})         // 蓝 · 🔥95 灰
                ArticleCard(sampleTechCrunch, 4, {})    // 绿 · 无热度不显示
            }
        }
    }
}

@Preview(name = "列表页（含顶栏）", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreviewListScreen() {
    MaterialTheme {
        ListScreen(
            articles = listOf(sampleHn, sampleVerge, sampleWired, sampleTechCrunch),
            generatedAt = hoursAgo(1),
            isRefreshing = false,
            refreshError = null,
            onErrorShown = {},
            onItemClick = {},
            onRefresh = {}
        )
    }
}

@Preview(name = "首屏骨架屏", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreviewLoading() {
    MaterialTheme { LoadingScreen() }
}
