package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWordDao {
    @Query("SELECT * FROM user_words WHERE keySequence = :sequence ORDER BY frequency DESC, word ASC")
    suspend fun getWordsForSequence(sequence: String): List<UserWordEntity>

    @Query("SELECT * FROM user_words ORDER BY createdAt DESC")
    fun getAllWordsFlow(): Flow<List<UserWordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: UserWordEntity)

    @Delete
    suspend fun deleteWord(word: UserWordEntity)

    @Query("SELECT * FROM user_words WHERE word = :word LIMIT 1")
    suspend fun findWord(word: String): UserWordEntity?
}
