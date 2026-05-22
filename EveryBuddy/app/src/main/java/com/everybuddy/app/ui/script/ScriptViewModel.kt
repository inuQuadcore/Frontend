package com.everybuddy.app.ui.script

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.repository.TranslateRepository
import com.everybuddy.app.ui.chat.ScriptFolder
import com.everybuddy.app.ui.chat.ScriptItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ScriptUiState(
    val folders       : List<ScriptFolder> = emptyList(),
    val items         : List<ScriptItem>   = emptyList(),
    val selectedFolderIndex : Int          = 0,   // 0 = 전체, 1~ = 특정 폴더
    val sortOption    : ScriptSortOption   = ScriptSortOption.SAVED_AT_DESC,
    val searchQuery   : String             = "",
    val isRefreshing  : Boolean            = false,
    val toastMessage  : String?            = null,
    val isTtsLoading  : Boolean            = false,
)

enum class ScriptSortOption(val label: String) {
    SAVED_AT_DESC("추가된 순"),
    SAVED_AT_ASC("오래된 순"),
    ALPHABETICAL("알파벳 순"),
}

@HiltViewModel
class ScriptViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translateRepository: TranslateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptUiState())
    val uiState: StateFlow<ScriptUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    /** 현재 선택된 폴더 기준으로 필터링된 아이템 목록 */
    val filteredItems: List<ScriptItem>
        get() {
            val state = _uiState.value
            val base = when {
                state.selectedFolderIndex == 0 -> state.items  // 전체
                else -> {
                    val folderId = state.folders.getOrNull(state.selectedFolderIndex - 1)?.id
                    state.items.filter { it.folderId == folderId }
                }
            }
            val searched = if (state.searchQuery.isBlank()) base
            else base.filter {
                it.originalText.contains(state.searchQuery, ignoreCase = true) ||
                        it.translatedText.contains(state.searchQuery, ignoreCase = true) ||
                        it.memo1.contains(state.searchQuery, ignoreCase = true)
            }
            return when (state.sortOption) {
                ScriptSortOption.SAVED_AT_DESC -> searched  // 이미 최신순
                ScriptSortOption.SAVED_AT_ASC  -> searched.reversed()
                ScriptSortOption.ALPHABETICAL  -> searched.sortedBy { it.originalText }
            }
        }

    fun saveScriptItem(
        originalText   : String,
        translatedText : String,
        memo1          : String,
        memo2          : String,
        folderId       : String?,
    ) {
        val newItem = ScriptItem(
            id             = UUID.randomUUID().toString(),
            originalText   = originalText,
            translatedText = translatedText,
            memo1          = memo1,
            memo2          = memo2,
            folderId       = folderId,
        )
        _uiState.update { state ->
            val updatedItems   = listOf(newItem) + state.items
            // 폴더 아이템 카운트 갱신
            val updatedFolders = state.folders.map { f ->
                if (f.id == folderId) f.copy(count = f.count + 1) else f
            }
            state.copy(items = updatedItems, folders = updatedFolders, toastMessage = "저장되었습니다.")
        }
    }

    fun addFolder(name: String, coverImageUri: String?, id: String = UUID.randomUUID().toString()) {
        val newFolder = ScriptFolder(
            id         = id,
            name       = name,
            count      = 0,
            coverImage = coverImageUri ?: "",
        )
        _uiState.update { it.copy(folders = it.folders + newFolder) }
    }

    fun deleteItem(id: String) {
        _uiState.update { state ->
            val item = state.items.find { it.id == id }
            val updatedFolders = if (item?.folderId != null) {
                state.folders.map { f -> if (f.id == item.folderId) f.copy(count = (f.count - 1).coerceAtLeast(0)) else f }
            } else state.folders
            state.copy(items = state.items.filter { it.id != id }, folders = updatedFolders)
        }
    }

    fun selectFolder(index: Int) {
        _uiState.update { it.copy(selectedFolderIndex = index) }
    }

    fun changeSortOption(option: ScriptSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(300)
            // TODO: real API call
            _uiState.update { it.copy(
                isRefreshing = false,
                folders      = emptyList(),
                items        = emptyList(),
            ) }
        }
    }

    fun updateItem(updatedItem: ScriptItem) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == updatedItem.id) updatedItem else it })
        }
    }

    fun updateFolder(folder: ScriptFolder) {
        _uiState.update { state ->
            state.copy(folders = state.folders.map { if (it.id == folder.id) folder else it })
        }
    }

    fun deleteItems(ids: Set<String>) {
        if (ids.isEmpty()) return
        _uiState.update { state ->
            val removed = state.items.filter { it.id in ids }
            val updatedFolders = state.folders.map { f ->
                val removedCount = removed.count { it.folderId == f.id }
                if (removedCount > 0) f.copy(count = (f.count - removedCount).coerceAtLeast(0)) else f
            }
            state.copy(items = state.items.filter { it.id !in ids }, folders = updatedFolders)
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun playAudio(item: ScriptItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTtsLoading = true) }
            val result = translateRepository.tts(item.originalText)
            _uiState.update { it.copy(isTtsLoading = false) }
            if (result is ApiResult.Success) {
                val bytes = result.data ?: return@launch
                try {
                    val tempFile = File.createTempFile("tts_", ".wav", context.cacheDir)
                    tempFile.writeBytes(bytes)
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(toastMessage = "재생 실패: ${e.message}") }
                }
            } else {
                _uiState.update { it.copy(toastMessage = "TTS 변환에 실패했습니다.") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
