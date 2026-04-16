package et.ahri.telederm

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var selectedCase by remember { mutableStateOf<PatientCase?>(null) }
    var isReviewing by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("All") }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Review Cases", "Settings")

    if (selectedCase != null) {
        if (isReviewing) {
            ReviewForm(
                patientCase = selectedCase!!,
                onBack = { isReviewing = false },
                onSubmit = { diag, diff, cert, labNeeded, tests, treat, dose, follow, refer, reason, feed ->
                    patientViewModel.updateCaseReview(
                        selectedCase!!.id,
                        diag,
                        diff,
                        cert,
                        labNeeded,
                        tests,
                        treat,
                        dose,
                        follow,
                        refer,
                        reason,
                        feed
                    ) { success: Boolean ->
                        if (success) {
                            isReviewing = false
                            selectedCase = null
                        }
                    }
                }
            )
        } else {
            CaseDetailView(
                viewModel = patientViewModel,
                patientCase = selectedCase!!,
                onBack = { selectedCase = null },
                onProceedToReview = { isReviewing = true }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("👨‍⚕️ Dermatologist Review Portal", style = MaterialTheme.typography.titleMedium)
                            Text("Review submitted cases and provide diagnostic recommendations", style = MaterialTheme.typography.labelSmall)
                        }
                    },
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
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Patient Cases",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF208090)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
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

                            val filteredCases =
                                if (filterStatus == "All") cases else cases.filter { it.status == filterStatus }

                            if (filteredCases.isEmpty()) {
                                Text(
                                    "No cases found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            } else {
                                filteredCases.forEach { patientCase ->
                                    CaseItem(patientCase) {
                                        selectedCase = patientCase
                                        isReviewing = false
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    1 -> UserSettingsTab(authViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseItem(patientCase: PatientCase, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Case #${patientCase.patientId}", style = MaterialTheme.typography.titleMedium)
                Badge(containerColor = when(patientCase.status) {
                    "Reviewed" -> Color(0xFF22C55E)
                    "Referred" -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }) {
                    Text(patientCase.status, color = Color.White)
                }
            }
            Text(
                "Patient: ${patientCase.age} ${patientCase.ageUnit} | ${
                    patientCase.sex.take(1).uppercase()
                }"
            )
            Text("Location: ${patientCase.lesionLocation}")
            Text("Submitted: ${patientCase.visitDate}")

            if (patientCase.isUpdatePending) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "🔔 Progress Update Pending",
                    color = Color(0xFF208090),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (patientCase.status == "Pending" || patientCase.isUpdatePending) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClick) {
                    Text(if (patientCase.isUpdatePending) "View Progress" else "Review")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailView(
    viewModel: PatientViewModel,
    patientCase: PatientCase,
    onBack: () -> Unit,
    onProceedToReview: () -> Unit
) {
    val context = LocalContext.current
    var followUpFeedback by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Case Details: #${patientCase.patientId}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("👤 Patient Information")
            InfoItem("Patient ID", patientCase.patientId)
            InfoItem("Age", "${patientCase.age} ${patientCase.ageUnit}")
            InfoItem("Sex", patientCase.sex)
            InfoItem("Health Worker", patientCase.healthWorkerType)
            InfoItem("Facility", "${patientCase.facility} (${patientCase.facilityType})")
            InfoItem("Residence", patientCase.residence)
            InfoItem("Contact Phone", patientCase.phoneNumber)
            InfoItem("Date of Visit", patientCase.visitDate)
            
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("🔍 Clinical Information")
            InfoItem("Dermatologic Symptoms", patientCase.associatedSymptoms)
            InfoItem("Duration of Lesion", patientCase.lesionDuration)
            InfoItem("Exposure History", patientCase.exposureHistory)
            InfoItem("Lesion Type", patientCase.lesionType)
            InfoItem("Number of Lesions", patientCase.numLesions)
            InfoItem("Lesion Location", patientCase.lesionLocation)
            InfoItem("Index Lesion Size (cm)", patientCase.lesionSize)
            InfoItem("Itching or Pain", patientCase.painLevel)
            InfoItem("Previous Treatment", patientCase.prevTreatment)
            InfoItem("Co-morbidities", patientCase.comorbidities)
            InfoItem("Additional Notes", patientCase.additionalNotes)
            
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("📸 Lesion Images")
            if (patientCase.images.isNotEmpty()) {
                val imageUrls = patientCase.images.split(",")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageUrls) { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = null,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Text("No images provided.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            if (patientCase.status != "Pending") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("📋 Dermatologist Review")
                InfoItem("Primary Diagnosis", patientCase.diagnosis ?: "N/A")
                InfoItem("Differential Diagnoses", patientCase.differentialDiagnosis ?: "N/A")
                InfoItem("Diagnostic Certainty", patientCase.certainty ?: "N/A")
                InfoItem("Lab Needed", if (patientCase.labConfirmationNeeded) "Yes" else "No")
                if (patientCase.labConfirmationNeeded) InfoItem(
                    "Lab Tests",
                    patientCase.labTests ?: "N/A"
                )
                InfoItem("Recommended Treatment", patientCase.treatmentType ?: "N/A")
                InfoItem("Dosage and Duration", patientCase.dosageDuration ?: "N/A")
                InfoItem("Follow-up Interval", "${patientCase.followUpInterval} days")
                InfoItem("Referral Needed", if (patientCase.isReferral) "Yes" else "No")
                if (patientCase.isReferral) InfoItem(
                    "Referral Reason",
                    patientCase.referralReason ?: "N/A"
                )
                InfoItem("Feedback", patientCase.feedback ?: "N/A")
            }

            if (patientCase.isUpdatePending) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("📈 Patient Progress Update")
                InfoItem("Update Stage", patientCase.followUpStage ?: "N/A")
                InfoItem("Treatment Outcome", patientCase.treatmentOutcome ?: "N/A")

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = followUpFeedback,
                    onValueChange = { followUpFeedback = it },
                    label = { Text("Progress Update Feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Button(
                    onClick = {
                        if (followUpFeedback.isNotBlank()) {
                            viewModel.updateFollowUpFeedback(
                                patientCase.id,
                                followUpFeedback
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, "Feedback sent!", Toast.LENGTH_SHORT)
                                        .show()
                                    onBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Send Progress Feedback")
                }
            } else if (patientCase.updateFeedback != null) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("📈 Follow-up Feedback Sent")
                InfoItem("Stage", patientCase.followUpStage ?: "N/A")
                InfoItem("Outcome", patientCase.treatmentOutcome ?: "N/A")
                InfoItem("Feedback", patientCase.updateFeedback)
            }

            Spacer(modifier = Modifier.height(32.dp))
            if (patientCase.status == "Pending") {
                Button(
                    onClick = onProceedToReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Review Case")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewForm(
    patientCase: PatientCase,
    onBack: () -> Unit,
    onSubmit: (String, String, String, Boolean, String, String, String, String, Boolean, String, String) -> Unit
) {
    val context = LocalContext.current
    var diagnosis by remember { mutableStateOf("") }
    var differentialDiagnosis by remember { mutableStateOf("") }
    var certainty by remember { mutableStateOf("") }
    var labConfirmationNeeded by remember { mutableStateOf(false) }

    val labTestOptions = listOf("SSS (Skin Smear)", "Biopsy", "Fungal Culture", "RPR/VDRL", "Other")
    var selectedLabTests by remember { mutableStateOf(setOf<String>()) }
    var otherLabTest by remember { mutableStateOf("") }

    var treatmentType by remember { mutableStateOf("") }
    var dosageDuration by remember { mutableStateOf("") }
    var selectedDosageTemplate by remember { mutableStateOf("") }
    var followUpInterval by remember { mutableStateOf("") }
    var referralNeeded by remember { mutableStateOf(false) }
    var referralReason by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    val diagnosisOptions = listOf(
        "Cutaneous Leishmaniasis (CL)",
        "Eczema",
        "Psoriasis",
        "Fungal Infection",
        "Bacterial Infection",
        "Viral Infection",
        "Scabies",
        "Other"
    )

    val dosageOptions = listOf(
        "Sodium stibogluconate (SSG) 20 mg/kg IM/IV for 20 days",
        "Meglumine antimoniate (MA) 20 mg/kg IM/IV for 20 days",
        "Intralesional SSG (1-3 mL per lesion) every 3-7 days",
        "Paromomycin ointment 15% twice daily for 20 days",
        "Fluconazole 200mg daily for 6 weeks",
        "Cryotherapy with liquid nitrogen (1-2 cycles) weekly",
        "Dosage and Duration"
    )

    val followUpOptions = listOf("30", "60", "90", "180")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Case Review & Diagnosis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Reviewing Case #${patientCase.patientId}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF59E0B)
            )
            Spacer(modifier = Modifier.height(16.dp))

            DropdownField(
                label = "Primary Diagnosis *",
                options = diagnosisOptions,
                selectedOption = diagnosis,
                onOptionSelected = { selectedVal: String -> diagnosis = selectedVal })
            OutlinedTextField(value = differentialDiagnosis, onValueChange = { differentialDiagnosis = it }, label = { Text("Differential Diagnoses") }, placeholder = { Text("e.g., Sporotrichosis, Wart") }, modifier = Modifier.fillMaxWidth())

            DropdownField(
                label = "Diagnostic Certainty *",
                options = listOf("High", "Moderate", "Low"),
                selectedOption = certainty,
                onOptionSelected = { selectedVal: String -> certainty = selectedVal })
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = labConfirmationNeeded, onCheckedChange = { labConfirmationNeeded = it })
                Text("Laboratory Confirmation Needed", style = MaterialTheme.typography.bodyMedium)
            }

            if (labConfirmationNeeded) {
                Text(
                    "Select Lab Tests:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    labTestOptions.forEach { test ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                selectedLabTests =
                                    if (selectedLabTests.contains(test)) selectedLabTests - test else selectedLabTests + test
                            }) {
                            Checkbox(
                                checked = selectedLabTests.contains(test),
                                onCheckedChange = { checked ->
                                    selectedLabTests =
                                        if (checked) selectedLabTests + test else selectedLabTests - test
                                })
                            Text(test)
                        }
                    }
                }

                if (selectedLabTests.contains("Other")) {
                    OutlinedTextField(
                        value = otherLabTest,
                        onValueChange = { otherLabTest = it },
                        label = { Text("Specify Other Lab Test *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DropdownField(
                label = "Treatment Type *",
                options = listOf(
                    "Topical (cream/ointment)",
                    "Systemic (oral medication)",
                    "Intralesional Injection",
                    "Cryotherapy (freezing)",
                    "Combination",
                    "Referral for specialist care"
                ),
                selectedOption = treatmentType,
                onOptionSelected = { selectedVal: String -> treatmentType = selectedVal }
            )

            Spacer(modifier = Modifier.height(8.dp))
            DropdownField(
                label = "Dosage & Duration Template *",
                options = dosageOptions,
                selectedOption = selectedDosageTemplate,
                onOptionSelected = { selectedVal: String ->
                    selectedDosageTemplate = selectedVal
                    if (selectedVal != "Dosage and Duration") {
                        dosageDuration = selectedVal
                    } else {
                        dosageDuration = ""
                    }
                }
            )

            if (selectedDosageTemplate == "Dosage and Duration") {
                OutlinedTextField(
                    value = dosageDuration,
                    onValueChange = { dosageDuration = it },
                    label = { Text("Specific Dosage and Duration *") },
                    placeholder = { Text("Enter custom dosage details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DropdownField(
                label = "Follow-up Interval (days) *",
                options = followUpOptions,
                selectedOption = followUpInterval,
                onOptionSelected = { selectedVal: String -> followUpInterval = selectedVal }
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = referralNeeded, onCheckedChange = { referralNeeded = it })
                Text("Referral Needed", style = MaterialTheme.typography.bodyMedium)
            }

            if (referralNeeded) {
                OutlinedTextField(
                    value = referralReason,
                    onValueChange = { referralReason = it },
                    label = { Text("Referral Reason *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Feedback for Health Worker *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (diagnosis.isNotBlank() && certainty.isNotBlank() && treatmentType.isNotBlank() && followUpInterval.isNotBlank() && feedback.isNotBlank() && (selectedDosageTemplate != "Dosage and Duration" || dosageDuration.isNotBlank())) {
                            val finalTests =
                                selectedLabTests.map { if (it == "Other") "Other: $otherLabTest" else it }
                                    .joinToString(", ")
                            onSubmit(
                                diagnosis,
                                differentialDiagnosis,
                                certainty,
                                labConfirmationNeeded,
                                finalTests,
                                treatmentType,
                                dosageDuration,
                                followUpInterval,
                                referralNeeded,
                                referralReason,
                                feedback
                            )
                            Toast.makeText(context, "✓ Review submitted!", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF208090))
                ) {
                    Text("✓ Submit Review")
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF208090),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
