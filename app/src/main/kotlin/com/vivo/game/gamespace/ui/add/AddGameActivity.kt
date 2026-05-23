package com.vivo.game.gamespace.ui.add

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vivo.game.R
import com.vivo.game.gamespace.spirit.CustomGameViewModel

/**
 * AddGameActivity — Interface de sélection d'application installée.
 *
 * Affiche toutes les applications installées dans une grille au style
 * "Game Space" (fond sombre, icônes arrondies, effet de sélection).
 * L'utilisateur peut filtrer par nom et ajouter une app à sa bibliothèque.
 */
class AddGameActivity : AppCompatActivity() {

    private lateinit var viewModel: CustomGameViewModel
    private lateinit var recycler: RecyclerView
    private lateinit var searchEdit: EditText
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val adapter = AppListAdapter()
    private val allApps = mutableListOf<AppItem>()

    data class AppItem(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_game)

        viewModel = ViewModelProvider(this)[CustomGameViewModel::class.java]

        recycler     = findViewById(R.id.rv_app_list)
        searchEdit   = findViewById(R.id.et_search)
        btnBack      = findViewById(R.id.btn_back)
        progressBar  = findViewById(R.id.progress_bar)
        tvEmpty      = findViewById(R.id.tv_empty)

        recycler.layoutManager = GridLayoutManager(this, 4)
        recycler.adapter = adapter

        btnBack.setOnClickListener { finish() }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Observe résultat d'ajout
        viewModel.addResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.gs_add_game_success), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.gs_add_game_already_exists), Toast.LENGTH_SHORT).show()
            }
        }

        loadApps()
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        Thread {
            val pm = packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val items = installed
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // apps non-système
                .map { AppItem(pm.getApplicationLabel(it).toString(), it.packageName, pm.getApplicationIcon(it)) }
                .sortedBy { it.label.lowercase() }

            runOnUiThread {
                allApps.clear()
                allApps.addAll(items)
                adapter.submitList(items)
                progressBar.visibility = View.GONE
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun filter(query: String) {
        val filtered = if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------------
    // Adapter
    // ------------------------------------------------------------------

    inner class AppListAdapter : RecyclerView.Adapter<AppListAdapter.VH>() {

        private val items = mutableListOf<AppItem>()

        fun submitList(list: List<AppItem>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_app, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val icon: ImageView = view.findViewById(R.id.app_icon)
            private val label: TextView = view.findViewById(R.id.app_label)
            private val btnAdd: ImageView = view.findViewById(R.id.btn_add)

            fun bind(item: AppItem) {
                Glide.with(icon).load(item.icon).into(icon)
                label.text = item.label

                itemView.setOnClickListener {
                    viewModel.addInstalledApp(item.packageName, item.label)
                }
                btnAdd.setOnClickListener {
                    viewModel.addInstalledApp(item.packageName, item.label)
                }
            }
        }
    }
}
