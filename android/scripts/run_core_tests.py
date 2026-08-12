#!/usr/bin/env python3
"""Compile and run core module JUnit tests without Gradle daemon."""

from __future__ import annotations

import glob
import os
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[1]
ROOT = ANDROID.parent
CORE = ANDROID / "core"
LIB = ANDROID / "test-lib"
BUILD = ANDROID / "build" / "core-test"
CLASSES = BUILD / "classes"
TEST_CLASSES = BUILD / "test-classes"

JAVA_HOME = Path(os.environ.get("JAVA_HOME", r"C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"))
JAVA = JAVA_HOME / "bin" / ("java.exe" if os.name == "nt" else "java")
KOTLINC = JAVA_HOME / "bin" / ("kotlinc.bat" if os.name == "nt" else "kotlinc")

DEPS = {
    "kotlin-stdlib-1.9.24.jar": "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.9.24/kotlin-stdlib-1.9.24.jar",
    "gson-2.11.0.jar": "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar",
    "junit-4.13.2.jar": "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar",
    "hamcrest-core-1.3.jar": "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
    "kotlin-reflect-1.9.24.jar": "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-reflect/1.9.24/kotlin-reflect-1.9.24.jar",
}


def download_deps() -> None:
    LIB.mkdir(parents=True, exist_ok=True)
    for name, url in DEPS.items():
        target = LIB / name
        if target.exists() and target.stat().st_size > 0:
            continue
        print(f"Downloading {name}...")
        urllib.request.urlretrieve(url, target)


def classpath(extra: Path | None = None) -> str:
    jars = sorted(str(p) for p in LIB.glob("*.jar"))
    parts = jars[:]
    if extra and extra.exists():
        parts.insert(0, str(extra))
    sep = ";" if os.name == "nt" else ":"
    return sep.join(parts)


def ensure_kotlin_compiler() -> Path:
    if KOTLINC.exists():
        return KOTLINC
    kotlin_home = ANDROID / "kotlin-compiler"
    kotlinc_dir = kotlin_home / "kotlinc" / "bin"
    kotlinc = kotlinc_dir / ("kotlinc.bat" if os.name == "nt" else "kotlinc")
    if kotlinc.exists():
        return kotlinc
    print("Downloading Kotlin compiler...")
    zip_path = ANDROID / "kotlin-compiler.zip"
    urllib.request.urlretrieve(
        "https://github.com/JetBrains/kotlin/releases/download/v1.9.24/kotlin-compiler-1.9.24.zip",
        zip_path,
    )
    import zipfile

    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(kotlin_home)
    return kotlinc


def run_kotlinc(kotlinc: Path, cp: str, dest: Path, sources: list[str]) -> None:
    src = " ".join(f'"{s}"' for s in sources)
    if os.name == "nt":
        cmd = f'"{kotlinc}" -cp "{cp}" -d "{dest}" {src}'
        print("$", cmd)
        result = subprocess.run(cmd, cwd=ROOT, shell=True)
    else:
        cmd = [str(kotlinc), "-cp", cp, "-d", str(dest), *sources]
        print("$", " ".join(cmd))
        result = subprocess.run(cmd, cwd=ROOT)
    if result.returncode != 0:
        raise SystemExit(result.returncode)


def main() -> int:
    if not JAVA.exists():
        print(f"Java not found at {JAVA}", file=sys.stderr)
        return 1

    download_deps()
    kotlinc = ensure_kotlin_compiler()

    if BUILD.exists():
        shutil.rmtree(BUILD)
    CLASSES.mkdir(parents=True)
    TEST_CLASSES.mkdir(parents=True)

    main_sources = glob.glob(str(CORE / "src" / "main" / "kotlin" / "**" / "*.kt"), recursive=True)
    test_sources = glob.glob(str(CORE / "src" / "test" / "kotlin" / "**" / "*.kt"), recursive=True)

    cp = classpath()
    run_kotlinc(kotlinc, cp, CLASSES, main_sources)
    run_kotlinc(kotlinc, classpath(CLASSES), TEST_CLASSES, test_sources)

    test_classes = [
        "com.yinling.guard.core.engine.ContentMatcherTest",
        "com.yinling.guard.core.engine.GuardEngineTest",
        "com.yinling.guard.core.engine.BlockLogRetentionTest",
        "com.yinling.guard.core.engine.HomeStatsCalculatorTest",
        "com.yinling.guard.core.engine.BlockLogFilterTest",
        "com.yinling.guard.core.engine.VideoTextParserTest",
        "com.yinling.guard.core.security.PasswordHasherTest",
        "com.yinling.guard.core.storage.GuardRepositoryTest",
        "com.yinling.guard.core.family.FamilyManagerTest",
        "com.yinling.guard.core.ui.PresentersTest",
        "com.yinling.guard.core.data.DefaultKeywordsTest",
    ]

    full_cp = classpath(CLASSES) + (";" if os.name == "nt" else ":") + str(TEST_CLASSES)
    failed = 0
    for cls in test_classes:
        print(f"\n=== Running {cls} ===")
        if os.name == "nt":
            cmd = f'"{JAVA}" -classpath "{full_cp}" org.junit.runner.JUnitCore {cls}'
            result = subprocess.run(cmd, cwd=ROOT, shell=True)
        else:
            result = subprocess.run([str(JAVA), "-classpath", full_cp, "org.junit.runner.JUnitCore", cls], cwd=ROOT)
        if result.returncode != 0:
            failed += 1

    print(f"\nSummary: {len(test_classes) - failed}/{len(test_classes)} test classes passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
