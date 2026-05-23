package com.vivo.game.gamespace.spirit

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CustomGameEntry — Entrée de jeu ajoutée manuellement par l'utilisateur.
 *
 * Deux types :
 *  - TYPE_INSTALLED : application Android installée (lancée via packageName)
 *  - TYPE_PEGASUS   : jeu issu d'une collection Pegasus (lancé via launchCommand)
 */
@Entity(tableName = "custom_games")
data class CustomGameEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nom affiché dans l'interface */
    @ColumnInfo(name = "title")
    val title: String,

    /** Type : voir constantes ci-dessous */
    @ColumnInfo(name = "type")
    val type: Int,

    /** Package Android — rempli pour TYPE_INSTALLED */
    @ColumnInfo(name = "package_name")
    val packageName: String? = null,

    /** Commande de lancement Pegasus — remplie pour TYPE_PEGASUS */
    @ColumnInfo(name = "launch_command")
    val launchCommand: String? = null,

    /** Chemin vers le fichier ROM / asset */
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    /** Description */
    @ColumnInfo(name = "description")
    val description: String? = null,

    /** Développeur */
    @ColumnInfo(name = "developer")
    val developer: String? = null,

    /** Genre */
    @ColumnInfo(name = "genre")
    val genre: String? = null,

    /** Nom de la collection parente */
    @ColumnInfo(name = "collection_name")
    val collectionName: String? = null,

    /** Chemin vers l'image de couverture / logo (assets.logo ou steam) */
    @ColumnInfo(name = "cover_path")
    val coverPath: String? = null,

    /** Chemin vers l'image de fond (assets.background) — Pegasus uniquement */
    @ColumnInfo(name = "background_path")
    val backgroundPath: String? = null,

    /** Timestamp d'ajout */
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_INSTALLED = 0
        const val TYPE_PEGASUS   = 1
    }
}
