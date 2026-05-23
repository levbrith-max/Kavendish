package com.vivo.game.gamespace.ui.widget

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.vivo.game.R
import com.vivo.game.gamespace.spirit.CustomGameEntry
import java.io.File

/**
 * CustomGameAdapter — Adapteur pour la grille de jeux personnalisés.
 *
 * Logique d'icône (priorité) :
 *  1. Pegasus → assets.logo (coverPath), affiché depuis le fichier local
 *  2. Installed → icône du package via PackageManager
 *  3. Fallback → ic_game_placeholder
 *
 * Sélection → notifie [onBackgroundChange] avec le backgroundPath du jeu
 * (null si le jeu n'a pas d'assets.background ou n'est pas Pegasus).
 */
class CustomGameAdapter(
    private val onItemClick: (CustomGameEntry) -> Unit,
    private val onItemLongClick: (CustomGameEntry) -> Boolean = { false },
    /**
     * Appelé quand un jeu est sélectionné/focus :
     *  - backgroundPath (String?) : chemin local de l'assets.background, ou null → fond par défaut
     */
    private val onBackgroundChange: (backgroundPath: String?) -> Unit = {}
) : ListAdapter<CustomGameEntry, CustomGameAdapter.GameVH>(DIFF_CALLBACK) {

    private var selectedId: Long = -1L

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CustomGameEntry>() {
            override fun areItemsTheSame(a: CustomGameEntry, b: CustomGameEntry) = a.id == b.id
            override fun areContentsTheSame(a: CustomGameEntry, b: CustomGameEntry) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_game_grid, parent, false)
        return GameVH(view)
    }

    override fun onBindViewHolder(holder: GameVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GameVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.game_icon)
        private val tvName: TextView  = view.findViewById(R.id.game_name)
        private val tvBadge: TextView = view.findViewById(R.id.game_badge)
        private val selector: View    = view.findViewById(R.id.item_selector)

        fun bind(entry: CustomGameEntry) {
            tvName.text = entry.title

            // Badge collection (Pegasus uniquement)
            if (entry.type == CustomGameEntry.TYPE_PEGASUS && !entry.collectionName.isNullOrBlank()) {
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = entry.collectionName
            } else {
                tvBadge.visibility = View.GONE
            }

            // Indicateur de sélection
            selector.visibility = if (entry.id == selectedId) View.VISIBLE else View.INVISIBLE

            loadIcon(entry)

            itemView.setOnClickListener {
                val prev = selectedId
                selectedId = entry.id
                // Refresh previous selection visuellement
                if (prev != -1L) {
                    val prevPos = currentList.indexOfFirst { it.id == prev }
                    if (prevPos >= 0) notifyItemChanged(prevPos)
                }
                notifyItemChanged(adapterPosition)

                // Notifier le fragment du changement d'arrière-plan
                val bg = if (entry.type == CustomGameEntry.TYPE_PEGASUS)
                    entry.backgroundPath else null
                onBackgroundChange(bg)

                onItemClick(entry)
            }
            itemView.setOnLongClickListener { onItemLongClick(entry) }
        }

        /**
         * Charge l'icône selon la priorité, en s'assurant que l'image
         * reste strictement dans les limites du conteneur (clipToOutline).
         */
        private fun loadIcon(entry: CustomGameEntry) {
            val ctx = itemView.context

            // Activer le clip sur les coins arrondis du fond
            ivIcon.clipToOutline = true

            when {
                // 1. Pegasus avec assets.logo
                entry.type == CustomGameEntry.TYPE_PEGASUS
                        && !entry.coverPath.isNullOrBlank() -> {
                    loadLocalFile(ctx, entry.coverPath)
                }

                // 2. App installée → icône PackageManager
                !entry.packageName.isNullOrBlank() -> {
                    loadPackageIcon(ctx, entry.packageName)
                }

                // 3. Fallback
                else -> setFallbackIcon()
            }
        }

        private fun loadLocalFile(ctx: Context, path: String) {
            Glide.with(ctx)
                .load(File(path))
                .centerCrop()          // remplit sans déborder
                .placeholder(R.drawable.ic_game_placeholder)
                .error(R.drawable.ic_game_placeholder)
                .listener(failListener { setFallbackIcon() })
                .into(ivIcon)
        }

        private fun loadPackageIcon(ctx: Context, packageName: String) {
            try {
                val icon: Drawable = ctx.packageManager.getApplicationIcon(packageName)
                Glide.with(ctx)
                    .load(icon)
                    .centerInside()    // icônes système ne débordent pas
                    .placeholder(R.drawable.ic_game_placeholder)
                    .error(R.drawable.ic_game_placeholder)
                    .into(ivIcon)
            } catch (_: PackageManager.NameNotFoundException) {
                setFallbackIcon()
            }
        }

        private fun setFallbackIcon() {
            ivIcon.setImageResource(R.drawable.ic_game_placeholder)
        }

        private fun failListener(onFail: () -> Unit) = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?,
                target: Target<Drawable>?, isFirstResource: Boolean
            ): Boolean { onFail(); return false }
            override fun onResourceReady(
                resource: Drawable?, model: Any?,
                target: Target<Drawable>?, dataSource: DataSource?,
                isFirstResource: Boolean
            ) = false
        }
    }
}
