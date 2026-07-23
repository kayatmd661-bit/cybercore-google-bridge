package com.example.t9keyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "t9_words")
data class T9Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequence: String,
    val word: String,
    val language: String = "BN", // "BN" or "EN"
    val frequency: Int = 100
)
