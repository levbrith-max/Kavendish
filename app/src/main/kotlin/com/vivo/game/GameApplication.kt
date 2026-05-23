package com.vivo.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vivo.game.gamespace.utils.ActivityStack

/**
 * GameApplication — Application principale.
 *
 * Fonctionnalités ajoutées :
 *  - Suivi de toutes les activités via ActivityStack
 *  - Nettoyage automatique de toutes les activités quand l'app passe en arrière-plan
 *    (comportement configurable via FINISH_ON_BACKGROUND)
 */
class GameApplication : Application() {

    companion object {
        lateinit var instance: GameApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Enregistrer le suivi du cycle de vie des activités
        registerActivityLifecycleCallbacks(ActivityStack.lifecycleCallbacks)

        // Observer du cycle de vie du processus entier
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    /**
     * Observer qui réagit quand l'application entière passe en arrière-plan.
     * Quand l'utilisateur quitte l'app (retour launcher, pression Home, etc.),
     * toutes les activités en cours sont détruites proprement.
     */
    private inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            // L'application est passée entièrement en arrière-plan
            // → nettoyage de toute la pile d'activités
            ActivityStack.finishAll()
        }
    }
}
