package com.vivo.game.gamespace.spirit

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CustomGameDao {

    @Query("SELECT * FROM custom_games ORDER BY added_at DESC")
    fun getAllGames(): LiveData<List<CustomGameEntry>>

    @Query("SELECT * FROM custom_games ORDER BY added_at DESC")
    suspend fun getAllGamesSync(): List<CustomGameEntry>

    @Query("SELECT * FROM custom_games WHERE type = :type ORDER BY title ASC")
    fun getGamesByType(type: Int): LiveData<List<CustomGameEntry>>

    @Query("SELECT * FROM custom_games WHERE collection_name = :collection ORDER BY title ASC")
    fun getGamesByCollection(collection: String): LiveData<List<CustomGameEntry>>

    @Query("SELECT DISTINCT collection_name FROM custom_games WHERE collection_name IS NOT NULL")
    fun getAllCollectionNames(): LiveData<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: CustomGameEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<CustomGameEntry>)

    @Delete
    suspend fun delete(game: CustomGameEntry)

    @Query("DELETE FROM custom_games WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM custom_games WHERE collection_name = :collectionName")
    suspend fun deleteCollection(collectionName: String)

    @Query("SELECT COUNT(*) FROM custom_games WHERE package_name = :packageName")
    suspend fun countByPackage(packageName: String): Int

    @Query("SELECT COUNT(*) FROM custom_games")
    suspend fun count(): Int
}
