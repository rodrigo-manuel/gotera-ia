package co.edu.ucc.ojodegallo.ui

import android.app.Application
import android.net.Uri
import android.os.Debug
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import co.edu.ucc.ojodegallo.data.Diagnostico
import co.edu.ucc.ojodegallo.data.Repositorio
import co.edu.ucc.ojodegallo.ml.Analisis
import co.edu.ucc.ojodegallo.ml.Clasificador
import co.edu.ucc.ojodegallo.ml.Preprocesador
import co.edu.ucc.ojodegallo.ml.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil

sealed class EstadoAnalisis {
    object Inactivo : EstadoAnalisis()
    /** La foto se carga antes del análisis para mostrarla siempre, aunque después se descarte. */
    data class Procesando(val foto: ImageBitmap? = null) : EstadoAnalisis()
    data class Listo(val archivo: File, val analisis: Analisis, val guardado: Boolean, val foto: ImageBitmap?) : EstadoAnalisis()
    data class Error(val mensaje: String) : EstadoAnalisis()
}

/** Una fila del modo de prueba (equivalencia Python–Android, PB-15). */
data class FilaPrueba(val imagen: String, val a: Analisis)

data class EstadoPrueba(
    val ocupado: Boolean = false,
    val progreso: String = "",
    val filas: List<FilaPrueba> = emptyList(),
    val tiemposMs: List<Double> = emptyList(),
    val primerUsoMs: Double? = null,
    val cargaModelosMs: Double? = null,
    val memoriaPssMb: Double? = null,
    val aviso: String = "",
)

/**
 * ViewModel de la aplicación (patrón MVVM): conecta la interfaz con la lógica (ml) y los datos (data).
 * Se comparte entre pantallas para cargar los modelos una sola vez.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repositorio(app)
    private var clasificador: Clasificador? = null
    private var primerUsoMs: Double? = null

    private val _estado = MutableStateFlow<EstadoAnalisis>(EstadoAnalisis.Inactivo)
    val estado: StateFlow<EstadoAnalisis> = _estado.asStateFlow()

    val historial: StateFlow<List<Diagnostico>> =
        repo.historial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _prueba = MutableStateFlow(EstadoPrueba())
    val prueba: StateFlow<EstadoPrueba> = _prueba.asStateFlow()

    private fun modelo(): Clasificador = clasificador ?: Clasificador(getApplication()).also { clasificador = it }

    private fun analizarMidiendo(archivo: File): Analisis {
        val t0 = System.nanoTime()
        val a = modelo().analizar(archivo)
        if (primerUsoMs == null) primerUsoMs = (System.nanoTime() - t0) / 1e6   // incluye la carga de los modelos
        return a
    }

    /** Analiza una foto y, si el resultado es un diagnóstico, lo guarda en el historial (CU-01, RF-07). */
    fun analizar(archivo: File) {
        _estado.value = EstadoAnalisis.Procesando()
        viewModelScope.launch {
            try {
                val foto = withContext(Dispatchers.IO) {
                    try { Preprocesador.miniatura(archivo)?.asImageBitmap() } catch (e: Throwable) { null }
                }
                _estado.value = EstadoAnalisis.Procesando(foto)
                val a = withContext(Dispatchers.Default) { analizarMidiendo(archivo) }
                val esDiagnostico = a.resultado is Resultado.Compatible || a.resultado is Resultado.SinSintomas ||
                    a.resultado is Resultado.CompatibleRoya || a.resultado is Resultado.OjoORoya
                if (esDiagnostico) repo.guardar(archivo, a) else repo.descartar(archivo)
                _estado.value = EstadoAnalisis.Listo(archivo, a, esDiagnostico, foto)
            } catch (e: Throwable) {
                repo.descartar(archivo)
                _estado.value = EstadoAnalisis.Error("No se pudo analizar la imagen. Intenta con otra foto.")
            }
        }
    }

    fun analizarDeGaleria(uri: Uri) {
        _estado.value = EstadoAnalisis.Procesando()
        viewModelScope.launch {
            try {
                analizar(repo.copiarDeGaleria(uri))
            } catch (e: Throwable) {
                _estado.value = EstadoAnalisis.Error("No se pudo abrir la imagen elegida.")
            }
        }
    }

    val fincas: StateFlow<List<String>> = repo.fincas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lotes: StateFlow<List<String>> = repo.lotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizar(d: Diagnostico) {
        viewModelScope.launch { repo.actualizar(d) }
    }

    fun eliminar(d: Diagnostico) {
        viewModelScope.launch { repo.eliminar(d) }
    }

    suspend fun porId(id: Long): Diagnostico? = repo.porId(id)

    // ------------------------------------------------------------ Modo de prueba
    private fun nombre(uri: Uri): String {
        getApplication<Application>().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) return c.getString(0) }
        return uri.lastPathSegment ?: "imagen"
    }

    private fun copiarTemporal(uri: Uri): File {
        val f = File.createTempFile("prueba_", ".jpg", getApplication<Application>().cacheDir)
        getApplication<Application>().contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        return f
    }

    /** Ejecuta los dos modelos sobre varias imágenes y registra sus salidas (equivalencia con Python). */
    fun probarImagenes(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            _prueba.value = _prueba.value.copy(ocupado = true, filas = emptyList())
            val filas = mutableListOf<FilaPrueba>()
            uris.forEachIndexed { i, uri ->
                _prueba.value = _prueba.value.copy(progreso = "Analizando ${i + 1} de ${uris.size}")
                try {
                    val f = copiarTemporal(uri)
                    try { filas += FilaPrueba(nombre(uri), analizarMidiendo(f)) } finally { f.delete() }
                } catch (e: Exception) { /* imagen ilegible: se omite y se sigue con las demás */ }
            }
            _prueba.value = _prueba.value.copy(ocupado = false, progreso = "Listo: ${filas.size} imágenes",
                filas = filas, primerUsoMs = primerUsoMs, cargaModelosMs = clasificador?.msCarga, memoriaPssMb = memoria())
            persistir()
        }
    }

    /** RNF-02: 5 ejecuciones de calentamiento (no se cuentan) y 100 mediciones del análisis completo. */
    fun medirTiempo(uri: Uri, calentamiento: Int = 5, repeticiones: Int = 100) {
        viewModelScope.launch(Dispatchers.Default) {
            _prueba.value = _prueba.value.copy(ocupado = true, tiemposMs = emptyList())
            val f = copiarTemporal(uri)
            try {
                repeat(calentamiento) {
                    _prueba.value = _prueba.value.copy(progreso = "Calentamiento ${it + 1} de $calentamiento")
                    analizarMidiendo(f)
                }
                val tiempos = mutableListOf<Double>()
                repeat(repeticiones) {
                    _prueba.value = _prueba.value.copy(progreso = "Medición ${it + 1} de $repeticiones")
                    tiempos += analizarMidiendo(f).msTotal
                }
                _prueba.value = _prueba.value.copy(ocupado = false, progreso = "Listo: $repeticiones mediciones",
                    tiemposMs = tiempos, primerUsoMs = primerUsoMs, cargaModelosMs = clasificador?.msCarga, memoriaPssMb = memoria())
                persistir()
            } finally { f.delete() }
        }
    }

    private fun memoria(): Double {
        val info = Debug.MemoryInfo(); Debug.getMemoryInfo(info)
        return info.totalPss / 1024.0
    }

    /** Archivo CSV con todo lo medido en el modo de prueba. */
    fun csvPrueba(): String {
        val p = _prueba.value
        val sb = StringBuilder()
        sb.appendLine("# dispositivo,${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL},android,${android.os.Build.VERSION.RELEASE},api,${android.os.Build.VERSION.SDK_INT}")
        clasificador?.let { sb.appendLine("# detector_roya,${if (it.usaDetectorRoya) "si" else "no"},umbral_roya,${it.umbralRoya}") }
        p.cargaModelosMs?.let { sb.appendLine("# carga_modelos_ms,%.2f".format(java.util.Locale.US, it)) }
        p.primerUsoMs?.let { sb.appendLine("# primer_uso_ms,%.2f".format(java.util.Locale.US, it)) }
        p.memoriaPssMb?.let { sb.appendLine("# memoria_pss_mb,%.1f".format(java.util.Locale.US, it)) }
        if (p.tiemposMs.isNotEmpty()) {
            val s = resumen(p.tiemposMs)
            sb.appendLine("# tiempo_n,${p.tiemposMs.size},mediana_ms,%.2f,p95_ms,%.2f,max_ms,%.2f".format(java.util.Locale.US, s.first, s.second, s.third))
        }
        sb.appendLine("imagen,p_hoja,p_ojo_de_gallo,p_roya,resultado,ms_preprocesamiento,ms_inferencia,ms_total")
        p.filas.forEach { f ->
            sb.appendLine("\"${f.imagen}\",%.6f,%.6f,%.6f,%s,%.2f,%.2f,%.2f".format(java.util.Locale.US,
                f.a.pHoja, f.a.pOjo, f.a.pRoya, f.a.resultado.codigo, f.a.msPreprocesamiento, f.a.msInferencia, f.a.msTotal))
        }
        if (p.tiemposMs.isNotEmpty()) {
            sb.appendLine("medicion,ms_total")
            p.tiemposMs.forEachIndexed { i, t -> sb.appendLine("${i + 1},%.2f".format(java.util.Locale.US, t)) }
        }
        return sb.toString()
    }

    /**
     * Copia de seguridad del último CSV en la memoria interna. Al abrir el selector de archivos, algunos
     * teléfonos (por ejemplo, MIUI) cierran la aplicación en segundo plano y se pierden los datos en memoria.
     */
    private val archivoCsv get() = File(getApplication<Application>().filesDir, "ultima_prueba.csv")

    private fun persistir() {
        try { archivoCsv.writeText(csvPrueba()) } catch (_: Exception) { }
    }

    private fun hayDatos() = _prueba.value.filas.isNotEmpty() || _prueba.value.tiemposMs.isNotEmpty()

    fun guardarCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val texto = if (hayDatos()) csvPrueba() else if (archivoCsv.exists()) archivoCsv.readText() else null
            if (texto == null) {
                _prueba.value = _prueba.value.copy(aviso = "No hay mediciones para exportar. Ejecuta primero la prueba 1 o la 2.")
                return@launch
            }
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(texto.toByteArray()) }
                _prueba.value = _prueba.value.copy(aviso = "CSV guardado.")
            } catch (e: Exception) {
                _prueba.value = _prueba.value.copy(aviso = "No se pudo guardar el CSV.")
            }
        }
    }

    override fun onCleared() { clasificador?.close() }

    companion object {
        /** Mediana, percentil 95 (método del rango más cercano) y máximo. */
        fun resumen(t: List<Double>): Triple<Double, Double, Double> {
            val s = t.sorted()
            val med = if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
            val p95 = s[(ceil(0.95 * s.size).toInt() - 1).coerceIn(0, s.size - 1)]
            return Triple(med, p95, s.last())
        }
    }
}
