package com.vivo.game.gamespace.utils

import java.io.File

/**
 * PegasusMetadataParser
 *
 * Parse le format metadata.pegasus.txt / metadata.txt de Pegasus Frontend.
 *
 * Format supporté :
 *   collection: Nom
 *   shortname:  short
 *   assets.logo: chemin/relatif.png
 *   launch: am start \
 *     -n com.package/Activity \
 *     -e arg val
 *
 *   game: Titre du jeu
 *   file: chemin/ou/android:com.package
 *   assets.logo: chemin/logo.png
 *   assets.steam: chemin/header.png
 *   assets.background: chemin/bg.png
 *   assets.poster: chemin/poster.png
 *   developer: Studio
 *   description: Texte...
 *   genre: Action
 *
 * Les champs multilignes (indentés par espace/tab) sont concaténés.
 * Les chemins d'assets sont résolus relativement au dossier du fichier metadata.
 */
object PegasusMetadataParser {

    data class PegasusCollection(
        val name: String,
        val shortName: String,
        val logoPath: String?,        // chemin absolu résolu
        val launchTemplate: String?,  // commande de lancement (peut contenir {file.path})
        val games: List<PegasusGame>
    )

    data class PegasusGame(
        val title: String,
        val filePath: String?,
        val packageName: String?,
        val launchCommand: String?,
        val logoPath: String?,         // assets.logo  → icône de la tuile
        val steamPath: String?,        // assets.steam → header Steam
        val backgroundPath: String?,   // assets.background → fond de l'écran
        val posterPath: String?,       // assets.poster
        val videoPath: String?,        // assets.video
        val description: String?,
        val developer: String?,
        val genre: String?,
        val collectionName: String
    ) {
        /**
         * Retourne la commande finale à exécuter pour lancer ce jeu.
         * - Si le jeu a sa propre commande → on l'utilise
         * - Sinon on utilise le template de la collection en remplaçant {file.path}
         */
        fun resolvedLaunchCommand(collectionLaunch: String?): String? {
            val cmd = launchCommand ?: collectionLaunch ?: return null
            val fp = filePath ?: return cmd
            return cmd.replace("{file.path}", fp)
                      .replace("{file.dir}", File(fp).parent ?: "")
                      .replace("{file.name}", File(fp).name)
                      .replace("{file.stem}", File(fp).nameWithoutExtension)
        }

        /** True si ce jeu est une application Android directement installable */
        val isAndroidApp: Boolean get() = packageName != null
    }

    /**
     * Parse un fichier metadata Pegasus et retourne la liste des collections trouvées.
     *
     * @param metadataFile   Le fichier metadata.txt / metadata.pegasus.txt
     * @return               Liste des collections avec leurs jeux
     */
    fun parse(metadataFile: File): List<PegasusCollection> {
        if (!metadataFile.exists()) return emptyList()

        val baseDir = metadataFile.parentFile ?: File(".")
        val lines = metadataFile.readLines(Charsets.UTF_8)

        // On travaille avec un tokenizer de blocs
        val rawBlocks = splitIntoBlocks(lines)
        return parseBlocks(rawBlocks, baseDir)
    }

    // -------------------------------------------------------------------------
    // Tokenisation : découpe en blocs séparés par lignes vides
    // -------------------------------------------------------------------------

    private fun splitIntoBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        val current = mutableListOf<String>()

        for (line in lines) {
            if (line.isBlank()) {
                if (current.isNotEmpty()) {
                    blocks.add(current.toList())
                    current.clear()
                }
            } else {
                current.add(line)
            }
        }
        if (current.isNotEmpty()) blocks.add(current.toList())
        return blocks
    }

    // -------------------------------------------------------------------------
    // Parsing des blocs
    // -------------------------------------------------------------------------

    private fun parseBlocks(blocks: List<List<String>>, baseDir: File): List<PegasusCollection> {
        val collections = mutableListOf<PegasusCollection>()
        var currentCollection: CollectionBuilder? = null
        var currentGame: GameBuilder? = null

        fun flushGame() {
            currentGame?.let { g ->
                currentCollection?.games?.add(g.build(currentCollection?.name ?: ""))
                currentGame = null
            }
        }

        fun flushCollection() {
            flushGame()
            currentCollection?.let { c ->
                collections.add(c.build())
                currentCollection = null
            }
        }

        for (block in blocks) {
            val fields = parseFields(block)

            when {
                // Bloc collection
                fields.containsKey("collection") -> {
                    flushCollection()
                    currentCollection = CollectionBuilder(
                        name = fields["collection"] ?: "",
                        shortName = fields["shortname"] ?: fields["collection"] ?: "",
                        logoPath = resolveAsset(fields["assets.logo"], baseDir),
                        launchTemplate = fields["launch"]
                    )
                }

                // Bloc game
                fields.containsKey("game") -> {
                    flushGame()
                    val fileRaw = fields["file"]
                    val pkgName = if (fileRaw?.startsWith("android:") == true)
                        fileRaw.removePrefix("android:") else null

                    currentGame = GameBuilder(
                        title = fields["game"] ?: "",
                        filePath = fileRaw,
                        packageName = pkgName,
                        launchCommand = fields["launch"],
                        logoPath = resolveAsset(fields["assets.logo"], baseDir),
                        steamPath = resolveAsset(fields["assets.steam"], baseDir),
                        backgroundPath = resolveAsset(fields["assets.background"], baseDir),
                        posterPath = resolveAsset(fields["assets.poster"], baseDir),
                        videoPath = resolveAsset(fields["assets.video"], baseDir),
                        description = fields["description"],
                        developer = fields["developer"],
                        genre = fields["genre"]
                    )
                }

                // Bloc sans en-tête connu → on essaie de l'attacher au bloc courant
                else -> {
                    // Peut être des champs supplémentaires pour le jeu ou la collection courant
                    val extra = parseFields(block)
                    extra.forEach { (key, value) ->
                        when {
                            currentGame != null -> currentGame!!.mergeField(key, value, baseDir)
                            currentCollection != null -> currentCollection!!.mergeField(key, value, baseDir)
                        }
                    }
                }
            }
        }

        flushCollection()
        return collections
    }

    // -------------------------------------------------------------------------
    // Parse les champs d'un bloc (gère le multilignes indenté)
    // -------------------------------------------------------------------------

    private fun parseFields(lines: List<String>): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        var currentKey: String? = null
        val currentValue = StringBuilder()

        fun flush() {
            currentKey?.let { k ->
                fields[k.lowercase().trim()] = currentValue.toString().trim()
                currentValue.clear()
            }
        }

        for (line in lines) {
            // Ligne indentée → continuation du champ précédent
            if ((line.startsWith(" ") || line.startsWith("\t")) && currentKey != null) {
                currentValue.append(" ").append(line.trim())
                continue
            }

            // Nouvelle clé : valeur
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                flush()
                currentKey = line.substring(0, colonIdx).trim()
                currentValue.setLength(0)
                currentValue.append(line.substring(colonIdx + 1).trim())
            }
        }
        flush()
        return fields
    }

    // -------------------------------------------------------------------------
    // Résolution des chemins d'assets (relatif au dossier du fichier metadata)
    // -------------------------------------------------------------------------

    private fun resolveAsset(relative: String?, baseDir: File): String? {
        if (relative.isNullOrBlank()) return null
        val f = File(baseDir, relative.trim())
        return if (f.exists()) f.absolutePath else f.absolutePath // on stocke le chemin même si absent
    }

    // -------------------------------------------------------------------------
    // Builders internes
    // -------------------------------------------------------------------------

    private class CollectionBuilder(
        val name: String,
        val shortName: String,
        var logoPath: String?,
        var launchTemplate: String?
    ) {
        val games = mutableListOf<GameBuilder>()

        fun build() = PegasusCollection(
            name = name,
            shortName = shortName,
            logoPath = logoPath,
            launchTemplate = launchTemplate,
            games = games.map { it.build(name) }
        )

        fun mergeField(key: String, value: String, baseDir: File) {
            when (key) {
                "assets.logo" -> logoPath = resolveAsset(value, baseDir)
                "launch"      -> launchTemplate = value
            }
        }

        private fun resolveAsset(rel: String?, base: File) = PegasusMetadataParser.resolveAsset(rel, base)
    }

    private class GameBuilder(
        val title: String,
        var filePath: String?,
        var packageName: String?,
        var launchCommand: String?,
        var logoPath: String?,
        var steamPath: String?,
        var backgroundPath: String?,
        var posterPath: String?,
        var videoPath: String?,
        var description: String?,
        var developer: String?,
        var genre: String?
    ) {
        fun build(collectionName: String) = PegasusGame(
            title = title,
            filePath = filePath,
            packageName = packageName,
            launchCommand = launchCommand,
            logoPath = logoPath,
            steamPath = steamPath,
            backgroundPath = backgroundPath,
            posterPath = posterPath,
            videoPath = videoPath,
            description = description,
            developer = developer,
            genre = genre,
            collectionName = collectionName
        )

        fun mergeField(key: String, value: String, baseDir: File) {
            when (key) {
                "file"               -> {
                    filePath = value
                    packageName = if (value.startsWith("android:")) value.removePrefix("android:") else null
                }
                "launch"             -> launchCommand = value
                "assets.logo"        -> logoPath = PegasusMetadataParser.resolveAsset(value, baseDir)
                "assets.steam"       -> steamPath = PegasusMetadataParser.resolveAsset(value, baseDir)
                "assets.background"  -> backgroundPath = PegasusMetadataParser.resolveAsset(value, baseDir)
                "assets.poster"      -> posterPath = PegasusMetadataParser.resolveAsset(value, baseDir)
                "assets.video"       -> videoPath = PegasusMetadataParser.resolveAsset(value, baseDir)
                "description"        -> description = value
                "developer"          -> developer = value
                "genre"              -> genre = value
            }
        }
    }
}
