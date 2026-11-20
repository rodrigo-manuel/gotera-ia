from pathlib import Path
import random
import shutil

# ============================================================
# CONFIGURACIÓN
# ============================================================

# IMÁGENES ORIGINALES
ORIGEN_OJO = Path(r"C:\Users\cliente\Music\Ojo_Gallo")
ORIGEN_SANA = Path(r"C:\Users\cliente\Music\Sanas")

# CARPETA DEL ENTREGABLE
PROYECTO = Path(__file__).resolve().parent

DATASET = PROYECTO / "01_DATASET"
ORIGINAL = DATASET / "original"
MODELO = DATASET / "dataset_modelo"

# Semilla fija para que la división sea reproducible
SEED = 42

# Proporciones
TRAIN = 0.70
VALIDATION = 0.15
TEST = 0.15

EXTENSIONES = {".jpg", ".jpeg", ".png", ".webp"}

# ============================================================
# CREAR CARPETAS
# ============================================================

print("=" * 70)
print("PREPARACIÓN DEL DATASET DEL PROYECTO")
print("=" * 70)

for carpeta in [
    PROYECTO,
    DATASET,
    ORIGINAL,
    MODELO
]:
    carpeta.mkdir(parents=True, exist_ok=True)

# Crear estructura del dataset
for conjunto in ["train", "validation", "test"]:

    for clase in ["sana", "ojo_de_gallo"]:

        carpeta = MODELO / conjunto / clase

        carpeta.mkdir(
            parents=True,
            exist_ok=True
        )

# ============================================================
# FUNCIÓN PARA OBTENER IMÁGENES
# ============================================================

def obtener_imagenes(carpeta):

    return sorted([
        archivo
        for archivo in carpeta.iterdir()
        if archivo.is_file()
        and archivo.suffix.lower() in EXTENSIONES
    ])


# ============================================================
# COPIAR ORIGINALES
# ============================================================

print("\nCopiando originales...")

fuentes = {
    "sana": ORIGEN_SANA,
    "ojo_de_gallo": ORIGEN_OJO
}

imagenes_por_clase = {}

for clase, origen in fuentes.items():

    imagenes = obtener_imagenes(origen)

    imagenes_por_clase[clase] = imagenes

    destino = ORIGINAL / clase
    destino.mkdir(parents=True, exist_ok=True)

    print(
        f"\n{clase}: {len(imagenes)} imágenes"
    )

    for imagen in imagenes:

        shutil.copy2(
            imagen,
            destino / imagen.name
        )

# ============================================================
# DIVISIÓN ESTRATIFICADA
# ============================================================

print("\n" + "=" * 70)
print("DIVISIÓN DEL DATASET")
print("=" * 70)

random.seed(SEED)

resumen = {}

for clase, imagenes in imagenes_por_clase.items():

    imagenes = imagenes.copy()

    random.shuffle(imagenes)

    total = len(imagenes)

    n_train = int(total * TRAIN)
    n_validation = int(total * VALIDATION)

    train = imagenes[:n_train]

    validation = imagenes[
        n_train:n_train + n_validation
    ]

    test = imagenes[
        n_train + n_validation:
    ]

    conjuntos = {
        "train": train,
        "validation": validation,
        "test": test
    }

    resumen[clase] = {}

    for nombre_conjunto, archivos in conjuntos.items():

        destino = (
            MODELO
            / nombre_conjunto
            / clase
        )

        for archivo in archivos:

            shutil.copy2(
                archivo,
                destino / archivo.name
            )

        resumen[clase][nombre_conjunto] = len(archivos)

# ============================================================
# MOSTRAR RESULTADOS
# ============================================================

print("\nRESULTADO FINAL")
print("-" * 70)

for clase in ["sana", "ojo_de_gallo"]:

    print(f"\n{clase.upper()}")

    for conjunto in [
        "train",
        "validation",
        "test"
    ]:

        cantidad = resumen[clase][conjunto]

        print(
            f"  {conjunto}: {cantidad}"
        )

# ============================================================
# TOTALES
# ============================================================

print("\n" + "-" * 70)

for conjunto in [
    "train",
    "validation",
    "test"
]:

    total = sum(
        resumen[clase][conjunto]
        for clase in resumen
    )

    print(
        f"TOTAL {conjunto.upper()}: {total}"
    )

total_general = sum(
    sum(resumen[clase].values())
    for clase in resumen
)

print(
    f"\nTOTAL GENERAL: {total_general}"
)

# ============================================================
# INFORME
# ============================================================

informe = DATASET / "informe_division_dataset.txt"

with open(
    informe,
    "w",
    encoding="utf-8"
) as archivo:

    archivo.write(
        "INFORME DE DIVISIÓN DEL DATASET\n"
    )

    archivo.write("=" * 70 + "\n\n")

    archivo.write(
        f"Semilla utilizada: {SEED}\n"
    )

    archivo.write(
        "Proporciones: 70% train / 15% validation / 15% test\n\n"
    )

    for clase in [
        "sana",
        "ojo_de_gallo"
    ]:

        archivo.write(
            f"{clase}\n"
        )

        for conjunto in [
            "train",
            "validation",
            "test"
        ]:

            archivo.write(
                f"  {conjunto}: "
                f"{resumen[clase][conjunto]}\n"
            )

        archivo.write("\n")

    archivo.write(
        f"Total general: {total_general}\n"
    )

print("\n" + "=" * 70)
print("TERMINADO")
print("=" * 70)

print("\nDataset creado en:")
print(MODELO)

print("\nInforme:")
print(informe)

print("\nIMPORTANTE:")
print("Los originales de Música NO fueron modificados.")