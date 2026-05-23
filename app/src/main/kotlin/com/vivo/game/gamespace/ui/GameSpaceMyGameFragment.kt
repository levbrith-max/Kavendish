package com.vivo.game.gamespace.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.vivo.game.R
import com.vivo.game.gamespace.spirit.CustomGameEntry
import com.vivo.game.gamespace.spirit.CustomGameViewModel
import com.vivo.game.gamespace.ui.add.AddGameActivity
import com.vivo.game.gamespace.ui.collection.AddCollectionActivity
import com.vivo.game.gamespace.ui.widget.CustomGameAdapter
import com.vivo.game.gamespace.utils.GameLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * GameSpaceMyGameFragment — Fragment principal du Game Space étendu.
 *
 * Nouvelles fonctionnalités :
 *  - Bouton "Ajouter un jeu"       (en bas à gauche, style gaming)
 *  - Bouton "Ajouter une collection" (en dessous)
 *  - Arrière-plan dynamique : quand un jeu avec assets.background est
 *    sélectionné, le fond change avec un crossfade.
 *    Si le jeu n'a pas d'assets.background → fond par défaut de l'app.
 *  - Icônes strictement clippées dans leur cadre.
 */
class GameSpaceMyGameFragment : Fragment() {

    private lateinit var viewModel: CustomGameViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddGame: View
    private lateinit var btnAddCollection: View
    private lateinit var emptyLayout: View

    /** Couche de fond dynamique (assets.background du jeu sélectionné) */
    private lateinit var ivDynamicBg: ImageView
    /** Fond par défaut de l'app (toujours présent sous le dynamique) */
    private lateinit var ivDefaultBg: ImageView

    private val adapter = CustomGameAdapter(
        onItemClick      = { entry -> launchGame(entry) },
        onItemLongClick  = { entry -> showDeleteDialog(entry); true },
        onBackgroundChange = { path -> updateBackground(path) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my_game_extended, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CustomGameViewModel::class.java]

        recyclerView     = view.findViewById(R.id.rv_custom_games)
        btnAddGame       = view.findViewById(R.id.btn_add_game)
        btnAddCollection = view.findViewById(R.id.btn_add_collection)
        emptyLayout      = view.findViewById(R.id.empty_layout)
        ivDynamicBg      = view.findViewById(R.id.iv_dynamic_bg)
        ivDefaultBg      = view.findViewById(R.id.iv_default_bg)

        // Grille 3 colonnes — même format que l'app originale
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter

        viewModel.allGames.observe(viewLifecycleOwner) { games ->
            adapter.submitList(games)
            emptyLayout.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
        }

        btnAddGame.setOnClickListener {
            startActivity(Intent(requireContext(), AddGameActivity::class.java))
        }
        btnAddCollection.setOnClickListener {
            startActivity(Intent(requireContext(), AddCollectionActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Arrière-plan dynamique
    // ─────────────────────────────────────────────────────────────────

    /**
     * Met à jour l'arrière-plan selon le jeu sélectionné.
     *
     * @param backgroundPath Chemin absolu vers assets.background, ou null.
     *   - Non null et fichier existant → charge l'image avec crossfade,
     *     masque ivDefaultBg pendant la transition pour éviter un flash.
     *   - Null ou fichier absent       → revient au fond par défaut.
     */
    private fun updateBackground(backgroundPath: String?) {
        val ctx = context ?: return

        if (!backgroundPath.isNullOrBlank() && File(backgroundPath).exists()) {
            // Crossfade vers l'arrière-plan Pegasus
            Glide.with(ctx)
                .load(File(backgroundPath))
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(400))
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?, model: Any?,
                        target: Target<Drawable>?, dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Une fois chargé, on peut masquer légèrement le fond par défaut
                        ivDefaultBg.animate().alpha(0.15f).setDuration(400).start()
                        return false
                    }
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: Target<Drawable>?, isFirstResource: Boolean
                    ): Boolean {
                        restoreDefaultBackground()
                        return false
                    }
                })
                .into(ivDynamicBg)

            ivDynamicBg.animate().alpha(1f).setDuration(400).start()

        } else {
            restoreDefaultBackground()
        }
    }

    /** Revient au fond par défaut de l'app (fond Pegasus effacé en fondu). */
    private fun restoreDefaultBackground() {
        ivDynamicBg.animate().alpha(0f).setDuration(400).withEndAction {
            Glide.with(this).clear(ivDynamicBg)
        }.start()
        ivDefaultBg.animate().alpha(1f).setDuration(400).start()
    }

    // ─────────────────────────────────────────────────────────────────
    //  Lancement
    // ─────────────────────────────────────────────────────────────────

    private fun launchGame(entry: CustomGameEntry) {
        CoroutineScope(Dispatchers.Main).launch {
            val ok = GameLauncher.launch(requireContext(), entry)
            if (!ok) Toast.makeText(
                requireContext(),
                getString(R.string.gs_launch_failed, entry.title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Suppression (appui long)
    // ─────────────────────────────────────────────────────────────────

    private fun showDeleteDialog(entry: CustomGameEntry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(entry.title)
            .setMessage(R.string.gs_delete_game_confirm)
            .setPositiveButton(R.string.gs_delete) { _, _ -> viewModel.deleteGame(entry) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
