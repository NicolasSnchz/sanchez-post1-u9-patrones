"""Arma paginas HTML con la salida real de mvn verify y los reportes de Surefire para capturarlas."""
import html
import re
import xml.etree.ElementTree as ET
from pathlib import Path

SALIDA = Path("evidencias")
log = (SALIDA / "mvn-verify.log").read_text(encoding="utf-8", errors="replace").splitlines()

ESTILO = """<style>
body{margin:0;background:#0d1117;font:14px/1.5 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;color:#c9d1d9}
.barra{background:#161b22;padding:10px 18px;color:#8b949e;border-bottom:1px solid #30363d;font-family:system-ui,sans-serif}
pre{margin:0;padding:18px;white-space:pre-wrap}
.ok{color:#3fb950}.info{color:#58a6ff}.exito{color:#3fb950;font-weight:bold}
table{border-collapse:collapse;margin:18px;font-family:system-ui,sans-serif;color:#c9d1d9}
td,th{padding:4px 14px;text-align:left;border-bottom:1px solid #21262d}
h2{font-family:system-ui,sans-serif;margin:18px 18px 0;font-size:16px;color:#e6edf3}
</style>"""


def colorear(linea):
    texto = html.escape(linea)
    if "BUILD SUCCESS" in linea or "Started Recordatorios" in linea:
        return f'<span class="exito">{texto}</span>'
    if re.search(r"Tests run: \d+, Failures: 0, Errors: 0", linea):
        return f'<span class="ok">{texto}</span>'
    return texto


elegidas = []
for i, linea in enumerate(log):
    if " -- in com.clinica" in linea or "Running com.clinica" in linea:
        elegidas.append(linea)
inicio = next(i for i, l in enumerate(log) if l.strip() == "[INFO] Results:")
fin = next(i for i, l in enumerate(log) if "Finished at" in l)
elegidas += [""] + log[inicio:fin + 1]
titulo = "mvn -B verify --no-transfer-progress  (GitHub Actions, ubuntu-24.04, JDK 21)"
(SALIDA / "salida-mvn-verify.html").write_text(
    f"<!doctype html><meta charset='utf-8'>{ESTILO}<div class='barra'>{html.escape(titulo)}</div>"
    f"<pre>{chr(10).join(colorear(l) for l in elegidas)}</pre>", encoding="utf-8")

arranque = SALIDA / "arranque.log"
if arranque.exists():
    claves = ("Starting RecordatoriosCitasServiceApplication", "H2 console", "HikariPool-1 - Start completed",
              "Tomcat started", "Started RecordatoriosCitasServiceApplication")
    lineas = [l for l in arranque.read_text(encoding="utf-8", errors="replace").splitlines()
              if any(c in l for c in claves)]
    (SALIDA / "arranque.html").write_text(
        f"<!doctype html><meta charset='utf-8'>{ESTILO}<div class='barra'>mvn spring-boot:run  (GitHub Actions, base H2 vacia)</div><pre>{chr(10).join(colorear(l) for l in lineas)}</pre>", encoding="utf-8")

filas = []
for reporte in sorted(Path("target/surefire-reports").glob("TEST-*.xml")):
    raiz = ET.parse(reporte).getroot()
    clase = raiz.get("name").rsplit(".", 1)[-1]
    for caso in raiz.iter("testcase"):
        fallo = caso.find("failure") is not None or caso.find("error") is not None
        marca = "<span style='color:#f85149'>FALLA</span>" if fallo else "<span class='ok'>&#10003; verde</span>"
        filas.append(f"<tr><td>{html.escape(clase)}</td><td><code>{html.escape(caso.get('name'))}</code></td>"
                     f"<td>{marca}</td><td>{float(caso.get('time', 0)):.3f} s</td></tr>")
(SALIDA / "pruebas-por-metodo.html").write_text(
    f"<!doctype html><meta charset='utf-8'>{ESTILO}<h2>Pruebas ejecutadas por Surefire ({len(filas)} casos)</h2>"
    f"<table><tr><th>Clase</th><th>Prueba</th><th>Resultado</th><th>Tiempo</th></tr>{''.join(filas)}</table>",
    encoding="utf-8")
print("Paginas listas:", len(elegidas), "lineas de log y", len(filas), "casos")
