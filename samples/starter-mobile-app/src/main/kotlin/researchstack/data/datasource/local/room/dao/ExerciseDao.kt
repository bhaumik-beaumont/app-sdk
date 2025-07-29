package researchstack.data.datasource.local.room.dao

import androidx.room.Dao
import researchstack.domain.model.healthConnect.EXERCISE_TABLE_NAME
import researchstack.domain.model.healthConnect.Exercise

@Dao
abstract class ExerciseDao : TimestampEntityBaseDao<Exercise>(EXERCISE_TABLE_NAME)
