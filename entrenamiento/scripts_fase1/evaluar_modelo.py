
from pathlib import Path
import json
import csv
import numpy as np
import tensorflow as tf

# ============================================================
# CONFIGURACIÓN
# ============================================================

PROYECTO = Path(__file__).resolve().parent.parent
CARPETA_TEST = PROYECTO / "01_DATASET" / "dataset_modelo" / "test"
RUTA_MODELO = PROYECTO / "02_MODELO" / "mejor_modelo.keras"
CARPETA_SALIDA = PROYECTO / "02_MODELO"

TAMANO_IMAGEN = (224, 224)
TAMANO_LOTE = 16
UMBRAL = 0.5

# Las clases se ordenan alfabéticamente.
# 0 = ojo_de_gallo
# 1 = sana
CLASES = ["ojo_de_gallo", "sana"]


def main():
    print("=" * 65)
    print("EVALUACIÓN DEL MODELO CON EL CONJUNTO DE PRUEBA")
    print("=" * 65)

    if not RUTA_MODELO.exists():
        raise FileNotFoundError(f"No se encontró el modelo: {RUTA_MODELO}")

    if not CARPETA_TEST.exists():
        raise FileNotFoundError(f"No se encontró el conjunto de prueba: {CARPETA_TEST}")

    # Cargar el modelo guardado sin volver a entrenarlo.
    print("\nCargando modelo...")
    modelo = tf.keras.models.load_model(RUTA_MODELO)

    # Cargar las imágenes sin mezclarlas para conservar su orden.
    print("Cargando imágenes de prueba...")
    dataset = tf.keras.utils.image_dataset_from_directory(
        CARPETA_TEST,
        labels="inferred",
        label_mode="binary",
        class_names=CLASES,
        color_mode="rgb",
        image_size=TAMANO_IMAGEN,
        batch_size=TAMANO_LOTE,
        shuffle=False
    )

    etiquetas_reales = np.concatenate([
        etiquetas.numpy().reshape(-1).astype(int)
        for _, etiquetas in dataset
    ])

    # El modelo entrega la probabilidad de la clase "sana".
    probabilidades_sanas = modelo.predict(dataset, verbose=1).reshape(-1)

    predicciones = (probabilidades_sanas >= UMBRAL).astype(int)

    # Matriz de confusión:
    # filas = clase real; columnas = clase predicha.
    matriz = np.zeros((2, 2), dtype=int)
    for real, predicha in zip(etiquetas_reales, predicciones):
        matriz[real, predicha] += 1

    total = len(etiquetas_reales)
    aciertos = int(np.sum(etiquetas_reales == predicciones))
    exactitud = aciertos / total if total else 0.0

    resultados_clases = {}

    for indice, nombre in enumerate(CLASES):
        verdaderos_positivos = int(np.sum(
            (etiquetas_reales == indice) & (predicciones == indice)
        ))
        falsos_positivos = int(np.sum(
            (etiquetas_reales != indice) & (predicciones == indice)
        ))
        falsos_negativos = int(np.sum(
            (etiquetas_reales == indice) & (predicciones != indice)
        ))

        precision = (
            verdaderos_positivos / (verdaderos_positivos + falsos_positivos)
            if verdaderos_positivos + falsos_positivos else 0.0
        )
        recall = (
            verdaderos_positivos / (verdaderos_positivos + falsos_negativos)
            if verdaderos_positivos + falsos_negativos else 0.0
        )
        f1 = (
            2 * precision * recall / (precision + recall)
            if precision + recall else 0.0
        )

        resultados_clases[nombre] = {
            "precision": precision,
            "recall": recall,
            "f1_score": f1,
            "soporte": int(np.sum(etiquetas_reales == indice))
        }

    print("\n" + "=" * 65)
    print("RESULTADOS DEL CONJUNTO DE PRUEBA")
    print("=" * 65)
    print(f"Total de imágenes: {total}")
    print(f"Aciertos: {aciertos}")
    print(f"Errores: {total - aciertos}")
    print(f"Exactitud (accuracy): {exactitud:.4f} ({exactitud * 100:.2f}%)")

    print("\nMATRIZ DE CONFUSIÓN")
    print("Filas = clase real; columnas = clase predicha")
    print(f"{'':20s}{'Pred. ojo gallo':>18s}{'Pred. sana':>14s}")
    for i, nombre in enumerate(CLASES):
        print(f"{nombre:20s}{matriz[i, 0]:18d}{matriz[i, 1]:14d}")

    print("\nMÉTRICAS POR CLASE")
    for nombre, metricas in resultados_clases.items():
        print(f"\nClase: {nombre}")
        print(f"  Precisión: {metricas['precision']:.4f}")
        print(f"  Sensibilidad (recall): {metricas['recall']:.4f}")
        print(f"  F1-score: {metricas['f1_score']:.4f}")
        print(f"  Imágenes reales: {metricas['soporte']}")

    # Guardar informe JSON.
    informe = {
        "conjunto": "test",
        "total_imagenes": total,
        "aciertos": aciertos,
        "errores": total - aciertos,
        "umbral": UMBRAL,
        "clases": CLASES,
        "exactitud": exactitud,
        "matriz_confusion": matriz.tolist(),
        "metricas_por_clase": resultados_clases,
        "nota": (
            "Evaluación sobre el conjunto de prueba reservado. "
            "Estos resultados no demuestran por sí solos la generalización "
            "a imágenes de campo de Minca."
        )
    }

    ruta_json = CARPETA_SALIDA / "evaluacion_test.json"
    with open(ruta_json, "w", encoding="utf-8") as archivo:
        json.dump(informe, archivo, indent=4, ensure_ascii=False)

    # Guardar matriz de confusión CSV.
    ruta_csv = CARPETA_SALIDA / "matriz_confusion.csv"
    with open(ruta_csv, "w", newline="", encoding="utf-8-sig") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["Real / Predicha", *CLASES])
        for nombre, fila in zip(CLASES, matriz):
            escritor.writerow([nombre, *fila.tolist()])

    # Guardar predicciones individuales para poder revisarlas.
    ruta_predicciones = CARPETA_SALIDA / "predicciones_test.csv"
    with open(ruta_predicciones, "w", newline="", encoding="utf-8-sig") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow([
            "ruta_imagen",
            "clase_real",
            "clase_predicha",
            "probabilidad_sana",
            "probabilidad_ojo_de_gallo",
            "correcta"
        ])

        indice_global = 0
        for rutas, _ in dataset:
            # Las rutas no se incluyen en el tensor; se escriben abajo
            # recorriendo la lista ordenada de archivos.
            pass

        archivos_imagen = sorted(
            p for p in CARPETA_TEST.rglob("*")
            if p.is_file() and p.suffix.lower() in
            {".jpg", ".jpeg", ".png", ".bmp", ".gif"}
        )

        for i, ruta in enumerate(archivos_imagen):
            real = int(etiquetas_reales[i])
            predicha = int(predicciones[i])
            p_sana = float(probabilidades_sanas[i])
            escritor.writerow([
                str(ruta.relative_to(PROYECTO)),
                CLASES[real],
                CLASES[predicha],
                round(p_sana, 6),
                round(1.0 - p_sana, 6),
                real == predicha
            ])

    print("\nArchivos guardados:")
    print(f"- {ruta_json}")
    print(f"- {ruta_csv}")
    print(f"- {ruta_predicciones}")
    print("\nEvaluación terminada. El modelo no fue reentrenado.")


if __name__ == "__main__":
    main()