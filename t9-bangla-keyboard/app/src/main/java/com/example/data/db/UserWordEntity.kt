package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val keySequence: String,
    val frequency: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
