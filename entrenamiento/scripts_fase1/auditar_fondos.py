
from pathlib import Path
from PIL import Image
import numpy as np
import csv

BASE = Path("01_DATASET/original")
CLASES = ["ojo_de_gallo", "sana"]

# Tamaño de cada esquina que se analizará.
TAMANO = 0.10  # 10 % del ancho y alto

salida = Path("02_MODELO/resumen_fondos.csv")
resultados = []

for clase in CLASES:
    carpeta = BASE / clase
    archivos = sorted(
        p for p in carpeta.iterdir()
        if p.suffix.lower() in {".jpg", ".jpeg", ".png"}
    )

    print(f"\nAnalizando clase: {clase} ({len(archivos)} imágenes)")

    for archivo in archivos:
        try:
            with Image.open(archivo) as imagen:
                imagen = imagen.convert("RGB")
                ancho, alto = imagen.size

                w = max(1, int(ancho * TAMANO))
                h = max(1, int(alto * TAMANO))

                esquinas = [
                    imagen.crop((0, 0, w, h)),
                    imagen.crop((ancho - w, 0, ancho, h)),
                    imagen.crop((0, alto - h, w, alto)),
                    imagen.crop((ancho - w, alto - h, ancho, alto)),
                ]

                pixeles = np.concatenate([
                    np.asarray(e, dtype=np.float32).reshape(-1, 3)
                    for e in esquinas
                ])

                rgb = pixeles.mean(axis=0)
                brillo = float(rgb.mean())
                saturacion_aprox = float(
                    (pixeles.max(axis=1) - pixeles.min(axis=1)).mean()
                )

                resultados.append({
                    "clase": clase,
                    "imagen": archivo.name,
                    "ancho": ancho,
                    "alto": alto,
                    "rojo_esquinas": round(float(rgb[0]), 2),
                    "verde_esquinas": round(float(rgb[1]), 2),
                    "azul_esquinas": round(float(rgb[2]), 2),
                    "brillo_esquinas": round(brillo, 2),
                    "saturacion_aprox_esquinas": round(
                        saturacion_aprox, 2
                    ),
                })

        except Exception as e:
            print(f"Error en {archivo.name}: {e}")

campos = [
    "clase", "imagen", "ancho", "alto",
    "rojo_esquinas", "verde_esquinas", "azul_esquinas",
    "brillo_esquinas", "saturacion_aprox_esquinas"
]

salida.parent.mkdir(parents=True, exist_ok=True)

with salida.open("w", newline="", encoding="utf-8-sig") as f:
    escritor = csv.DictWriter(f, fieldnames=campos)
    escritor.writeheader()
    escritor.writerows(resultados)

print("\nRESUMEN POR CLASE")

for clase in CLASES:
    filas = [r for r in resultados if r["clase"] == clase]
    print(f"\n{clase}: {len(filas)} imágenes")

    for campo in [
        "rojo_esquinas",
        "verde_esquinas",
        "azul_esquinas",
        "brillo_esquinas",
        "saturacion_aprox_esquinas"
    ]:
        valores = [r[campo] for r in filas]
        if valores:
            print(
                f"  {campo}: media={np.mean(valores):.2f}; "
                f"mín={np.min(valores):.2f}; "
                f"máx={np.max(valores):.2f}"
            )

print(f"\nArchivo guardado: {salida.resolve()}")