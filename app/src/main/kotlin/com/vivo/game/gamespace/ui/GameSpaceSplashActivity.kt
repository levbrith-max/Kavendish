package com.vivo.game.gamespace.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vivo.game.R

/**
 * GameSpaceSplashActivity — Écran de démarrage.
 * Redirige vers GameSpaceHostActivity après un court délai.
 */
class GameSpaceSplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_space_splash)

        // Démarrage direct sans animation pour garder la fluidité
        window.decorView.postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, GameSpaceHostActivity::class.java))
                finish()
            }
        }, 800)
    }
}
