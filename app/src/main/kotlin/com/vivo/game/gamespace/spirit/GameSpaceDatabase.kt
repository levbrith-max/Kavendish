package com.vivo.game.gamespace.spirit

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CustomGameEntry::class],
    version = 1,
    exportSchema = false
)
abstract class GameSpaceDatabase : RoomDatabase() {

    abstract fun customGameDao(): CustomGameDao

    companion object {
        @Volatile
        private var INSTANCE: GameSpaceDatabase? = null

        fun getInstance(context: Context): GameSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameSpaceDatabase::class.java,
                    "game_space_custom.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
