package com.zensa.app.ui.screens.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.R
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.ui.common.localizedMessage

/**
 * ModulesScreen - "My Modules": the student's academic subjects.
 * Floating action button creates a module; tap a card to edit; bin icon deletes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    onBack: () -> Unit,
    viewModel: ModulesViewModel = viewModel()
) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ModuleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.modules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.modules_new)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (modules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.modules_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.modules_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(modules, key = { it.localId }) { module ->
                    ModuleCard(
                        module = module,
                        onClick = { viewModel.openEdit(module) },
                        onDelete = { pendingDelete = module }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ---------- Create / edit dialog ----------
    if (form.visible) {
        AlertDialog(
            onDismissRequest = viewModel::closeForm,
            title = {
                Text(
                    if (form.editingLocalId == null) stringResource(R.string.modules_new)
                    else stringResource(R.string.modules_edit)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.code,
                        onValueChange = viewModel::onCodeChange,
                        label = { Text(stringResource(R.string.module_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(R.string.module_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text(stringResource(R.string.module_description)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    form.error?.let {
                        Text(
                            localizedMessage(it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::save, enabled = !form.isSaving) {
                    if (form.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeForm) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // ---------- Delete confirmation ----------
    pendingDelete?.let { module ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.modules_delete_title)) },
            text = { Text(stringResource(R.string.modules_delete_message, module.moduleCode)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(module)
                    pendingDelete = null
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ModuleCard(
    module: ModuleEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(parseColour(module.colour))
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.moduleCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Pending-sync indicator (offline mode): text, not colour alone.
                if (!module.isSynced) {
                    Text(
                        text = stringResource(R.string.pending_sync_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Parses "#RRGGBB"; falls back to the Zensa purple if the stored value is malformed. */
private fun parseColour(hex: String): androidx.compose.ui.graphics.Color =
    try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        androidx.compose.ui.graphics.Color(0xFF6A4BAF)
    }
