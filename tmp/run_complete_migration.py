import os
import shutil
import re
import subprocess

print("Step 1: Cloning FlowRepo...")
subprocess.run(["git", "clone", "--depth", "1", "https://github.com/Flow-Tube/Flow.git", "/tmp/FlowRepo"], check=True)

SRC_FLOW = "/tmp/FlowRepo"
APP_ROOT = "/app/applet/app"

print("Step 2: Copying org/tartarus/snowball...")
if os.path.exists(f"{SRC_FLOW}/app/src/main/java/org"):
    shutil.copytree(f"{SRC_FLOW}/app/src/main/java/org", f"{APP_ROOT}/src/main/java/org", dirs_exist_ok=True)

print("Step 3: Copying missing Kotlin source files...")
src_java = f"{SRC_FLOW}/app/src/main/java/io/github/aedev/flow"
dst_java = f"{APP_ROOT}/src/main/java/com/jmtube/app"

count_copied = 0
for root, dirs, files in os.walk(src_java):
    for f in files:
        if f.endswith(".kt") or f.endswith(".java"):
            src_file = os.path.join(root, f)
            rel_path = os.path.relpath(src_file, src_java)
            dst_file = os.path.join(dst_java, rel_path)
            
            # Skip if file already exists in JM Tube (preserves user customizations)
            if os.path.exists(dst_file):
                continue
            
            # Skip FlowApplication.kt since JMTubeApplication.kt is the application class
            if f == "FlowApplication.kt":
                continue

            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            with open(src_file, "r", encoding="utf-8", errors="ignore") as fin:
                content = fin.read()
            
            content = content.replace("io.github.aedev.flow", "com.jmtube.app")
            content = content.replace("FlowApplication", "JMTubeApplication")
            
            with open(dst_file, "w", encoding="utf-8") as fout:
                fout.write(content)
            count_copied += 1

print(f"Copied {count_copied} missing source files.")

print("Step 4: Copying drawables...")
src_drawables = f"{SRC_FLOW}/app/src/main/res/drawable"
dst_drawables = f"{APP_ROOT}/src/main/res/drawable"
os.makedirs(dst_drawables, exist_ok=True)

for f in os.listdir(src_drawables):
    sf = os.path.join(src_drawables, f)
    df = os.path.join(dst_drawables, f)
    if os.path.isfile(sf) and not os.path.exists(df):
        if f.endswith(".xml"):
            with open(sf, "r", encoding="utf-8") as fin:
                c = fin.read()
            c = c.replace("io.github.aedev.flow", "com.jmtube.app")
            with open(df, "w", encoding="utf-8") as fout:
                fout.write(c)
        else:
            shutil.copy2(sf, df)

print("Step 5: Copying layouts...")
src_layout = f"{SRC_FLOW}/app/src/main/res/layout"
dst_layout = f"{APP_ROOT}/src/main/res/layout"
if os.path.exists(src_layout):
    os.makedirs(dst_layout, exist_ok=True)
    for f in os.listdir(src_layout):
        sf = os.path.join(src_layout, f)
        df = os.path.join(dst_layout, f)
        if not os.path.exists(df):
            with open(sf, "r", encoding="utf-8") as fin:
                c = fin.read()
            c = c.replace("io.github.aedev.flow", "com.jmtube.app")
            with open(df, "w", encoding="utf-8") as fout:
                fout.write(c)

print("Step 6: Copying xml files...")
src_xml = f"{SRC_FLOW}/app/src/main/res/xml"
dst_xml = f"{APP_ROOT}/src/main/res/xml"
if os.path.exists(src_xml):
    os.makedirs(dst_xml, exist_ok=True)
    for f in os.listdir(src_xml):
        sf = os.path.join(src_xml, f)
        df = os.path.join(dst_xml, f)
        if not os.path.exists(df):
            with open(sf, "r", encoding="utf-8") as fin:
                c = fin.read()
            c = c.replace("io.github.aedev.flow", "com.jmtube.app")
            with open(df, "w", encoding="utf-8") as fout:
                fout.write(c)

print("Step 7: Copying other resources (drawable-xhdpi, values-night, resources/neuro)...")
src_dx = f"{SRC_FLOW}/app/src/main/res/drawable-xhdpi"
dst_dx = f"{APP_ROOT}/src/main/res/drawable-xhdpi"
if os.path.exists(src_dx) and not os.path.exists(dst_dx):
    shutil.copytree(src_dx, dst_dx)

src_vn = f"{SRC_FLOW}/app/src/main/res/values-night"
dst_vn = f"{APP_ROOT}/src/main/res/values-night"
if os.path.exists(src_vn) and not os.path.exists(dst_vn):
    shutil.copytree(src_vn, dst_vn)

src_neuro = f"{SRC_FLOW}/app/src/main/resources/neuro"
dst_neuro = f"{APP_ROOT}/src/main/resources/neuro"
if os.path.exists(src_neuro) and not os.path.exists(dst_neuro):
    shutil.copytree(src_neuro, dst_neuro)

print("Step 8: Copying Room schemas...")
schema_src = f"{SRC_FLOW}/app/schemas/io.github.aedev.flow.sync2.data.LinkedDatabase"
schema_dst = f"{APP_ROOT}/schemas/com.jmtube.app.sync2.data.LinkedDatabase"
if os.path.exists(schema_src):
    os.makedirs(schema_dst, exist_ok=True)
    for s_name in ["5.json", "6.json"]:
        sf = os.path.join(schema_src, s_name)
        df = os.path.join(schema_dst, s_name)
        if os.path.exists(sf) and not os.path.exists(df):
            with open(sf, "r", encoding="utf-8") as fin:
                c = fin.read()
            c = c.replace("io.github.aedev.flow", "com.jmtube.app")
            with open(df, "w", encoding="utf-8") as fout:
                fout.write(c)

print("Step 9: Adding missing strings from Flow...")
with open(f"{APP_ROOT}/src/main/res/values/strings.xml", "r", encoding="utf-8") as f:
    curr_strings_content = f.read()

with open(f"{SRC_FLOW}/app/src/main/res/values/strings.xml", "r", encoding="utf-8") as f:
    flow_strings_content = f.read()

curr_strings = set(re.findall(r"<string name=\"([^\"]+)\"", curr_strings_content))
flow_string_matches = re.findall(r"(    <string name=\"([^\"]+)\"[^>]*>.*?</string>)", flow_strings_content, re.DOTALL)
missing_strings = []
for full_tag, name in flow_string_matches:
    if name not in curr_strings:
        tag = full_tag.replace("Flow Tube", "JM Tube").replace("Flow", "JM Tube")
        missing_strings.append(tag)

if missing_strings:
    insertion = "\n" + "\n".join(missing_strings) + "\n</resources>"
    curr_strings_content = curr_strings_content.replace("</resources>", insertion)
    with open(f"{APP_ROOT}/src/main/res/values/strings.xml", "w", encoding="utf-8") as f:
        f.write(curr_strings_content)
    print(f"Added {len(missing_strings)} missing strings.")

print("Step 10: Cleaning up FlowRepo...")
shutil.rmtree(SRC_FLOW, ignore_errors=True)

print("Step 11: Removing FlowApplication.kt if any and test dirs...")
if os.path.exists(f"{APP_ROOT}/src/main/java/com/jmtube/app/FlowApplication.kt"):
    os.remove(f"{APP_ROOT}/src/main/java/com/jmtube/app/FlowApplication.kt")
shutil.rmtree(f"{APP_ROOT}/src/androidTest", ignore_errors=True)
shutil.rmtree(f"{APP_ROOT}/src/androidTestGithub", ignore_errors=True)

print("Step 12: Fixing resources.properties...")
with open(f"{APP_ROOT}/src/main/res/resources.properties", "w", encoding="utf-8") as f:
    f.write("unqualifiedResLocale=en\n")

print("Step 13: Updating AndroidManifest.xml...")
with open(f"{APP_ROOT}/src/main/AndroidManifest.xml", "r", encoding="utf-8") as f:
    manifest = f.read()

manifest = re.sub(r"@mipmap/ic_launcher_[a-z_]+_round", "@mipmap/ic_launcher_round", manifest)
manifest = re.sub(r"@mipmap/ic_launcher_[a-z_]+", "@mipmap/ic_launcher", manifest)

with open(f"{APP_ROOT}/src/main/AndroidManifest.xml", "w", encoding="utf-8") as f:
    f.write(manifest)

print("Step 14: Updating AppIcons.kt...")
app_icons_path = f"{APP_ROOT}/src/main/java/com/jmtube/app/util/AppIcons.kt"
if os.path.exists(app_icons_path):
    with open(app_icons_path, "r", encoding="utf-8") as f:
        ai_content = f.read()
    ai_content = re.sub(r"R\.mipmap\.ic_launcher_[a-z_]+", "R.mipmap.ic_launcher", ai_content)
    with open(app_icons_path, "w", encoding="utf-8") as f:
        f.write(ai_content)

print("Step 15: Fixing AboutScreen.kt...")
about_path = f"{APP_ROOT}/src/main/java/com/jmtube/app/ui/screens/settings/about/AboutScreen.kt"
if os.path.exists(about_path):
    with open(about_path, "r", encoding="utf-8") as f:
        about_content = f.read()
    about_content = about_content.replace(
        "private enum class AboutDialog { CHANGELOG, DEVICE, NOTICES, LICENSE }",
        "private enum class AboutDialog { CHANGELOG, DEVICE, NOTICES }"
    )
    if "LICENSE_URL" not in about_content:
        about_content = about_content.replace(
            "private const val CREATOR_AVATAR_URL",
            "private const val LICENSE_URL = \"https://github.com/jmm1112/JM-Tube/blob/main/License\"\nprivate const val CREATOR_AVATAR_URL"
        )
    about_content = about_content.replace(
        "nav(AboutIndex.license, icon = Icons.Outlined.Description, showChevron = false, onClick = { dialog = AboutDialog.LICENSE })",
        "nav(AboutIndex.license, icon = Icons.Outlined.Description, showChevron = false, onClick = { open(LICENSE_URL) })"
    )
    about_content = about_content.replace(
        "AboutDialog.LICENSE -> LicenseDialog(onDismiss = { dialog = null })\n",
        ""
    )
    with open(about_path, "w", encoding="utf-8") as f:
        f.write(about_content)

print("Step 16: Fixing JMTubeApplication.kt volatile memory cleanup...")
app_kt_path = f"{APP_ROOT}/src/main/java/com/jmtube/app/JMTubeApplication.kt"
if os.path.exists(app_kt_path):
    with open(app_kt_path, "r", encoding="utf-8") as f:
        app_content = f.read()
    
    old_cleanup = """    private fun releaseVolatileMemory() {
        if (::imageLoader.isInitialized) {
            imageLoader.memoryCache?.clear()
        }
        if (::okHttpClient.isInitialized) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                okHttpClient.connectionPool.evictAll()
            }
        }
    }"""
    new_cleanup = """    private fun releaseVolatileMemory() {
        try {
            imageLoader.memoryCache?.clear()
        } catch (_: Throwable) {
        }
        try {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                okHttpClient.connectionPool.evictAll()
            }
        } catch (_: Throwable) {
        }
    }"""
    if old_cleanup in app_content:
        app_content = app_content.replace(old_cleanup, new_cleanup)
        with open(app_kt_path, "w", encoding="utf-8") as f:
            f.write(app_content)

print("All steps completed successfully!")
