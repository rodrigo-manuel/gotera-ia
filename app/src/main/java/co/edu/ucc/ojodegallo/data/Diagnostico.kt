package co.edu.ucc.ojodegallo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un diagnóstico guardado en el teléfono (RF-07): foto, fecha y resultado.
 * Desde la versión 2 de la base de datos, el caficultor puede anotar la finca o vereda, el lote y observaciones,
 * y corregir la fecha. La ubicación se escribe a mano: la aplicación no pide permiso de GPS (solo usa la cámara).
 */
@Entity(tableName = "diagnosticos")
data class Diagnostico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutaFoto: String,
    val fecha: Long,
    val resultado: String,   // código de Resultado (por ejemplo, COMPATIBLE_OJO_DE_GALLO)
    val certeza: String?,    // ALTA, MEDIA o null
    val pHoja: Float,
    val pOjo: Float,
    val finca: String? = null,
    val lote: String? = null,
    val notas: String? = null,
)
