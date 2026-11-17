#!/usr/bin/env python3
"""Check the published Java8 reactor against its compatibility BOM."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
seen = set()
artifacts = set()

def visit(path):
    path = path.resolve()
    if path in seen:
        return
    seen.add(path)
    pom = ET.parse(path).getroot()
    if pom.findtext("m:packaging", "jar", ns) != "pom":
        artifacts.add(pom.findtext("m:artifactId", namespaces=ns))
    for module in pom.findall("m:modules/m:module", ns):
        visit(path.parent / module.text.strip() / "pom.xml")

visit(root / "pom.xml")
bom = ET.parse(root / "agentscope-distribution/agentscope-bom/pom.xml").getroot()
managed = {
    dependency.findtext("m:artifactId", namespaces=ns)
    for dependency in bom.findall("m:dependencyManagement/m:dependencies/m:dependency", ns)
    if dependency.findtext("m:groupId", namespaces=ns) == "cn.iocoder.agentscope"
    and dependency.findtext("m:type", "jar", ns) != "pom"
}
missing = sorted(artifacts - managed)
extra = sorted(managed - artifacts)
if missing or extra:
    raise SystemExit("Java8 BOM mismatch: missing=" + repr(missing) + ", inactive=" + repr(extra))
print("Java8 BOM manages all " + str(len(artifacts)) + " active runtime modules.")
print("agentscope-all is outside this Java8 reactor; no shaded distribution is advertised.")
