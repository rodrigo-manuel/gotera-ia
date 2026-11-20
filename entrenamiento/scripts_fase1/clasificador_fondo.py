
# -*- coding: utf-8 -*-
"""
PRUEBA DE CONTROL: clasificador que utiliza solamente estadisticas
del color de las esquinas de las imagenes.

Pregunta:
¿Las estadisticas de las esquinas permiten distinguir las clases
ojo_de_gallo y sana mejor que un clasificador trivial?

Requisitos:
- Python y NumPy.
- Archivo 02_MODELO/resumen_fondos.csv.
- No modifica imagenes, particiones ni el modelo MobileNet.
- Genera tres archivos de resultados en 02_MODELO.
"""

import csv
import json
import math
import sys
from pathlib import Path

try:
    import numpy as np
except ImportError:
    sys.exit(
        "ERROR: NumPy no esta disponible. Activa el entorno .venv."
    )

SEED = 42

BASE_DIR = Path(__file__).resolve().parent
CSV_PATH = BASE_DIR / "resumen_fondos.csv"
DATASET_DIR = (
    BASE_DIR.parent / "01_DATASET" / "dataset_modelo"
)

OUT_JSON = BASE_DIR / "resultados_clasificador_fondo.json"
OUT_PRED = BASE_DIR / "predicciones_prueba_fondo.csv"
OUT_PART = BASE_DIR / "particion_usada_fondo.csv"

FEATURES = [
    "rojo_esquinas",
    "verde_esquinas",
    "azul_esquinas",
    "brillo_esquinas",
    "saturacion_aprox_esquinas",
]

POS = "ojo_de_gallo"
NEG = "sana"

EXPECTED_COUNTS = {POS: 500, NEG: 499}
EXPECTED_TOTAL = 999

# Tamaños por clase: ojo de gallo, sana.
SPLIT_SIZES = {
    "train": (350, 349),
    "val": (75, 74),
    "test": (75, 76),
}

LAMBDA_GRID = [0.01, 0.1, 1.0, 10.0, 100.0]
N_PERM = 2000
CV_REPETITIONS = 5
CV_FOLDS = 5


def heading(text):
    print("\n" + "=" * 72)
    print(text)
    print("=" * 72)


def check(label, ok, detail=""):
    print(
        f"  [{'OK' if ok else 'FALLO'}] {label}"
        + (f": {detail}" if detail else "")
    )
    return bool(ok)


def json_safe(value):
    if isinstance(value, dict):
        return {str(k): json_safe(v) for k, v in value.items()}
    if isinstance(value, (list, tuple)):
        return [json_safe(v) for v in value]
    if isinstance(value, np.ndarray):
        return json_safe(value.tolist())
    if isinstance(value, np.integer):
        return int(value)
    if isinstance(value, (np.floating, float)):
        v = float(value)
        return v if math.isfinite(v) else None
    return value


def ratio(a, b):
    return float(a) / b if b else float("nan")


def fmt(value):
    if value is None or not math.isfinite(float(value)):
        return "N/A"
    return f"{float(value):.4f}"


def load_csv():
    heading("1. VALIDACION DEL CSV")

    if not CSV_PATH.exists():
        sys.exit(
            f"ERROR: No se encontro el archivo:\n{CSV_PATH}\n"
            "Ejecuta primero auditar_fondos.py."
        )

    try:
        with CSV_PATH.open(
            "r", newline="", encoding="utf-8-sig"
        ) as file:
            reader = csv.DictReader(file)
            columns = reader.fieldnames or []
            rows = list(reader)
    except Exception as exc:
        sys.exit(f"ERROR leyendo el CSV: {exc}")

    required = ["clase", "imagen"] + FEATURES
    missing = [c for c in required if c not in columns]

    check(
        "Columnas requeridas",
        not missing,
        f"faltantes={missing}" if missing else "presentes",
    )

    if missing:
        sys.exit(f"Columnas encontradas: {columns}")

    ok = True
    ok &= check(
        "Numero de filas",
        len(rows) == EXPECTED_TOTAL,
        f"{len(rows)} filas; se esperaban {EXPECTED_TOTAL}",
    )

    classes = [str(r["clase"]).strip() for r in rows]
    unexpected = sorted(set(classes) - {POS, NEG})

    ok &= check(
        "Clases esperadas",
        not unexpected,
        f"inesperadas={unexpected}" if unexpected else "correctas",
    )

    counts = {c: classes.count(c) for c in (POS, NEG)}
    ok &= check(
        "Cantidad por clase",
        counts == EXPECTED_COUNTS,
        str(counts),
    )

    names = [str(r["imagen"]).strip() for r in rows]
    duplicate_names = len(names) - len(set(names))

    check(
        "Nombres unicos globalmente",
        duplicate_names == 0,
        f"nombres repetidos={duplicate_names}",
    )

    empty_cells = sum(
        1
        for row in rows
        for col in required
        if row.get(col) is None or not str(row[col]).strip()
    )

    ok &= check(
        "Celdas requeridas no vacias",
        empty_cells == 0,
        f"celdas vacias={empty_cells}",
    )

    X = np.full((len(rows), len(FEATURES)), np.nan)
    conversion_errors = 0

    for i, row in enumerate(rows):
        for j, feature in enumerate(FEATURES):
            try:
                X[i, j] = float(
                    str(row[feature]).strip().replace(",", ".")
                )
            except (ValueError, TypeError):
                conversion_errors += 1

    finite = bool(np.isfinite(X).all())

    ok &= check(
        "Variables numericas finitas",
        conversion_errors == 0 and finite,
        f"conversiones fallidas={conversion_errors}",
    )

    if finite:
        range_ok = bool(
            (X[:, :4] >= 0).all()
            and (X[:, :4] <= 255).all()
            and (X[:, 4] >= 0).all()
        )

        ok &= check(
            "Rangos RGB/brillo/saturacion",
            range_ok,
            "RGB y brillo [0,255]; saturacion >= 0",
        )

        expected_brightness = X[:, :3].mean(axis=1)
        brightness_difference = float(
            np.max(np.abs(X[:, 3] - expected_brightness))
        )

        check(
            "Brillo redundante con RGB",
            brightness_difference < 0.05,
            f"diferencia maxima={brightness_difference:.6f}",
        )

        channel_range = (
            X[:, :3].max(axis=1) - X[:, :3].min(axis=1)
        )

        corr = float(np.corrcoef(channel_range, X[:, 4])[0, 1])

        check(
            "Relacion entre saturacion y dispersion RGB",
            corr > 0.99,
            f"correlacion={corr:.4f}",
        )

    if not ok:
        sys.exit(
            "ERROR: Fallaron comprobaciones criticas. "
            "No se ejecutara el analisis."
        )

    y = np.array([1 if c == POS else 0 for c in classes], dtype=int)

    print("\nAVISO: la saturacion calculada es una aproximacion RGB,")
    print("no una medida HSV normalizada.")

    return X, y, np.array(names), np.array(classes), counts


def read_original_partition(names):
    """
    Reutiliza la particion actual si cada nombre aparece exactamente
    una vez y se puede asignar sin ambiguedades.
    """
    if not DATASET_DIR.exists():
        return None, "No existe dataset_modelo."

    mapping = {}
    duplicate_names = set()
    extensions = {".jpg", ".jpeg", ".png"}

    for path in DATASET_DIR.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in extensions:
            continue

        relative_parts = [
            p.lower()
            for p in path.relative_to(DATASET_DIR).parts
        ]

        partition = None
        for part in relative_parts[:-1]:
            if part.startswith(("train", "entren")):
                partition = "train"
            elif part.startswith("val"):
                partition = "val"
            elif part.startswith(("test", "prueba")):
                partition = "test"

        if partition is None:
            continue

        name = path.name
        if name in mapping:
            duplicate_names.add(name)
        else:
            mapping[name] = partition

    if duplicate_names:
        return None, (
            "Hay nombres repetidos entre carpetas del dataset_modelo; "
            "no se puede asignar la particion solo por nombre."
        )

    missing = [name for name in names if name not in mapping]

    if missing:
        return None, (
            f"{len(missing)} imagenes no se encontraron en dataset_modelo."
        )

    partitions = np.array([mapping[name] for name in names])

    return partitions, "Particion existente recuperada por nombre unico."


def make_partition(y):
    rng = np.random.default_rng(SEED)
    partition = np.empty(len(y), dtype="<U5")

    for label, index in ((1, 0), (0, 1)):
        indices = np.where(y == label)[0].copy()
        rng.shuffle(indices)

        n_train, n_val, n_test = SPLIT_SIZES[
            "train"
        ][index], SPLIT_SIZES["val"][index], SPLIT_SIZES["test"][index]

        partition[indices[:n_train]] = "train"
        partition[indices[n_train:n_train + n_val]] = "val"
        partition[indices[n_train + n_val:n_train + n_val + n_test]] = "test"

    return partition


def get_partition(names, y):
    heading("2. PARTICION DE DATOS")

    partition, message = read_original_partition(names)

    if partition is not None:
        source = "dataset_modelo"
        print("  " + message)
    else:
        partition = make_partition(y)
        source = "nueva_particion_estratificada_seed_42"
        print("  No se pudo recuperar la particion original.")
        print("  Motivo: " + message)
        print("  Se crea una particion estratificada nueva.")
        print(
            "  ATENCION: esta particion no debe considerarse identica "
            "a la de MobileNet."
        )

    for part in ("train", "val", "test"):
        mask = partition == part
        print(
            f"  {part:5s}: total={int(mask.sum())}, "
            f"ojo={int((y[mask] == 1).sum())}, "
            f"sana={int((y[mask] == 0).sum())}"
        )

    if not {"train", "val", "test"}.issubset(set(partition)):
        sys.exit("ERROR: faltan particiones.")

    return partition, source


def standardize_fit(X):
    mean = X.mean(axis=0)
    std = X.std(axis=0)
    std[std == 0] = 1.0
    return mean, std


def sigmoid(z):
    z = np.clip(z, -35, 35)
    return 1.0 / (1.0 + np.exp(-z))


def fit_logistic(X, y, lam, max_iter=100, tol=1e-8):
    """
    Regresion logistica con regularizacion L2 mediante Newton.
    Las columnas de X deben haberse estandarizado previamente.
    """
    n, d = X.shape
    A = np.column_stack([np.ones(n), X])
    weights = np.zeros(d + 1)

    regularization = np.eye(d + 1) * lam
    regularization[0, 0] = 0.0

    for _ in range(max_iter):
        p = sigmoid(A @ weights)
        gradient = A.T @ (p - y) + regularization @ weights

        hessian = (
            (A * (p * (1 - p))[:, None]).T @ A
            + regularization
            + 1e-10 * np.eye(d + 1)
        )

        step = np.linalg.solve(hessian, gradient)
        weights -= step

        if np.max(np.abs(step)) < tol:
            break

    return weights


def predict_probability(weights, X):
    A = np.column_stack([np.ones(len(X)), X])
    return sigmoid(A @ weights)


def log_loss(y, probability):
    probability = np.clip(probability, 1e-12, 1 - 1e-12)
    return float(
        -np.mean(
            y * np.log(probability)
            + (1 - y) * np.log(1 - probability)
        )
    )


def auc_roc(y, scores):
    y = np.asarray(y)
    scores = np.asarray(scores, dtype=float)

    positives = int((y == 1).sum())
    negatives = int((y == 0).sum())

    if positives == 0 or negatives == 0:
        return float("nan")

    order = np.argsort(scores, kind="mergesort")
    sorted_scores = scores[order]
    ranks = np.empty(len(scores), dtype=float)

    i = 0
    while i < len(scores):
        j = i
        while (
            j + 1 < len(scores)
            and sorted_scores[j + 1] == sorted_scores[i]
        ):
            j += 1

        average_rank = (i + j) / 2.0 + 1.0
        ranks[order[i:j + 1]] = average_rank
        i = j + 1

    return float(
        (
            ranks[y == 1].sum()
            - positives * (positives + 1) / 2.0
        ) / (positives * negatives)
    )


def wilson_interval(correct, n, z=1.96):
    if n == 0:
        return [float("nan"), float("nan")]

    p = correct / n
    denominator = 1 + z * z / n
    center = (p + z * z / (2 * n)) / denominator
    half = (
        z
        * math.sqrt(
            p * (1 - p) / n
            + z * z / (4 * n * n)
        )
        / denominator
    )

    return [center - half, center + half]


def calculate_metrics(y, pred, scores=None):
    tp = int(((y == 1) & (pred == 1)).sum())
    tn = int(((y == 0) & (pred == 0)).sum())
    fp = int(((y == 0) & (pred == 1)).sum())
    fn = int(((y == 1) & (pred == 0)).sum())

    precision_pos = ratio(tp, tp + fp)
    recall_pos = ratio(tp, tp + fn)
    precision_neg = ratio(tn, tn + fn)
    recall_neg = ratio(tn, tn + fp)

    def f1(precision, recall):
        if (
            not math.isfinite(precision)
            or not math.isfinite(recall)
            or precision + recall == 0
        ):
            return float("nan")
        return 2 * precision * recall / (precision + recall)

    n = len(y)

    return {
        "n": n,
        "exactitud": ratio(tp + tn, n),
        "IC95_Wilson": wilson_interval(tp + tn, n),
        "exactitud_balanceada": (
            recall_pos + recall_neg
        ) / 2,
        "AUC_ROC": auc_roc(y, scores) if scores is not None else float("nan"),
        "matriz_confusion": {
            "orden_clases": [NEG, POS],
            "filas_real_columnas_predicho": [
                [tn, fp],
                [fn, tp],
            ],
        },
        POS: {
            "precision": precision_pos,
            "recall": recall_pos,
            "f1": f1(precision_pos, recall_pos),
            "soporte": int((y == 1).sum()),
        },
        NEG: {
            "precision": precision_neg,
            "recall": recall_neg,
            "f1": f1(precision_neg, recall_neg),
            "soporte": int((y == 0).sum()),
        },
    }


def print_metrics(title, metrics):
    print(f"\n--- {title} (n={metrics['n']}) ---")
    print("Exactitud:", fmt(metrics["exactitud"]))
    print("IC 95 % Wilson:", [
        fmt(v) for v in metrics["IC95_Wilson"]
    ])
    print(
        "Exactitud balanceada:",
        fmt(metrics["exactitud_balanceada"]),
    )
    print("AUC-ROC:", fmt(metrics["AUC_ROC"]))

    for label in (POS, NEG):
        item = metrics[label]
        print(
            f"{label}: precision={fmt(item['precision'])}, "
            f"recall={fmt(item['recall'])}, "
            f"F1={fmt(item['f1'])}, "
            f"soporte={item['soporte']}"
        )

    print("Matriz de confusion, filas=real, columnas=predicho:")
    print(f"                  pred {NEG:13s} pred {POS}")
    matrix = metrics["matriz_confusion"]["filas_real_columnas_predicho"]
    print(f"real {NEG:13s} {matrix[0][0]:8d} {matrix[0][1]:8d}")
    print(f"real {POS:13s} {matrix[1][0]:8d} {matrix[1][1]:8d}")


def permutation_test(X_train, y_train, X_test, y_test, lam,
                     observed_accuracy, observed_balanced, rng):
    """
    Prueba exploratoria: se barajan las etiquetas de entrenamiento,
    se vuelve a ajustar el modelo y se evalua en el test intacto.
    """
    accuracies = np.empty(N_PERM)
    balanced = np.empty(N_PERM)

    for i in range(N_PERM):
        shuffled_y = rng.permutation(y_train)
        weights = fit_logistic(X_train, shuffled_y, lam)
        scores = predict_probability(weights, X_test)
        predictions = (scores >= 0.5).astype(int)

        accuracies[i] = np.mean(predictions == y_test)
        recall_pos = np.mean(predictions[y_test == 1] == 1)
        recall_neg = np.mean(predictions[y_test == 0] == 0)
        balanced[i] = (recall_pos + recall_neg) / 2

    p_accuracy = (
        1 + np.sum(accuracies >= observed_accuracy - 1e-12)
    ) / (N_PERM + 1)

    p_balanced = (
        1 + np.sum(balanced >= observed_balanced - 1e-12)
    ) / (N_PERM + 1)

    return {
        "permutaciones": N_PERM,
        "p_valor_exactitud": float(p_accuracy),
        "p_valor_exactitud_balanceada": float(p_balanced),
        "exactitud_nula_media": float(accuracies.mean()),
        "exactitud_nula_percentil_95": float(np.percentile(accuracies, 95)),
        "exactitud_nula_maxima": float(accuracies.max()),
        "nota": (
            "Prueba exploratoria. Las etiquetas de prueba se mantienen "
            "fijas; no demuestra causalidad."
        ),
    }


def repeated_cross_validation(X, y, lam):
    """
    Validacion cruzada repetida con estandarizacion dentro de cada pliegue.
    Es un analisis descriptivo complementario, no sustituye una prueba
    independiente agrupada por sesion de captura.
    """
    results = []

    for repetition in range(CV_REPETITIONS):
        rng = np.random.default_rng(SEED + 1000 + repetition)
        folds = np.empty(len(y), dtype=int)

        for label in (0, 1):
            indices = np.where(y == label)[0].copy()
            rng.shuffle(indices)
            folds[indices] = np.arange(len(indices)) % CV_FOLDS

        for fold in range(CV_FOLDS):
            test_mask = folds == fold
            train_mask = ~test_mask

            mean, std = standardize_fit(X[train_mask])
            X_train = (X[train_mask] - mean) / std
            X_test = (X[test_mask] - mean) / std

            weights = fit_logistic(
                X_train, y[train_mask], lam
            )
            scores = predict_probability(weights, X_test)
            predictions = (scores >= 0.5).astype(int)

            results.append(
                calculate_metrics(
                    y[test_mask], predictions, scores
                )
            )

    summary = {}

    for key in (
        "exactitud",
        "exactitud_balanceada",
        "AUC_ROC",
    ):
        values = np.array(
            [r[key] for r in results], dtype=float
        )
        summary[key] = {
            "media": float(values.mean()),
            "desviacion": float(values.std(ddof=1)),
            "minimo": float(values.min()),
            "maximo": float(values.max()),
        }

    summary["numero_evaluaciones"] = len(results)
    summary["lambda_fijo"] = lam
    return summary


def main():
    print("PRUEBA DE CONTROL: clasificador basado en esquinas")
    print("Script:", Path(__file__).resolve())
    print("NumPy:", np.__version__)
    print("Semilla:", SEED)

    X, y, names, classes, class_counts = load_csv()
    partitions, partition_source = get_partition(names, y)

    train = partitions == "train"
    val = partitions == "val"
    test = partitions == "test"

    if train.sum() == 0 or val.sum() == 0 or test.sum() == 0:
        sys.exit("ERROR: una particion esta vacia.")

    mean, std = standardize_fit(X[train])
    X_scaled = (X - mean) / std

    heading("3. SELECCION DE REGULARIZACION CON VALIDACION")
    print("La particion de prueba no se utiliza para seleccionar lambda.")

    best_lambda = None
    best_loss = float("inf")
    validation_table = []

    for lam in LAMBDA_GRID:
        weights = fit_logistic(
            X_scaled[train], y[train], lam
        )
        val_scores = predict_probability(weights, X_scaled[val])
        val_predictions = (val_scores >= 0.5).astype(int)

        loss = log_loss(y[val], val_scores)
        accuracy = float(np.mean(val_predictions == y[val]))

        validation_table.append({
            "lambda": lam,
            "log_loss_validacion": loss,
            "exactitud_validacion": accuracy,
        })

        print(
            f"lambda={lam:<7} "
            f"log_loss={loss:.4f} exactitud={accuracy:.4f}"
        )

        if loss < best_loss:
            best_loss = loss
            best_lambda = lam

    print("Lambda elegido:", best_lambda)

    weights = fit_logistic(
        X_scaled[train], y[train], best_lambda
    )

    val_scores = predict_probability(weights, X_scaled[val])
    test_scores = predict_probability(weights, X_scaled[test])

    val_metrics = calculate_metrics(
        y[val], (val_scores >= 0.5).astype(int), val_scores
    )
    test_metrics = calculate_metrics(
        y[test], (test_scores >= 0.5).astype(int), test_scores
    )

    heading("4. RESULTADOS DEL CLASIFICADOR DE FONDO")
    print_metrics("VALIDACION", val_metrics)
    print_metrics("PRUEBA", test_metrics)

    heading("5. REFERENCIA TRIVIAL")
    majority_class = (
        1 if np.sum(y[train] == 1) >= np.sum(y[train] == 0)
        else 0
    )
    trivial_predictions = np.full(
        int(test.sum()), majority_class, dtype=int
    )
    trivial_metrics = calculate_metrics(
        y[test], trivial_predictions,
        trivial_predictions.astype(float)
    )

    print("Clase mayoritaria de entrenamiento:",
          POS if majority_class == 1 else NEG)
    print_metrics("CLASIFICADOR TRIVIAL EN PRUEBA", trivial_metrics)

    heading("6. AUC DE CADA VARIABLE POR SEPARADO")
    individual_auc = {}

    for j, feature in enumerate(FEATURES):
        score = X[test, j]
        value = auc_roc(y[test], score)
        individual_auc[feature] = value
        print(f"{feature:30s} AUC={fmt(value)}")

    print(
        "Un AUC cercano a 0.5 indica poca separacion por esa variable "
        "en esta particion. Un AUC inferior a 0.5 puede indicar que "
        "los valores altos se asocian a la clase negativa."
    )

    heading("7. PRUEBA DE PERMUTACION")
    rng = np.random.default_rng(SEED + 7)
    permutation = permutation_test(
        X_scaled[train],
        y[train],
        X_scaled[test],
        y[test],
        best_lambda,
        test_metrics["exactitud"],
        test_metrics["exactitud_balanceada"],
        rng,
    )

    print(json.dumps(json_safe(permutation), indent=2, ensure_ascii=False))

    heading("8. VALIDACION CRUZADA REPETIDA")
    cv = repeated_cross_validation(X, y, lam=1.0)

    for key in ("exactitud", "exactitud_balanceada", "AUC_ROC"):
        print(key, json.dumps(cv[key], ensure_ascii=False))

    print(
        "Nota: los pliegues no son independientes. Esta dispersion "
        "es descriptiva y no equivale a un intervalo de confianza formal."
    )

    heading("9. GUARDAR RESULTADOS")

    coefficients = {
        "intercepto": float(weights[0])
    }
    coefficients.update({
        feature: float(coef)
        for feature, coef in zip(FEATURES, weights[1:])
    })

    results = {
        "descripcion": (
            "Clasificador de control basado unicamente en "
            "estadisticas de esquinas"
        ),
        "semilla": SEED,
        "fuente_particion": partition_source,
        "particion_original_reutilizada": (
            partition_source.startswith("Particion existente")
        ),
        "variables": FEATURES,
        "conteos_clase": class_counts,
        "tamanos_particion": {
            p: int((partitions == p).sum())
            for p in ("train", "val", "test")
        },
        "seleccion_lambda": {
            "tabla": validation_table,
            "elegido": best_lambda,
        },
        "coeficientes": coefficients,
        "validacion": val_metrics,
        "prueba": test_metrics,
        "referencia_trivial": trivial_metrics,
        "auc_por_variable": individual_auc,
        "permutacion": permutation,
        "validacion_cruzada_repetida": cv,
        "limitaciones": [
            "Las dos clases fueron capturadas en fechas distintas.",
            "Las esquinas pueden contener partes de las hojas.",
            "Una asociacion no demuestra causalidad.",
            "Esta prueba no demuestra que MobileNet use el fondo.",
            "Una particion nueva no es directamente comparable "
            "con la particion original de MobileNet.",
            "La validacion cruzada aleatoria no separa por sesion "
            "de captura y puede sobreestimar generalizacion.",
        ],
    }

    with OUT_JSON.open("w", encoding="utf-8") as file:
        json.dump(
            json_safe(results), file, indent=2, ensure_ascii=False
        )

    with OUT_PRED.open(
        "w", newline="", encoding="utf-8-sig"
    ) as file:
        writer = csv.writer(file)
        writer.writerow([
            "imagen", "clase_real", "probabilidad_ojo",
            "prediccion", "correcto"
        ])

        test_indices = np.where(test)[0]
        for i, score in zip(test_indices, test_scores):
            prediction = POS if score >= 0.5 else NEG
            writer.writerow([
                names[i],
                classes[i],
                f"{score:.6f}",
                prediction,
                int(prediction == classes[i]),
            ])

    with OUT_PART.open(
        "w", newline="", encoding="utf-8-sig"
    ) as file:
        writer = csv.writer(file)
        writer.writerow([
            "imagen", "clase", "particion", "fuente"
        ])

        for i in range(len(names)):
            writer.writerow([
                names[i], classes[i],
                partitions[i], partition_source
            ])

    print("Resultados guardados:")
    print(OUT_JSON)
    print(OUT_PRED)
    print(OUT_PART)
    print("\nFIN. Copia la salida completa de la terminal.")


if __name__ == "__main__":
    main()