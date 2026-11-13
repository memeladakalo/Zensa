package com.zensa.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.BuildConfig
import com.zensa.app.R
import com.zensa.app.core.locale.AppLanguage
import com.zensa.app.ui.common.localizedMessage
import com.zensa.app.core.constants.AppConstants
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.ui.theme.ZensaError
import com.zensa.app.ui.theme.ZensaTheme
import com.zensa.app.ui.common.priorityLabel

/**
 * SettingsScreen
 *
 * Sections:
 *   1. Profile         — name, email, edit name
 *   2. Appearance      — theme picker
 *   3. Study defaults  — duration, priority
 *   4. Notifications   — toggle
 *   5. Synchronisation — sync now + last sync
 *   6. Account         — change password, log out
 *   7. About           — app version, tagline
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ====================================================
                // PROFILE
                // ====================================================
                SectionTitle(stringResource(R.string.settings_profile))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReadOnlyRow(stringResource(R.string.settings_name), state.name.ifBlank { "—" })
                        ReadOnlyRow(stringResource(R.string.login_email), state.email.ifBlank { "—" })

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!state.showEditName) {
                            Button(
                                onClick = { viewModel.onEditNameToggle(true) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(stringResource(R.string.settings_edit_name))
                            }
                        } else {
                            OutlinedTextField(
                                value = state.editName,
                                onValueChange = viewModel::onEditNameChange,
                                label = { Text(stringResource(R.string.settings_new_name)) },
                                singleLine = true,
                                isError = state.editNameError != null,
                                supportingText = {
                                    state.editNameError?.let {
                                        Text(localizedMessage(it), color = ZensaError)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.saveName() },
                                    enabled = !state.isSavingName,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (state.isSavingName) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else Text(stringResource(R.string.action_save))
                                }
                                TextButton(onClick = { viewModel.onEditNameToggle(false) }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        }

                        if (state.editNameSuccess) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.settings_name_updated),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // ====================================================
                // APPEARANCE
                // ====================================================
                SectionTitle(stringResource(R.string.settings_appearance))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeChip(stringResource(R.string.theme_system), UserPreferences.THEME_SYSTEM, state.theme) {
                                viewModel.onThemeChange(it)
                            }
                            ThemeChip(stringResource(R.string.theme_light), UserPreferences.THEME_LIGHT, state.theme) {
                                viewModel.onThemeChange(it)
                            }
                            ThemeChip(stringResource(R.string.theme_dark), UserPreferences.THEME_DARK, state.theme) {
                                viewModel.onThemeChange(it)
                            }
                        }
                    }
                }

                // ====================================================
                // STUDY DEFAULTS
                // ====================================================
                SectionTitle(stringResource(R.string.settings_study_defaults))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            stringResource(R.string.settings_default_duration, state.defaultStudyDuration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60).forEach { minutes ->
                                FilterChip(
                                    selected = state.defaultStudyDuration == minutes,
                                    onClick = { viewModel.onDefaultDurationChange(minutes) },
                                    label = { Text("$minutes") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            stringResource(R.string.settings_default_priority),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                                FilterChip(
                                    selected = state.defaultPriority == p,
                                    onClick = { viewModel.onDefaultPriorityChange(p) },
                                    label = { Text(priorityLabel(p)) }
                                )
                            }
                        }
                    }
                }

                // ====================================================
                // NOTIFICATIONS
                // ====================================================
                SectionTitle(stringResource(R.string.settings_notifications))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_study_reminders),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.settings_reminders_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = viewModel::onNotificationsToggle
                        )
                    }
                }

                PushTestCard(
                    isSending = state.isSendingTest,
                    message = state.testNotificationMessage,
                    onSend = viewModel::sendTestNotification
                )

                // ====================================================
                // LANGUAGE
                // ====================================================
                SectionTitle(stringResource(R.string.settings_language_section))
                LanguageCard()

                // ====================================================
                // GAMIFICATION (optional - FR-14)
                // ====================================================
                SectionTitle(stringResource(R.string.settings_gamification_section))
                GamificationCard(
                    enabled = state.gamificationEnabled,
                    onToggle = viewModel::onGamificationToggle
                )

                // ====================================================
                // SYNCHRONISATION
                // ====================================================
                SectionTitle(stringResource(R.string.settings_sync))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReadOnlyRow(
                            stringResource(R.string.settings_last_sync),
                            state.lastSyncAt?.replace("T", " ") ?: stringResource(R.string.settings_never)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !state.isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else Text(stringResource(R.string.settings_sync_now))
                        }
                        state.syncMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                localizedMessage(it),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ====================================================
                // ACCOUNT
                // ====================================================
                SectionTitle(stringResource(R.string.settings_account))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        if (!state.showChangePassword) {
                            Button(
                                onClick = { viewModel.onPasswordToggle(true) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(stringResource(R.string.settings_change_password))
                            }
                        } else {
                            PasswordField(
                                value = state.currentPassword,
                                onValueChange = viewModel::onCurrentPasswordChange,
                                label = stringResource(R.string.settings_current_password)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PasswordField(
                                value = state.newPassword,
                                onValueChange = viewModel::onNewPasswordChange,
                                label = stringResource(R.string.settings_new_password)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PasswordField(
                                value = state.confirmPassword,
                                onValueChange = viewModel::onConfirmPasswordChange,
                                label = stringResource(R.string.settings_confirm_password)
                            )
                            state.passwordError?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(localizedMessage(it), color = ZensaError,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.savePassword() },
                                    enabled = !state.isSavingPassword,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (state.isSavingPassword) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else Text(stringResource(R.string.settings_save_password))
                                }
                                TextButton(onClick = { viewModel.onPasswordToggle(false) }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        }

                        if (state.passwordSuccess) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.settings_password_updated),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showLogoutDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.action_log_out))
                        }
                    }
                }

                // ====================================================
                // ABOUT
                // ====================================================
                SectionTitle(stringResource(R.string.settings_about))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReadOnlyRow(stringResource(R.string.settings_app), AppConstants.APP_NAME)
                        ReadOnlyRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
                        ReadOnlyRow(stringResource(R.string.settings_tagline), stringResource(R.string.app_tagline))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.settings_logout_title)) },
                text = { Text(stringResource(R.string.settings_logout_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text(stringResource(R.string.action_log_out), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

// ---------------- Small reusable pieces ----------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    value: String,
    currentValue: String,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = currentValue == value,
        onClick = { onClick(value) },
        label = { Text(label) }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible)
            VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Sends a REAL push notification through the backend and Firebase Cloud Messaging to this
 * phone, so the notification pipeline can be demonstrated and tested from the app itself.
 */
@Composable
private fun PushTestCard(isSending: Boolean, message: String?, onSend: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_push_test_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSend,
                enabled = !isSending,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.settings_push_test_button))
                }
            }
            if (message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val sent = message == SettingsViewModel.TEST_SENT
                Text(
                    text = if (sent) stringResource(R.string.push_test_sent) else localizedMessage(message),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sent) MaterialTheme.colorScheme.primary else ZensaError
                )
            }
        }
    }
}

/**
 * Language picker. Uses the official per-app language API (AppLanguage.apply),
 * which re-creates the Activity so every stringResource() updates immediately.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageCard() {
    var selected by remember { mutableStateOf(AppLanguage.current()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_language_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = selected == language,
                        onClick = {
                            selected = language
                            AppLanguage.apply(language)
                        },
                        label = { Text(language.nativeName) }
                    )
                }
            }
        }
    }
}

/** On/off switch for the optional gamification features (points, streaks, badges, levels). */
@Composable
private fun GamificationCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_gamification_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.settings_gamification_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    ZensaTheme {
        SettingsScreen(onBack = {}, onLogout = {})
    }
}
