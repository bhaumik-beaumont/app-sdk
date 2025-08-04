package researchstack.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import researchstack.domain.model.priv.BIA_TABLE_NAME
import researchstack.domain.model.priv.Bia

@Dao
abstract class BiaDao : PrivDao<Bia>(BIA_TABLE_NAME) {

    @Query("SELECT COUNT(*) FROM $BIA_TABLE_NAME WHERE timestamp BETWEEN :start AND :end")
    abstract fun countBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM $BIA_TABLE_NAME WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    abstract fun getBetween(start: Long, end: Long): Flow<List<Bia>>
}
