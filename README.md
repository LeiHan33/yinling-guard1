# 🛡️ 银龄守护 · YinLing Guard

<p align="center">
  <strong>帮子女守护老人，刷抖音时自动识别并跳过「洗脑、煽动、谣言类」短视频</strong><br>
  <em>Help children protect elderly parents — auto-detect and skip harmful short videos on Douyin</em>
</p>

<p align="center">
  <a href="https://github.com/LeiHan33/yinling-guard1/releases/tag/v1.0.2"><img src="https://img.shields.io/badge/version-v1.0.2-2E7D32?style=flat-square" alt="version"></a>
  <a href="https://github.com/LeiHan33/yinling-guard1/actions/workflows/build-apk.yml"><img src="https://img.shields.io/badge/build-GitHub%20Actions-1B5E20?style=flat-square" alt="build"></a>
  <a href="https://leihan33.github.io/yinling-guard1/"><img src="https://img.shields.io/badge/UI-Live%20Preview-A5D6A7?style=flat-square&color=212121" alt="ui preview"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="license"></a>
</p>

---

## 📥 下载 · Download

| | |
|---|---|
| **中文** | **[⬇️ 下载 APK v1.0.2](https://github.com/LeiHan33/yinling-guard1/releases/download/v1.0.2/yinling-guard-v1.0.2-debug.apk)** — 修复设置页闪退 |
| **English** | **[⬇️ Download APK v1.0.2](https://github.com/LeiHan33/yinling-guard1/releases/download/v1.0.2/yinling-guard-v1.0.2-debug.apk)** — Fixes Settings crash |

> **自动构建 Auto-build：** 推送 `v*` 标签到 `main` 后，[GitHub Actions](https://github.com/LeiHan33/yinling-guard1/actions/workflows/build-apk.yml) 自动构建并发布 APK。  
> Push a `v*` tag to `main` to trigger an automated APK build and release.

---

## 🎨 UI 演示 · UI Preview

<p align="center">
  <a href="https://leihan33.github.io/yinling-guard1/"><strong>👉 在线预览 · Live Preview</strong></a>
  &nbsp;·&nbsp;
  <a href="https://leihan33.github.io/yinling-guard1/ui-preview.html">备用链接 · Mirror</a>
</p>

12 个界面可交互预览（引导、首页、记录、设置、帮助、子女管理、白名单等），支持 **中 / EN** 语言切换。

Interactive preview of 12 screens (onboarding, home, records, settings, help, family manage, whitelist, etc.) with **CN / EN** toggle.

> 修改 `android/ui-preview.html` 推送到 `main` 后约 1 分钟自动同步到 GitHub Pages。  
> Changes to `android/ui-preview.html` auto-sync to GitHub Pages within ~1 minute.

---

## ✨ 功能特性 · Features

| 中文 | English |
|------|---------|
| 🤖 **自动跳过有害内容** — AccessibilityService + ContentMatcher | 🤖 **Auto-skip harmful content** — AccessibilityService + ContentMatcher |
| 📝 **140+ 内置屏蔽词** — 养生骗局 / 谣言 / 煽动 / 标题党 | 📝 **140+ built-in block words** — health scams, rumors, incitement, clickbait |
| ✅ **白名单放行** · 📊 **拦截记录与统计** | ✅ **Whitelist bypass** · 📊 **Block logs & stats** |
| 👨‍👩‍👧 **子女密码保护** · 📦 **词库导入 / 导出** | 👨‍👩‍👧 **Family password lock** · 📦 **Keyword import / export** |
| 🔒 **100% 本地运行**，无云端依赖 | 🔒 **100% on-device**, no cloud dependency |
| 💬 **跳过浮层提示** · 📖 **使用帮助页** | 💬 **Skip overlay toast** · 📖 **In-app help page** |

---

## 📱 界面一览 · Screens

| 模块 Module | 说明 Description |
|-------------|------------------|
| 引导 Onboarding | 4 步引导 + 权限检测 · 4-step flow + permission auto-detect |
| 首页 Home | 守护状态、统计卡片、最近拦截 · Status, stats, recent blocks |
| 记录 Records | 按日期筛选、分类标签 · Date filters, category tags |
| 设置 Settings | 开关、过滤模式、子女入口 · Toggles, filter mode, family entry |
| 子女管理 Family | 屏蔽词 / 黑名单 / 白名单 / 备份 · Keywords, blacklist, whitelist, backup |
| 帮助 Help | 首次使用与隐私说明 · Getting started & privacy |

---

## 🏗️ 架构 · Architecture

```
android/
├── app/           # UI 层 · UI layer (Fragment + Navigation + ViewBinding)
├── core/          # 纯逻辑层 · Pure logic (no Android deps)
│   ├── engine/    # ContentMatcher + GuardEngine + VideoTextParser
│   ├── family/    # FamilyManager (密码/词库/黑名单 · password / keywords / lists)
│   ├── model/     # 数据模型 · Data models
│   ├── storage/   # GuardRepository (JSON 文件 I/O)
│   ├── security/  # PasswordHasher (SHA-256)
│   └── ui/        # Presenters (Home / Records / Settings)
└── core/src/test/ # 122+ 单元测试 · unit tests ✅
```

| 技术栈 Stack | 说明 Notes |
|--------------|------------|
| 语言 Language | Kotlin |
| 架构 Architecture | 多模块 app + core，逻辑与 UI 分离 · Multi-module, logic/UI split |
| 导航 Navigation | Jetpack Navigation Component |
| 测试 Testing | JUnit + MockK + Robolectric |
| 构建 Build | Gradle 8.5 + AGP 8.2 · CI via GitHub Actions |

---

## 🚀 快速开始 · Quick Start

```bash
git clone https://github.com/LeiHan33/yinling-guard1.git
cd yinling-guard1/android
./gradlew :core:test          # 运行核心测试 · Run core tests
./gradlew assembleDebug       # 构建 APK · Build APK
```

**Windows 用户 Windows users：**

```powershell
powershell -ExecutionPolicy Bypass -File android/scripts/build.ps1 -Task all
```

---

## 👤 Author · 作者

**[LeiHan33](https://github.com/LeiHan33)** — 项目唯一维护者与贡献者 · Sole maintainer and contributor

---

## 📄 License

MIT — 详见 [LICENSE](LICENSE) · See [LICENSE](LICENSE) for details.
