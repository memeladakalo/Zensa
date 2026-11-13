package com.zensa.app.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.R
import com.zensa.app.core.constants.AppConstants
import com.zensa.app.domain.model.AchievementSnapshot
import com.zensa.app.domain.usecase.FocusedAssignment
import com.zensa.app.ui.common.dueLabel
import com.zensa.app.ui.common.focusReasonLabel
import com.zensa.app.ui.common.levelTitle
import com.zensa.app.ui.common.priorityLabel
import com.zensa.app.ui.theme.PriorityHigh
import com.zensa.app.ui.theme.PriorityLow
import com.zensa.app.ui.theme.PriorityMedium
import com.zensa.app.ui.theme.PriorityUrgent
import com.zensa.app.ui.theme.ZensaTheme

/**
 * DashboardScreen
 *
 * Landing screen after successful login.
 *
 * Layout:
 *   - Top bar: app name + Timer / Notes / Achievements / Settings / Logout
 *   - Welcome card (name, tagline, signed-in email)
 *   - Sync status (online/offline + changes waiting to be synchronised)
 *   - Progress card (level, points, streak) - only when gamification is enabled (User Defined 5)
 *   - Smart Focus section: top-N assignments with the reasons they were ranked (User Defined 4)
 *   - "Open Assignments" / "Modules" buttons
 *   - Session card (proof of real authentication)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenModules: () -> Unit = {},
    onOpenAssignmentDetail: (String) -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Android 13+: ask for the notification permission once the student is signed in.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not: the notification code checks the permission before showing */ }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.registerForPush()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppConstants.APP_NAME,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenTimer) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_zen_timer),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenNotes) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_notes),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state.gamificationEnabled) {
                        IconButton(onClick = onOpenAchievements) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = stringResource(R.string.cd_achievements),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_log_out),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ---------- Welcome card ----------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = if (state.name.isNotBlank())
                                stringResource(R.string.dashboard_welcome_named, state.name)
                            else stringResource(R.string.dashboard_welcome),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        if (state.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.dashboard_signed_in_as, state.email),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // ---------- Offline / sync status ----------
            item {
                SyncStatusRow(isOnline = state.isOnline, pending = state.pendingSyncCount)
            }

            // ---------- Gamification progress (optional) ----------
            val progress = state.progress
            if (state.gamificationEnabled && progress != null) {
                item {
                    ProgressCard(snapshot = progress, onClick = onOpenAchievements)
                }
            }

            // ---------- Smart Focus section ----------
            item {
                Text(
                    text = stringResource(R.string.dashboard_todays_focus),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.focus.isEmpty()) {
                item {
                    EmptyFocusCard()
                }
            } else {
                items(state.focus, key = { it.assignment.localId }) { focused ->
                    FocusCard(
                        focused = focused,
                        onToggle = { completed ->
                            viewModel.toggleFocusCompleted(
                                focused.assignment.localId,
                                completed
                            )
                        },
                        onClick = {
                            onOpenAssignmentDetail(focused.assignment.localId)
                        }
                    )
                }
            }

            // ---------- Navigation buttons ----------
            item {
                Button(
                    onClick = onOpenAssignments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        stringResource(R.string.dashboard_open_assignments),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenModules,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.dashboard_open_modules),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ---------- Session card ----------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_session),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_status),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (state.isAuthenticated)
                                    stringResource(R.string.dashboard_authenticated)
                                else stringResource(R.string.dashboard_not_signed_in),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_token),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (state.isAuthenticated)
                                    stringResource(R.string.dashboard_stored_securely)
                                else "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ---------------- Sync status ----------------

/**
 * Shows whether the device is online and how many changes are still waiting to be
 * synchronised. Colour is NOT the only signal: the text always states the status.
 */
@Composable
private fun SyncStatusRow(isOnline: Boolean, pending: Int) {
    val text = when {
        !isOnline -> stringResource(R.string.sync_status_offline)
        pending > 0 -> stringResource(R.string.sync_status_pending, pending)
        else -> stringResource(R.string.sync_status_synced)
    }
    val dot = when {
        !isOnline -> MaterialTheme.colorScheme.error
        pending > 0 -> PriorityMedium
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- Gamification progress ----------------

@Composable
private fun ProgressCard(snapshot: AchievementSnapshot, onClick: () -> Unit) {
    val level = snapshot.level
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(
                    R.string.progress_level_line,
                    level.level,
                    levelTitle(level.titleKey)
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { level.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.progress_points_streak,
                    snapshot.points,
                    snapshot.currentStreakDays
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------- Focus card ----------------

@Composable
private fun FocusCard(
    focused: FocusedAssignment,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val a = focused.assignment
    val reasonText = focused.reasons
        .mapNotNull { focusReasonLabel(it) }
        .joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = a.isCompleted,
                onCheckedChange = onToggle
            )

            Spacer(modifier = Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(priorityColour(a.priority))
            )

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = a.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${dueLabel(focused.daysUntilDue, a.dueDate)} · ${priorityLabel(a.priority)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Smart Focus transparency: WHY this assignment is shown.
                if (reasonText.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.focus_why, reasonText),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFocusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.dashboard_focus_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- Helpers ----------------

private fun priorityColour(priority: String): Color = when (priority.uppercase()) {
    "LOW" -> PriorityLow
    "MEDIUM" -> PriorityMedium
    "HIGH" -> PriorityHigh
    "URGENT" -> PriorityUrgent
    else -> PriorityMedium
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    ZensaTheme {
        DashboardScreen(
            onLogout = {},
            onOpenAssignments = {},
            onOpenSettings = {},
            onOpenTimer = {},
            onOpenNotes = {},
            onOpenAchievements = {},
            onOpenAssignmentDetail = {}
        )
    }
}
