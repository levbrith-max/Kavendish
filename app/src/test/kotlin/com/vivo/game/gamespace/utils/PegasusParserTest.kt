package com.vivo.game.gamespace.utils

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Tests unitaires du parser Pegasus.
 * Vérifie le parsing correct du format metadata Pegasus.
 */
class PegasusParserTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun writeMetadata(content: String): File {
        val f = tmpFolder.newFile("metadata.txt")
        f.writeText(content.trimIndent())
        return f
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 1 : Collection PC avec jeu ROM (comme votre exemple)
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `parse PC collection with file-based game`() {
        val file = writeMetadata("""
            collection: pc
            shortname: pc
            assets.logo: m/pc.png
            launch: am start
             -n com.ludashi.benchmark/com.winlator.cmod.XServerDisplayActivity
             -e shortcut_path {file.path}

            game: super mario 64
            file: sm64.us.desktop
            assets.steam: gth/png
        """)

        val collections = PegasusMetadataParser.parse(file)

        assertEquals(1, collections.size)
        val col = collections[0]
        assertEquals("pc", col.name)
        assertEquals("pc", col.shortName)
        assertNotNull(col.launchTemplate)
        assertTrue(col.launchTemplate!!.contains("XServerDisplayActivity"))
        assertTrue(col.launchTemplate!!.contains("{file.path}"))

        assertEquals(1, col.games.size)
        val game = col.games[0]
        assertEquals("super mario 64", game.title)
        assertEquals("sm64.us.desktop", game.filePath)
        assertFalse(game.isAndroidApp)
        assertNull(game.packageName)
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 2 : Collection Android avec app native
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `parse Android collection with package-based game`() {
        val file = writeMetadata("""
            collection: Android
            shortname: Mobile
            assets.logo: m/android.png

            game: Honkai Impact 3rd
            file: android:com.miHoYo.enterprise.NGHSoDBak
            launch: am start
             -n com.miHoYo.enterprise.NGHSoDBak/com.miHoYo.overridenativeactivity.OverrideNativeActivity
            assets.steam: m/BAHI3.png
            assets.logo: m/LHI3.png
            assets.background: m/BHI3.png
            assets.poster: m/PHI3.png
            assets.video: m/HK3.mp4
        """)

        val collections = PegasusMetadataParser.parse(file)

        assertEquals(1, collections.size)
        val col = collections[0]
        assertEquals("Android", col.name)
        assertEquals("Mobile", col.shortName)

        assertEquals(1, col.games.size)
        val game = col.games[0]
        assertEquals("Honkai Impact 3rd", game.title)
        assertEquals("android:com.miHoYo.enterprise.NGHSoDBak", game.filePath)
        assertTrue(game.isAndroidApp)
        assertEquals("com.miHoYo.enterprise.NGHSoDBak", game.packageName)
        assertEquals("Android", game.collectionName)

        // Vérifier la commande de lancement spécifique
        assertNotNull(game.launchCommand)
        assertTrue(game.launchCommand!!.contains("OverrideNativeActivity"))
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 3 : Résolution de commande avec {file.path}
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `resolved launch command replaces file placeholders`() {
        val file = writeMetadata("""
            collection: pc
            launch: am start -n pkg/Activity -e shortcut_path {file.path}

            game: test game
            file: /sdcard/roms/game.iso
        """)

        val col = PegasusMetadataParser.parse(file)[0]
        val game = col.games[0]
        val cmd = game.resolvedLaunchCommand(col.launchTemplate)

        assertNotNull(cmd)
        assertTrue(cmd!!.contains("/sdcard/roms/game.iso"))
        assertFalse(cmd.contains("{file.path}"))
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 4 : Plusieurs collections dans un fichier
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `parse multiple collections`() {
        val file = writeMetadata("""
            collection: SNES
            shortname: snes

            game: Super Mario World
            file: smw.sfc

            game: Zelda LTTP
            file: zelda.sfc

            collection: Android
            shortname: android

            game: Minecraft
            file: android:com.mojang.minecraftpe
            launch: am start -n com.mojang.minecraftpe/.MainActivity
        """)

        val collections = PegasusMetadataParser.parse(file)
        assertEquals(2, collections.size)

        assertEquals("SNES", collections[0].name)
        assertEquals(2, collections[0].games.size)

        assertEquals("Android", collections[1].name)
        assertEquals(1, collections[1].games.size)
        assertTrue(collections[1].games[0].isAndroidApp)
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 5 : Commande multiligne (continuation avec espace)
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `parse multiline launch command`() {
        val file = writeMetadata("""
            collection: Test
            launch: am start
             -n com.example.emulator/com.example.MainActivity
             -e rom_path {file.path}
             --user 0

            game: Game
            file: rom.bin
        """)

        val col = PegasusMetadataParser.parse(file)[0]
        val launch = col.launchTemplate ?: ""

        assertTrue("doit contenir le composant", launch.contains("com.example.emulator"))
        assertTrue("doit contenir -e rom_path", launch.contains("-e rom_path"))
        assertTrue("doit contenir {file.path}", launch.contains("{file.path}"))
        assertTrue("doit contenir --user 0", launch.contains("--user 0"))
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 6 : Fichier vide
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `empty file returns empty list`() {
        val file = tmpFolder.newFile("empty.txt")
        file.writeText("")
        val result = PegasusMetadataParser.parse(file)
        assertTrue(result.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    //  Test 7 : Fichier inexistant
    // ──────────────────────────────────────────────────────────────
    @Test
    fun `non-existent file returns empty list`() {
        val result = PegasusMetadataParser.parse(File("/does/not/exist.txt"))
        assertTrue(result.isEmpty())
    }
}
