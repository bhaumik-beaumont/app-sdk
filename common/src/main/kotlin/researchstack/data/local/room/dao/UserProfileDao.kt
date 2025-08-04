package researchstack.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import researchstack.domain.model.USER_PROFILE_TABLE_NAME
import researchstack.domain.model.UserProfile

@Dao
abstract class UserProfileDao : PrivDao<UserProfile>(USER_PROFILE_TABLE_NAME) {

    @Query("SELECT * FROM user_profile ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLatest(): Flow<UserProfile?>

    @Query("SELECT COUNT(*) FROM $USER_PROFILE_TABLE_NAME WHERE timestamp BETWEEN :start AND :end")
    abstract fun countBetween(start: Long, end: Long): Flow<Int>

    @Query("SELECT * FROM $USER_PROFILE_TABLE_NAME WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    abstract fun getBetween(start: Long, end: Long): Flow<List<UserProfile>>
}
