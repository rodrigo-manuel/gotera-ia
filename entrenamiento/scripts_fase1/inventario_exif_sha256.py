import csv
import hashlib
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from PIL import Image

RAIZ = Path(r"C:\Users\cliente\Music\ENTREGABLE_PROYECTO_GRADO")
BASE = RAIZ / "01_DATASET" / "dataset_modelo"
SALIDA = RAIZ / "02_MODELO" / "auditoria_exif_completa"
EXTENSIONES = {".jpg", ".jpeg", ".png"}

def sha256(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as archivo:
        for bloque in iter(lambda: archivo.read(1024 * 1024), b""):
            h.update(bloque)
    return h.hexdigest()

def metadatos(ruta):
    with Image.open(ruta) as im:
        ancho, alto = im.size
        exif = im.getexif()
        try:
            sub = exif.get_ifd(0x8769)
        except Exception:
            sub = {}
    fecha = str(sub.get(0x9003, "")).strip().strip("\x00")
    return {
        "ancho": ancho,
        "alto": alto,
        "exif_fecha_original": fecha,
        "exif_dia": fecha[:10].replace(":", "-") if fecha else "",
        "exif_fabricante": str(exif.get(0x010F, "")).strip(),
        "exif_modelo": str(exif.get(0x0110, "")).strip(),
        "exif_software": str(exif.get(0x0131, "")).strip(),
        "exif_orientacion": str(exif.get(0x0112, "")).strip(),
    }

def main():
    if not BASE.is_dir():
        raise SystemExit(f"No existe la carpeta: {BASE}")

    SALIDA.mkdir(parents=True, exist_ok=True)
    sello = datetime.now().strftime("%Y%m%d_%H%M%S")
    archivos = sorted(
        p for p in BASE.rglob("*")
        if p.is_file() and p.suffix.lower() in EXTENSIONES
    )
    print("Imágenes encontradas:", len(archivos))

    campos = [
        "particion", "clase", "archivo", "ruta_relativa", "bytes",
        "ancho", "alto", "exif_fecha_original", "exif_dia",
        "exif_fabricante", "exif_modelo", "exif_software",
        "exif_orientacion", "sha256", "error"
    ]
    filas = []

    for i, ruta in enumerate(archivos, 1):
        partes = ruta.relative_to(BASE).parts
        fila = {
            "particion": partes[0] if len(partes) > 0 else "",
            "clase": partes[1] if len(partes) > 1 else "",
            "archivo": ruta.name,
            "ruta_relativa": "/".join(partes),
            "bytes": ruta.stat().st_size,
            "sha256": "",
            "error": "",
        }
        try:
            fila.update(metadatos(ruta))
            fila["sha256"] = sha256(ruta)
        except Exception as exc:
            fila["error"] = repr(exc)
        filas.append(fila)

        if i % 100 == 0 or i == len(archivos):
            print(f"Procesadas {i}/{len(archivos)}")

    ruta_csv = SALIDA / f"inventario_{sello}.csv"
    with open(ruta_csv, "w", newline="", encoding="utf-8-sig") as archivo:
        escritor = csv.DictWriter(archivo, fieldnames=campos)
        escritor.writeheader()
        escritor.writerows(filas)

    resumen = [
        f"Imágenes encontradas: {len(filas)}",
        f"Errores de lectura: {sum(bool(f['error']) for f in filas)}",
    ]

    conteos = Counter(
        (f["particion"], f["clase"], f.get("exif_dia", ""),
         f.get("exif_modelo", ""))
        for f in filas
    )
    resumen.append("\nConteo por partición, clase, día EXIF y modelo:")
    for clave, cantidad in sorted(conteos.items()):
        resumen.append(f"{clave}: {cantidad}")

    grupos = defaultdict(list)
    for fila in filas:
        if fila["sha256"]:
            grupos[fila["sha256"]].append(fila)

    duplicados = {h: g for h, g in grupos.items() if len(g) > 1}
    resumen.append(f"\nGrupos con SHA-256 duplicado: {len(duplicados)}")
    for hash_, grupo in duplicados.items():
        resumen.append(f"\n{hash_}")
        for fila in grupo:
            resumen.append(f"  {fila['ruta_relativa']}")

    ruta_txt = SALIDA / f"resumen_{sello}.txt"
    ruta_txt.write_text("\n".join(resumen), encoding="utf-8")

    print("\nCSV:", ruta_csv)
    print("Resumen:", ruta_txt)
    print("\nAuditoría finalizada. No se modificaron las imágenes.")

if __name__ == "__main__":
    main()
