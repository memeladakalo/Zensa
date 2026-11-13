package com.zensa.app.ui.screens.modules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ModulesViewModel - module management (FR-06): list, create, edit, delete.
 *
 * Offline-first: ModuleRepository writes to Room first and queues the change for
 * synchronisation, so every action here works without an internet connection.
 */
class ModulesViewModel(
    private val repository: ModuleRepository = ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage()
) : ViewModel() {

    val modules: StateFlow<List<ModuleEntity>> =
        repository.observeModules(tokenStorage.getUserId())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(ModuleFormState())
    val form: StateFlow<ModuleFormState> = _form.asStateFlow()

    init {
        // Best-effort refresh from the server; failure (offline) keeps showing saved modules.
        viewModelScope.launch {
            repository.refresh(tokenStorage.getUserId())
        }
    }

    // ---------- Form events ----------

    fun openCreate() {
        _form.value = ModuleFormState(visible = true)
    }

    fun openEdit(module: ModuleEntity) {
        _form.value = ModuleFormState(
            visible = true,
            editingLocalId = module.localId,
            code = module.moduleCode,
            name = module.name,
            description = module.description
        )
    }

    fun closeForm() {
        _form.value = ModuleFormState()
    }

    fun onCodeChange(value: String) =
        _form.update { it.copy(code = value.take(MAX_CODE), error = null) }

    fun onNameChange(value: String) =
        _form.update { it.copy(name = value.take(MAX_NAME), error = null) }

    fun onDescriptionChange(value: String) =
        _form.update { it.copy(description = value.take(MAX_DESCRIPTION), error = null) }

    // ---------- Save / delete ----------

    fun save() {
        val f = _form.value
        val error = validate(f.code, f.name)
        if (error != null) {
            _form.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, error = null) }
            val result = if (f.editingLocalId == null) {
                repository.create(
                    userId = tokenStorage.getUserId(),
                    moduleCode = f.code.trim(),
                    name = f.name.trim(),
                    description = f.description.trim(),
                    colour = DEFAULT_COLOUR
                )
            } else {
                repository.update(
                    localId = f.editingLocalId,
                    moduleCode = f.code.trim(),
                    name = f.name.trim(),
                    description = f.description.trim(),
                    colour = DEFAULT_COLOUR
                )
            }
            when (result) {
                is ApiResult.Success -> closeForm()
                is ApiResult.Error -> {
                    Log.w(TAG, "Saving module failed: ${result.message}")
                    _form.update { it.copy(isSaving = false, error = result.message) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun delete(module: ModuleEntity) {
        viewModelScope.launch {
            val result = repository.delete(module.localId)
            if (result is ApiResult.Error) Log.w(TAG, "Deleting module failed: ${result.message}")
        }
    }

    companion object {
        private const val TAG = "ModulesViewModel"
        const val MAX_CODE = 20
        const val MAX_NAME = 80
        const val MAX_DESCRIPTION = 300
        const val DEFAULT_COLOUR = "#6A4BAF"

        /** Pure validation (unit-testable). Returns an English message or null when valid. */
        fun validate(code: String, name: String): String? = when {
            code.isBlank() -> "Module code is required"
            name.isBlank() -> "Module name is required"
            else -> null
        }
    }
}

data class ModuleFormState(
    val visible: Boolean = false,
    val editingLocalId: String? = null,
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val error: String? = null,
    val isSaving: Boolean = false
)
