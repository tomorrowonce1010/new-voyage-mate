import os
import re
import time
import json
import urllib.parse
from typing import List, Dict

import requests
from bs4 import BeautifulSoup
from markdownify import markdownify as md

from src.utils import RAW_DIR

BASE = "https://zh.wikivoyage.org"
HEADERS = {"User-Agent": "RAG-Study-Bot/0.1 (+https://example.com)"}


def fetch(url: str) -> str:
    resp = requests.get(url, headers=HEADERS, timeout=20)
    resp.raise_for_status()
    return resp.text


def parse_destination_links(category_url: str) -> List[str]:
	html = fetch(category_url)
	soup = BeautifulSoup(html, "lxml")
	links = []
	# Try multiple selectors for different page layouts
	selectors = [
		"#mw-pages a",
		".mw-category-group a", 
		".mw-content-ltr a[href*='/wiki/']",
		"a[href*='/wiki/'][title]"
	]
	for selector in selectors:
		for a in soup.select(selector):
			href = a.get("href", "")
			title = a.get("title", "")
			if (href.startswith("/wiki/") and 
				not href.startswith("/wiki/Category:") and
				not href.startswith("/wiki/File:") and
				not href.startswith("/wiki/Template:") and
				not href.startswith("/wiki/Help:") and
				title and len(title) > 2):
				links.append(BASE + href)
	return list(dict.fromkeys(links))


def clean_markdown(md_text: str) -> str:
    # Remove edit footers and navigation
    text = re.sub(r"\[编辑\]", "", md_text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def page_to_markdown(url: str) -> Dict:
    html = fetch(url)
    soup = BeautifulSoup(html, "lxml")
    # main content
    content = soup.select_one("#mw-content-text")
    if not content:
        return {"url": url, "title": url, "markdown": ""}
    for el in content.select(".toc, .navbox, table.infobox"):  # remove non-article blocks
        el.decompose()
    md_text = md(str(content))
    return {"url": url, "title": soup.title.text if soup.title else url, "markdown": clean_markdown(md_text)}


def save_jsonl(records: List[Dict], path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        for r in records:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")


def scrape_direct_cities(limit: int = 15) -> str:
    """直接抓取知名中国城市页面，绕过分类页面问题"""
    # 扩展的中国城市列表，包含更多城市
    cities = [
        "北京", "上海", "广州", "深圳", "杭州", "南京", "苏州", "成都", "西安", "青岛",
        "厦门", "大连", "武汉", "重庆", "天津", "长沙", "郑州", "济南", "福州", "合肥",
        "石家庄", "太原", "呼和浩特", "沈阳", "长春", "哈尔滨", "南昌", "南宁", "海口", "昆明",
        "贵阳", "拉萨", "兰州", "西宁", "银川", "乌鲁木齐", "台北", "香港", "澳门"
    ]
    
    links = [f"{BASE}/wiki/{city}" for city in cities[:limit]]
    records: List[Dict] = []
    
    print(f"开始直接抓取 {len(links)} 个城市页面...")
    
    for i, url in enumerate(links, 1):
        try:
            record = page_to_markdown(url)
            if record["markdown"].strip():
                records.append(record)
                print(f"[{i}/{len(links)}] ✅ 成功抓取: {record['title']}")
            else:
                print(f"[{i}/{len(links)}] ⚠️  内容为空: {url}")
            time.sleep(1.2)  # 增加延迟避免被封
        except Exception as e:
            print(f"[{i}/{len(links)}] ❌ 抓取失败: {url} - {e}")
    
    fname = "wikivoyage_chinese_cities.jsonl"
    path = os.path.join(RAW_DIR, fname)
    save_jsonl(records, path)
    print(f"✅ 成功保存 {len(records)} 条记录到 {path}")
    return path


def scrape_by_category(category: str, limit: int = 20) -> str:
    """尝试抓取分类，失败则使用直接抓取城市的方式"""
    print(f"尝试抓取分类: {category}")
    
    # 尝试不同的URL格式
    url_formats = [
        f"{BASE}/wiki/{urllib.parse.quote(category)}",
        f"{BASE}/wiki/{category.replace(':', '%3A')}",
        f"{BASE}/wiki/Category:{urllib.parse.quote(category.split(':', 1)[1] if ':' in category else category)}"
    ]
    
    for i, category_url in enumerate(url_formats, 1):
        print(f"尝试URL格式 {i}: {category_url}")
        try:
            # 先测试URL是否可访问
            test_resp = requests.head(category_url, headers=HEADERS, timeout=10)
            if test_resp.status_code == 200:
                print(f"✅ URL可访问，开始解析链接...")
                links = parse_destination_links(category_url)[:limit]
                if links:
                    print(f"找到 {len(links)} 个链接")
                    records: List[Dict] = []
                    for j, url in enumerate(links, 1):
                        try:
                            record = page_to_markdown(url)
                            if record["markdown"].strip():
                                records.append(record)
                                print(f"[{j}/{len(links)}] 抓取: {record['title']}")
                            time.sleep(1.0)
                        except Exception as e:
                            print(f"抓取失败: {url} - {e}")
                    
                    if records:
                        safe_category = re.sub(r'[<>:"/\\|?*]', '_', category)
                        fname = f"wikivoyage_{safe_category.replace(':', '_')}.jsonl"
                        path = os.path.join(RAW_DIR, fname)
                        save_jsonl(records, path)
                        print(f"✅ 分类抓取成功，保存 {len(records)} 条记录到 {path}")
                        return path
                else:
                    print(f"❌ 未找到有效链接")
            else:
                print(f"❌ URL不可访问 (状态码: {test_resp.status_code})")
        except Exception as e:
            print(f"❌ URL测试失败: {e}")
    
    print("所有分类URL都失败，改用直接抓取城市页面...")
    return scrape_direct_cities(limit)


if __name__ == "__main__":
    # Example: Category:中国/城市 collects Chinese cities
    out = scrape_by_category("Category:中国/城市", limit=15)
    print("saved to", out)