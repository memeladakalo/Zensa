package com.zensa.app.ui.screens.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.FileRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.NoteRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteFormViewModel(
    private val noteRepository: NoteRepository = ServiceLocator.noteRepository,
    private val moduleRepository: ModuleRepository = ServiceLocator.moduleRepository,
    private val fileRepository: FileRepository = ServiceLocator.fileRepository,
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteFormUiState())
    val uiState: StateFlow<NoteFormUiState> = _uiState.asStateFlow()

    private var editingLocalId: String? = null

    fun start(localId: String?) {
        editingLocalId = localId
        loadModules()

        if (localId == null) {
            _uiState.update {
                it.copy(
                    title = "",
                    content = "",
                    attachmentBlobName = null,
                    moduleLocalId = null,
                    titleError = null,
                    generalError = null,
                    isLoading = false
                )
            }
        } else {
            viewModelScope.launch {
                val existing = noteRepository.getByLocalId(localId)
                if (existing == null) {
                    _uiState.update { it.copy(generalError = "Note not found") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        title = existing.title,
                        content = existing.content,
                        attachmentBlobName = existing.attachmentUrl,
                        moduleLocalId = existing.moduleLocalId,
                        titleError = null,
                        generalError = null
                    )
                }
            }
        }
    }

    fun onTitleChange(v: String) =
        _uiState.update { it.copy(title = v, titleError = null, generalError = null) }

    fun onContentChange(v: String) =
        _uiState.update { it.copy(content = v) }

    fun onModuleChange(moduleLocalId: String?) =
        _uiState.update { it.copy(moduleLocalId = moduleLocalId) }

    // ---------------- Attachment (Azure Blob Storage) ----------------

    /** Uploads the picked file to the API (which stores it in Azure Blob Storage). */
    fun onAttachmentPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, generalError = null) }
            when (val result = fileRepository.upload(uri)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isUploading = false, attachmentBlobName = result.data.blobName)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isUploading = false, generalError = result.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    /** Detaches the file from the note (the blob is removed from storage as well, best effort). */
    fun removeAttachment() {
        val blob = _uiState.value.attachmentBlobName ?: return
        _uiState.update { it.copy(attachmentBlobName = null) }
        viewModelScope.launch {
            try {
                fileRepository.delete(blob)
            } catch (_: Exception) { /* offline: the blob is simply left behind */ }
        }
    }

    /** Asks the API for a short-lived read-only link and hands it to the UI to open. */
    fun openAttachment(onLink: (String) -> Unit) {
        val blob = _uiState.value.attachmentBlobName ?: return
        viewModelScope.launch {
            when (val result = fileRepository.downloadLink(blob)) {
                is ApiResult.Success -> onLink(result.data)
                is ApiResult.Error -> _uiState.update { it.copy(generalError = result.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.title.isBlank() && s.content.isBlank()) {
            _uiState.update { it.copy(titleError = "Write a title or some content") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = if (editingLocalId == null) {
                noteRepository.create(
                    userId = tokenStorage.getUserId(),
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    content = s.content,
                    attachmentUrl = s.attachmentBlobName
                )
            } else {
                noteRepository.update(
                    localId = editingLocalId!!,
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    content = s.content,
                    attachmentUrl = s.attachmentBlobName
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = editingLocalId ?: return
        viewModelScope.launch {
            noteRepository.delete(id)
            onDeleted()
        }
    }

    private fun loadModules() {
        viewModelScope.launch {
            val modules: List<ModuleEntity> =
                moduleRepository.observeModules(tokenStorage.getUserId()).first()
            _uiState.update { it.copy(availableModules = modules) }
        }
    }
}

data class NoteFormUiState(
    val title: String = "",
    val content: String = "",
    val moduleLocalId: String? = null,
    val titleError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    /** Blob name of the attached file in Azure Blob Storage ("<userId>/<uuid>-name.pdf"), or null. */
    val attachmentBlobName: String? = null,
    val isUploading: Boolean = false,
    val availableModules: List<ModuleEntity> = emptyList()
)
