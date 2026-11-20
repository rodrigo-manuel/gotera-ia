
from pathlib import Path
from collections import defaultdict
from PIL import Image
import imagehash
import csv

# Raíz del proyecto
PROYECTO = Path(__file__).resolve().parent.parent
DATASET = PROYECTO / "01_DATASET" / "dataset_modelo"
SALIDA = PROYECTO / "02_MODELO" / "posibles_similares.csv"

PARTICIONES = ["train", "validation", "test"]
EXTENSIONES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}

# Distancia de Hamming entre huellas perceptuales.
# Un valor <= 6 marca candidatos para inspección; no confirma duplicados.
UMBRAL_DISTANCIA = 6


def obtener_imagenes():
    archivos = []
    for particion in PARTICIONES:
        carpeta = DATASET / particion
        for ruta in sorted(carpeta.rglob("*")):
            if ruta.is_file() and ruta.suffix.lower() in EXTENSIONES:
                archivos.append((particion, ruta))
    return archivos


def main():
    print("=" * 65)
    print("BÚSQUEDA DE IMÁGENES VISUALMENTE SIMILARES")
    print("=" * 65)

    archivos = obtener_imagenes()
    print(f"Imágenes encontradas: {len(archivos)}")

    huellas = []
    errores = []

    for numero, (particion, ruta) in enumerate(archivos, start=1):
        try:
            with Image.open(ruta) as imagen:
                huella = imagehash.phash(imagen.convert("RGB"))
            huellas.append((particion, ruta, huella))
        except Exception as error:
            errores.append((str(ruta), str(error)))

        if numero % 100 == 0 or numero == len(archivos):
            print(f"Procesadas: {numero}/{len(archivos)}")

    # Comparar solo imágenes de particiones diferentes.
    candidatos = []
    for i in range(len(huellas)):
        particion_a, ruta_a, huella_a = huellas[i]

        for j in range(i + 1, len(huellas)):
            particion_b, ruta_b, huella_b = huellas[j]

            if particion_a == particion_b:
                continue

            distancia = huella_a - huella_b
            if distancia <= UMBRAL_DISTANCIA:
                candidatos.append(
                    (distancia, particion_a, ruta_a,
                     particion_b, ruta_b)
                )

    candidatos.sort(key=lambda fila: fila[0])

    with open(SALIDA, "w", newline="", encoding="utf-8-sig") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow([
            "distancia_hash",
            "particion_1",
            "imagen_1",
            "particion_2",
            "imagen_2"
        ])

        for distancia, p1, r1, p2, r2 in candidatos:
            escritor.writerow([
                distancia,
                p1,
                str(r1.relative_to(PROYECTO)),
                p2,
                str(r2.relative_to(PROYECTO))
            ])

    print("\n" + "=" * 65)
    print("RESULTADO")
    print("=" * 65)
    print(f"Imágenes con huella calculada: {len(huellas)}")
    print(f"Posibles pares similares: {len(candidatos)}")
    print(f"Errores de lectura: {len(errores)}")
    print(f"Informe guardado en: {SALIDA}")

    if candidatos:
        print("\nPrimeros 15 candidatos para inspección:")
        for distancia, p1, r1, p2, r2 in candidatos[:15]:
            print(f"\nDistancia: {distancia}")
            print(f"  {p1}: {r1.name}")
            print(f"  {p2}: {r2.name}")

    if errores:
        print("\nPrimeros errores de lectura:")
        for ruta, error in errores[:5]:
            print(f"- {ruta}: {error}")

    print(
        "\nNota: una distancia pequeña indica similitud visual posible, "
        "no confirma que sean duplicados. Hay que inspeccionar los candidatos."
    )


if __name__ == "__main__":
    main()