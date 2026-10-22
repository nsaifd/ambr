import re, sys, json, os
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
pkg, out, names = sys.argv[1], sys.argv[2], sys.argv[3].split(",")
css = open(os.path.join(pkg, "css/regular/rounded.css")).read()
woff = [f for f in os.listdir(os.path.join(pkg, "css")) if f.startswith("uicons-regular-rounded") and f.endswith(".woff2")][0]
font = TTFont(os.path.join(pkg, "css", woff))
cmap, gs = font.getBestCmap(), font.getGlyphSet()
upm = font["head"].unitsPerEm
asc = font["hhea"].ascent
os.makedirs(out, exist_ok=True)
res = {}
for n in names:
    m = re.search(r"\.fi-rr-%s:before\{content:\"\\([0-9a-f]+)\"" % re.escape(n), css)
    if not m: print("missing", n); continue
    g = cmap[int(m.group(1), 16)]
    pen = SVGPathPen(gs)
    gs[g].draw(TransformPen(pen, (1, 0, 0, -1, 0, asc)))
    d = pen.getCommands()
    res[n] = d
    open(os.path.join(out, n + ".svg"), "w").write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {upm} {upm}"><path d="{d}"/></svg>\n')
json.dump({"viewBox": f"0 0 {upm} {upm}", "icons": res}, open(os.path.join(out, "icons.json"), "w"))
print(len(res), "icons, upm", upm, "asc", asc)
