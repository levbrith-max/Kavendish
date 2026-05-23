package com.vivo.game.gamespace.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.Stack

/**
 * ActivityStack — Gestionnaire global de la pile d'activités.
 *
 * Permet de :
 *  - Suivre toutes les activités ouvertes
 *  - Fermer toutes les activités (utile lors de la sortie de l'app)
 *  - Récupérer l'activité courante en premier plan
 */
object ActivityStack {

    private val stack = Stack<Activity>()

    /** Callbacks à enregistrer dans Application.onCreate() */
    val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            stack.push(activity)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            stack.remove(activity)
        }
    }

    /** Ferme toutes les activités en cours. */
    fun finishAll() {
        val copy = stack.toList()
        for (activity in copy) {
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.finish()
            }
        }
        stack.clear()
    }

    /** Retourne l'activité au sommet de la pile (la plus récente). */
    fun currentActivity(): Activity? = stack.lastOrNull()

    /** Retourne le nombre d'activités dans la pile. */
    fun size(): Int = stack.size
}
