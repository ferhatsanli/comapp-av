"""Regenerates the launcher icon drawables.

Usage: python3 docs/generate_icon.py app/src/main/res/drawable [preview.html]
"""
import math, sys, pathlib

CX = CY = 54.0
BEAD_R = 12.0
ARC_RADII = (21.0, 30.0)
ARC_HALF_SPAN = 48.0
ARC_WIDTH = 3.4

def pt(r, deg):
    a = math.radians(deg)
    return CX + r * math.cos(a), CY - r * math.sin(a)

def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")

def arc_path(r, side):
    if side == "right":
        (x1, y1), (x2, y2), sweep = pt(r, ARC_HALF_SPAN), pt(r, -ARC_HALF_SPAN), 1
    else:
        (x1, y1), (x2, y2), sweep = pt(r, 180 - ARC_HALF_SPAN), pt(r, 180 + ARC_HALF_SPAN), 0
    return f"M{f(x1)},{f(y1)} A{f(r)},{f(r)} 0 0 {sweep} {f(x2)},{f(y2)}"

def circle_path(cx, cy, r):
    return f"M{f(cx - r)},{f(cy)} a{f(r)},{f(r)} 0 1 1 {f(2 * r)},0 a{f(r)},{f(r)} 0 1 1 {f(-2 * r)},0 Z"

RIGHT_TINT, LEFT_TINT = "#FFD3EA", "#CFF4FF"
ARC_ALPHA = (0.98, 0.62)  # inner, outer

# ---------------- Android XML ----------------
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n')

def grad(kind, attrs, stops, name="android:fillColor", indent="    "):
    a = " ".join(f'android:{k}="{v}"' for k, v in attrs.items())
    items = "\n".join(f'{indent}        <item android:offset="{o}" android:color="{c}" />' for o, c in stops)
    return (f'{indent}<aapt:attr name="{name}">\n{indent}    <gradient android:type="{kind}" {a}>\n'
            f'{items}\n{indent}    </gradient>\n{indent}</aapt:attr>\n')

def path(data, inner="", **attrs):
    a = "".join(f'\n        android:{k}="{v}"' for k, v in attrs.items())
    return f'    <path\n        android:pathData="{data}"{a}>\n{inner}    </path>\n' if inner else f'    <path\n        android:pathData="{data}"{a} />\n'

def background_xml():
    s = HEAD
    s += path("M0,0h108v108h-108z", grad("linear", dict(startX=0, startY=0, endX=108, endY=108),
              [(0, "#FF3B2A9E"), (0.55, "#FF1A1250"), (1, "#FF0C1A3F")]))
    s += path(circle_path(86, 24, 62), grad("radial", dict(centerX=86, centerY=24, gradientRadius=62),
              [(0, "#C7FF4FA8"), (1, "#00FF4FA8")]))
    s += path(circle_path(20, 92, 62), grad("radial", dict(centerX=20, centerY=92, gradientRadius=62),
              [(0, "#B32BD9FF"), (1, "#002BD9FF")]))
    s += path("M0,0h108v54h-108z", grad("linear", dict(startX=0, startY=0, endX=0, endY=54),
              [(0, "#2EFFFFFF"), (1, "#00FFFFFF")]))
    return s + "</vector>\n"

def foreground_xml():
    s = HEAD
    s += path(circle_path(CX, CY, 21), grad("radial", dict(centerX=CX, centerY=CY, gradientRadius=21),
              [(0, "#59FFFFFF"), (1, "#00FFFFFF")]))
    for r, alpha in zip(ARC_RADII, ARC_ALPHA):
        for side, tint in (("right", RIGHT_TINT), ("left", LEFT_TINT)):
            s += path(arc_path(r, side), strokeColor=f"#{round(alpha*255):02X}{tint[1:]}",
                      strokeWidth=ARC_WIDTH, strokeLineCap="round")
    s += path(circle_path(CX, CY, BEAD_R), grad("radial", dict(centerX=CX - 4, centerY=CY - 5, gradientRadius=17),
              [(0, "#FFFFFFFF"), (0.45, "#FFBDEBFF"), (1, "#FF7C8CFF")]))
    s += path(circle_path(CX, CY, BEAD_R - 0.6), strokeColor="#CCFFFFFF", strokeWidth=1.2)
    s += path(circle_path(CX - 4.2, CY - 5, 3.2), fillColor="#E6FFFFFF")
    return s + "</vector>\n"

def monochrome_xml():
    s = HEAD
    for r in ARC_RADII:
        for side in ("right", "left"):
            s += path(arc_path(r, side), strokeColor="#FF000000", strokeWidth=ARC_WIDTH, strokeLineCap="round")
    s += path(circle_path(CX, CY, BEAD_R), fillColor="#FF000000")
    return s + "</vector>\n"

# ---------------- SVG preview ----------------
def svg_preview(mono=False):
    arcs = ""
    for r, alpha in zip(ARC_RADII, ARC_ALPHA):
        for side, tint in (("right", RIGHT_TINT), ("left", LEFT_TINT)):
            col, op = ("#000", 1) if mono else (tint, alpha)
            arcs += f'<path d="{arc_path(r, side)}" fill="none" stroke="{col}" stroke-opacity="{op}" stroke-width="{ARC_WIDTH}" stroke-linecap="round"/>'
    if mono:
        bead = f'<circle cx="{CX}" cy="{CY}" r="{BEAD_R}" fill="#000"/>'
        return arcs + bead
    bead = (f'<circle cx="{CX}" cy="{CY}" r="21" fill="url(#glow)"/>' + arcs +
            f'<circle cx="{CX}" cy="{CY}" r="{BEAD_R}" fill="url(#bead)"/>'
            f'<circle cx="{CX}" cy="{CY}" r="{BEAD_R-0.6}" fill="none" stroke="#fff" stroke-opacity=".8" stroke-width="1.2"/>'
            f'<circle cx="{CX-4.2}" cy="{CY-5}" r="3.2" fill="#fff" fill-opacity=".9"/>')
    return bead

DEFS = f'''<defs>
<linearGradient id="bg" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="108" y2="108"><stop offset="0" stop-color="#3B2A9E"/><stop offset=".55" stop-color="#1A1250"/><stop offset="1" stop-color="#0C1A3F"/></linearGradient>
<radialGradient id="pink" gradientUnits="userSpaceOnUse" cx="86" cy="24" r="62"><stop offset="0" stop-color="#FF4FA8" stop-opacity=".78"/><stop offset="1" stop-color="#FF4FA8" stop-opacity="0"/></radialGradient>
<radialGradient id="cyan" gradientUnits="userSpaceOnUse" cx="20" cy="92" r="62"><stop offset="0" stop-color="#2BD9FF" stop-opacity=".70"/><stop offset="1" stop-color="#2BD9FF" stop-opacity="0"/></radialGradient>
<linearGradient id="gloss" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="54"><stop offset="0" stop-color="#fff" stop-opacity=".18"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
<radialGradient id="glow" gradientUnits="userSpaceOnUse" cx="{CX}" cy="{CY}" r="21"><stop offset="0" stop-color="#fff" stop-opacity=".35"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
<radialGradient id="bead" gradientUnits="userSpaceOnUse" cx="{CX-4}" cy="{CY-5}" r="17"><stop offset="0" stop-color="#fff"/><stop offset=".45" stop-color="#BDEBFF"/><stop offset="1" stop-color="#7C8CFF"/></radialGradient>
</defs>'''

BG = ('<rect width="108" height="108" fill="url(#bg)"/>'
      f'<circle cx="86" cy="24" r="62" fill="url(#pink)"/><circle cx="20" cy="92" r="62" fill="url(#cyan)"/>'
      '<rect width="108" height="54" fill="url(#gloss)"/>')

def icon_group(mono=False):
    return svg_preview(mono)

if __name__ == "__main__":
    out = pathlib.Path(sys.argv[1])
    (out / "ic_launcher_background.xml").write_text(background_xml())
    (out / "ic_launcher_foreground.xml").write_text(foreground_xml())
    (out / "ic_launcher_monochrome.xml").write_text(monochrome_xml())
    # squircle-ish mask (One UI style) for the preview
    sq = "M54,0 C92,0 108,16 108,54 C108,92 92,108 54,108 C16,108 0,92 0,54 C0,16 16,0 54,0 Z"
    def tile(mono):
        if mono:
            return f'<g transform="translate(0,0)"><clipPath id="m"><path d="{sq}"/></clipPath><g clip-path="url(#m)"><rect width="108" height="108" fill="#2b2b33"/><g>{svg_preview(True).replace("#000","#E8E4FF")}</g></g></g>'
        return f'<clipPath id="c"><path d="{sq}"/></clipPath><g clip-path="url(#c)">{BG}{svg_preview(False)}</g>'
    html = f'''<html><body style="margin:0;background:#101018">
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="520" viewBox="0 0 1200 520">
<rect width="1200" height="520" fill="#14141f"/>{DEFS}
<g transform="translate(40,40) scale(3.6)">{tile(False)}</g>
<g transform="translate(470,40) scale(1.9)">{tile(False)}</g>
<g transform="translate(700,40) scale(1.0)">{tile(False)}</g>
<g transform="translate(820,40) scale(0.55)">{tile(False)}</g>
<g transform="translate(470,270) scale(1.9)"><clipPath id="m"><path d="{sq}"/></clipPath><g clip-path="url(#m)"><rect width="108" height="108" fill="#2b2b33"/>{svg_preview(True).replace('#000','#E8E4FF')}</g></g>
<text x="470" y="505" fill="#9a9ab0" font-family="sans-serif" font-size="16">themed (monochrome)</text>
<text x="700" y="170" fill="#9a9ab0" font-family="sans-serif" font-size="16">small sizes</text>
</svg></body></html>'''
    if len(sys.argv) > 2:
        pathlib.Path(sys.argv[2]).write_text(html)
