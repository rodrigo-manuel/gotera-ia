package co.edu.ucc.ojodegallo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Diagnostico::class], version = 2, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {
    abstract fun dao(): DiagnosticoDao

    companion object {
        /** Versión 1 → 2: finca, lote y notas. Conserva el historial que ya existe en el teléfono. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diagnosticos ADD COLUMN finca TEXT")
                db.execSQL("ALTER TABLE diagnosticos ADD COLUMN lote TEXT")
                db.execSQL("ALTER TABLE diagnosticos ADD COLUMN notas TEXT")
            }
        }

        @Volatile private var instancia: BaseDatos? = null
        fun obtener(context: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, BaseDatos::class.java, "ojo_de_gallo.db")
                .addMigrations(MIGRACION_1_2)
                .build().also { instancia = it }
        }
    }
}
