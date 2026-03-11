package top.nihoru.app.data.repository

import androidx.room.*
import top.nihoru.app.data.model.AnimeEntry

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime_list WHERE mode = :mode ORDER BY rowid ASC")
    suspend fun getAll(mode: String = "anime"): List<AnimeEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AnimeEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<AnimeEntry>)

    @Delete
    suspend fun delete(entry: AnimeEntry)

    @Query("DELETE FROM anime_list WHERE malId = :malId")
    suspend fun deleteById(malId: Int)

    @Query("DELETE FROM anime_list WHERE mode = :mode")
    suspend fun clearAll(mode: String)

    @Query("SELECT * FROM anime_list WHERE malId = :malId LIMIT 1")
    suspend fun findById(malId: Int): AnimeEntry?

    @Update
    suspend fun update(entry: AnimeEntry)
}

@Database(entities = [AnimeEntry::class], version = 1, exportSchema = false)
abstract class NihoruDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
}
