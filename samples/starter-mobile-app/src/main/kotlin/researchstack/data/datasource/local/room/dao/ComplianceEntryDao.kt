package researchstack.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import researchstack.domain.model.COMPLIANCE_ENTRY_TABLE_NAME
import researchstack.domain.model.ComplianceEntry

@Dao
interface ComplianceEntryDao {
    @Query("SELECT * FROM $COMPLIANCE_ENTRY_TABLE_NAME ORDER BY weekNumber ASC")
    fun getAll(): Flow<List<ComplianceEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entries: ComplianceEntry)

    @Query("DELETE FROM $COMPLIANCE_ENTRY_TABLE_NAME")
    suspend fun clear()
}

