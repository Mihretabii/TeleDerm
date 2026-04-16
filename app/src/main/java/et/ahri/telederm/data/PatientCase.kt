package et.ahri.telederm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_cases")
data class PatientCase(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "patientId") val patientId: String,
    @ColumnInfo(name = "visitDate") val visitDate: String,
    @ColumnInfo(name = "age") val age: String,
    @ColumnInfo(name = "ageUnit") val ageUnit: String = "Years",
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "healthWorkerType") val healthWorkerType: String = "",
    @ColumnInfo(name = "facility") val facility: String,
    @ColumnInfo(name = "facilityType") val facilityType: String = "",
    @ColumnInfo(name = "residence") val residence: String,
    @ColumnInfo(name = "federal") val federal: String = "Ethiopia",
    @ColumnInfo(name = "region") val region: String = "",
    @ColumnInfo(name = "zone") val zone: String = "",
    @ColumnInfo(name = "woreda") val woreda: String = "",
    @ColumnInfo(name = "kebele") val kebele: String = "",
    @ColumnInfo(name = "phoneNumber") val phoneNumber: String,
    @ColumnInfo(name = "lesionDuration") val lesionDuration: String,
    @ColumnInfo(name = "exposureHistory") val exposureHistory: String,
    @ColumnInfo(name = "lesionType") val lesionType: String,
    @ColumnInfo(name = "numLesions") val numLesions: String,
    @ColumnInfo(name = "lesionLocation") val lesionLocation: String,
    @ColumnInfo(name = "lesionSize") val lesionSize: String,
    @ColumnInfo(name = "painLevel") val painLevel: String,
    @ColumnInfo(name = "prevTreatment") val prevTreatment: String,
    @ColumnInfo(name = "associatedSymptoms") val associatedSymptoms: String,
    @ColumnInfo(name = "comorbidities") val comorbidities: String,
    @ColumnInfo(name = "additionalNotes") val additionalNotes: String,
    @ColumnInfo(name = "images") val images: String = "",
    @ColumnInfo(name = "status") val status: String = "Pending",

    // Review Fields
    @ColumnInfo(name = "diagnosis") val diagnosis: String? = null,
    @ColumnInfo(name = "differentialDiagnosis") val differentialDiagnosis: String? = null,
    @ColumnInfo(name = "certainty") val certainty: String? = null,
    @ColumnInfo(name = "labConfirmationNeeded") val labConfirmationNeeded: Boolean = false,
    @ColumnInfo(name = "labTests") val labTests: String? = null,
    @ColumnInfo(name = "treatmentType") val treatmentType: String? = null,
    @ColumnInfo(name = "dosageDuration") val dosageDuration: String? = null,
    @ColumnInfo(name = "followUpInterval") val followUpInterval: String? = null,
    @ColumnInfo(name = "isReferral") val isReferral: Boolean = false,
    @ColumnInfo(name = "referralReason") val referralReason: String? = null,
    @ColumnInfo(name = "feedback") val feedback: String? = null,

    // Follow-up Update Fields
    @ColumnInfo(name = "followUpStage") val followUpStage: String? = null,
    @ColumnInfo(name = "treatmentOutcome") val treatmentOutcome: String? = null,
    @ColumnInfo(name = "updateFeedback") val updateFeedback: String? = null,
    @ColumnInfo(name = "isUpdatePending") val isUpdatePending: Boolean = false
)
