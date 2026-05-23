package com.vivo.game.gamespace.spirit

import android.content.Context
import androidx.lifecycle.LiveData
import com.vivo.game.gamespace.utils.PegasusMetadataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CustomGameRepository(context: Context) {

    private val dao = GameSpaceDatabase.getInstance(context).customGameDao()

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    fun getAllGames(): LiveData<List<CustomGameEntry>> = dao.getAllGames()

    fun getGamesByCollection(collection: String): LiveData<List<CustomGameEntry>> =
        dao.getGamesByCollection(collection)

    fun getAllCollectionNames(): LiveData<List<String>> = dao.getAllCollectionNames()

    // ------------------------------------------------------------------
    // Ajout d'une application installée (TYPE_INSTALLED)
    // ------------------------------------------------------------------

    suspend fun addInstalledApp(
        packageName: String,
        title: String
    ): Long = withContext(Dispatchers.IO) {
        // Évite les doublons
        if (dao.countByPackage(packageName) > 0) return@withContext -1L
        dao.insert(
            CustomGameEntry(
                title = title,
                type = CustomGameEntry.TYPE_INSTALLED,
                packageName = packageName
            )
        )
    }

    // ------------------------------------------------------------------
    // Import d'une collection Pegasus
    // ------------------------------------------------------------------

    /**
     * Parse un fichier metadata Pegasus et insère tous les jeux en base.
     * @return nombre de jeux importés
     */
    suspend fun importPegasusCollection(metadataFile: File): Int =
        withContext(Dispatchers.IO) {
            val collections = PegasusMetadataParser.parse(metadataFile)
            var count = 0
            for (col in collections) {
                for (game in col.games) {
                    val entry = CustomGameEntry(
                        title          = game.title,
                        type           = CustomGameEntry.TYPE_PEGASUS,
                        packageName    = game.packageName,
                        launchCommand  = game.resolvedLaunchCommand(col.launchTemplate),
                        filePath       = game.filePath,
                        description    = game.description,
                        developer      = game.developer,
                        genre          = game.genre,
                        collectionName = col.name,
                        coverPath      = game.logoPath ?: game.steamPath ?: game.posterPath,
                        backgroundPath = game.backgroundPath
                    )
                    dao.insert(entry)
                    count++
                }
            }
            count
        }

    // ------------------------------------------------------------------
    // Suppression
    // ------------------------------------------------------------------

    suspend fun delete(game: CustomGameEntry) = withContext(Dispatchers.IO) {
        dao.delete(game)
    }

    suspend fun deleteCollection(name: String) = withContext(Dispatchers.IO) {
        dao.deleteCollection(name)
    }
}
