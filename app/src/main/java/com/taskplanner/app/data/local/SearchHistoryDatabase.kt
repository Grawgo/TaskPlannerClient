package com.taskplanner.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "search_history", primaryKeys = ["ownerUid", "query"])
data class SearchHistoryEntity(val ownerUid: String, val query: String, val selectedAt: Long)

@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history WHERE ownerUid = :uid ORDER BY selectedAt DESC, query DESC LIMIT 10")
    fun observe(uid: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE ownerUid = :uid AND query NOT IN (SELECT query FROM search_history WHERE ownerUid = :uid ORDER BY selectedAt DESC, query DESC LIMIT 10)")
    suspend fun trim(uid: String)

    @Query("DELETE FROM search_history WHERE ownerUid = :uid")
    suspend fun clear(uid: String)

    @Transaction
    suspend fun remember(entity: SearchHistoryEntity) {
        upsert(entity)
        trim(entity.ownerUid)
    }
}

@Database(entities = [SearchHistoryEntity::class], version = 1, exportSchema = true)
abstract class SearchHistoryDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
}
