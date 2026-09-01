#!/usr/bin/env python3
"""
AI快报 · 每日聚合脚本 (fetch_daily.py)
=====================================
从可公开访问的源拉取 AI 高热度内容，去重后产出 articles.json。

数据源（第一版，均有免费公开接口）：
  - Reddit   : r/artificialintelligence, r/singularity, r/LocalLLaMA, r/MachineLearning
  - HackerNews: Algolia Search API
  - RSS      : VentureBeat AI / TechCrunch AI / The Verge AI / The Decoder / Wired AI

设计要点：
  - 纯标准库，无第三方依赖（GitHub Actions 自带 Python 可直接跑）
  - 各源失败隔离，不会因单个源挂掉而整体失败
  - 支持代理：自动读取 http_proxy / https_proxy 环境变量，或用 --proxy 覆盖
  - 去重：归一化 URL 严格去重 + 标题相似度(Jaccard)软去重
  - 输出 articles.json（供 Android App 消费）

用法：
  python3 fetch_daily.py                 # 输出到 ./articles.json
  python3 fetch_daily.py -o out.json    # 指定输出
  http_proxy=http://127.0.0.1:7890 python3 fetch_daily.py   # 走代理
  python3 fetch_daily.py --proxy http://127.0.0.1:7890      # 或显式指定
"""

import argparse
import hashlib
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone, timedelta

UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
TIMEOUT = 20
MAX_PER_SOURCE = 50
TOP_N = 60            # 最终保留条数
PER_SOURCE_CAP = 20   # 每个来源最多保留条数（保证来源多样性）

# ----------------------------------------------------------------------------
# 网络层（带代理 / UA / 超时 / 失败隔离）
# ----------------------------------------------------------------------------

def make_opener(proxy: str | None):
    handlers = []
    p = proxy or os.environ.get("https_proxy") or os.environ.get("http_proxy")
    if p:
        handlers.append(urllib.request.ProxyHandler({"http": p, "https": p}))
    return urllib.request.build_opener(*handlers)


def http_get(opener, url: str, as_json=False, retries=1):
    last_err = None
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                raw = resp.read()
            if as_json:
                return json.loads(raw.decode("utf-8", "replace"))
            return raw.decode("utf-8", "replace")
        except Exception as e:  # noqa: BLE001
            last_err = e
            if attempt < retries:
                time.sleep(1.5)
    print(f"  [warn] 请求失败 {url[:80]} -> {last_err}", file=sys.stderr)
    return None


# ----------------------------------------------------------------------------
# 文本清洗工具
# ----------------------------------------------------------------------------

_TAG_RE = re.compile(r"<[^>]+>")
_WS_RE = re.compile(r"\s+")

def strip_html(s: str | None) -> str:
    if not s:
        return ""
    s = _TAG_RE.sub(" ", s)
    s = html.unescape(s)
    return _WS_RE.sub(" ", s).strip()


def truncate(s: str, n: int = 200) -> str:
    s = s.strip()
    return s if len(s) <= n else s[: n - 1].rstrip() + "…"


def norm_url(u: str | None) -> str:
    """归一化 URL 用于严格去重：去 scheme、小写 host、去尾斜杠、去常见追踪参数。"""
    if not u:
        return ""
    try:
        p = urllib.parse.urlparse(u)
        host = p.netloc.lower().lstrip("www.")
        path = p.path.rstrip("/")
        # 只保留核心查询参数（忽略 utm_* / fbclid 等追踪）
        q = urllib.parse.parse_qs(p.query)
        keep = {k: v for k, v in q.items() if not k.lower().startswith(("utm_", "fbclid"))}
        qstr = urllib.parse.urlencode(keep, doseq=True)
        return f"{host}{path}" + (f"?{qstr}" if qstr else "")
    except Exception:  # noqa: BLE001
        return u.lower().strip()


_STOPWORDS = set("the a an of to in is for on and or with this that it at by from as be are was were".split())
_TOKEN_RE = re.compile(r"[a-z0-9]+")


def title_tokens(t: str) -> set:
    t = t.lower()
    return {w for w in _TOKEN_RE.findall(t) if w not in _STOPWORDS and len(w) > 1}


def jaccard(a: set, b: set) -> float:
    if not a or not b:
        return 0.0
    return len(a & b) / len(a | b)


# ----------------------------------------------------------------------------
# 各源解析
# ----------------------------------------------------------------------------

def fetch_reddit(opener) -> list[dict]:
    subs = ["artificialintelligence", "singularity", "LocalLLaMA", "MachineLearning"]
    out = []
    for sub in subs:
        url = f"https://old.reddit.com/r/{sub}/hot.json?limit={MAX_PER_SOURCE}&raw_json=1"
        data = http_get(opener, url, as_json=True)
        if not data or "data" not in data:
            continue
        for c in data["data"]["children"]:
            d = c.get("data", {})
            title = d.get("title", "").strip()
            if not title:
                continue
            # 图片
            img = d.get("thumbnail")
            if not (img and img.startswith("http")):
                img = None
            prev = d.get("preview", {}).get("images", [])
            if prev and not img:
                src = prev[0].get("source", {}).get("url")
                if src:
                    img = src.replace("&amp;", "&")
            # 视频
            video = None
            media = d.get("media", {})
            if d.get("is_video") and media.get("reddit_video"):
                video = media["reddit_video"].get("fallback_url")
            permalink = d.get("permalink", "")
            out.append({
                "title": title,
                "summary": truncate(strip_html(d.get("selftext", "")), 200) or title,
                "source": "reddit",
                "source_name": f"r/{sub}",
                "url": "https://www.reddit.com" + permalink if permalink else "",
                "image_url": img,
                "video_url": video,
                "author": d.get("author"),
                "published_at": _ts(d.get("created_utc")),
                "score": int(d.get("score") or 0),
                "comments": int(d.get("num_comments") or 0),
                "tags": ["reddit"],
            })
    return out


def fetch_hn(opener) -> list[dict]:
    queries = ["AI", "GPT", "LLM", "machine learning", "OpenAI", "Anthropic", "neural network"]
    seen = set()
    out = []
    cutoff = int(time.time()) - 14 * 86400  # 近 14 天窗口，保证拉到的是"近期"热度
    for q in queries:
        url = ("https://hn.algolia.com/api/v1/search?tags=story&query=" + urllib.parse.quote(q)
               + f"&numericFilters=created_at_i>{cutoff}&hitsPerPage={MAX_PER_SOURCE}")
        data = http_get(opener, url, as_json=True)
        if not data or "hits" not in data:
            continue
        for h in data["hits"]:
            title = (h.get("title") or h.get("story_title") or "").strip()
            if not title:
                continue
            uid = h.get("objectID")
            if uid in seen:
                continue
            seen.add(uid)
            story_url = h.get("url") or (f"https://news.ycombinator.com/item?id={uid}")
            out.append({
                "title": title,
                "summary": title,
                "source": "hn",
                "source_name": "Hacker News",
                "url": story_url,
                "image_url": None,
                "video_url": None,
                "author": h.get("author"),
                "published_at": _ts(h.get("created_at_i")),
                "score": int(h.get("points") or 0),
                "comments": int(h.get("num_comments") or 0),
                "tags": ["hackernews"],
            })
    return out


RSS_FEEDS = [
    ("VentureBeat AI", "https://venturebeat.com/category/ai/feed/"),
    ("TechCrunch AI", "https://techcrunch.com/category/artificial-intelligence/feed/"),
    ("The Verge AI", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"),
    ("The Decoder", "https://www.thedecoder.com/feed/"),
    ("Wired AI", "https://www.wired.com/feed/tag/ai/latest/rss"),
    ("Ars Technica", "https://arstechnica.com/feed/"),
]

_AI_KW = re.compile(r"\b(ai|a\.i\.|artificial intelligence|gpt|llm|chatgpt|machine learning|deep learning|neural|openai|anthropic|claude|gemini|deepseek|mistral|llama|diffusion|agent|robot|model)\b", re.I)


def _local(tag):
    return tag.split("}")[-1] if "}" in tag else tag


def fetch_rss(opener) -> list[dict]:
    out = []
    for name, feed in RSS_FEEDS:
        xml = http_get(opener, feed)
        if not xml:
            continue
        try:
            root = ET.fromstring(xml)
        except ET.ParseError:
            print(f"  [warn] RSS 解析失败: {feed}", file=sys.stderr)
            continue
        items = []
        for el in root.iter():
            if _local(el.tag) in ("item", "entry"):
                items.append(el)
        for it in items[:MAX_PER_SOURCE]:
            title = _text(it, "title")
            if not title:
                continue
            summary_raw = _text(it, "description") or _text(it, "summary") or _text(it, "content")
            summary = strip_html(summary_raw)
            # 全量源（Ars）做 AI 关键词过滤
            if name == "Ars Technica" and not _AI_KW.search(title + " " + summary):
                continue
            link = _link(it) or _text(it, "id")
            img = _media_image(it)
            video = _media_video(it)
            out.append({
                "title": title,
                "summary": truncate(summary, 220),
                "source": "rss",
                "source_name": name,
                "url": link,
                "image_url": img,
                "video_url": video,
                "author": _text(it, "creator") or _text(it, "author"),
                "published_at": _parse_date(_text(it, "pubDate") or _text(it, "published") or _text(it, "updated")),
                "score": 0,  # RSS 无热度，置 0，排序时靠时间兜底
                "comments": None,
                "tags": ["news"],
            })
    return out


def _text(parent, tag):
    for el in parent.iter():
        if _local(el.tag) == tag and el.text:
            return el.text.strip()
    return ""


def _link(parent):
    # RSS <link> 可能是文本或 Atom 的 href 属性
    for el in parent.iter():
        if _local(el.tag) == "link":
            if el.text and el.text.strip():
                return el.text.strip()
            if el.get("href"):
                return el.get("href")
    return ""


def _media_image(parent):
    for el in parent.iter():
        ln = _local(el.tag)
        if ln == "content" and el.get("medium") == "image" and el.get("url"):
            return el.get("url")
        if ln == "thumbnail" and el.get("url"):
            return el.get("url")
        if ln == "content" and el.get("type", "").startswith("image") and el.get("url"):
            return el.get("url")
    # 兜底：从 description 里抠 <img src>
    desc = _text(parent, "description") or _text(parent, "summary")
    m = re.search(r'<img[^>]+src=["\']([^"\']+)', desc or "")
    if m:
        return m.group(1)
    return None


def _media_video(parent):
    for el in parent.iter():
        if _local(el.tag) == "content" and el.get("medium") == "video" and el.get("url"):
            return el.get("url")
        if _local(el.tag) == "enclosure" and (el.get("type") or "").startswith("video") and el.get("url"):
            return el.get("url")
    return None


def _ts(epoch) -> str | None:
    try:
        if isinstance(epoch, (int, float)):
            return datetime.fromtimestamp(epoch, tz=timezone.utc).isoformat()
        return None
    except Exception:  # noqa: BLE001
        return None


def _parse_date(s: str | None) -> str | None:
    if not s:
        return None
    for fmt in ("%a, %d %b %Y %H:%M:%S %z", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%dT%H:%M:%SZ"):
        try:
            dt = datetime.strptime(s.strip(), fmt)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)
            return dt.isoformat()
        except ValueError:
            continue
    return None


# ----------------------------------------------------------------------------
# 去重 + 排序
# ----------------------------------------------------------------------------

def dedupe(items: list[dict]) -> list[dict]:
    by_url: dict[str, dict] = {}
    ordered: list[dict] = []
    for it in items:
        nu = norm_url(it.get("url"))
        tok = title_tokens(it.get("title", ""))
        # 1) 严格 URL 去重
        if nu and nu in by_url:
            _merge(by_url[nu], it)
            continue
        # 2) 标题相似度软去重
        dup = False
        for seen in ordered:
            if tok and jaccard(tok, seen["_tok"]) >= 0.85:
                _merge(seen, it)
                dup = True
                break
        if dup:
            continue
        it["_tok"] = tok
        if nu:
            by_url[nu] = it
        ordered.append(it)
    for it in ordered:
        it.pop("_tok", None)
    return ordered


def _merge(keep: dict, other: dict):
    """保留热度/评论更高者，补齐缺失字段。"""
    keep["score"] = max(keep.get("score") or 0, other.get("score") or 0)
    keep["comments"] = max(keep.get("comments") or 0, other.get("comments") or 0)
    for k in ("image_url", "video_url", "summary", "author", "published_at"):
        if not keep.get(k) and other.get(k):
            keep[k] = other[k]


def finalize(items: list[dict]) -> list[dict]:
    # 按来源保底，保证列表里 reddit / hn / rss 都有，避免被单方面淹没
    by_src: dict[str, list[dict]] = {}
    for it in items:
        by_src.setdefault(it["source"], []).append(it)
    chosen = []
    for src, lst in by_src.items():
        lst.sort(key=lambda x: (x.get("score") or 0, x.get("published_at") or ""), reverse=True)
        chosen += lst[:PER_SOURCE_CAP]
    chosen.sort(key=lambda x: (x.get("score") or 0, x.get("published_at") or ""), reverse=True)
    chosen = chosen[:TOP_N]
    result = []
    for it in chosen:
        art_id = hashlib.sha1((it.get("url") or it.get("title", "")).encode("utf-8")).hexdigest()[:12]
        result.append({
            "id": art_id,
            "title": it.get("title", "").strip(),
            "summary": it.get("summary", ""),
            "source": it.get("source"),
            "source_name": it.get("source_name"),
            "url": it.get("url"),
            "image_url": it.get("image_url"),
            "video_url": it.get("video_url"),
            "author": it.get("author"),
            "published_at": it.get("published_at"),
            "score": it.get("score") or 0,
            "comments": it.get("comments"),
            "tags": it.get("tags", []),
        })
    return result


# ----------------------------------------------------------------------------
# main
# ----------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-o", "--output", default="articles.json")
    ap.add_argument("--proxy", default=None, help="显式代理地址，覆盖环境变量")
    args = ap.parse_args()

    opener = make_opener(args.proxy)
    proxy_used = args.proxy or os.environ.get("https_proxy") or os.environ.get("http_proxy")
    print(f"[info] 代理: {proxy_used or '直连'}")
    print("[info] 开始拉取…")

    collected = []
    collected += fetch_reddit(opener)
    print(f"  Reddit 拉到 {len(collected)} 条")
    collected += fetch_hn(opener)
    print(f"  HN 拉到 {len(collected)} 条(累计)")
    collected += fetch_rss(opener)
    print(f"  RSS 拉到 {len(collected)} 条(累计)")

    deduped = dedupe(collected)
    print(f"[info] 去重后 {len(deduped)} 条")

    final = finalize(deduped)

    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "count": len(final),
        "sources": {"reddit": sum(1 for x in final if x["source"] == "reddit"),
                    "hn": sum(1 for x in final if x["source"] == "hn"),
                    "rss": sum(1 for x in final if x["source"] == "rss")},
        "articles": final,
    }

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    print(f"[done] 已写入 {args.output}（{len(final)} 条）")


if __name__ == "__main__":
    main()
