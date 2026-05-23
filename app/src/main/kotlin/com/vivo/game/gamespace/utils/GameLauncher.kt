package com.vivo.game.gamespace.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.vivo.game.gamespace.spirit.CustomGameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GameLauncher — Lance un jeu selon son type.
 *
 * - TYPE_INSTALLED : on utilise le package manager Android standard
 * - TYPE_PEGASUS   : on exécute la commande `am start ...` via Runtime.exec()
 */
object GameLauncher {

    private const val TAG = "GameLauncher"

    /**
     * Lance un jeu.
     * @return true si le lancement a réussi, false sinon
     */
    suspend fun launch(context: Context, game: CustomGameEntry): Boolean =
        withContext(Dispatchers.IO) {
            try {
                when (game.type) {
                    CustomGameEntry.TYPE_INSTALLED -> launchInstalledApp(context, game)
                    CustomGameEntry.TYPE_PEGASUS   -> launchViaPegasusCommand(context, game)
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lancement ${game.title}", e)
                false
            }
        }

    // -------------------------------------------------------------------------
    // Application Android installée
    // -------------------------------------------------------------------------

    private fun launchInstalledApp(context: Context, game: CustomGameEntry): Boolean {
        val pkg = game.packageName ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    // -------------------------------------------------------------------------
    // Commande Pegasus (am start / shell)
    // -------------------------------------------------------------------------

    private fun launchViaPegasusCommand(context: Context, game: CustomGameEntry): Boolean {
        val rawCmd = game.launchCommand ?: return false

        // Remplacer les placeholders
        val filePath = game.filePath ?: ""
        val cmd = rawCmd
            .replace("{file.path}", filePath)
            .replace("{file.dir}", java.io.File(filePath).parent ?: "")
            .replace("{file.name}", java.io.File(filePath).name)
            .replace("{file.stem}", java.io.File(filePath).nameWithoutExtension)
            .trim()

        Log.d(TAG, "Lancement Pegasus: $cmd")

        // Si la commande commence par "am start", on tente via Intent
        if (cmd.startsWith("am start")) {
            return tryAmStartViaIntent(context, cmd) || tryAmStartViaShell(cmd)
        }

        // Sinon exécution shell directe
        return tryShellExec(cmd)
    }

    /**
     * Parse une commande `am start -n pkg/activity -e key value`
     * et la traduit en Intent Android natif (pas besoin de root).
     */
    private fun tryAmStartViaIntent(context: Context, cmd: String): Boolean {
        return try {
            val args = tokenize(cmd)
            var component: String? = null
            val extras = mutableMapOf<String, String>()
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "-n" -> { component = args.getOrNull(++i); i++ }
                    "-e" -> {
                        val k = args.getOrNull(++i)
                        val v = args.getOrNull(++i)
                        if (k != null && v != null) extras[k] = v
                        i++
                    }
                    else -> i++
                }
            }
            if (component == null) return false

            val parts = component.split("/")
            if (parts.size != 2) return false
            val pkg = parts[0]
            val cls = if (parts[1].startsWith(".")) pkg + parts[1] else parts[1]

            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                extras.forEach { (k, v) -> putExtra(k, v) }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "tryAmStartViaIntent failed", e)
            false
        }
    }

    /**
     * Fallback : exécuter `am start` en shell (nécessite que l'app ait les droits,
     * ou que l'appareil soit rooted).
     */
    private fun tryAmStartViaShell(cmd: String): Boolean {
        return tryShellExec(cmd)
    }

    private fun tryShellExec(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val exitCode = process.waitFor()
            Log.d(TAG, "Shell exec exitCode=$exitCode for: $cmd")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Shell exec failed", e)
            false
        }
    }

    /** Tokenize une ligne de commande en respectant les guillemets. */
    private fun tokenize(cmd: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var quoteChar = ' '
        for (c in cmd) {
            when {
                inQuote && c == quoteChar -> { inQuote = false }
                !inQuote && (c == '"' || c == '\'') -> { inQuote = true; quoteChar = c }
                !inQuote && c == ' ' -> {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }
}
