package et.ahri.telederm.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_cases")
data class PatientCase(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id") 
    @get:Exclude @set:Exclude
    var id: Int = 0,

    @get:Exclude @set:Exclude 
    var docId: String = "", // Firebase document ID

    @ColumnInfo(name = "patientId") 
    @get:PropertyName("patientId") @set:PropertyName("patientId")
    var patientId: String = "",

    @ColumnInfo(name = "visitDate") 
    @get:PropertyName("visitDate") @set:PropertyName("visitDate")
    var visitDate: String = "",

    @ColumnInfo(name = "age") 
    @get:PropertyName("age") @set:PropertyName("age")
    var age: String = "",

    @ColumnInfo(name = "ageUnit") 
    @get:PropertyName("ageUnit") @set:PropertyName("ageUnit")
    var ageUnit: String = "Years",

    @ColumnInfo(name = "sex") 
    @get:PropertyName("sex") @set:PropertyName("sex")
    var sex: String = "",

    @ColumnInfo(name = "healthWorkerType") 
    @get:PropertyName("healthWorkerType") @set:PropertyName("healthWorkerType")
    var healthWorkerType: String = "",

    @ColumnInfo(name = "facility") 
    @get:PropertyName("facility") @set:PropertyName("facility")
    var facility: String = "",

    @ColumnInfo(name = "facilityType") 
    @get:PropertyName("facilityType") @set:PropertyName("facilityType")
    var facilityType: String = "",

    @ColumnInfo(name = "residence") 
    @get:PropertyName("residence") @set:PropertyName("residence")
    var residence: String = "",

    @ColumnInfo(name = "federal") 
    @get:PropertyName("federal") @set:PropertyName("federal")
    var federal: String = "Ethiopia",

    @ColumnInfo(name = "region") 
    @get:PropertyName("region") @set:PropertyName("region")
    var region: String = "",

    @ColumnInfo(name = "zone") 
    @get:PropertyName("zone") @set:PropertyName("zone")
    var zone: String = "",

    @ColumnInfo(name = "woreda") 
    @get:PropertyName("woreda") @set:PropertyName("woreda")
    var woreda: String = "",

    @ColumnInfo(name = "kebele") 
    @get:PropertyName("kebele") @set:PropertyName("kebele")
    var kebele: String = "",

    @ColumnInfo(name = "phoneNumber") 
    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @ColumnInfo(name = "lesionDuration") 
    @get:PropertyName("lesionDuration") @set:PropertyName("lesionDuration")
    var lesionDuration: String = "",

    @ColumnInfo(name = "exposureHistory") 
    @get:PropertyName("exposureHistory") @set:PropertyName("exposureHistory")
    var exposureHistory: String = "",

    @ColumnInfo(name = "lesionType") 
    @get:PropertyName("lesionType") @set:PropertyName("lesionType")
    var lesionType: String = "",
    
    @ColumnInfo(name = "lesionTypeOther")
    @get:PropertyName("lesionTypeOther") @set:PropertyName("lesionTypeOther")
    var lesionTypeOther: String? = null,

    @ColumnInfo(name = "numLesions") 
    @get:PropertyName("numLesions") @set:PropertyName("numLesions")
    var numLesions: String = "",

    @ColumnInfo(name = "lesionLocation") 
    @get:PropertyName("lesionLocation") @set:PropertyName("lesionLocation")
    var lesionLocation: String = "",
    
    @ColumnInfo(name = "lesionLocationOther")
    @get:PropertyName("lesionLocationOther") @set:PropertyName("lesionLocationOther")
    var lesionLocationOther: String? = null,

    @ColumnInfo(name = "lesionSize") 
    @get:PropertyName("lesionSize") @set:PropertyName("lesionSize")
    var lesionSize: String = "",

    @ColumnInfo(name = "painLevel") 
    @get:PropertyName("painLevel") @set:PropertyName("painLevel")
    var painLevel: String = "",

    @ColumnInfo(name = "prevTreatment") 
    @get:PropertyName("prevTreatment") @set:PropertyName("prevTreatment")
    var prevTreatment: String = "",
    
    @ColumnInfo(name = "prevTreatmentOther")
    @get:PropertyName("prevTreatmentOther") @set:PropertyName("prevTreatmentOther")
    var prevTreatmentOther: String? = null,

    @ColumnInfo(name = "associatedSymptoms") 
    @get:PropertyName("associatedSymptoms") @set:PropertyName("associatedSymptoms")
    var associatedSymptoms: String = "",

    @ColumnInfo(name = "comorbidities") 
    @get:PropertyName("comorbidities") @set:PropertyName("comorbidities")
    var comorbidities: String = "" ,

    @ColumnInfo(name = "additionalNotes") 
    @get:PropertyName("additionalNotes") @set:PropertyName("additionalNotes")
    var additionalNotes: String = "",

    @ColumnInfo(name = "images") 
    @get:PropertyName("images") @set:PropertyName("images")
    var images: String = "",

    @ColumnInfo(name = "status") 
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "Pending",

    // Review Fields
    @ColumnInfo(name = "diagnosis") 
    @get:PropertyName("diagnosis") @set:PropertyName("diagnosis")
    var diagnosis: String? = null,
    
    @ColumnInfo(name = "diagnosisOther")
    @get:PropertyName("diagnosisOther") @set:PropertyName("diagnosisOther")
    var diagnosisOther: String? = null,

    @ColumnInfo(name = "differentialDiagnosis") 
    @get:PropertyName("differentialDiagnosis") @set:PropertyName("differentialDiagnosis")
    var differentialDiagnosis: String? = null,

    @ColumnInfo(name = "certainty") 
    @get:PropertyName("certainty") @set:PropertyName("certainty")
    var certainty: String? = null,

    @ColumnInfo(name = "labConfirmationNeeded") 
    @get:PropertyName("labConfirmationNeeded") @set:PropertyName("labConfirmationNeeded")
    var labConfirmationNeeded: Boolean = false,

    @ColumnInfo(name = "labTests") 
    @get:PropertyName("labTests") @set:PropertyName("labTests")
    var labTests: String? = null,

    @ColumnInfo(name = "treatmentType") 
    @get:PropertyName("treatmentType") @set:PropertyName("treatmentType")
    var treatmentType: String? = null,
    
    @ColumnInfo(name = "treatmentTypeOther")
    @get:PropertyName("treatmentTypeOther") @set:PropertyName("treatmentTypeOther")
    var treatmentTypeOther: String? = null,

    @ColumnInfo(name = "dosageDuration") 
    @get:PropertyName("dosageDuration") @set:PropertyName("dosageDuration")
    var dosageDuration: String? = null,
    
    @ColumnInfo(name = "dosageDurationOther")
    @get:PropertyName("dosageDurationOther") @set:PropertyName("dosageDurationOther")
    var dosageDurationOther: String? = null,

    @ColumnInfo(name = "followUpInterval") 
    @get:PropertyName("followUpInterval") @set:PropertyName("followUpInterval")
    var followUpInterval: String? = null,

    @ColumnInfo(name = "isReferral") 
    @get:PropertyName("isReferral") @set:PropertyName("isReferral")
    var isReferral: Boolean = false,

    @ColumnInfo(name = "referralReason") 
    @get:PropertyName("referralReason") @set:PropertyName("referralReason")
    var referralReason: String? = null,

    @ColumnInfo(name = "feedback") 
    @get:PropertyName("feedback") @set:PropertyName("feedback")
    var feedback: String? = null,

    // Follow-up Update Fields (Latest)
    @ColumnInfo(name = "followUpStage") 
    @get:PropertyName("followUpStage") @set:PropertyName("followUpStage")
    var followUpStage: String? = null,

    @ColumnInfo(name = "treatmentOutcome") 
    @get:PropertyName("treatmentOutcome") @set:PropertyName("treatmentOutcome")
    var treatmentOutcome: String? = null,
    
    @ColumnInfo(name = "treatmentOutcomeOther")
    @get:PropertyName("treatmentOutcomeOther") @set:PropertyName("treatmentOutcomeOther")
    var treatmentOutcomeOther: String? = null,

    @ColumnInfo(name = "updateFeedback") 
    @get:PropertyName("updateFeedback") @set:PropertyName("updateFeedback")
    var updateFeedback: String? = null,

    @ColumnInfo(name = "isUpdatePending") 
    @get:PropertyName("isUpdatePending") @set:PropertyName("isUpdatePending")
    var isUpdatePending: Boolean = false,
    
    // Independent follow-up storage
    @get:PropertyName("followUps") @set:PropertyName("followUps")
    var followUps: Map<String, Map<String, String>>? = null
)
