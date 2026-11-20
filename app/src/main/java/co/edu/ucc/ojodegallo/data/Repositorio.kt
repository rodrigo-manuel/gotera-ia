package co.edu.ucc.ojodegallo.data

import android.content.Context
import android.net.Uri
import co.edu.ucc.ojodegallo.ml.Analisis
import co.edu.ucc.ojodegallo.ml.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/** Capa de datos: fotos en la memoria interna del teléfono e historial en Room (SQLite). */
class Repositorio(private val context: Context) {
    private val dao = BaseDatos.obtener(context).dao()
    private val carpetaFotos = File(context.filesDir, "fotos").apply { mkdirs() }

    val historial: Flow<List<Diagnostico>> = dao.todos()

    fun nuevoArchivoFoto(): File = File(carpetaFotos, "hoja_${System.currentTimeMillis()}.jpg")

    /** Copia a la memoria interna una foto elegida de la galería (RF-02). */
    suspend fun copiarDeGaleria(uri: Uri): File = withContext(Dispatchers.IO) {
        val destino = nuevoArchivoFoto()
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: throw IllegalArgumentException("No se pudo abrir la imagen")
        destino
    }

    suspend fun guardar(archivo: File, a: Analisis): Long = withContext(Dispatchers.IO) {
        val certeza = when (val r = a.resultado) {
            is Resultado.Compatible -> r.certeza.name
            is Resultado.SinSintomas -> r.certeza.name
            is Resultado.CompatibleRoya -> r.certeza.name
            else -> null
        }
        dao.insertar(Diagnostico(rutaFoto = archivo.absolutePath, fecha = System.currentTimeMillis(),
            resultado = a.resultado.codigo, certeza = certeza, pHoja = a.pHoja, pOjo = a.pOjo))
    }

    suspend fun porId(id: Long): Diagnostico? = dao.porId(id)

    /** Guarda la fecha, la finca, el lote y las notas que el caficultor editó. */
    suspend fun actualizar(d: Diagnostico) = withContext(Dispatchers.IO) { dao.actualizar(d) }

    val fincas: Flow<List<String>> = dao.fincas()
    val lotes: Flow<List<String>> = dao.lotes()

    /** Elimina el registro y su foto (RF-08, CU-04). */
    suspend fun eliminar(d: Diagnostico) = withContext(Dispatchers.IO) {
        dao.eliminar(d)
        File(d.rutaFoto).delete()
    }

    /** Borra una foto que no se guardó en el historial (por ejemplo, una imagen no válida). */
    fun descartar(archivo: File) { archivo.delete() }
}
