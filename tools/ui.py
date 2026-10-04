"""Emulator helper for manual checks: dump | tap LABEL [n] | xy X Y | type TEXT | key CODE | shot FILE"""
import os, re, subprocess, sys

ADB = os.environ.get("ADB", r"C:\Users\FAyaz\AppData\Local\Android\Sdk\platform-tools\adb.exe")


def adb(*a, binary=False):
    r = subprocess.run([ADB, *a], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    out = []
    for m in re.finditer(r"<node [^>]*>", adb("exec-out", "cat", "/sdcard/ui.xml")):
        n = m.group(0)
        g = lambda k: (re.search(k + r'="([^"]*)"', n) or [None, ""])[1]
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", g("bounds")))
        out.append(dict(text=g("text"), desc=g("content-desc"), cls=g("class").split(".")[-1],
                        cx=(x1 + x2) // 2, cy=(y1 + y2) // 2, bounds=g("bounds")))
    return out


cmd, *rest = sys.argv[1:]
if cmd == "dump":
    for n in nodes():
        if n["text"] or n["desc"] or n["cls"] == "EditText":
            print(n["cls"], repr(n["text"]), repr(n["desc"]), n["bounds"])
elif cmd == "tap":
    hits = [n for n in nodes() if rest[0].lower() in (n["text"] + "|" + n["desc"]).lower()]
    k = int(rest[1]) if len(rest) > 1 else 0
    if len(hits) <= k:
        sys.exit(f"not found: {rest[0]}")
    adb("shell", "input", "tap", str(hits[k]["cx"]), str(hits[k]["cy"]))
    print("tapped", rest[0])
elif cmd == "xy":
    adb("shell", "input", "tap", rest[0], rest[1])
elif cmd == "type":
    adb("shell", "input", "text", rest[0].replace(" ", "%s"))
elif cmd == "key":
    adb("shell", "input", "keyevent", rest[0])
elif cmd == "shot":
    open(rest[0], "wb").write(adb("exec-out", "screencap", "-p", binary=True))
    print("saved", rest[0])
