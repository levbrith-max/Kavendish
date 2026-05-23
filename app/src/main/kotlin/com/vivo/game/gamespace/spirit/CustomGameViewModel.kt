package com.vivo.game.gamespace.spirit

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class CustomGameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CustomGameRepository(application)

    val allGames: LiveData<List<CustomGameEntry>> = repo.getAllGames()
    val allCollections: LiveData<List<String>> = repo.getAllCollectionNames()

    private val _importResult = MutableLiveData<ImportResult>()
    val importResult: LiveData<ImportResult> = _importResult

    private val _addResult = MutableLiveData<Boolean>()
    val addResult: LiveData<Boolean> = _addResult

    // ------------------------------------------------------------------

    fun addInstalledApp(packageName: String, title: String) {
        viewModelScope.launch {
            val id = repo.addInstalledApp(packageName, title)
            _addResult.postValue(id > 0)
        }
    }

    fun importPegasusFile(file: File) {
        viewModelScope.launch {
            try {
                val count = repo.importPegasusCollection(file)
                _importResult.postValue(ImportResult.Success(count))
            } catch (e: Exception) {
                _importResult.postValue(ImportResult.Error(e.message ?: "Erreur inconnue"))
            }
        }
    }

    fun deleteGame(game: CustomGameEntry) {
        viewModelScope.launch { repo.delete(game) }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch { repo.deleteCollection(name) }
    }

    // ------------------------------------------------------------------

    sealed class ImportResult {
        data class Success(val count: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
