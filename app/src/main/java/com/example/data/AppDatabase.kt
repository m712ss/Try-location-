package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_pins")
data class SavedPin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val originalUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String, // e.g., "أكل وشرب", "تسوق", "ترفيه", "سياحة", "عمل", "أخرى"
    val description: String? = null,
    val timeSpentMinutes: Int = 45
)

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val originName: String,
    val originLat: Double,
    val originLng: Double,
    val destName: String,
    val destLat: Double,
    val destLng: Double,
    val optimizedOrderJson: String, // JSON array of SavedPin or comma separated IDs
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String? = null
)

@Dao
interface MapRouteDao {
    @Query("SELECT * FROM saved_pins ORDER BY id DESC")
    fun getAllPins(): Flow<List<SavedPin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: SavedPin): Long

    @Query("DELETE FROM saved_pins WHERE id = :id")
    suspend fun deletePinById(id: Int)

    @Query("DELETE FROM saved_pins")
    suspend fun clearAllPins()

    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    fun getAllRoutes(): Flow<List<SavedRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRoute): Long

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRouteById(id: Int)

    @Query("UPDATE saved_routes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateRouteFavorite(id: Int, isFavorite: Boolean)
}

@Database(entities = [SavedPin::class, SavedRoute::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MapRouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "map_route_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
