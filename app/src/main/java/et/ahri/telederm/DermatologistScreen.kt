package et.ahri.telederm

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import et.ahri.telederm.data.PatientCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DermatologistScreen(
    authViewModel: AuthViewModel,
    patientViewModel: PatientViewModel,
    onLogout: () -> Unit
) {
    val cases by patientViewModel.allCases.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Cases", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👨‍⚕️ Dermatologist Portal") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> MyCasesTab(patientViewModel, authViewModel, cases)
                1 -> UserSettingsTab(authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCasesTab(viewModel: PatientViewModel, authViewModel: AuthViewModel, cases: List<PatientCase>) {
    var filterStatus by remember { mutableStateOf("All") }
    var selectedCase by remember { mutableStateOf<PatientCase?>(null) }
    var startInReviewMode by remember { mutableStateOf(false) }
    val currentUser by authViewModel.currentUser.collectAsState()

    val filteredCases = when (filterStatus) {
        "Pending" -> cases.filter { it.status == "Pending" || it.isUpdatePending }
        "Reviewed" -> cases.filter { it.status == "Reviewed" }
        "Referred" -> cases.filter { it.status == "Referred" }
        else -> cases
    }

    if (selectedCase != null) {
        ReviewCaseDialog(
            viewModel = viewModel,
            userEmail = currentUser?.email ?: "unknown",
            patientCase = selectedCase!!,
            onDismiss = { 
                selectedCase = null
                startInReviewMode = false
            },
            initialReviewMode = startInReviewMode
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusOptions = listOf("All", "Pending", "Reviewed", "Referred")
            statusOptions.forEach { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { filterStatus = status },
                    label = { Text(status) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (filteredCases.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No $filterStatus cases found.", color = Color.Gray)
                }
            } else {
                filteredCases.forEach { patientCase ->
                    val isCasePending = patientCase.status == "Pending"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { 
                            selectedCase = patientCase 
                            startInReviewMode = isCasePending 
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("Patient ID: ${patientCase.patientId}") },
                            supportingContent = { Text("Visit: ${patientCase.visitDate} | Age: ${patientCase.age} ${patientCase.ageUnit}") },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Badge(containerColor = when {
                                        patientCase.isUpdatePending -> Color(0xFF3B82F6)
                                        patientCase.status == "Reviewed" -> Color(0xFF22C55E)
                                        patientCase.status == "Referred" -> Color(0xFFEF4444)
                                        else -> Color(0xFFF59E0B)
                                    }) {
                                        Text(if (patientCase.isUpdatePending) "Update Pending" else patientCase.status, color = Color.White)
                                    }
                                    
                                    TextButton(
                                        onClick = {
                                            selectedCase = patientCase
                                            startInReviewMode = isCasePending
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (isCasePending) "Review" else "View Details", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewCaseDialog(
    viewModel: PatientViewModel,
    userEmail: String,
    patientCase: PatientCase,
    onDismiss: () -> Unit,
    initialReviewMode: Boolean = false
) {
    val context = LocalContext.current
    var isReviewing by remember { mutableStateOf(initialReviewMode) }
    
    var diagnosis by remember { mutableStateOf(patientCase.diagnosis ?: "") }
    var diagnosisOther by remember { mutableStateOf(patientCase.diagnosisOther ?: "") }
    
    var differentialDiagnosis by remember { mutableStateOf(patientCase.differentialDiagnosis ?: "") }
    var certainty by remember { mutableStateOf(patientCase.certainty ?: "Low") }
    
    var labNeeded by remember { mutableStateOf(patientCase.labConfirmationNeeded) }
    
    val labOptions = listOf("SSS (Skin Smear)", "Biopsy", "Fungal Culture", "RPR/VDRL", "Other")
    var selectedLabs by remember { mutableStateOf(setOf<String>()) }
    var otherLabText by remember { mutableStateOf("") }
    
    var treatmentType by remember { mutableStateOf(patientCase.treatmentType ?: "") }
    var treatmentTypeOther by remember { mutableStateOf(patientCase.treatmentTypeOther ?: "") }
    
    var dosageDuration by remember { mutableStateOf(patientCase.dosageDuration ?: "") }
    var dosageDurationOther by remember { mutableStateOf(patientCase.dosageDurationOther ?: "") }
    
    var followUpInterval by remember { mutableStateOf(patientCase.followUpInterval ?: "") }
    
    var referralNeeded by remember { mutableStateOf(patientCase.status == "Referred") }
    var referralReason by remember { mutableStateOf(patientCase.referralReason ?: "") }
    var feedback by remember { mutableStateOf(patientCase.feedback ?: "") }
    
    var updateFeedback by remember { mutableStateOf("") }
    var enlargedImageUri by remember { mutableStateOf<String?>(null) }
    
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Error") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                Button(onClick = { errorDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (enlargedImageUri != null) {
        EnlargedImageDialog(imageUrl = enlargedImageUri!!, onDismiss = { enlargedImageUri = null })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Case: #${patientCase.patientId}")
                if (!isReviewing && patientCase.status != "Pending") {
                    IconButton(onClick = { isReviewing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Review", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("🏥 Facility Information", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ResponseItem("Facility", "${patientCase.facility} (${patientCase.facilityType})")
                ResponseItem("HW Type", patientCase.healthWorkerType)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("👤 Patient Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ResponseItem("Patient", "${patientCase.age} ${patientCase.ageUnit}, ${patientCase.sex}")
                ResponseItem("Location", patientCase.residence)
                ResponseItem("Region/Zone", "${patientCase.region} / ${patientCase.zone}")
                ResponseItem("Woreda/Kebele", "${patientCase.woreda} / ${patientCase.kebele}")
                ResponseItem("Phone", patientCase.phoneNumber)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔍 Clinical History", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ResponseItem("Symptoms", patientCase.associatedSymptoms)
                ResponseItem("Exposure", patientCase.exposureHistory)
                ResponseItem("Lesion Type", if (patientCase.lesionType == "Other") "Other: ${patientCase.lesionTypeOther ?: ""}" else patientCase.lesionType)
                ResponseItem("Lesion Count", patientCase.numLesions)
                ResponseItem("Lesion Location", if (patientCase.lesionLocation.contains("Other")) "${patientCase.lesionLocation} (${patientCase.lesionLocationOther ?: ""})" else patientCase.lesionLocation)
                ResponseItem("Lesion Size", "${patientCase.lesionSize} cm")
                ResponseItem("Duration", patientCase.lesionDuration)
                ResponseItem("Itching/Pain", patientCase.painLevel)
                ResponseItem("Prev Treatment", if (patientCase.prevTreatment == "Other") "Other: ${patientCase.prevTreatmentOther ?: ""}" else patientCase.prevTreatment)
                ResponseItem("Comorbidities", patientCase.comorbidities)
                ResponseItem("Notes", patientCase.additionalNotes)

                Spacer(modifier = Modifier.height(8.dp))
                Text("🖼️ Images (Click to enlarge)", fontWeight = FontWeight.Bold)
                if (patientCase.images.isNotEmpty()) {
                    val imageUrls = patientCase.images.split(",")
                    LazyRow(modifier = Modifier.height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUrls) { url ->
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { enlargedImageUri = url }
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                if (!isReviewing) {
                    Text("🩺 Review Summary (Read-Only)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ResponseItem("Diagnosis", if (diagnosis == "Other") "Other: ${patientCase.diagnosisOther ?: ""}" else diagnosis)
                    ResponseItem("Diff. Diagnosis", differentialDiagnosis.takeIf { it.isNotBlank() } ?: "N/A")
                    ResponseItem("Certainty", certainty)
                    ResponseItem("Lab Needed", if(labNeeded) "Yes: ${patientCase.labTests ?: ""}" else "No")
                    ResponseItem("Treatment", if (treatmentType == "Other") "Other: ${patientCase.treatmentTypeOther ?: ""}" else treatmentType)
                    ResponseItem("Dosage", if (dosageDuration == "Other") "Other: ${patientCase.dosageDurationOther ?: ""}" else dosageDuration.takeIf { it.isNotBlank() } ?: "N/A")
                    ResponseItem("Follow-up", "$followUpInterval days")
                    ResponseItem("Referral", if(referralNeeded) "Yes: ${patientCase.referralReason ?: ""}" else "No")
                    ResponseItem("Feedback", feedback.takeIf { it.isNotBlank() } ?: "N/A")
                } else {
                    Text("🩺 Clinical Decision (Editing Mode)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    val diagnosisOptions = listOf("Cutaneous Leishmaniasis (CL)", "Eczema", "Psoriasis", "Fungal Infection", "Bacterial Infection", "Viral Infection", "Scabies", "Other")
                    DropdownField("Primary Diagnosis Choice *", diagnosisOptions, diagnosis, { 
                        diagnosis = it 
                        if (it != "Other") diagnosisOther = ""
                    })

                    if (diagnosis == "Other") {
                        OutlinedTextField(
                            value = diagnosisOther,
                            onValueChange = { diagnosisOther = it },
                            label = { Text("Specify Other Diagnosis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = differentialDiagnosis, 
                        onValueChange = { differentialDiagnosis = it }, 
                        label = { Text("Differential Diagnoses") },
                        placeholder = { Text("e.g., Sporotrichosis, Wart") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    DropdownField("Diagnostic Certainty *", listOf("Low", "Moderate", "High"), certainty, { certainty = it })
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Lab Confirmation Needed", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = labNeeded, onClick = { labNeeded = true })
                        Text("Yes", modifier = Modifier.clickable { labNeeded = true })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !labNeeded, onClick = { labNeeded = false })
                        Text("No", modifier = Modifier.clickable { labNeeded = false })
                    }
                    
                    if (labNeeded) {
                        Text("Select Lab Tests", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth()) {
                            labOptions.forEach { lab ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                    Checkbox(
                                        checked = selectedLabs.contains(lab),
                                        onCheckedChange = { checked ->
                                            selectedLabs = if (checked) selectedLabs + lab else selectedLabs - lab
                                        }
                                    )
                                    Text(lab, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (selectedLabs.contains("Other")) {
                            OutlinedTextField(
                                value = otherLabText,
                                onValueChange = { otherLabText = it },
                                label = { Text("Specify Other Lab Test") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val treatmentOptions = listOf("Topical (cream/ointment)", "Systemic (oral medication)", "Intralesional Injection", "Cryotherapy (freezing)", "Combination (Topical and Systemic)", "Referral for Specialist Care", "Other")
                    DropdownField("Treatment Type *", treatmentOptions, treatmentType, { 
                        treatmentType = it 
                        if (it != "Other") treatmentTypeOther = ""
                    })
                    
                    if (treatmentType == "Other") {
                        OutlinedTextField(
                            value = treatmentTypeOther,
                            onValueChange = { treatmentTypeOther = it },
                            label = { Text("Specify Other Treatment") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val dosageOptions = listOf("Sodium stibogluconate (SSG) 20 mg/kg IM/IV for 20 days", "Meglumine antimoniate (MA) 20 mg/kg IM/IV for 20 days", "Intralesional SSG (1–3 mL per lesion) every 3–7 days", "Paromomycin ointment 15% twice daily for 20 days", "Fluconazole 200 mg daily for 6 weeks", "Cryotherapy with liquid nitrogen (1–2 cycles) weekly", "Other")
                    DropdownField("Dosage & Duration *", dosageOptions, dosageDuration, {
                        dosageDuration = it
                        if (it != "Other") dosageDurationOther = ""
                    })

                    if (dosageDuration == "Other") {
                        OutlinedTextField(
                            value = dosageDurationOther,
                            onValueChange = { dosageDurationOther = it },
                            label = { Text("Specify Other Dosage & Duration") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    val intervalOptions = listOf("30", "60", "90", "180", "None")
                    DropdownField("Follow-up Interval (Days)", intervalOptions, followUpInterval, { followUpInterval = it })

                    Text("Referral Needed?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = referralNeeded, onClick = { referralNeeded = true })
                        Text("Yes", modifier = Modifier.clickable { referralNeeded = true })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !referralNeeded, onClick = { referralNeeded = false })
                        Text("No", modifier = Modifier.clickable { referralNeeded = false })
                    }
                    
                    if (referralNeeded) {
                        OutlinedTextField(value = referralReason, onValueChange = { referralReason = it }, label = { Text("Reason for Referral *") }, modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("Feedback to Health Worker") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (patientCase.status != "Pending") {
                             OutlinedButton(onClick = { isReviewing = false }, modifier = Modifier.weight(1f)) {
                                 Text("Cancel")
                             }
                        }
                        Button(onClick = {
                            val finalDiagnosis = if (diagnosis == "Other") diagnosisOther else diagnosis
                            if (finalDiagnosis.isNotBlank()) {
                                val finalTreatment = if (treatmentType == "Other") treatmentTypeOther else treatmentType
                                val finalDosage = if (dosageDuration == "Other") dosageDurationOther else dosageDuration
                                val finalLabs = if (labNeeded) {
                                    val labs = selectedLabs.filter { it != "Other" }.toMutableList()
                                    if (selectedLabs.contains("Other")) labs.add(otherLabText)
                                    labs.joinToString(", ")
                                } else ""

                                viewModel.updateCaseReview(
                                    userEmail, patientCase.docId, patientCase.patientId, 
                                    diagnosis, if(diagnosis == "Other") diagnosisOther else null,
                                    differentialDiagnosis, certainty, labNeeded, finalLabs, 
                                    treatmentType, if(treatmentType == "Other") treatmentTypeOther else null,
                                    dosageDuration, if(dosageDuration == "Other") dosageDurationOther else null,
                                    followUpInterval, referralNeeded, referralReason, feedback
                                ) { success, error ->
                                    if (success) isReviewing = false
                                    else errorDialogMessage = error ?: "Review update failed"
                                }
                            } else {
                                Toast.makeText(context, "Diagnosis is required", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.weight(1f)) { 
                            Text("Submit Review") 
                        }
                    }
                }

                // Display all independent follow-ups
                if (patientCase.followUps != null && patientCase.followUps!!.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("📈 Follow-up History", fontWeight = FontWeight.Bold, color = Color(0xFF208090))
                    patientCase.followUps!!.toSortedMap().forEach { (stage, data) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Stage: $stage", fontWeight = FontWeight.Bold)
                                ResponseItem("Treatment Outcome", if(data["outcome"] == "Other") "Other: ${data["outcomeOther"] ?: ""}" else data["outcome"] ?: "")
                                ResponseItem("Dermatologist Feedback", data["feedback"] ?: "Pending review...")
                            }
                        }
                    }
                }

                if (patientCase.isUpdatePending) {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("🔔 Provide Feedback for Latest Update", fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    ResponseItem("Current stage", patientCase.followUpStage ?: "N/A")
                    ResponseItem("Reported outcome", if(patientCase.treatmentOutcome == "Other") "Other: ${patientCase.treatmentOutcomeOther ?: ""}" else patientCase.treatmentOutcome ?: "N/A")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = updateFeedback,
                        onValueChange = { updateFeedback = it },
                        label = { Text("Dermatologist Feedback for Update") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (updateFeedback.isNotBlank()) {
                                viewModel.updateFollowUpFeedbackExtended(userEmail, patientCase.docId, patientCase.patientId, patientCase.followUpStage ?: "Unknown", updateFeedback) { success, error ->
                                    if (success) onDismiss()
                                    else errorDialogMessage = error ?: "Failed to send feedback"
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    ) { Text("Submit Follow-up Feedback") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
