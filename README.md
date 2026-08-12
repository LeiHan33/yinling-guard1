# 🛡️ 银龄守护 / YinLing Guard

**帮子女守护老人，刷抖音时自动识别并跳过「洗脑、煽动、谣言类」短视频。**

**Help children protect elderly parents — automatically detect and skip harmful short videos on Douyin.**

---

## 📥 下载 / Download

**[⬇️ 下载 APK (v1.0.0)](https://github.com/LeiHan33/yinling-guard1/releases/download/v1.0.0/yinling-guard-v1.0.0-debug.apk)**

---

## 🎨 UI 演示 / UI Preview

**[👉 在线预览 UI 原型](https://leihan33.github.io/yinling-guard1/ui-preview.html)**

---

## ✨ 功能特性 / Features

- 🤖 自动跳过有害内容 — AccessibilityService + ContentMatcher
- 📝 140+ 内置屏蔽词 — 养生骗局/谣言/煽动/标题党
- ✅ 白名单放行 / 📊 拦截记录与统计
- 👨‍👩‍👧 子女密码保护 / 📦 词库导出备份
- 🔒 100% 本地运行，无云端依赖

## 🏗️ 架构 / Architecture

```
android/
├── app/          # UI 层 (Fragment + Navigation + ViewBinding)
├── core/         # 纯逻辑层 (无 Android 依赖)
│   ├── engine/   # ContentMatcher + GuardEngine + VideoTextParser
│   ├── family/   # FamilyManager (密码/词库/黑名单)
│   ├── model/    # 数据模型
│   ├── storage/  # GuardRepository (JSON 文件 I/O)
│   ├── security/ # PasswordHasher (SHA-256)
│   └── ui/       # Presenters (Home/Records/Settings)
└── core/src/test/ # 122 个单元测试 ✅
```

| 技术栈 | 说明 |
|--------|------|
| 语言 | Kotlin |
| 架构 | 多模块 (app + core)，逻辑与 UI 分离 |
| 导航 | Jetpack Navigation Component |
| 测试 | JUnit + MockK + Robolectric |
| 构建 | Gradle 8.5 + AGP 8.2 |

## 🚀 快速开始

```bash
git clone https://github.com/LeiHan33/yinling-guard1.git
cd yinling-guard1/android
./gradlew test        # 运行 122 个测试
./gradlew assembleDebug  # 构建 APK
```

## 📄 License

MIT
