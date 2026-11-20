package co.edu.ucc.ojodegallo.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Prepara la foto igual que en Google Colab (contrato preprocesamiento.json de la Fase 2):
 * 1. corrige la orientación según los metadatos EXIF;
 * 2. usa los canales RGB;
 * 3. redimensiona la imagen completa a 224 x 224 con interpolación bilineal de centros de píxel
 *    (la misma que tf.image.resize de TensorFlow, sin antialias);
 * 4. entrega valores de 0 a 255 en float32, sin normalizar (el modelo normaliza internamente).
 */
object Preprocesador {
    const val TAM = 224

    fun aEntrada(archivo: File): ByteBuffer {
        val orientacion = leerOrientacion(archivo)
        val opciones = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bmp = BitmapFactory.decodeFile(archivo.absolutePath, opciones)
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        try {
            return redimensionar(bmp, orientacion)
        } finally {
            bmp.recycle()
        }
    }

    private fun leerOrientacion(archivo: File): Int = try {
        ExifInterface(archivo.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** Ancho y alto de la imagen ya orientada. */
    private fun dimensiones(w: Int, h: Int, o: Int): Pair<Int, Int> = when (o) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE -> Pair(h, w)
        else -> Pair(w, h)
    }

    /** Lee el píxel (x, y) de la imagen orientada sin crear una copia rotada en memoria. */
    private fun pixel(bmp: Bitmap, x: Int, y: Int, o: Int): Int {
        val w = bmp.width; val h = bmp.height
        val (sx, sy) = when (o) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Pair(w - 1 - x, y)
            ExifInterface.ORIENTATION_ROTATE_180 -> Pair(w - 1 - x, h - 1 - y)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Pair(x, h - 1 - y)
            ExifInterface.ORIENTATION_TRANSPOSE -> Pair(y, x)
            ExifInterface.ORIENTATION_ROTATE_90 -> Pair(y, h - 1 - x)
            ExifInterface.ORIENTATION_TRANSVERSE -> Pair(w - 1 - y, h - 1 - x)
            ExifInterface.ORIENTATION_ROTATE_270 -> Pair(w - 1 - y, x)
            else -> Pair(x, y)
        }
        return bmp.getPixel(sx, sy)
    }

    private fun redimensionar(bmp: Bitmap, o: Int): ByteBuffer {
        val (w, h) = dimensiones(bmp.width, bmp.height, o)
        val buf = ByteBuffer.allocateDirect(TAM * TAM * 3 * 4).order(ByteOrder.nativeOrder())
        val escX = w.toFloat() / TAM
        val escY = h.toFloat() / TAM
        for (y in 0 until TAM) {
            val fy = max(0f, (y + 0.5f) * escY - 0.5f)
            val y0 = min(floor(fy).toInt(), h - 1); val y1 = min(y0 + 1, h - 1); val dy = fy - y0
            for (x in 0 until TAM) {
                val fx = max(0f, (x + 0.5f) * escX - 0.5f)
                val x0 = min(floor(fx).toInt(), w - 1); val x1 = min(x0 + 1, w - 1); val dx = fx - x0
                val p00 = pixel(bmp, x0, y0, o); val p01 = pixel(bmp, x1, y0, o)
                val p10 = pixel(bmp, x0, y1, o); val p11 = pixel(bmp, x1, y1, o)
                for (desp in intArrayOf(16, 8, 0)) {   // R, G, B
                    val a = (p00 shr desp and 0xFF).toFloat(); val b = (p01 shr desp and 0xFF).toFloat()
                    val c = (p10 shr desp and 0xFF).toFloat(); val d = (p11 shr desp and 0xFF).toFloat()
                    val arriba = a + (b - a) * dx
                    val abajo = c + (d - c) * dx
                    buf.putFloat(arriba + (abajo - arriba) * dy)
                }
            }
        }
        buf.rewind()
        return buf
    }

    /** Desviación estándar de todos los valores de la entrada (filtro de calidad, igual que numpy.std). */
    fun desviacion(buf: ByteBuffer): Float {
        val fb = buf.duplicate().order(ByteOrder.nativeOrder()).apply { rewind() }.asFloatBuffer()
        val n = fb.remaining(); var s = 0.0; var s2 = 0.0
        for (i in 0 until n) { val v = fb.get(i).toDouble(); s += v; s2 += v * v }
        val m = s / n
        return kotlin.math.sqrt(maxOf(0.0, s2 / n - m * m)).toFloat()
    }

    /** Miniatura orientada para mostrar en pantalla (no se usa para el modelo). */
    fun miniatura(archivo: File, ladoMax: Int = 1024): Bitmap? {
        val lim = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, lim)
        if (lim.outWidth <= 0) return null
        var muestra = 1
        while (max(lim.outWidth, lim.outHeight) / (muestra * 2) >= ladoMax) muestra *= 2
        val bmp = BitmapFactory.decodeFile(archivo.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestra }) ?: return null
        val grados = when (leerOrientacion(archivo)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return bmp
        val m = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
