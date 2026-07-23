package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "context_pairs")
data class ContextPairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prevWord: String,
    val nextWord: String,
    val frequency: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ContextPairDao {
    @Query("SELECT nextWord FROM context_pairs WHERE prevWord = :prevWord ORDER BY frequency DESC, timestamp DESC LIMIT 6")
    suspend fun getNextWordPredictions(prevWord: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(pair: ContextPairEntity)

    @Query("SELECT * FROM context_pairs ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAllPairs(): List<ContextPairEntity>
}
