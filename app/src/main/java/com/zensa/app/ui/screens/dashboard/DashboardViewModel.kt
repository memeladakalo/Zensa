package com.zensa.app.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.NetworkMonitor
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AchievementRepository
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.SettingsRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.data.repository.SyncRepository
import com.zensa.app.di.ServiceLocator
import com.zensa.app.domain.model.AchievementSnapshot
import com.zensa.app.domain.usecase.AchievementsUseCase
import com.zensa.app.domain.usecase.FocusedAssignment
import com.zensa.app.domain.usecase.SmartFocusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * DashboardViewModel
 *
 * Provides the dashboard with:
 *   - The current user's name, email, auth status
 *   - Smart Focus (User Defined 4): the top-N highest-scoring assignments, each with
 *     the reasons it was ranked
 *   - Gamification (User Defined 5): points, level and streak summary (when enabled)
 *   - Offline status: whether the device is online and how many changes are waiting
 *     to be synchronised
 *
 * Everything is recomputed whenever Room emits new assignments or study sessions, so the
 * dashboard stays live without any manual refresh.
 */
class DashboardViewModel(
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage(),
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val subtaskRepository: SubtaskRepository =
        ServiceLocator.subtaskRepository,
    private val studySessionRepository: StudySessionRepository =
        ServiceLocator.studySessionRepository,
    private val achievementRepository: AchievementRepository =
        ServiceLocator.achievementRepository,
    private val settingsRepository: SettingsRepository =
        ServiceLocator.settingsRepository,
    private val syncRepository: SyncRepository =
        ServiceLocator.syncRepository,
    private val networkMonitor: NetworkMonitor =
        ServiceLocator.networkMonitor(),
    private val smartFocus: SmartFocusUseCase = SmartFocusUseCase(),
    private val achievementsUseCase: AchievementsUseCase = AchievementsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            name = tokenStorage.getUserName().orEmpty(),
            email = tokenStorage.getUserEmail().orEmpty(),
            isAuthenticated = tokenStorage.hasToken()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeFocusAndProgress()
        observeSyncStatus()
        observeGamificationSetting()
    }

    // ---------- Smart Focus + gamification pipeline ----------

    /**
     * Re-runs whenever the assignments OR the study sessions change in Room.
     * The number of assignments on a student device is small (tens), so a small
     * read per assignment is fine here.
     */
    private fun observeFocusAndProgress() {
        viewModelScope.launch {
            val userId = tokenStorage.getUserId()
            combine(
                assignmentRepository.observeAssignments(userId),
                studySessionRepository.observeForUser(userId)
            ) { assignments, sessions -> assignments to sessions }
                .collect { (assignments, sessions) ->
                    try {
                        val subtasksByAssignment: Map<String, List<SubtaskEntity>> =
                            assignments.associate { a ->
                                a.localId to subtaskRepository.getForAssignment(a.localId)
                            }

                        val focused: List<FocusedAssignment> = smartFocus.rank(
                            assignments = assignments,
                            subtasksByAssignment = subtasksByAssignment,
                            limit = SmartFocusUseCase.DEFAULT_LIMIT
                        )

                        val unlockedAt = achievementRepository.unlockedAtMap()
                        val snapshot: AchievementSnapshot = achievementsUseCase.compute(
                            assignments = assignments,
                            subtasks = subtasksByAssignment.values.flatten(),
                            sessions = sessions,
                            unlockedAtMap = unlockedAt
                        )

                        // Remember newly earned badges so they stay unlocked permanently.
                        val newIds = snapshot.achievements
                            .filter { it.unlocked && it.unlockedAt == null }
                            .map { it.achievement.id }
                        if (newIds.isNotEmpty()) achievementRepository.recordAll(newIds)

                        _uiState.update { it.copy(focus = focused, progress = snapshot) }
                    } catch (e: Exception) {
                        // Never crash the dashboard because of a derived-data problem.
                        Log.e(TAG, "Could not compute focus/progress", e)
                    }
                }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncRepository.pendingCount.collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun observeGamificationSetting() {
        viewModelScope.launch {
            settingsRepository.gamificationEnabled.collect { enabled ->
                _uiState.update { it.copy(gamificationEnabled = enabled) }
            }
        }
    }

    // ---------- Actions ----------

    /** Best-effort refresh of assignments from the API. */
    fun refresh() {
        viewModelScope.launch {
            assignmentRepository.refresh()
        }
    }

    fun toggleFocusCompleted(localId: String, completed: Boolean) {
        viewModelScope.launch {
            assignmentRepository.setCompleted(localId, completed)
        }
    }

    /** Registers this phone for push notifications (no-op if Firebase is not configured). */
    fun registerForPush() {
        viewModelScope.launch {
            try {
                ServiceLocator.pushTokenRegistrar.registerCurrentToken()
            } catch (e: Exception) {
                Log.w(TAG, "Push registration skipped: ${e.javaClass.simpleName}")
            }
        }
    }

    fun logout() {
        ServiceLocator.logout()
    }

    private companion object {
        const val TAG = "DashboardViewModel"
    }
}

data class DashboardUiState(
    val name: String = "",
    val email: String = "",
    val isAuthenticated: Boolean = false,
    val focus: List<FocusedAssignment> = emptyList(),
    val progress: AchievementSnapshot? = null,
    val gamificationEnabled: Boolean = true,
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0
)
