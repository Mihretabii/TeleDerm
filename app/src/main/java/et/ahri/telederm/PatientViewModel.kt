package et.ahri.telederm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import et.ahri.telederm.data.AppDatabase
import et.ahri.telederm.data.PatientCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PatientViewModel(application: Application) : AndroidViewModel(application) {
    private val patientCaseDao = AppDatabase.getDatabase(application).patientCaseDao()

    val allCases: Flow<List<PatientCase>> = patientCaseDao.getAllCases()

    fun submitCase(patientCase: PatientCase, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                patientCaseDao.insertCase(patientCase)

                // Notify about new case submission
                NotificationHelper.showNotification(
                    getApplication(),
                    "New Case Submitted",
                    "A new patient case (#${patientCase.patientId}) has been submitted for review."
                )
                
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateCaseReview(
        caseId: Int,
        diagnosis: String,
        differentialDiagnosis: String,
        certainty: String,
        labConfirmationNeeded: Boolean,
        labTests: String,
        treatmentType: String,
        dosageDuration: String,
        followUpInterval: String,
        isReferral: Boolean,
        referralReason: String,
        feedback: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existingCase = patientCaseDao.getCaseById(caseId)
            if (existingCase != null) {
                val status = if (isReferral || treatmentType == "Referral for specialist care") "Referred" else "Reviewed"
                val updatedCase = existingCase.copy(
                    diagnosis = diagnosis,
                    differentialDiagnosis = differentialDiagnosis,
                    certainty = certainty,
                    labConfirmationNeeded = labConfirmationNeeded,
                    labTests = labTests,
                    treatmentType = treatmentType,
                    dosageDuration = dosageDuration,
                    followUpInterval = followUpInterval,
                    isReferral = isReferral,
                    referralReason = referralReason,
                    feedback = feedback,
                    status = status
                )
                patientCaseDao.updateCase(updatedCase)

                // Notify health worker that case has been reviewed
                NotificationHelper.showNotification(
                    getApplication(),
                    "Case Reviewed",
                    "Case #${existingCase.patientId} has been $status by the dermatologist."
                )

                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun submitFollowUpUpdate(
        caseId: Int,
        stage: String,
        outcome: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existingCase = patientCaseDao.getCaseById(caseId)
            if (existingCase != null) {
                val updatedCase = existingCase.copy(
                    followUpStage = stage,
                    treatmentOutcome = outcome,
                    isUpdatePending = true,
                    updateFeedback = null // Clear old feedback for the new update
                )
                patientCaseDao.updateCase(updatedCase)

                // Notify dermatologist about new progress update
                NotificationHelper.showNotification(
                    getApplication(),
                    "Progress Update Received",
                    "A new follow-up update ($stage) has been submitted for Case #${existingCase.patientId}."
                )

                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateFollowUpFeedback(
        caseId: Int,
        feedback: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existingCase = patientCaseDao.getCaseById(caseId)
            if (existingCase != null) {
                val updatedCase = existingCase.copy(
                    updateFeedback = feedback,
                    isUpdatePending = false
                )
                patientCaseDao.updateCase(updatedCase)

                // Notify health worker about follow-up feedback
                NotificationHelper.showNotification(
                    getApplication(),
                    "Follow-up Feedback",
                    "The dermatologist has provided feedback on the latest update for Case #${existingCase.patientId}."
                )

                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}
