package com.example.t9keyboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.t9keyboard.logic.T9Engine

@Database(entities = [T9Word::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun t9WordDao(): T9WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "t9_keyboard_db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.t9WordDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: T9WordDao) {
                if (dao.getWordCount() == 0) {
                    val bnWords = listOf(
                        "আমি" to 950,
                        "আমার" to 920,
                        "আমাগো" to 400,
                        "তুমি" to 920,
                        "তুমিও" to 500,
                        "তোমার" to 880,
                        "কেন" to 850,
                        "কে" to 800,
                        "বাংলা" to 980,
                        "বাংলাদেশ" to 990,
                        "কিবোর্ড" to 700,
                        "ভালো" to 880,
                        "আছি" to 780,
                        "আছ" to 720,
                        "কীভাবে" to 650,
                        "আজকে" to 820,
                        "আজ" to 800,
                        "ধন্যবাদ" to 880,
                        "স্বাগতম" to 700,
                        "কর" to 920,
                        "করা" to 900,
                        "করে" to 890,
                        "করব" to 870,
                        "করতেছি" to 750,
                        "কারণ" to 820,
                        "কাজ" to 880,
                        "কথা" to 880,
                        "কাল" to 820,
                        "কোথায়" to 780,
                        "কোথাও" to 650,
                        "খবর" to 800,
                        "ঘর" to 750,
                        "বই" to 750,
                        "বন্ধ" to 750,
                        "বন্ধু" to 850,
                        "মন" to 800,
                        "মানুষ" to 820,
                        "না" to 950,
                        "হ্যাঁ" to 950,
                        "সব" to 850,
                        "সবাই" to 820,
                        "বলো" to 820,
                        "বল" to 780,
                        "বলা" to 780,
                        "একটু" to 820,
                        "এখন" to 880,
                        "চা" to 750,
                        "পানি" to 800,
                        "ভাত" to 800,
                        "বাড়ি" to 800,
                        "ভাই" to 850,
                        "বোন" to 800,
                        "মা" to 950,
                        "বাবা" to 950,
                        "কেমন" to 880,
                        "কোথায়" to 800,
                        "সময়" to 820
                    )

                    val enWords = listOf(
                        "hello" to 900,
                        "hi" to 950,
                        "world" to 850,
                        "thank" to 850,
                        "thanks" to 900,
                        "you" to 950,
                        "your" to 900,
                        "are" to 900,
                        "how" to 900,
                        "good" to 900,
                        "morning" to 850,
                        "night" to 800,
                        "fine" to 800,
                        "yes" to 900,
                        "no" to 900,
                        "bangla" to 800,
                        "keyboard" to 850,
                        "typing" to 800,
                        "love" to 850,
                        "friend" to 850,
                        "work" to 800,
                        "today" to 800,
                        "time" to 850,
                        "see" to 850,
                        "again" to 750,
                        "under" to 400
                    )

                    val wordEntities = mutableListOf<T9Word>()

                    for ((word, freq) in bnWords) {
                        val seq = T9Engine.textToT9Sequence(word, isBangla = true)
                        wordEntities.add(T9Word(sequence = seq, word = word, language = "BN", frequency = freq))
                    }

                    for ((word, freq) in enWords) {
                        val seq = T9Engine.textToT9Sequence(word, isBangla = false)
                        wordEntities.add(T9Word(sequence = seq, word = word, language = "EN", frequency = freq))
                    }

                    dao.insertAll(wordEntities)
                }
            }
        }
    }
}
