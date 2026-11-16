#!/usr/bin/env python3
"""Assemble lingua/web/index.html from src/app.html + every courses/*.js, and bump the version.
Usage: python3 lingua/build_web.py            (bumps "web" in version.json by 1)
       python3 lingua/build_web.py --keep     (rebuild without bumping)"""
import json, os, sys, glob
D = os.path.dirname(os.path.abspath(__file__))
vpath = os.path.join(D, "version.json"); v = json.load(open(vpath))
if "--keep" not in sys.argv: v["web"] = int(v["web"]) + 1
app = open(os.path.join(D, "src", "app.html"), encoding="utf-8").read()
courses = sorted(glob.glob(os.path.join(D, "courses", "*.js")))
blob = "\n".join(open(c, encoding="utf-8").read().strip() for c in courses)
out = app.replace("/*COURSES*/", blob).replace("__VERSION__", str(v["web"]))
os.makedirs(os.path.join(D, "web"), exist_ok=True)
open(os.path.join(D, "web", "index.html"), "w", encoding="utf-8").write(out)
langs = [os.path.basename(c)[:-3] for c in courses]
v["courses"] = langs
json.dump(v, open(vpath, "w"), indent=2); open(vpath, "a").write("\n")
print("web", v["web"], "courses", langs, "bytes", len(out))
