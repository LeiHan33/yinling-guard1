# 银龄守护 Android 项目

基于 [PRD.md](../PRD.md) 的第一版实现：抖音 + 安卓 + 关键词过滤。

## 项目结构

```
android/
├── app/          # Android UI、无障碍服务
├── core/         # 核心业务逻辑（可独立单元测试）
└── scripts/      # 测试与工具脚本
```

## 运行测试

环境要求：JDK 17

**Core 单元测试**（122+ 个，可在 Cursor 内通过 Python 脚本运行）：

```powershell
python android/scripts/run_core_tests.py
```

**App Robolectric UI 测试**（Fragment + Espresso，需 Gradle，请在 Windows Terminal 执行）：

```powershell
powershell -ExecutionPolicy Bypass -File android/scripts/build.ps1 -Task test
```

UI 测试覆盖：引导页、首页、拦截记录、设置、帮助页。

## 构建 APK

**注意：** Cursor Agent 终端会阻止 Gradle 连接 localhost 守护进程，构建请在系统终端执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\test\Cursor\android\scripts\build.ps1 -Task all
```

或：

```powershell
D:\test\Cursor\android\scripts\gradle.bat :app:assembleDebug
```

### Gradle 构建失败排查

1. 运行诊断：`build.ps1 -Task diagnose`
2. 临时关闭 VPN（Tailscale、UU 加速器等）
3. 确认 localhost TCP 回环正常
4. 执行 `gradlew --stop` 后重试
5. 检查 `D:\test\Cursor\.gradle\daemon\8.7\daemon-*.out.log`

## 开发阶段

- Phase 1：守护引擎、词库、首页、设置开关
- Phase 2：引导流程、拦截记录、子女管理、Toast
- Phase 3：黑名单、词库导出

## 安装使用

1. 安装 APK 到安卓手机
2. 完成首次引导，开启无障碍权限
3. 打开抖音刷视频，可疑内容将被自动跳过
