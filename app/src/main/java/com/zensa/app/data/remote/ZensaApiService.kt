package com.zensa.app.data.remote

import com.zensa.app.data.remote.dto.AssignmentDto
import com.zensa.app.data.remote.dto.AssignmentRequestDto
import com.zensa.app.data.remote.dto.ChangePasswordRequestDto
import com.zensa.app.data.remote.dto.LoginRequestDto
import com.zensa.app.data.remote.dto.LoginResponseDto
import com.zensa.app.data.remote.dto.ModuleDto
import com.zensa.app.data.remote.dto.ModuleRequestDto
import com.zensa.app.data.remote.dto.RegisterRequestDto
import com.zensa.app.data.remote.dto.RegisterResponseDto
import com.zensa.app.data.remote.dto.StudySessionDto
import com.zensa.app.data.remote.dto.StudySessionRequestDto
import com.zensa.app.data.remote.dto.SubtaskDto
import com.zensa.app.data.remote.dto.SubtaskRequestDto
import com.zensa.app.data.remote.dto.SyncRequestDto
import com.zensa.app.data.remote.dto.SyncResponseDto
import com.zensa.app.data.remote.dto.UpdateProfileRequestDto
import com.zensa.app.data.remote.dto.UserProfileDto
import com.zensa.app.data.remote.dto.NoteDto
import com.zensa.app.data.remote.dto.NoteRequestDto
import com.zensa.app.data.remote.dto.DeviceTokenRequestDto
import com.zensa.app.data.remote.dto.FileLinkDto
import com.zensa.app.data.remote.dto.FileUploadResponseDto
import com.zensa.app.data.remote.dto.GamificationProfileDto
import com.zensa.app.data.remote.dto.GamificationProfileRequestDto
import com.zensa.app.data.remote.dto.GoogleLoginRequestDto
import com.zensa.app.data.remote.dto.TestNotificationResultDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ZensaApiService {

    // ---------------- Auth ----------------
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    /** SSO: exchanges a Google ID token (from OAuth 2.0 + PKCE) for a Zensa JWT. */
    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequestDto): Response<LoginResponseDto>

    // ---------------- Push notifications (Firebase Cloud Messaging) ----------------
    @POST("api/devices")
    suspend fun registerDevice(@Body request: DeviceTokenRequestDto): Response<Unit>

    @HTTP(method = "DELETE", path = "api/devices", hasBody = true)
    suspend fun unregisterDevice(@Body request: DeviceTokenRequestDto): Response<Unit>

    @POST("api/notifications/test")
    suspend fun sendTestNotification(): Response<TestNotificationResultDto>

    // ---------------- Files (Azure Blob Storage) ----------------
    @Multipart
    @POST("api/files")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileUploadResponseDto>

    @GET("api/files/link")
    suspend fun fileLink(@Query("name") blobName: String): Response<FileLinkDto>

    @DELETE("api/files")
    suspend fun deleteFile(@Query("name") blobName: String): Response<Unit>

    // ---------------- Gamification (Azure Cosmos DB) ----------------
    @PUT("api/gamification/me")
    suspend fun saveGamification(
        @Body request: GamificationProfileRequestDto
    ): Response<GamificationProfileDto>

    @GET("api/gamification/me")
    suspend fun getGamification(): Response<GamificationProfileDto>

    // ---------------- Users ----------------
    @GET("api/users/me")
    suspend fun me(): Response<UserProfileDto>

    @PUT("api/users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequestDto
    ): Response<Map<String, String>>

    @PUT("api/users/me/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequestDto
    ): Response<Map<String, String>>

    // ---------------- Modules ----------------
    @GET("api/modules")
    suspend fun getModules(): Response<List<ModuleDto>>

    @POST("api/modules")
    suspend fun createModule(@Body request: ModuleRequestDto): Response<ModuleDto>

    @PUT("api/modules/{id}")
    suspend fun updateModule(
        @Path("id") id: String,
        @Body request: ModuleRequestDto
    ): Response<ModuleDto>

    @DELETE("api/modules/{id}")
    suspend fun deleteModule(@Path("id") id: String): Response<Unit>

    // ---------------- Assignments ----------------
    @GET("api/assignments")
    suspend fun getAssignments(): Response<List<AssignmentDto>>

    @POST("api/assignments")
    suspend fun createAssignment(
        @Body request: AssignmentRequestDto
    ): Response<AssignmentDto>

    @PUT("api/assignments/{id}")
    suspend fun updateAssignment(
        @Path("id") id: String,
        @Body request: AssignmentRequestDto
    ): Response<AssignmentDto>

    @DELETE("api/assignments/{id}")
    suspend fun deleteAssignment(@Path("id") id: String): Response<Unit>

    // ---------------- Subtasks ----------------
    @GET("api/assignments/{assignmentId}/subtasks")
    suspend fun getSubtasks(
        @Path("assignmentId") assignmentId: String
    ): Response<List<SubtaskDto>>

    @POST("api/assignments/{assignmentId}/subtasks")
    suspend fun createSubtask(
        @Path("assignmentId") assignmentId: String,
        @Body request: SubtaskRequestDto
    ): Response<SubtaskDto>

    @PUT("api/subtasks/{id}")
    suspend fun updateSubtask(
        @Path("id") id: String,
        @Body request: SubtaskRequestDto
    ): Response<SubtaskDto>

    @DELETE("api/subtasks/{id}")
    suspend fun deleteSubtask(@Path("id") id: String): Response<Unit>

    // ---------------- Study Sessions ----------------
    @GET("api/study-sessions")
    suspend fun getStudySessions(): Response<List<StudySessionDto>>

    @POST("api/study-sessions")
    suspend fun createStudySession(
        @Body request: StudySessionRequestDto
    ): Response<StudySessionDto>

    @DELETE("api/study-sessions/{id}")
    suspend fun deleteStudySession(@Path("id") id: String): Response<Unit>

    // ---------------- Notes ----------------
    @GET("api/notes")
    suspend fun getNotes(): Response<List<NoteDto>>

    @POST("api/notes")
    suspend fun createNote(@Body request: NoteRequestDto): Response<NoteDto>

    @PUT("api/notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body request: NoteRequestDto
    ): Response<NoteDto>

@DELETE("api/notes/{id}")
suspend fun deleteNote(@Path("id") id: String): Response<Unit>
    
    // ---------------- Sync ----------------
    @POST("api/sync")
    suspend fun sync(@Body request: SyncRequestDto): Response<SyncResponseDto>
}
