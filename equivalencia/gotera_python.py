"""
Réplica en Python del análisis de Gotera IA (Clasificador.kt, Preprocesador.kt y ReglaDecision.kt).
Usa los mismos tres archivos TFLite de la aplicación (copiados en ./modelos) y la misma regla.
Sirve como referencia para la prueba de equivalencia Python–Android (PB-15).
"""
import numpy as np
from PIL import Image, ImageOps
from ai_edge_litert.interpreter import Interpreter

TAM = 224
UMBRAL_DESVIACION = 13.5
UMBRAL_HOJA = 0.5
BANDA_INFERIOR = 0.30
BANDA_SUPERIOR = 0.70
LIMITE_CERTEZA_ALTA = 0.90


def entrada(ruta):
    """EXIF -> RGB -> bilineal con centros de píxel (sin antialias), igual que Preprocesador.redimensionar."""
    im = ImageOps.exif_transpose(Image.open(ruta)).convert("RGB")
    a = np.asarray(im, dtype=np.float32)
    h, w = a.shape[:2]
    escX = np.float32(w) / np.float32(TAM)
    escY = np.float32(h) / np.float32(TAM)
    idx = np.arange(TAM, dtype=np.float32)
    fy = np.maximum(np.float32(0), (idx + np.float32(0.5)) * escY - np.float32(0.5))
    fx = np.maximum(np.float32(0), (idx + np.float32(0.5)) * escX - np.float32(0.5))
    y0 = np.minimum(np.floor(fy).astype(int), h - 1); y1 = np.minimum(y0 + 1, h - 1); dy = (fy - y0).astype(np.float32)
    x0 = np.minimum(np.floor(fx).astype(int), w - 1); x1 = np.minimum(x0 + 1, w - 1); dx = (fx - x0).astype(np.float32)
    p00 = a[y0][:, x0]; p01 = a[y0][:, x1]; p10 = a[y1][:, x0]; p11 = a[y1][:, x1]
    dx = dx[None, :, None]; dy = dy[:, None, None]
    arriba = p00 + (p01 - p00) * dx
    abajo = p10 + (p11 - p10) * dx
    return (arriba + (abajo - arriba) * dy).astype(np.float32)


class Gotera:
    def __init__(self, carpeta="modelos"):
        def cargar(n):
            it = Interpreter(model_path=f"{carpeta}/{n}", num_threads=4); it.allocate_tensors(); return it
        self.verificador = cargar("modelo_verificador.tflite")
        self.diagnostico = cargar("modelo_diagnostico.tflite")
        self.roya = cargar("modelo_roya.tflite")
        self.umbral_roya = float(open(f"{carpeta}/modelo_roya_umbral.txt").read().strip())

    @staticmethod
    def _correr(m, x):
        m.set_tensor(m.get_input_details()[0]["index"], x[None].astype(np.float32))
        m.invoke()
        return float(m.get_tensor(m.get_output_details()[0]["index"]).reshape(-1)[0])

    def analizar(self, ruta):
        x = entrada(ruta)
        desv = float(np.sqrt(max(0.0, np.mean(x.astype(np.float64) ** 2) - np.mean(x.astype(np.float64)) ** 2)))
        if desv < UMBRAL_DESVIACION:
            return dict(desviacion=desv, p_hoja=np.nan, p_ojo=np.nan, p_roya=np.nan, resultado="IMAGEN_NO_VALIDA")
        p_hoja = self._correr(self.verificador, x)
        vistas = [x, x[:, ::-1], x[::-1, :], x[::-1, ::-1]]   # original, espejo horizontal, vertical, 180°
        p_ojo = float(np.mean([1.0 - self._correr(self.diagnostico, v) for v in vistas]))
        p_roya = float(np.mean([self._correr(self.roya, v) for v in vistas])) if p_ojo >= BANDA_INFERIOR else np.nan
        return dict(desviacion=desv, p_hoja=p_hoja, p_ojo=p_ojo, p_roya=p_roya,
                    resultado=decidir_con_roya(p_hoja, p_ojo, p_roya, self.umbral_roya))


def decidir(p_hoja, p_ojo):
    if p_hoja < UMBRAL_HOJA: return "IMAGEN_NO_VALIDA"
    if p_ojo >= BANDA_SUPERIOR: return "COMPATIBLE_OJO_DE_GALLO"
    if p_ojo <= BANDA_INFERIOR: return "SIN_SINTOMAS"
    return "NO_CONCLUYENTE"


def decidir_con_roya(p_hoja, p_ojo, p_roya, umbral):
    if np.isnan(p_roya): return decidir(p_hoja, p_ojo)
    if p_hoja < UMBRAL_HOJA: return "IMAGEN_NO_VALIDA"
    if p_ojo >= BANDA_INFERIOR and p_roya >= umbral: return "OJO_DE_GALLO_O_ROYA"
    return decidir(p_hoja, p_ojo)
