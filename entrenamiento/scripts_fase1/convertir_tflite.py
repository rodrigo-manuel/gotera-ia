from pathlib import Path
import numpy as np
import tensorflow as tf

base = Path(__file__).resolve().parent.parent
carpeta_modelo = base / "02_MODELO"
archivo_modelo = carpeta_modelo / "mejor_modelo.keras"
archivo_tflite = carpeta_modelo / "modelo_ojo_de_gallo.tflite"
carpeta_test = base / "01_DATASET" / "dataset_modelo" / "test"

print("TensorFlow:", tf.__version__)
print("Modelo original:", archivo_modelo)

if not archivo_modelo.exists():
    raise FileNotFoundError(f"No existe el modelo: {archivo_modelo}")

# Cargar el modelo existente; no se realiza entrenamiento.
modelo = tf.keras.models.load_model(archivo_modelo, compile=False)
print("Entrada:", modelo.input_shape)
print("Salida:", modelo.output_shape)

if tuple(modelo.input_shape[1:]) != (224, 224, 3):
    raise ValueError("La entrada no coincide con 224x224x3.")
if tuple(modelo.output_shape[1:]) != (1,):
    raise ValueError("La salida no coincide con una neurona.")

# Convertir conservando la precisión float32.
convertidor = tf.lite.TFLiteConverter.from_keras_model(modelo)
contenido = convertidor.convert()
archivo_tflite.write_bytes(contenido)

print("\nModelo TFLite guardado:", archivo_tflite)
print("Tamaño en bytes:", archivo_tflite.stat().st_size)

# Seleccionar hasta cinco imágenes de prueba, de ambas clases.
rutas = []
for clase in ("ojo_de_gallo", "sana"):
    carpeta = carpeta_test / clase
    if carpeta.exists():
        rutas.extend(sorted(carpeta.glob("*.jpg"))[:3])

if not rutas:
    raise FileNotFoundError("No se encontraron imágenes JPG de prueba.")

interprete = tf.lite.Interpreter(model_path=str(archivo_tflite))
interprete.allocate_tensors()
entrada = interprete.get_input_details()[0]
salida = interprete.get_output_details()[0]

print("\nEntrada TFLite:", entrada["shape"], entrada["dtype"])
print("Salida TFLite:", salida["shape"], salida["dtype"])
print("\nCOMPARACIÓN DE PREDICCIONES")

for ruta in rutas:
    imagen = tf.keras.utils.load_img(
        ruta, target_size=(224, 224), color_mode="rgb"
    )
    datos = tf.keras.utils.img_to_array(imagen)
    datos = np.expand_dims(datos, axis=0).astype(np.float32)

    pred_original = float(modelo(datos, training=False).numpy().reshape(-1)[0])

    if entrada["dtype"] != np.float32:
        raise TypeError(f"Entrada inesperada de TFLite: {entrada['dtype']}")

    interprete.set_tensor(entrada["index"], datos)
    interprete.invoke()
    pred_convertido = float(
        interprete.get_tensor(salida["index"]).reshape(-1)[0]
    )

    diferencia = abs(pred_original - pred_convertido)
    print(
        f"{ruta.parent.name}/{ruta.name}: "
        f"original={pred_original:.8f}, "
        f"TFLite={pred_convertido:.8f}, "
        f"diferencia={diferencia:.8f}"
    )

print("\nConversión y comparación finalizadas.")
print("Revisa las diferencias antes de integrar el modelo en Android.")
