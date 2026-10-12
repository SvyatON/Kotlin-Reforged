from pathlib import Path
import hashlib
import io
import json
import urllib.request
import shutil
import zipfile
def write_entry(archive, name, data):
    entry = zipfile.ZipInfo(name, (2020, 1, 1, 0, 0, 0))
    entry.compress_type = zipfile.ZIP_DEFLATED
    if name == "gradlew":
        entry.external_attr = 0o100755 << 16
    archive.writestr(entry, data)

ROOT = Path(__file__).resolve().parent
VERSION = "7.0.0+1.21.6-26.3"
FILE_NAME = "rekotlin-f-" + VERSION
runtime = json.loads((ROOT / "gradle/kotlin-runtime.json").read_text())
additional_libraries = json.loads((ROOT / "gradle/additional-kotlin-libraries.json").read_text())["libraries"]
additional_cache = ROOT / "build/additional-kotlin-libraries"
additional_cache.mkdir(parents=True, exist_ok=True)
for library in additional_libraries:
    jar = additional_cache / f"{library['artifact']}-{library['version']}.jar"
    if not jar.exists():
        request = urllib.request.Request(library["url"], headers={"User-Agent": "Kotlin-Reforged-build"})
        jar.write_bytes(urllib.request.urlopen(request, timeout=60).read())
    if hashlib.sha512(jar.read_bytes()).hexdigest() != library["sha512"]:
        raise ValueError(f"{jar.name} SHA-512 does not match the pinned artifact")
cache = ROOT / "build" / "kotlin-runtime-template.jar"
if not cache.exists():
    request = urllib.request.Request(runtime["url"], headers={"User-Agent": "Kotlin-Reforged-build"})
    cache.write_bytes(urllib.request.urlopen(request, timeout=60).read())
if hashlib.sha512(cache.read_bytes()).hexdigest() != runtime["sha512"]:
    raise ValueError("Kotlin runtime template SHA-512 does not match the pinned upstream artifact")
output = ROOT / "build" / "releases"
output.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(cache) as original, zipfile.ZipFile(output / ("kotlinforforge-" + VERSION + "-all.jar"), "w", zipfile.ZIP_DEFLATED, compresslevel=9) as result:
    jj = json.loads(original.read("META-INF/jarjar/metadata.json"))
    replaced = set()
    for name in original.namelist():
        if name == "META-INF/jarjar/metadata.json": continue
        kind = next((k for k in ("lang", "lib", "mod") if name.startswith("META-INF/jarjar/thedarkcolour.kff" + k + "-")), None)
        if kind:
            replaced.add(kind)
            blob = (ROOT / "build/libs" / (kind + "Forge-" + VERSION + ".jar")).read_bytes()
            if kind == "mod":
                buffer = io.BytesIO()
                with zipfile.ZipFile(io.BytesIO(blob)) as source, zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as dest:
                    for n in source.namelist():
                        if n != "META-INF/mods.toml": write_entry(dest, n, source.read(n))
                    text = (ROOT / "src/kffmod/templates/META-INF/mods.toml").read_text().replace("${kff_version}", VERSION).replace("${min_mc_version}", "1.21.6").replace("${unsupported_mc_version}", "26.4")
                    write_entry(dest, "META-INF/mods.toml", text)
                blob = buffer.getvalue()
            newname = "META-INF/jarjar/kff" + kind + "-" + VERSION + ".jar"
            write_entry(result, newname, blob)
            item = next(i for i in jj["jars"] if i["path"] == name)
            item["path"] = newname
            item["version"] = {"range": "[7.0.0,8)", "artifactVersion": VERSION}
        else: write_entry(result, name, original.read(name))
    assert replaced == {"lang", "lib", "mod"}
    for library in additional_libraries:
        name = f"{library['artifact']}-{library['version']}.jar"
        path = "META-INF/jarjar/" + name
        write_entry(result, path, (additional_cache / name).read_bytes())
        jj["jars"].append({
            "identifier": {"group": library["group"], "artifact": library["artifact"]},
            "version": {"range": f"[{library['version']},)", "artifactVersion": library["version"]},
            "path": path,
            "isObfuscated": False,
        })
    metadata_root = ROOT / "src/kffmod/common/resources/META-INF"
    for path in sorted(metadata_root.rglob("*")):
        if path.is_file():
            write_entry(result, "META-INF/" + path.relative_to(metadata_root).as_posix(), path.read_bytes())
    write_entry(result, "META-INF/jarjar/metadata.json", json.dumps(jj, indent=2))
    write_entry(result, "LICENSE", (ROOT / "LICENSE").read_bytes())
print(output)

with zipfile.ZipFile(output / ("kotlinforforge-" + VERSION + "-all.jar")) as combined, zipfile.ZipFile(ROOT / "build" / ("kotlinforforge-" + VERSION + "-maven.jar"), "w", zipfile.ZIP_DEFLATED) as maven:
    metadata = json.loads(combined.read("META-INF/jarjar/metadata.json"))
    adapters = {item["path"] for item in metadata["jars"] if item["identifier"]["group"] == "thedarkcolour"}
    metadata["jars"] = [item for item in metadata["jars"] if item["path"] not in adapters]
    for name in combined.namelist():
        if name not in adapters and name != "META-INF/jarjar/metadata.json": write_entry(maven, name, combined.read(name))
    write_entry(maven, "META-INF/jarjar/metadata.json", json.dumps(metadata, indent=2))

with zipfile.ZipFile(output / ("kotlinforforge-" + VERSION + "-all.jar")) as combined:
    assert combined.testzip() is None
    entries = json.loads(combined.read("META-INF/jarjar/metadata.json"))["jars"]
    assert len({item["path"] for item in entries}) == len(entries)
    for library in additional_libraries:
        path = f"META-INF/jarjar/{library['artifact']}-{library['version']}.jar"
        assert hashlib.sha512(combined.read(path)).hexdigest() == library["sha512"]
    for item in entries:
        assert item["path"] in combined.namelist()
        with zipfile.ZipFile(io.BytesIO(combined.read(item["path"]))) as module:
            assert module.testzip() is None
            if item["identifier"]["group"] == "thedarkcolour":
                assert not any(n.startswith(("net/minecraft/", "net/minecraftforge/")) and n.endswith(".class") for n in module.namelist())

install_output = ROOT / "releases"
install_output.mkdir(exist_ok=True)
install_jar = install_output / (FILE_NAME + ".jar")
shutil.copy2(output / ("kotlinforforge-" + VERSION + "-all.jar"), install_jar)
print(install_jar)

source_archive = output / (FILE_NAME + "-sources.zip")
source_files = []
for directory in ("src", "gradle", ".github"):
    for path in (ROOT / directory).rglob("*"):
        if path.is_file() and not any(part in {"build", ".gradle", "__pycache__"} for part in path.relative_to(ROOT).parts):
            source_files.append(path)
for name in ("README.md", "LICENSE", "changelog.md", ".gitignore", ".gitattributes", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "package-forge.py"):
    source_files.append(ROOT / name)
with zipfile.ZipFile(source_archive, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(source_files):
        write_entry(archive, path.relative_to(ROOT).as_posix(), path.read_bytes())
with zipfile.ZipFile(source_archive) as archive:
    assert archive.testzip() is None
print(source_archive)
source_jar_name = FILE_NAME + "-sources.jar"
shutil.copy2(source_archive, output / source_jar_name)
source_jar = install_output / source_jar_name
shutil.copy2(source_archive, source_jar)
print(source_jar)
