package com.yuyagioh.tcgreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CardDao {

    @Query(
        "SELECT * FROM cards WHERE game = :game AND name LIKE :pattern ORDER BY name LIMIT :limit",
    )
    suspend fun search(game: String, pattern: String, limit: Int): List<CardEntity>

    @Query(
        "SELECT * FROM cards WHERE game = :game AND name = :name COLLATE NOCASE LIMIT 1",
    )
    suspend fun byName(game: String, name: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int
}
