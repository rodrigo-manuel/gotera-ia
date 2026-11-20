package co.edu.ucc.ojodegallo

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.edu.ucc.ojodegallo.ml.Clasificador
import co.edu.ucc.ojodegallo.ml.Preprocesador
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/**
 * Prueba de equivalencia Python–Android (PB-15).
 *
 * Toma un lote fijo de fotos (src/androidTest/assets/equivalencia/imagenes), las analiza con el mismo
 * Clasificador que usa la aplicación y compara cada paso con la réplica en Python
 * (03_Modelo_IA/equivalencia/gotera_python.py), cuyos resultados vienen en referencia_python.csv:
 *  1. la entrada de 224 x 224 x 3 después de la corrección EXIF y el redimensionamiento (archivo .bin por foto);
 *  2. P(hoja de café), P(ojo de gallo) con cuatro vistas y P(roya);
 *  3. el resultado final de la regla de decisión.
 * Criterio fijado antes de ejecutar (sección 3.3.2 de la tesis): diferencia de probabilidad de 0,01 o menos
 * por imagen y el mismo resultado en todas.
 * El detalle queda en equivalencia_android.csv (Android/data/co.edu.ucc.ojodegallo/files) y en Logcat (etiqueta EQUIVALENCIA).
 */
@RunWith(AndroidJUnit4::class)
class EquivalenciaTest {

    private val tolerancia = 0.01f

    @Test
    fun androidDaLoMismoQuePython() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val assetsPrueba = inst.context.assets
        val app = inst.targetContext

        val lineas = assetsPrueba.open("equivalencia/referencia_python.csv").bufferedReader().readLines()
        val cab = lineas.first().split(",")
        fun col(n: String) = cab.indexOf(n).also { require(it >= 0) { "Falta la columna $n" } }
        val iArchivo = col("archivo"); val iClase = col("clase"); val iHoja = col("p_hoja"); val iOjo = col("p_ojo")
        val iRoya = col("p_roya"); val iRes = col("resultado"); val iDesv = col("desviacion")

        val salida = File(app.getExternalFilesDir(null), "equivalencia_android.csv")
        val fallas = mutableListOf<String>()
        var maxDifEntrada = 0f; var maxDifProb = 0f; var coincidencias = 0

        Clasificador(app).use { clasificador ->
            salida.printWriter().use { w ->
                w.println("clase,archivo,dif_max_entrada,dif_media_entrada,desviacion_android,desviacion_python," +
                    "p_hoja_android,p_hoja_python,p_ojo_android,p_ojo_python,p_roya_android,p_roya_python," +
                    "dif_max_prob,resultado_android,resultado_python,coincide,ms_analisis")
                for (linea in lineas.drop(1).filter { it.isNotBlank() }) {
                    val c = linea.split(",")
                    val nombre = c[iArchivo]
                    // Copia exacta del archivo (conserva los metadatos EXIF), como si viniera de la galería.
                    val foto = File(app.cacheDir, nombre)
                    assetsPrueba.open("equivalencia/imagenes/$nombre").use { e -> foto.outputStream().use { e.copyTo(it) } }

                    // 1. Entrada del modelo
                    val entrada = Preprocesador.aEntrada(foto)
                    val fa = entrada.duplicate().order(ByteOrder.nativeOrder()).apply { rewind() }.asFloatBuffer()
                    val bytesPy = assetsPrueba.open("equivalencia/entradas/${nombre.substringBeforeLast('.')}.bin").use { it.readBytes() }
                    val fp = ByteBuffer.wrap(bytesPy).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                    require(fp.remaining() == fa.remaining()) { "Tamaño de entrada distinto en $nombre" }
                    var difMax = 0f; var difSuma = 0.0
                    for (k in 0 until fa.remaining()) {
                        val d = abs(fa.get(k) - fp.get(k)); difSuma += d; if (d > difMax) difMax = d
                    }
                    val difMedia = (difSuma / fa.remaining()).toFloat()
                    maxDifEntrada = max(maxDifEntrada, difMax)
                    val desvAndroid = Preprocesador.desviacion(entrada)

                    // 2 y 3. Probabilidades y resultado con el mismo método que usa la pantalla de análisis
                    val a = clasificador.analizar(foto)
                    val porFiltro = a.resultado.codigo == "IMAGEN_NO_VALIDA" && a.msInferencia == 0.0
                    fun num(s: String) = s.toFloatOrNull() ?: Float.NaN
                    val hojaPy = num(c[iHoja]); val ojoPy = num(c[iOjo]); val royaPy = num(c[iRoya])
                    val hojaAn = if (porFiltro) Float.NaN else a.pHoja
                    val ojoAn = if (porFiltro) Float.NaN else a.pOjo
                    fun dif(x: Float, y: Float): Float = when {
                        x.isNaN() && y.isNaN() -> 0f
                        x.isNaN() || y.isNaN() -> 1f      // uno calculó la probabilidad y el otro no
                        else -> abs(x - y)
                    }
                    val difProb = maxOf(dif(hojaAn, hojaPy), dif(ojoAn, ojoPy), dif(a.pRoya, royaPy))
                    maxDifProb = max(maxDifProb, difProb)
                    val coincide = a.resultado.codigo == c[iRes]
                    if (coincide) coincidencias++
                    if (!coincide || difProb > tolerancia) {
                        fallas += "$nombre: Android ${a.resultado.codigo} / Python ${c[iRes]}, dif. prob. $difProb"
                    }
                    val fila = listOf(c[iClase], nombre, difMax, difMedia, desvAndroid, c[iDesv], hojaAn, hojaPy,
                        ojoAn, ojoPy, a.pRoya, royaPy, difProb, a.resultado.codigo, c[iRes], coincide,
                        "%.2f".format(java.util.Locale.US, a.msTotal)).joinToString(",")
                    w.println(fila)
                    Log.i("EQUIVALENCIA", fila)
                    foto.delete()
                }
            }
        }
        val total = lineas.drop(1).count { it.isNotBlank() }
        val resumen = "Imágenes: $total · resultados iguales: $coincidencias · dif. máx. de entrada: $maxDifEntrada · " +
            "dif. máx. de probabilidad: $maxDifProb · tolerancia: $tolerancia · archivo: ${salida.absolutePath}"
        Log.i("EQUIVALENCIA", resumen)
        assertTrue("$resumen\n" + fallas.joinToString("\n"), fallas.isEmpty())
    }
}
