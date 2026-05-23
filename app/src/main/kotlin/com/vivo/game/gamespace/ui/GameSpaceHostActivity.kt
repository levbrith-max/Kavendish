package com.vivo.game.gamespace.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.vivo.game.R

/**
 * GameSpaceHostActivity — Activité hôte principale du Game Space.
 *
 * Elle héberge les fragments et expose :
 *  - La navigation (bouton retour)
 *  - Le bouton paramètres (switch_settings, en bas à gauche comme dans l'original)
 *  - L'intégration de GameSpaceMyGameFragment qui contient
 *    les boutons "Ajouter un jeu" et "Ajouter une collection"
 */
class GameSpaceHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_space_host_activity)

        // Injecter notre fragment étendu dans le container principal
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameSpaceMyGameFragment())
                .commit()
        }

        // Bouton retour (en haut à gauche de game_space_main via la vue parente)
        val btnBack = findViewById<ImageView?>(R.id.game_back)
        btnBack?.setOnClickListener { finish() }

        // Bouton paramètres / switch (en bas à gauche — identique à l'original)
        val switchSettings = findViewById<View?>(R.id.switch_settings)
        switchSettings?.setOnClickListener {
            // Logique originale : ouvre les paramètres de confidentialité
            startActivity(android.content.Intent(this, PrivacySettingsActivity::class.java))
        }
    }
}
