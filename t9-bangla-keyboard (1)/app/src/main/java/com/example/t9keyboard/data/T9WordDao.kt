package com.example.t9keyboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface T9WordDao {
    @Query("SELECT * FROM t9_words WHERE sequence = :seq AND language = :lang ORDER BY frequency DESC")
    suspend fun getWordsForSequence(seq: String, lang: String): List<T9Word>

    @Query("SELECT * FROM t9_words WHERE sequence LIKE :prefix || '%' AND language = :lang ORDER BY frequency DESC LIMIT 15")
    suspend fun getWordsStartingWithSequence(prefix: String, lang: String): List<T9Word>

    @Query("SELECT * FROM t9_words ORDER BY sequence ASC")
    fun getAllWordsFlow(): Flow<List<T9Word>>

    @Query("SELECT * FROM t9_words")
    suspend fun getAllWordsSync(): List<T9Word>

    @Query("SELECT * FROM t9_words WHERE word LIKE '%' || :query || '%' OR sequence LIKE '%' || :query || '%' ORDER BY frequency DESC")
    fun searchWords(query: String): Flow<List<T9Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: T9Word): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<T9Word>)

    @Delete
    suspend fun deleteWord(word: T9Word)

    @Update
    suspend fun updateWord(word: T9Word)

    @Query("SELECT COUNT(*) FROM t9_words")
    suspend fun getWordCount(): Int
}
