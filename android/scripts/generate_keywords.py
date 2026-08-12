#!/usr/bin/env python3
"""Generate keywords_default.json with ~140 built-in keywords."""

import json
from datetime import datetime
from pathlib import Path

KEYWORDS = {
    "health_scam": [
        "治百病", "包治", "祖传秘方", "医院不想让你知道", "特效药", "根治", "药到病除",
        "一招见效", "三天见效", "七天见效", "万能", "神药", "偏方", "独家配方",
        "宫廷秘方", "老中医", "隐藏疗法", "不吃药", "零副作用", "100%有效",
        "癌症克星", "糖尿病克星", "高血压克星", "心脏病克星", "肝病克星", "肾病克星",
        "痛风克星", "失眠克星", "减肥神器", "排毒", "清毒", "洗髓", "换血",
        "打通经络", "激活细胞", "修复DNA", "逆龄", "返老还童", "长生", "延寿",
    ],
    "rumor": [
        "内部消息", "央视不敢播", "不转不是中国人", "刚刚发生", "速转", "马上删",
        "删前速看", "内部资料", "机密文件", "绝密", "未公开", "不便公开",
        "官方辟谣", "国家通知", "紧急通知", "红头文件", "中央通知", "最新通知",
        "全网封杀", "已被封杀", "禁止传播", "限内部", "只发一次", "看完删",
        "删了可惜", "转发保平安", "转给家人", "告诉家人", "国人必知", "全民必看",
        "刚刚曝光", "刚刚流出", "独家曝光",
    ],
    "incitement": [
        "崩溃", "亡我中华", "阴谋", "颜色革命", "必看", "国难", "民族危机",
        "亡国", "灭种", "渗透", "间谍", "叛国", "卖国", "汉奸", "带路党",
        "境外势力", "敌对势力", "亡我之心", "分裂国家", "颠覆", "暴动",
        "起义", "革命", "推翻", "政权", "政变", "内战", "战争即将",
        "备战", "屯粮", "逃命", "末日", "灾难降临", "天罚",
    ],
    "clickbait": [
        "震惊", "速看", "马上删", "99%的人不知道", "赶紧看", "太可怕了",
        "不看后悔", "惊呆", "吓傻", "疯了", "炸了", "火了", "爆了",
        "全网疯传", "刷屏", "看哭", "看傻", "惊了", "服了", "绝了",
        "重磅", "突发", "紧急", "警告", "注意", "危险", "可怕",
        "细思极恐", "毛骨悚然", "不寒而栗", "令人发指", "触目惊心",
        "不敢相信", "万万没想到", "竟然", "居然", "真相", "内幕", "黑幕",
    ],
}

def main() -> None:
    entries = []
    idx = 1
    now = datetime.now().strftime("%Y-%m-%dT10:00:00")
    for category, words in KEYWORDS.items():
        for word in words:
            entries.append({
                "id": f"kw_{idx:03d}",
                "word": word,
                "category": category,
                "source": "builtin",
                "blockCount": 0,
                "createdAt": now,
            })
            idx += 1

    store = {
        "version": 1,
        "updatedAt": now,
        "keywords": entries,
    }

    out = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "keywords_default.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(store, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generated {len(entries)} keywords -> {out}")

if __name__ == "__main__":
    main()
