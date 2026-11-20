package co.edu.ucc.ojodegallo.ml

import android.content.Context
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Resultado completo de un análisis, con las salidas de los dos modelos y los tiempos medidos. */
data class Analisis(
    val pHoja: Float,
    val pOjo: Float,
    val resultado: Resultado,
    val pRoya: Float = Float.NaN,
    val msPreprocesamiento: Double,
    val msInferencia: Double,
) {
    val msTotal: Double get() = msPreprocesamiento + msInferencia
}

/**
 * Ejecuta los dos modelos TensorFlow Lite dentro del teléfono, sin conexión.
 * - modelo_verificador.tflite: salida = P(hoja de café).
 * - modelo_diagnostico.tflite: salida = P(sana); por eso P(ojo de gallo) = 1 − salida.
 * - modelo_roya.tflite (opcional, Fase 2e): salida = P(roya). Si no está en assets, la app funciona sin él.
 */
class Clasificador(context: Context) : AutoCloseable {
    companion object {
        /** Umbral del filtro de calidad: mitad de la menor desviación observada en 4.559 hojas reales (fase2c_modelos_v3/tablas/filtro_calidad.json). */
        const val UMBRAL_DESVIACION = 13.5f
        /** Promedio de cuatro vistas (original, espejo horizontal, vertical y 180°) en el diagnóstico (Fase 2d). */
        const val USAR_TTA = true
    }

    private val verificador: Interpreter
    private val diagnostico: Interpreter
    private val roya: Interpreter?
    /** Umbral del detector de roya elegido con la validación de la Fase 2e (archivo modelo_roya_umbral.txt). */
    val umbralRoya: Float
    val usaDetectorRoya: Boolean get() = roya != null
    val msCarga: Double

    init {
        val t0 = SystemClock.elapsedRealtimeNanos()
        val opciones = { Interpreter.Options().setNumThreads(4) }
        verificador = Interpreter(cargar(context, "modelo_verificador.tflite"), opciones())
        diagnostico = Interpreter(cargar(context, "modelo_diagnostico.tflite"), opciones())
        val archivos = context.assets.list("")?.toSet() ?: emptySet()
        roya = if ("modelo_roya.tflite" in archivos) Interpreter(cargar(context, "modelo_roya.tflite"), opciones()) else null
        umbralRoya = if ("modelo_roya_umbral.txt" in archivos) {
            context.assets.open("modelo_roya_umbral.txt").bufferedReader().use { it.readText().trim().toFloatOrNull() }
                ?: ReglaDecision.UMBRAL_ROYA_PREDETERMINADO
        } else ReglaDecision.UMBRAL_ROYA_PREDETERMINADO
        msCarga = (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
    }

    private fun cargar(context: Context, nombre: String): ByteBuffer {
        val bytes = context.assets.open(nombre).use { it.readBytes() }
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes); rewind()
        }
    }

    /** Número de salidas del diagnóstico: 1 (binario, P(sana)) o 3 (ojo de gallo, roya, sana). */
    private val salidasDiagnostico: Int by lazy { diagnostico.getOutputTensor(0).shape().last() }

    private fun correr(modelo: Interpreter, entrada: ByteBuffer, n: Int = 1): FloatArray {
        val salida = Array(1) { FloatArray(n) }
        entrada.rewind()
        modelo.run(entrada, salida)
        return salida[0]
    }

    @Synchronized
    fun analizar(archivo: File): Analisis {
        val t0 = SystemClock.elapsedRealtimeNanos()
        val entrada = Preprocesador.aEntrada(archivo)
        val t1 = SystemClock.elapsedRealtimeNanos()
        // Filtro de calidad: una foto lisa, tapada o casi negra se rechaza sin usar los modelos.
        if (Preprocesador.desviacion(entrada) < UMBRAL_DESVIACION) {
            return Analisis(0f, 0f, Resultado.ImagenNoValida, msPreprocesamiento = (t1 - t0) / 1e6, msInferencia = 0.0)
        }
        val pHoja = correr(verificador, entrada)[0]
        val d = correr(diagnostico, entrada, salidasDiagnostico)
        val t2 = SystemClock.elapsedRealtimeNanos()
        return if (d.size == 3) {
            // Orden de salida: ojo de gallo, roya, sana (fase2c_modelos_v3/tablas/contrato_app.json)
            Analisis(pHoja, d[0], ReglaDecision.decidirTresClases(pHoja, d[0], d[1], d[2]), pRoya = d[1],
                msPreprocesamiento = (t1 - t0) / 1e6, msInferencia = (t2 - t1) / 1e6)
        } else {
            // El modelo binario entrega P(no es ojo de gallo); con TTA se promedian cuatro vistas.
            val vistas = if (USAR_TTA) listOf(Pair(false, false), Pair(true, false), Pair(false, true), Pair(true, true))
                         else listOf(Pair(false, false))
            val entradas = vistas.map { (h, v) -> if (!h && !v) entrada else vista(entrada, h, v) }
            val pOjo = entradas.map { 1f - correr(diagnostico, it, 1)[0] }.average().toFloat()
            // Detector de roya (Fase 2e): mismo promedio de vistas. Solo se consulta si hay lesiones (P(ojo) >= 0,30).
            val pRoya = roya?.takeIf { pOjo >= ReglaDecision.BANDA_INFERIOR }
                ?.let { m -> entradas.map { correr(m, it, 1)[0] }.average().toFloat() } ?: Float.NaN
            val t3 = SystemClock.elapsedRealtimeNanos()
            Analisis(pHoja, pOjo, ReglaDecision.decidirConRoya(pHoja, pOjo, pRoya, umbralRoya), pRoya = pRoya,
                msPreprocesamiento = (t1 - t0) / 1e6, msInferencia = (t3 - t1) / 1e6)
        }
    }

    /** Copia de la entrada volteada en horizontal (h) y/o vertical (v), igual que en el cuaderno de la Fase 2d. */
    private fun vista(src: ByteBuffer, h: Boolean, v: Boolean): ByteBuffer {
        val t = Preprocesador.TAM
        val fs = src.duplicate().order(ByteOrder.nativeOrder()).apply { rewind() }.asFloatBuffer()
        val out = ByteBuffer.allocateDirect(t * t * 3 * 4).order(ByteOrder.nativeOrder())
        val fo = out.asFloatBuffer()
        for (y in 0 until t) for (x in 0 until t) {
            val si = ((if (v) t - 1 - y else y) * t + (if (h) t - 1 - x else x)) * 3
            val di = (y * t + x) * 3
            fo.put(di, fs.get(si)); fo.put(di + 1, fs.get(si + 1)); fo.put(di + 2, fs.get(si + 2))
        }
        out.rewind()
        return out
    }

    override fun close() {
        verificador.close(); diagnostico.close(); roya?.close()
    }
}
