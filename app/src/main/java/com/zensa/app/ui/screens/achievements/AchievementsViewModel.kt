package com.zensa.app.ui.screens.achievements

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AchievementRepository
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.GamificationCloudRepository
import com.zensa.app.data.repository.SettingsRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.di.ServiceLocator
import com.zensa.app.domain.model.AchievementSnapshot
import com.zensa.app.domain.usecase.AchievementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AchievementsViewModel (User Defined 5: Gamification)
 *
 * Loads the raw data (assignments, subtasks, sessions), asks AchievementsUseCase to
 * compute the snapshot, persists newly unlocked badges to Room so they stay unlocked,
 * and backs the progress up to the cloud (Azure Cosmos DB, via the Spring Boot API).
 *
 * The cloud copy is read back and shown on screen, which demonstrates both storing
 * and reading NoSQL data. If the device is offline the cloud step is skipped silently:
 * the local (Room) result is always the source of truth for the UI.
 */
class AchievementsViewModel(
    private val achievementRepository: AchievementRepository =
        ServiceLocator.achievementRepository,
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val subtaskRepository: SubtaskRepository =
        ServiceLocator.subtaskRepository,
    private val studySessionRepository: StudySessionRepository =
        ServiceLocator.studySessionRepository,
    private val settingsRepository: SettingsRepository =
        ServiceLocator.settingsRepository,
    private val cloudRepository: GamificationCloudRepository =
        ServiceLocator.gamificationCloudRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage(),
    private val useCase: AchievementsUseCase = AchievementsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val enabled = settingsRepository.gamificationEnabled.first()
            if (!enabled) {
                _uiState.update { it.copy(isLoading = false, gamificationEnabled = false) }
                return@launch
            }

            try {
                val userId = tokenStorage.getUserId()

                // first() takes one value and stops collecting (the old hand-written helper
                // threw an uncaught exception to stop the flow, which crashed the screen).
                val assignments = assignmentRepository.observeAssignments(userId).first()
                val sessions = studySessionRepository.getForUser(userId)
                val subtasks = assignments.flatMap { a ->
                    subtaskRepository.getForAssignment(a.localId)
                }

                val unlockedAtMap = achievementRepository.unlockedAtMap()
                val snapshot = useCase.compute(assignments, subtasks, sessions, unlockedAtMap)

                // Persist newly unlocked badges (permanent), then re-read their dates.
                val newlyUnlockedIds = snapshot.achievements
                    .filter { it.unlocked && it.unlockedAt == null }
                    .map { it.achievement.id }
                if (newlyUnlockedIds.isNotEmpty()) {
                    achievementRepository.recordAll(newlyUnlockedIds)
                }

                val finalSnapshot = useCase.compute(
                    assignments, subtasks, sessions, achievementRepository.unlockedAtMap()
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gamificationEnabled = true,
                        snapshot = finalSnapshot,
                        error = null
                    )
                }

                backUpToCloud(finalSnapshot)
            } catch (e: Exception) {
                Log.e(TAG, "Could not load achievements", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error") }
            }
        }
    }

    /** Stores the snapshot in Cosmos DB, then reads the stored document back for display. */
    private suspend fun backUpToCloud(snapshot: AchievementSnapshot) {
        when (val saved = cloudRepository.save(snapshot)) {
            is ApiResult.Success -> {
                Log.i(TAG, "Gamification profile stored in cloud")
                when (val read = cloudRepository.load()) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(cloudStatus = CloudStatus.SAVED, cloudUpdatedAt = read.data?.updatedAt)
                    }
                    else -> _uiState.update { it.copy(cloudStatus = CloudStatus.SAVED) }
                }
            }
            is ApiResult.Error -> {
                // Offline or cloud not configured: not an error for the student.
                Log.i(TAG, "Cloud backup skipped: ${saved.message}")
                _uiState.update { it.copy(cloudStatus = CloudStatus.UNAVAILABLE) }
            }
            ApiResult.Loading -> Unit
        }
    }

    private companion object {
        const val TAG = "AchievementsViewModel"
    }
}

enum class CloudStatus { UNKNOWN, SAVED, UNAVAILABLE }

data class AchievementsUiState(
    val isLoading: Boolean = false,
    val gamificationEnabled: Boolean = true,
    val snapshot: AchievementSnapshot? = null,
    val cloudStatus: CloudStatus = CloudStatus.UNKNOWN,
    val cloudUpdatedAt: String? = null,
    val error: String? = null
)
