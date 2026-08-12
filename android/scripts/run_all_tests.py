#!/usr/bin/env python3
"""Run all project tests: core unit tests + asset validation + optional Gradle/Robolectric."""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
ASSETS = ANDROID / "app" / "src" / "main" / "assets" / "keywords_default.json"
GRADLE_WRAPPER = ANDROID / "scripts" / "gradle.bat"


def run_core_tests() -> int:
    script = ANDROID / "scripts" / "run_core_tests.py"
    result = subprocess.run([sys.executable, str(script)], cwd=ANDROID.parent)
    return result.returncode


def try_gradle_robolectric() -> int | None:
    """Return exit code if Gradle ran, None if skipped."""
    if os.environ.get("SKIP_GRADLE") == "1":
        print("[SKIP] Gradle tests skipped (SKIP_GRADLE=1)")
        return None
    if not GRADLE_WRAPPER.exists():
        return None
    print("\n=== Phase: Gradle Robolectric UI tests ===")
    result = subprocess.run(
        ["cmd", "/c", str(GRADLE_WRAPPER), ":app:testDebugUnitTest", "--console=plain"],
        cwd=ANDROID,
    )
    if result.returncode != 0:
        print("[WARN] Gradle tests failed in this environment.")
        print("       Run in Windows Terminal: powershell -File android/scripts/build.ps1 -Task test")
        return result.returncode
    print("[PASS] Gradle Robolectric tests passed")
    return 0


def validate_default_keywords() -> None:
    data = json.loads(ASSETS.read_text(encoding="utf-8"))
    keywords = data["keywords"]
    assert len(keywords) >= 140, f"Expected >=140 keywords, got {len(keywords)}"
    categories = {k["category"] for k in keywords}
    for expected in {"health_scam", "rumor", "incitement", "clickbait"}:
        assert expected in categories, f"Missing category {expected}"
    print(f"[PASS] Default keyword asset: {len(keywords)} keywords, 4 categories")


def validate_prd_files() -> None:
    prd = ANDROID.parent / "PRD.md"
    assert prd.exists(), "PRD.md missing"
    text = prd.read_text(encoding="utf-8")
    for section in ["产品目标", "页面框架", "验收标准", "第一版必须完成"]:
        assert section in text, f"PRD missing section: {section}"
    print("[PASS] PRD.md structure validated")


def validate_android_project() -> None:
    required = [
        ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
        ANDROID / "app" / "src" / "test" / "java" / "com" / "yinling" / "guard" / "UiRobolectricTest.kt",
        ANDROID / "app" / "src" / "main" / "java" / "com" / "yinling" / "guard" / "service" / "GuardAccessibilityService.kt",
        ANDROID / "scripts" / "build.ps1",
    ]
    for path in required:
        assert path.exists(), f"Missing required file: {path}"
    print("[PASS] Android project structure validated")


def main() -> int:
    print("=== Phase: Asset & PRD validation ===")
    validate_prd_files()
    validate_android_project()
    validate_default_keywords()

    print("\n=== Phase: Core unit tests ===")
    code = run_core_tests()
    if code != 0:
        print("[FAIL] Core unit tests failed")
        return code

    gradle_code = try_gradle_robolectric()
    if gradle_code is not None and gradle_code != 0:
        return gradle_code

    print("\n[PASS] All runnable tests passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
