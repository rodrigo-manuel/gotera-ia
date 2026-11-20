package co.edu.ucc.ojodegallo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Modo de prueba para la evaluación técnica (no lo usa el caficultor):
 * - equivalencia Python–Android (PB-15): salidas de los dos modelos por imagen;
 * - tiempo de respuesta (RNF-02): 5 de calentamiento + 100 mediciones, mediana y percentil 95;
 * - carga de los modelos, primer uso y memoria (PSS).
 * Todo se exporta a un archivo CSV.
 */
@Composable
fun PruebaScreen(vm: AppViewModel, volver: () -> Unit) {
    val p by vm.prueba.collectAsState()
    val elegirVarias = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.probarImagenes(uris)
    }
    val elegirUna = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.medirTiempo(uri)
    }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.guardarCsv(uri)
    }
    fun f(x: Double) = String.format(Locale.US, "%.1f", x)

    Pantalla("Modo de prueba", volver) {
        Parrafo("Herramienta para medir la aplicación. Los resultados se exportan a un archivo CSV.", color = TintaSuave)
        Espacio(12.dp)
        BotonPrincipal("1. Equivalencia: elegir imágenes", Iconos.Galeria) { if (!p.ocupado) elegirVarias.launch(arrayOf("image/*")) }
        Espacio(8.dp)
        BotonPrincipal("2. Tiempo: una imagen (5 + 100)", Iconos.Medir) { if (!p.ocupado) elegirUna.launch(arrayOf("image/*")) }
        Espacio(8.dp)
        BotonSecundario("3. Exportar CSV") {
            if (!p.ocupado) exportar.launch("prueba_${android.os.Build.MODEL.replace(' ', '_')}.csv")
        }
        Espacio(12.dp)
        if (p.aviso.isNotEmpty()) Parrafo(p.aviso, negrita = true, color = VerdeHoja)
        if (p.progreso.isNotEmpty()) Parrafo(p.progreso, negrita = true)
        p.cargaModelosMs?.let { Parrafo("Carga de los modelos: ${f(it)} ms") }
        p.primerUsoMs?.let { Parrafo("Primer análisis (incluye la carga): ${f(it)} ms") }
        p.memoriaPssMb?.let { Parrafo("Memoria usada (PSS): ${f(it)} MB") }
        if (p.tiemposMs.isNotEmpty()) {
            val (med, p95, max) = AppViewModel.resumen(p.tiemposMs)
            Parrafo("Tiempo de ${p.tiemposMs.size} análisis: mediana ${f(med)} ms · percentil 95 ${f(p95)} ms · máximo ${f(max)} ms", negrita = true)
        }
        p.filas.take(30).forEach {
            Parrafo("${it.imagen}: hoja ${String.format(Locale.US, "%.3f", it.a.pHoja)}, ojo ${String.format(Locale.US, "%.3f", it.a.pOjo)} → ${it.a.resultado.codigo}")
        }
        if (p.filas.size > 30) Parrafo("… y ${p.filas.size - 30} más en el CSV.")
    }
}
