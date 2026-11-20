from pathlib import Path
import json

import tensorflow as tf


# ============================================================
# CONFIGURACIÓN
# ============================================================

PROYECTO = Path(__file__).resolve().parent.parent

DATASET = PROYECTO / "01_DATASET" / "dataset_modelo"
SALIDA = PROYECTO / "02_MODELO"

TRAIN_DIR = DATASET / "train"
VAL_DIR = DATASET / "validation"

IMG_SIZE = (224, 224)
BATCH_SIZE = 16
SEED = 42

EPOCHS = 15


# ============================================================
# INFORMACIÓN INICIAL
# ============================================================

print("=" * 70)
print("ENTRENAMIENTO DEL MODELO - MOBILENETV3 SMALL")
print("=" * 70)
print()

print("Proyecto:")
print(PROYECTO)

print()
print("Dataset:")
print(DATASET)

print()
print("TensorFlow:", tf.__version__)

print()
print("Dispositivos:")
print(tf.config.list_physical_devices())


# ============================================================
# CARGAR DATASET
# ============================================================

print()
print("=" * 70)
print("CARGANDO DATASET")
print("=" * 70)

train_ds = tf.keras.utils.image_dataset_from_directory(
    TRAIN_DIR,
    labels="inferred",
    label_mode="binary",
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    shuffle=True,
    seed=SEED
)

val_ds = tf.keras.utils.image_dataset_from_directory(
    VAL_DIR,
    labels="inferred",
    label_mode="binary",
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    shuffle=False
)

class_names = train_ds.class_names

print()
print("Clases:")
print(class_names)

print()
print("Imágenes de entrenamiento: 699")
print("Imágenes de validación: 149")


# ============================================================
# OPTIMIZACIÓN DEL PIPELINE
# ============================================================

AUTOTUNE = tf.data.AUTOTUNE

train_ds = train_ds.prefetch(AUTOTUNE)
val_ds = val_ds.prefetch(AUTOTUNE)


# ============================================================
# AUMENTACIÓN DE DATOS
# ============================================================

data_augmentation = tf.keras.Sequential(
    [
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomRotation(0.10),
        tf.keras.layers.RandomZoom(0.10),
    ],
    name="data_augmentation"
)


# ============================================================
# MODELO BASE
# ============================================================

print()
print("=" * 70)
print("CREANDO MOBILENETV3 SMALL")
print("=" * 70)

base_model = tf.keras.applications.MobileNetV3Small(
    input_shape=(224, 224, 3),
    include_top=False,
    weights="imagenet"
)

# Congelamos la red base durante la primera fase.
base_model.trainable = False


# ============================================================
# CABEZA DE CLASIFICACIÓN
# ============================================================

inputs = tf.keras.Input(
    shape=(224, 224, 3),
    name="entrada_imagen"
)

x = data_augmentation(inputs)

# MobileNetV3Small incluye el preprocesamiento internamente.
x = base_model(x, training=False)

x = tf.keras.layers.GlobalAveragePooling2D(
    name="global_average_pooling"
)(x)

x = tf.keras.layers.Dropout(
    0.20,
    name="dropout"
)(x)

outputs = tf.keras.layers.Dense(
    1,
    activation="sigmoid",
    name="clasificacion"
)(x)

model = tf.keras.Model(
    inputs=inputs,
    outputs=outputs,
    name="modelo_ojo_de_gallo"
)


# ============================================================
# COMPILACIÓN
# ============================================================

model.compile(
    optimizer=tf.keras.optimizers.Adam(
        learning_rate=0.001
    ),
    loss="binary_crossentropy",
    metrics=[
        tf.keras.metrics.BinaryAccuracy(
            name="accuracy"
        ),
        tf.keras.metrics.Precision(
            name="precision"
        ),
        tf.keras.metrics.Recall(
            name="recall"
        )
    ]
)


# ============================================================
# MOSTRAR MODELO
# ============================================================

print()
print("=" * 70)
print("RESUMEN DEL MODELO")
print("=" * 70)

model.summary()


# ============================================================
# GUARDAR MEJOR MODELO
# ============================================================

SALIDA.mkdir(
    parents=True,
    exist_ok=True
)

MEJOR_MODELO = SALIDA / "mejor_modelo.keras"

checkpoint = tf.keras.callbacks.ModelCheckpoint(
    filepath=MEJOR_MODELO,
    monitor="val_loss",
    save_best_only=True,
    mode="min",
    verbose=1
)


# ============================================================
# EARLY STOPPING
# ============================================================

early_stopping = tf.keras.callbacks.EarlyStopping(
    monitor="val_loss",
    patience=4,
    mode="min",
    restore_best_weights=True,
    verbose=1
)


# ============================================================
# GUARDAR HISTORIAL
# ============================================================

class HistorialCallback(
    tf.keras.callbacks.Callback
):

    def on_train_end(self, logs=None):

        if logs is None:
            logs = {}

        historial = {
            clave: [
                float(valor)
                for valor in valores
            ]
            for clave, valores
            in self.model.history.history.items()
        }

        ruta = SALIDA / "historial_entrenamiento.json"

        with open(
            ruta,
            "w",
            encoding="utf-8"
        ) as archivo:

            json.dump(
                historial,
                archivo,
                indent=4
            )

        print()
        print("Historial guardado en:")
        print(ruta)


# ============================================================
# ENTRENAMIENTO
# ============================================================

print()
print("=" * 70)
print("INICIANDO ENTRENAMIENTO")
print("=" * 70)

print()
print("Épocas máximas:", EPOCHS)
print("Batch size:", BATCH_SIZE)
print("Tamaño de imagen:", IMG_SIZE)
print()
print("IMPORTANTE:")
print("Esta es la primera fase del entrenamiento.")
print("La red base MobileNetV3Small permanece congelada.")
print()

history = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=EPOCHS,
    callbacks=[
        checkpoint,
        early_stopping,
        HistorialCallback()
    ]
)


# ============================================================
# RESULTADO FINAL DE LA PRIMERA FASE
# ============================================================

print()
print("=" * 70)
print("ENTRENAMIENTO FINALIZADO")
print("=" * 70)

print()

print("Mejor modelo guardado en:")
print(MEJOR_MODELO)

print()

print("Últimas métricas registradas:")

for clave, valores in history.history.items():

    if valores:
        print(
            f"{clave}: "
            f"{valores[-1]:.4f}"
        )

print()
print("=" * 70)
print("FIN DE LA PRIMERA FASE")
print("=" * 70) 