import os
import shutil
import re

SRC_ROOT = "/tmp/Flow"
DST_ROOT = "/app/applet"

print("1. Copying files from /tmp/Flow to /app/applet...")

# Copy app module
if os.path.exists(f"{DST_ROOT}/app/src"):
    shutil.rmtree(f"{DST_ROOT}/app/src")

shutil.copytree(f"{SRC_ROOT}/app/src", f"{DST_ROOT}/app/src")

# Copy schemas if exists
if os.path.exists(f"{SRC_ROOT}/app/schemas"):
    if os.path.exists(f"{DST_ROOT}/app/schemas"):
        shutil.rmtree(f"{DST_ROOT}/app/schemas")
    shutil.copytree(f"{SRC_ROOT}/app/schemas", f"{DST_ROOT}/app/schemas")

# Copy proguard rules
if os.path.exists(f"{SRC_ROOT}/app/proguard-rules.pro"):
    shutil.copy2(f"{SRC_ROOT}/app/proguard-rules.pro", f"{DST_ROOT}/app/proguard-rules.pro")

# Copy gradle libs.versions.toml
shutil.copy2(f"{SRC_ROOT}/gradle/libs.versions.toml", f"{DST_ROOT}/gradle/libs.versions.toml")

# Remove androidTest if present
if os.path.exists(f"{DST_ROOT}/app/src/androidTest"):
    shutil.rmtree(f"{DST_ROOT}/app/src/androidTest")

print("2. Migrating package structure from io.github.aedev.flow to com.jmtube.app...")

# Main source directory migration
old_main_dir = f"{DST_ROOT}/app/src/main/java/io/github/aedev/flow"
new_main_dir = f"{DST_ROOT}/app/src/main/java/com/jmtube/app"

if os.path.exists(old_main_dir):
    os.makedirs(f"{DST_ROOT}/app/src/main/java/com/jmtube", exist_ok=True)
    shutil.move(old_main_dir, new_main_dir)
    # Clean up empty old dirs
    shutil.rmtree(f"{DST_ROOT}/app/src/main/java/io", ignore_errors=True)

# Test source directory migration
old_test_dir = f"{DST_ROOT}/app/src/test/java/io/github/aedev/flow"
new_test_dir = f"{DST_ROOT}/app/src/test/java/com/jmtube/app"

if os.path.exists(old_test_dir):
    os.makedirs(f"{DST_ROOT}/app/src/test/java/com/jmtube", exist_ok=True)
    shutil.move(old_test_dir, new_test_dir)
    shutil.rmtree(f"{DST_ROOT}/app/src/test/java/io", ignore_errors=True)

# Rename FlowApplication.kt to JMTubeApplication.kt
old_app_kt = f"{new_main_dir}/FlowApplication.kt"
new_app_kt = f"{new_main_dir}/JMTubeApplication.kt"
if os.path.exists(old_app_kt):
    with open(old_app_kt, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace("class FlowApplication", "class JMTubeApplication")
    with open(new_app_kt, "w", encoding="utf-8") as f:
        f.write(content)
    os.remove(old_app_kt)

print("3. Refactoring package names and imports across all code files...")

extensions = (".kt", ".java", ".xml", ".pro", ".kts", ".json")

for root, dirs, files in os.walk(f"{DST_ROOT}/app"):
    for file in files:
        if any(file.endswith(ext) for ext in extensions):
            file_path = os.path.join(root, file)
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    text = f.read()

                updated_text = text.replace("io.github.aedev.flow", "com.jmtube.app")
                updated_text = updated_text.replace("FlowApplication", "JMTubeApplication")

                if text != updated_text:
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.write(updated_text)
            except Exception as e:
                print(f"Error updating {file_path}: {e}")

print("4. Updating strings.xml with JMTube branding...")
strings_path = f"{DST_ROOT}/app/src/main/res/values/strings.xml"
if os.path.exists(strings_path):
    with open(strings_path, "r", encoding="utf-8") as f:
        s = f.read()

    s = s.replace('<string name="app_name">Flow</string>', '<string name="app_name">JMTube</string>')
    s = s.replace('<string name="app_launcher_name" translatable="false">Flow</string>', '<string name="app_launcher_name" translatable="false">JMTube</string>')
    s = s.replace("Flow Tube", "JMTube")
    s = s.replace("Welcome to Flow", "Welcome to JMTube")
    s = s.replace("Enjoying Flow?", "Enjoying JMTube?")
    s = s.replace("Make Flow yours", "Make JMTube yours")
    s = s.replace("What's new in Flow", "What's new in JMTube")
    s = s.replace("Search in Flow", "Search in JMTube")
    s = s.replace("Flow logo", "JMTube logo")
    s = s.replace("Flow is up to date!", "JMTube is up to date!")
    s = s.replace("Flow Downloads", "JMTube Downloads")
    s = s.replace("Flow Recap", "JMTube Recap")
    s = s.replace("Flow Control Center", "JMTube Control Center")

    with open(strings_path, "w", encoding="utf-8") as f:
        f.write(s)

print("Migration script finished successfully.")
