package com.zensa.app.ui.screens.notes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zensa.app.R
import com.zensa.app.data.repository.FileRepository
import com.zensa.app.ui.common.localizedMessage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.ui.theme.ZensaError
import com.zensa.app.ui.theme.ZensaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(
    noteLocalId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: NoteFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEdit = noteLocalId != null

    LaunchedEffect(noteLocalId) { viewModel.start(noteLocalId) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // System file picker (Storage Access Framework): no storage permission needed.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let(viewModel::onAttachmentPicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) stringResource(R.string.note_edit) else stringResource(R.string.note_new)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    isError = state.titleError != null,
                    supportingText = {
                        state.titleError?.let { Text(localizedMessage(it), color = ZensaError) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.content,
                    onValueChange = viewModel::onContentChange,
                    label = { Text(stringResource(R.string.note_content)) },
                    minLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.field_module_optional),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (state.availableModules.isEmpty()) {
                    Text(
                        stringResource(R.string.modules_empty_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.moduleLocalId == null,
                            onClick = { viewModel.onModuleChange(null) },
                            label = { Text(stringResource(R.string.action_none)) }
                        )
                        state.availableModules.take(3).forEach { m ->
                            FilterChip(
                                selected = state.moduleLocalId == m.localId,
                                onClick = { viewModel.onModuleChange(m.localId) },
                                label = { Text(m.moduleCode) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----- Attachment (stored in Azure Blob Storage through the API) -----
                Text(
                    stringResource(R.string.note_attachment),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val blob = state.attachmentBlobName
                if (blob != null) {
                    Text(
                        FileRepository.displayName(blob),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            viewModel.openAttachment { url ->
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text(stringResource(R.string.note_attachment_open)) }
                        TextButton(onClick = viewModel::removeAttachment) {
                            Text(
                                stringResource(R.string.note_attachment_remove),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { filePicker.launch(ATTACHMENT_TYPES) },
                        enabled = !state.isUploading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.note_attachment_add))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (state.generalError != null) {
                    Text(
                        localizedMessage(state.generalError!!),
                        color = ZensaError,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { viewModel.save(onSaved) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isEdit) stringResource(R.string.assignment_save_changes) else stringResource(R.string.note_save),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.note_delete_title)) },
                text = { Text(stringResource(R.string.note_delete_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onDeleted)
                    }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
    }
}

/** File types the API accepts (it validates again on the server). */
private val ATTACHMENT_TYPES = arrayOf(
    "application/pdf",
    "image/png",
    "image/jpeg",
    "text/plain",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NoteFormScreenPreview() {
    ZensaTheme {
        NoteFormScreen(
            noteLocalId = null,
            onNavigateBack = {},
            onSaved = {},
            onDeleted = {}
        )
    }
}
