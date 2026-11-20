package co.edu.ucc.ojodegallo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticoDao {
    @Query("SELECT * FROM diagnosticos ORDER BY fecha DESC")
    fun todos(): Flow<List<Diagnostico>>

    @Query("SELECT * FROM diagnosticos WHERE id = :id")
    suspend fun porId(id: Long): Diagnostico?

    @Insert
    suspend fun insertar(d: Diagnostico): Long

    @Update
    suspend fun actualizar(d: Diagnostico)

    @Delete
    suspend fun eliminar(d: Diagnostico)

    /** Fincas y lotes ya usados, para sugerirlos y no tener que escribirlos otra vez. */
    @Query("SELECT DISTINCT finca FROM diagnosticos WHERE finca IS NOT NULL AND finca != '' ORDER BY finca")
    fun fincas(): Flow<List<String>>

    @Query("SELECT DISTINCT lote FROM diagnosticos WHERE lote IS NOT NULL AND lote != '' ORDER BY lote")
    fun lotes(): Flow<List<String>>
}
