package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.EmergencyReport
import com.example.data.model.RegisteredDevice
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "photo_history")
data class PhotoHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val displayName: String,
    val dateTaken: String,
    val timeTaken: String,
    val latitude: Double?,
    val longitude: Double?,
    val city: String,
    val country: String,
    val inspectedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_history")
data class ActivityHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val detail: String,
    val country: String,
    val testEntry: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "demo_emergency_reports")
data class DemoEmergencyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val emergencyType: String,
    val status: String,
    val country: String,
    val region: String,
    val province: String?,
    val localArea: String,
    val latitude: Double,
    val longitude: Double,
    val reportedTime: String,
    val reportedDate: String,
    val testAlert: Boolean,
    val discreetMode: Boolean,
    val note: String?,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): EmergencyReport = EmergencyReport(
        id = id,
        userId = userId,
        emergencyType = emergencyType,
        status = status,
        country = country,
        region = region,
        province = province,
        localArea = localArea,
        latitude = latitude,
        longitude = longitude,
        reportedTime = reportedTime,
        reportedDate = reportedDate,
        testAlert = testAlert,
        discreetMode = discreetMode,
        note = note,
        localDemoOnly = true
    )
}

@Entity(tableName = "demo_devices")
data class DemoDeviceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceName: String,
    val deviceModel: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val lastLocationUpdateText: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): RegisteredDevice = RegisteredDevice(
        id = id,
        userId = userId,
        deviceName = deviceName,
        deviceModel = deviceModel,
        country = country,
        latitude = latitude,
        longitude = longitude,
        status = status,
        lastLocationUpdateText = lastLocationUpdateText
    )
}

@Dao
interface OsasDao {
    @Query("SELECT * FROM photo_history ORDER BY inspectedAtMillis DESC")
    fun observePhotoHistory(): Flow<List<PhotoHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoHistory(item: PhotoHistoryEntity)

    @Query("SELECT * FROM activity_history ORDER BY timestampMillis DESC LIMIT 100")
    fun observeActivityHistory(): Flow<List<ActivityHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(item: ActivityHistoryEntity)

    @Query("SELECT * FROM demo_emergency_reports WHERE country = :country ORDER BY createdAtMillis DESC")
    fun observeDemoReportsByCountry(country: String): Flow<List<DemoEmergencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDemoReport(item: DemoEmergencyEntity)

    @Query("UPDATE demo_emergency_reports SET latitude = :lat, longitude = :lon WHERE id = :id")
    suspend fun updateDemoReportLocation(id: String, lat: Double, lon: Double)

    @Query("UPDATE demo_emergency_reports SET status = :status, note = :note WHERE id = :id")
    suspend fun updateDemoReportStatus(id: String, status: String, note: String?)

    @Query("SELECT * FROM demo_devices ORDER BY updatedAtMillis DESC")
    fun observeDemoDevices(): Flow<List<DemoDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDemoDevice(item: DemoDeviceEntity)

    @Query("DELETE FROM demo_devices WHERE id = :id")
    suspend fun deleteDemoDevice(id: String)
}

@Database(
    entities = [
        PhotoHistoryEntity::class,
        ActivityHistoryEntity::class,
        DemoEmergencyEntity::class,
        DemoDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OsasLocalDatabase : RoomDatabase() {
    abstract fun osasDao(): OsasDao

    companion object {
        @Volatile
        private var INSTANCE: OsasLocalDatabase? = null

        fun getInstance(context: Context): OsasLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OsasLocalDatabase::class.java,
                    "osas_ben_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
