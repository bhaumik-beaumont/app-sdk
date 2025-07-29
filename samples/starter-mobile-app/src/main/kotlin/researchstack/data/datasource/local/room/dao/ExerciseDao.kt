package researchstack.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import researchstack.domain.model.healthConnect.EXERCISE_TABLE_NAME
import researchstack.domain.model.healthConnect.Exercise

@Dao
abstract class ExerciseDao : TimestampEntityBaseDao<Exercise>(EXERCISE_TABLE_NAME) {

    @Query("SELECT * FROM $EXERCISE_TABLE_NAME WHERE startTime >= :from ORDER BY startTime ASC")
    abstract fun getExercisesFrom(from: Long): Flow<List<Exercise>>
}
