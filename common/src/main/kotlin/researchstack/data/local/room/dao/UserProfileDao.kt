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
}
