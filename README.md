# AI快报 (AI Daily)

每天从公开可访问的源（Hacker News、RSS 科技媒体）聚合 AI 高热度内容，去重后生成 `articles.json`，由原生 Android (Jetpack Compose) App 展示。

## 架构
- `aggregator/fetch_daily.py`：纯标准库聚合脚本，输出 `aggregator/articles.json`
- `android/`：Jetpack Compose App（列表 → 详情，支持图片 / 外链 / 视频降级）
- GitHub Actions（`.github/workflows/daily.yml`）：每天 UTC 02:00 运行聚合，把 `articles.json` 发布到 GitHub Pages

## 云端数据地址
```
https://jw5812018.github.io/ai-kuaibao/articles.json
```
该地址由 GitHub Actions 每日自动更新；仓库为私有，仅 `articles.json` 通过 Pages 公开，聚合源码保持私有。

## 本地运行聚合
```bash
cd aggregator
python3 fetch_daily.py
# 可选：python3 -m http.server 8000  供本地 App 联调
```

## 本地联调 App
1. 在聚合目录起服务：`cd aggregator && python3 -m http.server 8000`
2. 把 `app/src/main/java/com/aikuaibao/network/ApiService.kt` 的 `BASE_URL` 改回 `http://10.0.2.2:8000/`
3. 用本机 Gradle 8.14.5 构建并安装到模拟器：
   ```bash
   cd android && /path/to/gradle-8.14.5/bin/gradle :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## 数据源说明
Reddit 对数据中心 IP 限流（403），目前由 HN + RSS 提供内容；Reddit 需后续接入 OAuth / 住宅代理。

## 已知待办
- [ ] Reddit OAuth / 住宅代理接入
- [ ] 生成 gradle-wrapper.jar 让 `./gradlew` 直接可用
- [ ] App 卡片样式优化 / 下拉刷新动画
