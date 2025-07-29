package researchstack.data.local.room.dao

import androidx.room.Dao
import researchstack.domain.model.USER_PROFILE_TABLE_NAME
import researchstack.domain.model.UserProfile

@Dao
abstract class UserProfileDao : PrivDao<UserProfile>(USER_PROFILE_TABLE_NAME)
