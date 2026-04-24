package et.ahri.telederm

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import et.ahri.telederm.data.AuditLog
import et.ahri.telederm.data.PatientCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseManager = FirebaseManager
    private val contentResolver: ContentResolver = application.contentResolver

    private val _allCases = MutableStateFlow<List<PatientCase>>(emptyList())
    val allCases: StateFlow<List<PatientCase>> = _allCases

    init {
        observeCases()
    }

    private fun observeCases() {
        viewModelScope.launch {
            firebaseManager.getAllCases().collectLatest {
                _allCases.value = it
            }
        }
    }

    private suspend fun logAction(email: String, action: String, details: String) {
        firebaseManager.logAction(AuditLog(userEmail = email, action = action, details = details))
    }

    private fun mapError(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return if (e is java.io.IOException || 
            msg.contains("network") || 
            msg.contains("connection") || 
            msg.contains("unavailable") ||
            msg.contains("timeout") ||
            e.javaClass.simpleName.contains("Network")) {
            "Connection error"
        } else {
            e.message ?: "Action failed"
        }
    }

    fun submitCase(userEmail: String, patientCase: PatientCase, imageUris: List<Uri>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("PatientViewModel", "Starting case submission for ${patientCase.patientId}")
                
                // Upload images sequentially to Cloudinary
                val downloadUrls = mutableListOf<String>()
                for (uri in imageUris) {
                    Log.d("PatientViewModel", "Uploading image: $uri")
                    try {
                        val url = firebaseManager.uploadImage(contentResolver, uri)
                        downloadUrls.add(url)
                    } catch (e: Exception) {
                        Log.e("PatientViewModel", "Failed to upload image: $uri", e)
                        onResult(false, mapError(e))
                        return@launch
                    }
                }
                
                // Update patientCase with the Cloudinary secure URLs
                val finalCase = patientCase.copy(images = downloadUrls.joinToString(","))
                
                Log.d("PatientViewModel", "Saving case data to Firestore...")
                firebaseManager.submitCase(finalCase)
                
                logAction(userEmail, "SUBMIT_CASE", "Submitted case #${patientCase.patientId}")

                NotificationHelper.showNotification(
                    getApplication(),
                    "New Case Submitted",
                    "A new patient case (#${patientCase.patientId}) has been submitted for review."
                )
                Log.d("PatientViewModel", "Case submitted successfully")
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("PatientViewModel", "Submission failed completely", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun updateCaseReview(
        userEmail: String,
        caseDocId: String,
        patientId: String,
        diagnosis: String,
        diagnosisOther: String?,
        differentialDiagnosis: String,
        certainty: String,
        labConfirmationNeeded: Boolean,
        labTests: String,
        treatmentType: String,
        treatmentTypeOther: String?,
        dosageDuration: String,
        dosageDurationOther: String?,
        followUpInterval: String,
        isReferral: Boolean,
        referralReason: String,
        feedback: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val status = if (isReferral || treatmentType == "Referral for specialist care") "Referred" else "Reviewed"
                val updates = mutableMapOf<String, Any>(
                    "diagnosis" to diagnosis,
                    "diagnosisOther" to (diagnosisOther ?: ""),
                    "differentialDiagnosis" to differentialDiagnosis,
                    "certainty" to certainty,
                    "labConfirmationNeeded" to labConfirmationNeeded,
                    "labTests" to labTests,
                    "treatmentType" to treatmentType,
                    "treatmentTypeOther" to (treatmentTypeOther ?: ""),
                    "dosageDuration" to dosageDuration,
                    "dosageDurationOther" to (dosageDurationOther ?: ""),
                    "followUpInterval" to followUpInterval,
                    "isReferral" to isReferral,
                    "referralReason" to referralReason,
                    "feedback" to feedback,
                    "status" to status
                )
                
                firebaseManager.updateCase(caseDocId, updates)
                logAction(userEmail, "REVIEW_CASE", "Reviewed case #$patientId as $status")

                NotificationHelper.showNotification(
                    getApplication(),
                    "Case Reviewed",
                    "Case #$patientId has been $status by the dermatologist."
                )
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("PatientViewModel", "Update review failed", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun submitFollowUpUpdateExtended(
        userEmail: String,
        caseDocId: String,
        patientId: String,
        stage: String,
        outcome: String,
        outcomeOther: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val followUpData = mapOf(
                    "stage" to stage,
                    "outcome" to outcome,
                    "outcomeOther" to (outcomeOther ?: ""),
                    "timestamp" to System.currentTimeMillis().toString()
                )
                
                val updates = mapOf(
                    "followUpStage" to stage,
                    "treatmentOutcome" to outcome,
                    "treatmentOutcomeOther" to (outcomeOther ?: ""),
                    "isUpdatePending" to true,
                    "updateFeedback" to "", // Clear old feedback
                    "followUps.$stage" to followUpData // Store independently by stage
                )
                
                firebaseManager.updateCase(caseDocId, updates)
                logAction(userEmail, "FOLLOW_UP_UPDATE", "Submitted follow-up ($stage) for case #$patientId")

                NotificationHelper.showNotification(
                    getApplication(),
                    "Progress Update Received",
                    "A new follow-up update ($stage) has been submitted for Case #$patientId."
                )
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("PatientViewModel", "Follow-up update failed", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun updateFollowUpFeedbackExtended(
        userEmail: String,
        caseDocId: String,
        patientId: String,
        stage: String,
        feedback: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "updateFeedback" to feedback,
                    "isUpdatePending" to false,
                    "followUps.$stage.feedback" to feedback
                )
                
                firebaseManager.updateCase(caseDocId, updates)
                logAction(userEmail, "FOLLOW_UP_FEEDBACK", "Provided feedback for case #$patientId ($stage)")

                NotificationHelper.showNotification(
                    getApplication(),
                    "Follow-up Feedback",
                    "The dermatologist has provided feedback on the $stage update for Case #$patientId."
                )
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("PatientViewModel", "Follow-up feedback failed", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun getAllAuditLogs(): Flow<List<AuditLog>> {
        return firebaseManager.getAllLogs()
    }
}
