package com.aikuaibao.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/*
 * 详情页 Preview —— 与列表页共用 ListScreenPreview.kt 里的样本数据。
 * 三种真实形态各一个，用来验证详情页与列表页的视觉是否一致。
 */

@Preview(name = "详情 · HN（无图无摘要）", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreviewDetailHn() {
    // 验证点：summary 与 title 相同时不重复显示，而是给"未提供摘要"提示
    MaterialTheme { DetailScreen(sampleHn) {} }
}

@Preview(name = "详情 · RSS（有摘要有热度）", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreviewDetailRss() {
    MaterialTheme { DetailScreen(sampleVerge) {} }
}

@Preview(name = "详情 · 带封面图", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PreviewDetailWithImage() {
    // Coil 在 Preview 中不加载网络图，封面区会是灰色占位块，真机正常
    MaterialTheme { DetailScreen(sampleTechCrunch) {} }
}
