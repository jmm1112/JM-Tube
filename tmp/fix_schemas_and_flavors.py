import os
import shutil

print("1. Migrating Room schemas...")

def migrate_schemas(src_name, dst_name):
    src_dir = f"app/schemas/{src_name}"
    dst_dir = f"app/schemas/{dst_name}"
    os.makedirs(dst_dir, exist_ok=True)
    if os.path.exists(src_dir):
        for fname in os.listdir(src_dir):
            if fname.endswith(".json"):
                src_path = os.path.join(src_dir, fname)
                dst_path = os.path.join(dst_dir, fname)
                with open(src_path, "r", encoding="utf-8") as f:
                    content = f.read()
                content = content.replace("io.github.aedev.flow", "com.jmtube.app")
                with open(dst_path, "w", encoding="utf-8") as f:
                    f.write(content)
        shutil.rmtree(src_dir)

migrate_schemas("io.github.aedev.flow.data.local.AppDatabase", "com.jmtube.app.data.local.AppDatabase")
migrate_schemas("io.github.aedev.flow.sync2.data.LinkedDatabase", "com.jmtube.app.sync2.data.LinkedDatabase")

print("2. Copying github flavor files into main...")

github_src = "app/src/github/java/io/github/aedev/flow"
main_dst = "app/src/main/java/com/jmtube/app"

if os.path.exists(github_src):
    for root, dirs, files in os.walk(github_src):
        for file in files:
            if file.endswith(".kt") or file.endswith(".java"):
                rel_path = os.path.relpath(os.path.join(root, file), github_src)
                target_path = os.path.join(main_dst, rel_path)
                os.makedirs(os.path.dirname(target_path), exist_ok=True)
                with open(os.path.join(root, file), "r", encoding="utf-8") as f:
                    content = f.read()
                content = content.replace("io.github.aedev.flow", "com.jmtube.app")
                content = content.replace("FlowApplication", "JMTubeApplication")
                with open(target_path, "w", encoding="utf-8") as f:
                    f.write(content)

print("3. Removing flavor source folders...")
shutil.rmtree("app/src/github", ignore_errors=True)
shutil.rmtree("app/src/foss", ignore_errors=True)

print("4. Updating AndroidManifest.xml with github flavor activities/receivers...")
manifest_path = "app/src/main/AndroidManifest.xml"
if os.path.exists(manifest_path):
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = f.read()
    
    if "DiscordLoginActivity" not in manifest:
        insertion = """
        <activity
            android:name="com.jmtube.app.discord.DiscordLoginActivity"
            android:exported="false"
            android:theme="@style/Theme.Flow" />
        <receiver
            android:name="com.jmtube.app.updater.UpdateInstallReceiver"
            android:exported="false" />
    </application>"""
        manifest = manifest.replace("</application>", insertion)
        with open(manifest_path, "w", encoding="utf-8") as f:
            f.write(manifest)

print("Fix completed.")
