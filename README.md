# AI快报 (AI Daily)

每天从公开可访问的源（Hacker News、RSS 科技媒体）聚合 AI 高热度内容，去重后生成 `articles.json`，由原生 Android (Jetpack Compose) App 展示。

## 架构
- `aggregator/fetch_daily.py`：纯标准库聚合脚本，输出 `aggregator/articles.json`
- `android/`：Jetpack Compose App（列表 → 详情，支持图片 / 外链 / 视频降级）
- GitHub Actions（`.github/workflows/daily.yml`）：每天 UTC 02:00 运行聚合，把更新后的 `articles.json` 提交回 `main` 分支

## 云端数据地址
```
https://raw.githubusercontent.com/jw5812018/ai-kuaibao/main/aggregator/articles.json
```
该地址由 GitHub Actions 每日自动更新（仓库为公开仓库，App 通过 raw 直取，无需 GitHub Pages）。
> 注意：`.github/workflows/daily.yml` 需在本地 MacBook 上推送（沙箱代理会拦截含该路径的 API 请求）。推送后 Actions 会自动把聚合结果提交回 main。

## 本地运行聚合
```bash
cd aggregator
python3 fetch_daily.py
# 可选：python3 -m http.server 8000  供本地 App 联调
```

## 本地联调 App
App 默认从云端拉取：`https://raw.githubusercontent.com/jw5812018/ai-kuaibao/main/aggregator/articles.json`。
如需本机调试（模拟器直连宿主机），把 `app/src/main/java/com/aikuaibao/network/ApiService.kt` 的 `BASE_URL` 临时改回 `http://10.0.2.2:8000/`，并在聚合目录起服务：`cd aggregator && python3 -m http.server 8000`。

构建并安装到模拟器（仓库已带 gradle wrapper）：
```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 数据源说明
Reddit 对数据中心 IP 限流（403），目前由 HN + RSS 提供内容；Reddit 需后续接入 OAuth / 住宅代理。

## App 特性
- 列表页下拉刷新（Material3 `PullToRefreshBox`），刷新时保留列表不闪屏；刷新失败仅轻提示，不覆盖内容
- 首次加载骨架屏（呼吸式 shimmer），优于居中转圈
- 卡片文字优先设计：来源色标 + 序号、相对时间（刚刚 / x小时前 / x天前）、🔥 热度按分数变色、HN 条目自动隐藏与标题重复的 summary
- 详情页与列表页视觉统一：来源色标顶栏、元信息行（时间 / 热度 / 评论 / 作者）、16:9 封面、标签行、阅读原文 + 复制链接 + 系统分享
- 详情页 WebView 已启用 JavaScript（否则 YouTube 嵌入无法播放），支持 `youtube.com/watch` 与 `youtu.be` 两种链接转 embed
- 无图 / 无视频 / 无摘要均优雅降级（不留空白块，无摘要时给明确提示）

## 在 Android Studio 里预览（不必启动模拟器）
项目内置 Compose `@Preview`，适合内存紧张的机器调视觉：
1. Android Studio → Open → 选择 `android/` 目录，等待 Gradle Sync
2. 打开 `ui/ListScreenPreview.kt` 或 `ui/DetailScreenPreview.kt`
3. 点编辑器右上角 `Split` / `Design`，右侧实时渲染

覆盖形态：HN 无图高热度卡、RSS 带摘要卡、四来源色标对比、完整列表页、骨架屏、详情页三态。
> 注意：Coil 的 `AsyncImage` 在 Preview 中不加载网络图，封面区显示占位灰块，真机正常。

## 聚合脚本
- Reddit / Hacker News / RSS（TechCrunch、Verge、Wired、Ars、The Decoder、VentureBeat）
- RSS 封面提取优先级：`<media:content>` / `<media:thumbnail>` → HTML 正文 `<img>` → 文章页 `og:image` 兜底
- 失败隔离：单个源挂掉不影响整体

## 已知待办
- [ ] Reddit OAuth / 住宅代理接入
- [x] 生成 gradle-wrapper.jar 让 `./gradlew` 直接可用
- [x] App 卡片样式优化 / 下拉刷新动画
- [x] RSS 封面图提取（含 og:image 兜底）
