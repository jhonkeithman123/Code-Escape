import re, sys, xml.etree.ElementTree as ET

path = sys.argv[1]
tree = ET.parse(path)
root = tree.getroot()
print("rotation:", root.attrib.get("rotation"))
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    desc = node.attrib.get("content-desc", "")
    cls = node.attrib.get("class", "")
    b = node.attrib.get("bounds", "")
    clickable = node.attrib.get("clickable", "")
    cls_short = cls.replace("android.widget.", "").replace("android.view.", "").replace("androidx.compose.ui.platform.", "")
    if text or desc or clickable == "true":
        print(f"{cls_short:24s} text={text!r} desc={desc!r} bounds={b} clickable={clickable}")
