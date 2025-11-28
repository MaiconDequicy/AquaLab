package br.iots.aqualab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.iots.aqualab.model.MedicaoManual
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicaoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(medicao: MedicaoManual)

    @Query("SELECT * FROM medicoes_manuais WHERE pontoIdNuvem = :pontoId ORDER BY timestamp DESC")
    fun getMedicoesPorPonto(pontoId: String): Flow<List<MedicaoManual>>

    @Query("SELECT * FROM medicoes_manuais WHERE sincronizado = 0")
    suspend fun getNaoSincronizadas(): List<MedicaoManual>

    @Query("DELETE FROM medicoes_manuais WHERE pontoIdNuvem = :pontoId")
    suspend fun deletarMedicoesDoPonto(pontoId: String)
}