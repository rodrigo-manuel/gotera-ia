
from pathlib import Path
from collections import Counter
from PIL import Image
from PIL.ExifTags import TAGS

proyecto = Path(__file__).resolve().parent.parent
carpeta_original = proyecto / "01_DATASET" / "original"
extensiones = {".jpg", ".jpeg", ".png", ".webp"}

campos = [
    "Make",
    "Model",
    "DateTime",
    "DateTimeOriginal",
    "Software",
    "Orientation",
]

for clase_dir in sorted(carpeta_original.iterdir()):
    if not clase_dir.is_dir():
        continue

    archivos = [
        p for p in clase_dir.rglob("*")
        if p.is_file() and p.suffix.lower() in extensiones
    ]

    resumen = {campo: Counter() for campo in campos}
    con_exif = 0
    errores = 0

    for ruta in archivos:
        try:
            with Image.open(ruta) as imagen:
                exif = imagen.getexif()

                if exif:
                    con_exif += 1

                datos = {
                    TAGS.get(etiqueta, str(etiqueta)): valor
                    for etiqueta, valor in exif.items()
                }

                for campo in campos:
                    valor = datos.get(campo)
                    if valor is not None:
                        resumen[campo][str(valor)] += 1

        except Exception:
            errores += 1

    print(f"\n{'=' * 50}")
    print(f"CLASE: {clase_dir.name}")
    print(f"Archivos: {len(archivos)}")
    print(f"Archivos con EXIF: {con_exif}")
    print(f"Errores: {errores}")

    for campo, conteo in resumen.items():
        print(f"\n{campo}:")
        if conteo:
            for valor, cantidad in conteo.most_common(10):
                print(f"  {valor}: {cantidad}")
        else:
            print("  Sin datos")