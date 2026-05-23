package com.vivo.game.gamespace.ui.collection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.vivo.game.R
import com.vivo.game.gamespace.spirit.CustomGameViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * AddCollectionActivity — Import d'une collection depuis un fichier metadata Pegasus.
 *
 * Aucun emoji ni symbole Unicode dans l'interface : les états (succes, erreur,
 * info) sont indiques par des icones SVG vectorielles.
 */
class AddCollectionActivity : AppCompatActivity() {

    private lateinit var viewModel: CustomGameViewModel

    private lateinit var btnBack: ImageView
    private lateinit var btnChooseFile: View
    private lateinit var tvFilePath: TextView
    private lateinit var btnImport: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var ivStatusIcon: ImageView

    private var selectedFile: File? = null

    companion object {
        private const val RC_PICK_FILE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_collection)

        viewModel = ViewModelProvider(this)[CustomGameViewModel::class.java]

        btnBack       = findViewById(R.id.btn_back)
        btnChooseFile = findViewById(R.id.btn_choose_file)
        tvFilePath    = findViewById(R.id.tv_file_path)
        btnImport     = findViewById(R.id.btn_import)
        progressBar   = findViewById(R.id.progress_bar)
        tvStatus      = findViewById(R.id.tv_status)
        ivStatusIcon  = findViewById(R.id.iv_status_icon)

        btnBack.setOnClickListener { finish() }
        btnChooseFile.setOnClickListener { openFilePicker() }

        btnImport.setOnClickListener {
            val file = selectedFile ?: return@setOnClickListener
            progressBar.visibility = View.VISIBLE
            setStatus(getString(R.string.gs_collection_importing), StatusType.INFO)
            btnImport.isEnabled = false
            viewModel.importPegasusFile(file)
        }

        viewModel.importResult.observe(this) { result ->
            progressBar.visibility = View.GONE
            btnImport.isEnabled = true
            when (result) {
                is CustomGameViewModel.ImportResult.Success -> {
                    setStatus(
                        getString(R.string.gs_collection_import_success, result.count),
                        StatusType.SUCCESS
                    )
                    btnBack.postDelayed({ finish() }, 1500)
                }
                is CustomGameViewModel.ImportResult.Error -> {
                    setStatus(
                        getString(R.string.gs_collection_import_error, result.message),
                        StatusType.ERROR
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Affichage du statut avec icone SVG
    // ------------------------------------------------------------------

    private enum class StatusType { INFO, SUCCESS, ERROR }

    private fun setStatus(message: String, type: StatusType) {
        tvStatus.text = message
        ivStatusIcon.visibility = View.VISIBLE
        when (type) {
            StatusType.SUCCESS -> {
                ivStatusIcon.setImageResource(R.drawable.ic_check_success)
                tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
            }
            StatusType.ERROR -> {
                ivStatusIcon.setImageResource(R.drawable.ic_status_error)
                tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
            }
            StatusType.INFO -> {
                ivStatusIcon.setImageResource(R.drawable.ic_status_info)
                tvStatus.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    // ------------------------------------------------------------------
    //  Selection du fichier
    // ------------------------------------------------------------------

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
        }
        startActivityForResult(intent, RC_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            copyUriToTempFile(uri)
        }
    }

    private fun copyUriToTempFile(uri: Uri) {
        try {
            val fileName = getFileName(uri) ?: "metadata.txt"
            val cacheDir = File(cacheDir, "pegasus")
            cacheDir.mkdirs()
            val dest = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }

            selectedFile = dest
            tvFilePath.text = fileName
            setStatus(getString(R.string.gs_collection_file_selected), StatusType.SUCCESS)
            btnImport.isEnabled = true

            tryResolveRealPath(uri, dest)

        } catch (e: Exception) {
            setStatus(getString(R.string.gs_collection_file_error), StatusType.ERROR)
        }
    }

    private fun tryResolveRealPath(uri: Uri, fallback: File) {
        try {
            val path = uri.path ?: return
            if (path.contains("/primary:") || path.contains("/document/")) {
                val relative = path.substringAfter("/primary:")
                    .substringAfter("/document/primary:")
                val real = File("/sdcard/$relative")
                if (real.exists()) {
                    selectedFile = real
                    tvFilePath.text = real.absolutePath
                }
            }
        } catch (_: Exception) { }
    }

    private fun getFileName(uri: Uri): String? = try {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            c.moveToFirst()
            if (col >= 0) c.getString(col) else null
        }
    } catch (_: Exception) { null }
}
