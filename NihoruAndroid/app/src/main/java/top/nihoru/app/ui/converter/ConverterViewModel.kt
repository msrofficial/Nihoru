package top.nihoru.app.ui.converter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.nihoru.app.data.model.AnimeEntry
import top.nihoru.app.data.repository.NihoruDatabase
import top.nihoru.app.network.AniListApi
import top.nihoru.app.utils.Exporter

data class ToastState(val message: String, val type: String) // type: loading, success, error, warning

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(application, NihoruDatabase::class.java, "nihoru_db").build()
    private val dao = db.animeDao()

    private val _animeList = MutableLiveData<List<AnimeEntry>>(emptyList())
    val animeList: LiveData<List<AnimeEntry>> = _animeList

    private val _toast = MutableLiveData<ToastState?>()
    val toast: LiveData<ToastState?> = _toast

    private val _progress = MutableLiveData<Triple<Int, Int, String>?>() // current, total, label
    val progress: LiveData<Triple<Int, Int, String>?> = _progress

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _exportXml = MutableLiveData<String?>()
    val exportXml: LiveData<String?> = _exportXml

    private val _exportJson = MutableLiveData<String?>()
    val exportJson: LiveData<String?> = _exportJson

    var currentMode = "anime"
    private var processJob: Job? = null
    private var isCancelled = false
    private val visitedRelations = mutableSetOf<Int>()

    fun loadSaved() {
        viewModelScope.launch {
            val saved = dao.getAll(currentMode)
            _animeList.postValue(saved)
        }
    }

    fun processInput(rawText: String, autoSeason: Boolean, defaultStatus: String) {
        if (_isProcessing.value == true) return

        isCancelled = false
        visitedRelations.clear()

        processJob = viewModelScope.launch {
            _isProcessing.postValue(true)

            val names = cleanAndParseInput(rawText, autoSeason)

            if (names.isEmpty()) {
                showToast("No valid names found!", "error")
                _isProcessing.postValue(false)
                return@launch
            }

            showToast("Processing list...", "success")

            val batchSize = 5
            var processed = 0

            try {
                for (i in names.indices step batchSize) {
                    if (!isActive || isCancelled) break
                    val chunk = names.subList(i, minOf(i + batchSize, names.size))

                    for (name in chunk) {
                        if (!isActive || isCancelled) break
                        _progress.postValue(Triple(++processed, names.size, "Processing: $name"))

                        val candidates = AniListApi.searchMedia(name, currentMode)
                        val best = AniListApi.findBestMatch(candidates, name, currentMode)

                        if (best != null && best.idMal != null) {
                            val existing = dao.findById(best.idMal)
                            if (existing == null) {
                                val entry = AniListApi.formatEntry(best, currentMode, defaultStatus)
                                dao.insert(entry)
                                val current = _animeList.value?.toMutableList() ?: mutableListOf()
                                current.add(entry)
                                _animeList.postValue(current.toList())

                                if (autoSeason && currentMode == "anime") {
                                    fetchAutoSeasons(best.idMal, defaultStatus)
                                }
                            }
                        }

                        delay(300)
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                showToast("Error occurred, saved progress", "warning")
            } finally {
                if (!isCancelled) {
                    showToast("Processing Complete!", "success")
                }
                _progress.postValue(null)
                _isProcessing.postValue(false)
            }
        }
    }

    private suspend fun fetchAutoSeasons(rootMalId: Int, defaultStatus: String) {
        if (isCancelled) return
        try {
            val edges = AniListApi.getRelations(rootMalId, currentMode)
            for (edge in edges) {
                if (isCancelled) break
                val node = edge.node ?: continue
                if (listOf("SEQUEL", "PREQUEL", "PARENT").contains(edge.relationType)) {
                    val title = (node.title.english ?: node.title.romaji ?: "").lowercase()
                    if (listOf("recap", "summary", "special").any { title.contains(it) }) continue
                    val malId = node.idMal ?: continue
                    if (visitedRelations.contains(malId)) continue
                    val existing = dao.findById(malId)
                    if (existing == null) {
                        visitedRelations.add(malId)
                        val entry = AniListApi.formatEntry(node, currentMode, defaultStatus)
                        dao.insert(entry)
                        val current = _animeList.value?.toMutableList() ?: mutableListOf()
                        current.add(entry)
                        _animeList.postValue(current.toList())
                        delay(400)
                        fetchAutoSeasons(malId, defaultStatus)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    fun stopProcessing() {
        isCancelled = true
        processJob?.cancel()
        _isProcessing.postValue(false)
        _progress.postValue(null)
        showToast("Stopped", "warning")
    }

    fun deleteEntry(entry: AnimeEntry) {
        viewModelScope.launch {
            dao.delete(entry)
            val updated = _animeList.value?.filter { it.malId != entry.malId } ?: emptyList()
            _animeList.postValue(updated)
        }
    }

    fun updateEntryStatus(malId: Int, newStatus: String) {
        viewModelScope.launch {
            val entry = dao.findById(malId) ?: return@launch
            val updated = entry.copy(userStatus = newStatus)
            dao.update(updated)
            val list = _animeList.value?.map { if (it.malId == malId) updated else it } ?: return@launch
            _animeList.postValue(list)
        }
    }

    fun clearList() {
        viewModelScope.launch {
            dao.clearAll(currentMode)
            _animeList.postValue(emptyList())
        }
    }

    fun exportXML() {
        val list = _animeList.value ?: return
        if (list.isEmpty()) { showToast("List is empty!", "error"); return }
        val xml = Exporter.generateXML(list, currentMode)
        _exportXml.postValue(xml)
    }

    fun exportJSON() {
        val list = _animeList.value ?: return
        if (list.isEmpty()) { showToast("List is empty!", "error"); return }
        val json = Exporter.generateJSON(list, currentMode)
        _exportJson.postValue(json)
    }

    fun clearExportState() {
        _exportXml.postValue(null)
        _exportJson.postValue(null)
    }

    fun switchMode(mode: String) {
        currentMode = mode
        loadSaved()
    }

    private fun showToast(msg: String, type: String) {
        _toast.postValue(ToastState(msg, type))
    }

    private fun cleanAndParseInput(rawText: String, autoSeason: Boolean): List<String> {
        return rawText.split("\n")
            .map { line ->
                var clean = line
                    .replace(Regex("(\\.mkv|\\.mp4|\\.avi|1080p|720p|480p|x264|x265)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("(\\[.*?]|\\(.*?\\))"), "")
                    .replace(Regex("^\\d+[.)\\-\\]\\s]+"), "")
                    .replace(Regex("\\s-\\s*(complete|finished|running|ongoing|dropped|watching).*$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                if (autoSeason) {
                    clean = clean.replace(Regex("\\s+(?:season|s|ep|episode)[\\s.]*\\d+$", RegexOption.IGNORE_CASE), "")
                }
                clean
            }
            .filter { it.length > 1 }
            .distinct()
    }
}
