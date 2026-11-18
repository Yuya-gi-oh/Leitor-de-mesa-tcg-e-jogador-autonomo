package com.yuyagioh.tcgreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CardEntity::class], version = 1, exportSchema = false)
abstract class TcgDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        const val NAME = "tcg_cache.db"
    }
}
