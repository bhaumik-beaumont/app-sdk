package researchstack.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import researchstack.data.local.room.converter.ECGConverter
import researchstack.data.local.room.converter.HeartRateConverter
import researchstack.data.local.room.dao.AccelerometerDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.EcgDao
import researchstack.data.local.room.dao.HeartRateDao
import researchstack.data.local.room.dao.PassiveDataStatusDao
import researchstack.data.local.room.dao.PpgGreenDao
import researchstack.data.local.room.dao.PpgIrDao
import researchstack.data.local.room.dao.PpgRedDao
import researchstack.data.local.room.dao.SpO2Dao
import researchstack.data.local.room.dao.SweatLossDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.data.local.room.entity.PassiveDataStatusEntity
import researchstack.domain.model.priv.Accelerometer
import researchstack.domain.model.priv.Bia
import researchstack.domain.model.priv.EcgSet
import researchstack.domain.model.priv.HeartRate
import researchstack.domain.model.priv.PpgGreen
import researchstack.domain.model.priv.PpgIr
import researchstack.domain.model.priv.PpgRed
import researchstack.domain.model.priv.SpO2
import researchstack.domain.model.priv.SweatLoss
import researchstack.domain.model.UserProfile
import researchstack.domain.model.priv.BIA_TABLE_NAME

@Database(
    version = 5,
    exportSchema = false,

    entities = [
        Accelerometer::class,
        Bia::class,
        EcgSet::class,
        PpgGreen::class,
        PpgIr::class,
        PpgRed::class,
        SpO2::class,
        SweatLoss::class,
        HeartRate::class,
        PassiveDataStatusEntity::class,
        UserProfile::class,
    ],
)
@TypeConverters(
    value = [
        ECGConverter::class,
        HeartRateConverter::class,
    ]
)
abstract class WearableAppDataBase : RoomDatabase() {
    abstract fun accelerometerDao(): AccelerometerDao
    abstract fun biaDao(): BiaDao
    abstract fun ecgDao(): EcgDao
    abstract fun ppgGreenDao(): PpgGreenDao
    abstract fun ppgIrDao(): PpgIrDao
    abstract fun ppgRedDao(): PpgRedDao
    abstract fun spO2Dao(): SpO2Dao
    abstract fun sweatLossDao(): SweatLossDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun passiveDataStatusDao(): PassiveDataStatusDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: WearableAppDataBase? = null

        private val MIGRATION_3_4 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE $BIA_TABLE_NAME ADD COLUMN weekNumber int NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE $BIA_TABLE_NAME ADD COLUMN id TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(
            context: Context,
        ): WearableAppDataBase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WearableAppDataBase::class.java,
                    "wearable_app_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .enableMultiInstanceInvalidation()
                    .addTypeConverter(ECGConverter())
                    .addTypeConverter(HeartRateConverter())
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
