package et.ahri.telederm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import et.ahri.telederm.data.AdminHierarchy
import et.ahri.telederm.data.PatientCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthWorkerScreen(
    authViewModel: AuthViewModel,
    patientViewModel: PatientViewModel,
    onLogout: () -> Unit
) {
    val cases by patientViewModel.allCases.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Submit Case", "My Cases", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏥 TELE-DERM", style = MaterialTheme.typography.titleLarge)
                        Text("Telemedicine for Remote Dermatological Care in Ethiopia", style = MaterialTheme.typography.labelSmall)
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
                0 -> SubmitCaseTab(patientViewModel)
                1 -> MyCasesTab(patientViewModel, cases)
                2 -> UserSettingsTab(authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubmitCaseTab(viewModel: PatientViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var patientId by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var patientAge by remember { mutableStateOf("") }
    var ageUnit by remember { mutableStateOf("Years") }
    var patientSex by remember { mutableStateOf("") }
    var healthWorkerType by remember { mutableStateOf("") }
    var healthFacility by remember { mutableStateOf("") }
    var healthFacilityType by remember { mutableStateOf("") }

    // Cascading Residence
    var selectedFederal by remember { mutableStateOf(AdminHierarchy.federal[0]) }
    var selectedRegion by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf("") }
    var selectedWoreda by remember { mutableStateOf("") }
    var selectedKebele by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("+251") }
    var consentObtained by remember { mutableStateOf(false) }

    var lesionDuration by remember { mutableStateOf("") }
    var exposureHistory by remember { mutableStateOf("") }
    var lesionType by remember { mutableStateOf("") }
    var numLesions by remember { mutableStateOf("") }

    val lesionLocations = listOf("Face", "Arm", "Leg", "Trunk", "Hand", "Foot", "Scalp", "Neck")
    var selectedLocations by remember { mutableStateOf(setOf<String>()) }
    
    var lesionSize by remember { mutableStateOf("") }
    var painLevel by remember { mutableStateOf("") }
    var prevTreatment by remember { mutableStateOf("") }
    var associatedSymptoms by remember { mutableStateOf("") }
    var comorbidities by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    var capturedImages by remember { mutableStateOf(listOf<Uri>()) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val healthWorkerTypes = listOf(
        "Health Extension Workers (HEWs)",
        "Health development Army (Community Health Volunteers)",
        "Nurses",
        "Midwives",
        "Health Officers",
        "Medical Doctors (General Practitioners)",
        "Specialist Physicians (Dermatologist, internal medicine, surgery, pediatrics, obstetrics/gynecology, psychiatry, etc.)",
        "Pharmacists",
        "Pharmacy Technicians",
        "Laboratory Technicians /Technologists",
        "Radiographers & Imaging Technicians (X-ray, ultrasound, CT, MRI)",
        "Public Health Experts (epidemiologist, health policy experts, Reproductive health and disease prevention programs etc.)",
        "Environmental Health Workers",
        "Nutritionists",
        "Health Educators",
        "Dentists & Dental Technicians",
        "Physiotherapists & Rehabilitation Workers",
        "Optometrists & Ophthalmic Nurses",
        "Anesthetists (Nurse Anesthetists/Physician Anesthetists)",
        "Emergency Medical Technicians"
    ).sorted()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            capturedImages = capturedImages + tempImageUri!!
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        capturedImages = capturedImages + uris
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var isCameraPending by remember { mutableStateOf(false) }

    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun startTakePhoto() {
        val file = File(context.cacheDir, "cam_image_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        tempImageUri = uri
        cameraLauncher.launch(uri)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTakePhoto()
        } else {
            showSettingsDialog = true
        }
    }

    fun handleTakePhotoClick() {
        if (isCameraPermissionGranted()) {
            startTakePhoto()
        } else {
            isCameraPending = true
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isCameraPending && isCameraPermissionGranted()) {
                    isCameraPending = false
                    startTakePhoto()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("Please allow camera permission in settings to take photos.") },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Go to Settings")
                }
            }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        visitDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("👤 Patient Information", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF208090))

        OutlinedTextField(
            value = patientId, 
            onValueChange = { patientId = it }, 
            label = { Text("Patient ID *") }, 
            placeholder = { Text("Auto-generated or assigned") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = visitDate,
            onValueChange = {},
            label = { Text("Date of Visit *") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.clickable { showDatePicker = true })
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Age Unit *", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = ageUnit == "Years", onClick = { ageUnit = "Years" })
            Text("Years", modifier = Modifier.clickable { ageUnit = "Years" })
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = ageUnit == "Months", onClick = { ageUnit = "Months" })
            Text("Months", modifier = Modifier.clickable { ageUnit = "Months" })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = patientAge, 
                onValueChange = { patientAge = it },
                label = { Text("Age ($ageUnit) *") },
                placeholder = { Text("Age in $ageUnit") },
                modifier = Modifier.weight(1f), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            DropdownField(
                label = "Sex *", 
                options = listOf("Male", "Female"), 
                selectedOption = patientSex,
                onOptionSelected = { selectedVal: String -> patientSex = selectedVal }, 
                modifier = Modifier.weight(1f)
            )
        }

        SearchableDropdownField(
            label = "Health Worker Type *",
            options = healthWorkerTypes,
            selectedOption = healthWorkerType,
            onOptionSelected = { selectedVal: String -> healthWorkerType = selectedVal }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(
                label = "Facility Type *",
                options = listOf("Health Center", "Hospital", "Clinic", "Health Post"),
                selectedOption = healthFacilityType,
                onOptionSelected = { selectedVal: String -> healthFacilityType = selectedVal },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = healthFacility,
                onValueChange = { healthFacility = it },
                label = { Text("Facility Name *") },
                placeholder = { Text("Enter facility name") },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "Residence Hierarchy *",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        DropdownField(
            label = "Federal *",
            options = AdminHierarchy.federal,
            selectedOption = selectedFederal,
            onOptionSelected = { selectedFederal = it }
        )

        SearchableDropdownField(
            label = "Region *",
            options = AdminHierarchy.regions,
            selectedOption = selectedRegion,
            onOptionSelected = { selectedVal: String ->
                selectedRegion = selectedVal
                selectedZone = ""
                selectedWoreda = ""
                selectedKebele = ""
            }
        )
        SearchableDropdownField(
            label = "Zone *",
            options = AdminHierarchy.zonesMap[selectedRegion] ?: emptyList(),
            selectedOption = selectedZone,
            onOptionSelected = { selectedVal: String ->
                selectedZone = selectedVal
                selectedWoreda = ""
                selectedKebele = ""
            }
        )
        SearchableDropdownField(
            label = "Woreda *",
            options = AdminHierarchy.woredasMap[selectedZone] ?: emptyList(),
            selectedOption = selectedWoreda,
            onOptionSelected = { selectedVal: String ->
                selectedWoreda = selectedVal
                selectedKebele = ""
            }
        )
        OutlinedTextField(
            value = selectedKebele,
            onValueChange = { selectedKebele = it },
            label = { Text("Kebele *") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { input ->
                if (input.startsWith("+251") && input.length <= 13) {
                    phoneNumber = input
                }
            },
            label = { Text("Contact Phone Number") },
            placeholder = { Text("+251XXXXXXXXX") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.3f
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Patient Consent / የሕመምተኛው ፈቃድ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "English: I voluntarily agree to participate in this telemedicine service. I understand that my clinical data and images will be shared with specialists for diagnosis and treatment recommendations. My identity will be kept confidential as per medical standards.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "አማርኛ፦ በዚህ የቴሌሜዲካል አገልግሎት ላይ ለመሳተፍ በፈቃደኝነት እስማማለሁ። የእኔ ክሊኒካዊ መረጃዎች እና ምስሎች ለምርመራ እና ለሕክምና ምክሮች ለቆዳ ስፔሻሊስቶች እንደሚጋሩ ተረድቻለሁ። ማንነቴ በሕክምና ሚስጥራዊነት ደንብ መሠረት ይጠበቃል።",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consentObtained, onCheckedChange = { consentObtained = it })
            Text("Informed Consent Obtained *", style = MaterialTheme.typography.bodyLarge)
        }

        if (consentObtained) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("🔍 Clinical Information", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF208090))

            OutlinedTextField(
                value = associatedSymptoms,
                onValueChange = { associatedSymptoms = it },
                label = { Text("Dermatologic Symptoms") },
                placeholder = { Text("e.g., Fever, swelling, lymph node enlargement") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lesionDuration, 
                onValueChange = { lesionDuration = it }, 
                label = { Text("Duration of Lesion *") }, 
                placeholder = { Text("e.g., 3 weeks, 2 months") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = exposureHistory, 
                onValueChange = { exposureHistory = it }, 
                label = { Text("Exposure History (Travel History)") },
                placeholder = { Text("e.g., Endemic area") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Lesion Type *", 
                    options = listOf("Papule", "Nodule", "Ulcer", "Plaque", "Pustule", "Other"), 
                    selectedOption = lesionType,
                    onOptionSelected = { selectedVal: String -> lesionType = selectedVal }, 
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = numLesions, 
                    onValueChange = { numLesions = it }, 
                    label = { Text("Number of Lesions *") }, 
                    placeholder = { Text("e.g., 1, 3, 5") },
                    modifier = Modifier.weight(1f), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Text(
                "Lesion Location (Checkboxes) *",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lesionLocations.forEach { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedLocations = if (selectedLocations.contains(location)) {
                                selectedLocations - location
                            } else {
                                selectedLocations + location
                            }
                        }
                    ) {
                        Checkbox(
                            checked = selectedLocations.contains(location),
                            onCheckedChange = { checked ->
                                selectedLocations = if (checked) selectedLocations + location
                                else selectedLocations - location
                            }
                        )
                        Text(location)
                    }
                }
            }

            OutlinedTextField(
                value = lesionSize,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char == '.' }) lesionSize = it
                },
                label = { Text("Index Lesion Size (cm) *") },
                placeholder = { Text("e.g., 2.5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            DropdownField(
                label = "Itching or Pain *", 
                options = listOf("None", "Mild", "Moderate", "Severe"), 
                selectedOption = painLevel,
                onOptionSelected = { selectedVal: String -> painLevel = selectedVal }
            )
            
            DropdownField(
                label = "Previous Treatment",
                options = listOf(
                    "None",
                    "Topical (Cream / Ointment) and Cryotherapy",
                    "Systemic",
                    "Both Topical & Systemic",
                    "Traditional",
                    "Other"
                ),
                selectedOption = prevTreatment,
                onOptionSelected = { selectedVal: String -> prevTreatment = selectedVal }
            )

            OutlinedTextField(
                value = comorbidities, 
                onValueChange = { comorbidities = it }, 
                label = { Text("Co-morbidities") }, 
                placeholder = { Text("e.g., Diabetes, HIV, Hypertension") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = additionalNotes, 
                onValueChange = { additionalNotes = it }, 
                label = { Text("Additional Notes") }, 
                placeholder = { Text("Any other observations") },
                modifier = Modifier.fillMaxWidth(), 
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("📸 Lesion Images", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { handleTakePhotoClick() }) { Text("📷 Take Photo") }
                OutlinedButton(onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) { Text("📁 Pick Images") }
            }

            if (capturedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (patientId.isNotBlank() && phoneNumber.length == 13) {
                        val newCase = PatientCase(
                            patientId = patientId,
                            visitDate = visitDate,
                            age = patientAge,
                            ageUnit = ageUnit,
                            sex = patientSex,
                            healthWorkerType = healthWorkerType,
                            facility = healthFacility,
                            facilityType = healthFacilityType,
                            region = selectedRegion,
                            zone = selectedZone,
                            woreda = selectedWoreda,
                            kebele = selectedKebele,
                            residence = "$selectedRegion/$selectedZone/$selectedWoreda/$selectedKebele",
                            phoneNumber = phoneNumber,
                            lesionDuration = lesionDuration,
                            exposureHistory = exposureHistory,
                            lesionType = lesionType,
                            numLesions = numLesions,
                            lesionLocation = selectedLocations.joinToString(", "),
                            lesionSize = lesionSize,
                            painLevel = painLevel,
                            prevTreatment = prevTreatment,
                            associatedSymptoms = associatedSymptoms,
                            comorbidities = comorbidities,
                            additionalNotes = additionalNotes, 
                            images = capturedImages.joinToString(",") { it.toString() }
                        )
                        viewModel.submitCase(newCase) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "✓ Case $patientId submitted!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                patientId = ""; patientAge = ""; patientSex = ""; healthFacility =
                                    ""; phoneNumber = "+251"
                                consentObtained = false; selectedLocations =
                                    emptySet(); capturedImages = emptyList()
                            } else {
                                Toast.makeText(context, "Submission Failed.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please fill required fields (ID, Phone, etc).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF208090))
            ) {
                Text("✓ Submit Case for Review")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCasesTab(viewModel: PatientViewModel, cases: List<PatientCase>) {
    var selectedCase by remember { mutableStateOf<PatientCase?>(null) }
    var filterStatus by remember { mutableStateOf("All") }

    val filteredCases =
        if (filterStatus == "All") cases else cases.filter { it.status == filterStatus }

    if (selectedCase != null) {
        CaseResponseDialog(
            viewModel,
            patientCase = selectedCase!!,
            onDismiss = { selectedCase = null })
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Your Submitted Cases", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF208090))

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
        filteredCases.forEach { patientCase ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { selectedCase = patientCase }) {
                ListItem(
                    headlineContent = { Text("Patient ID: ${patientCase.patientId}") },
                    supportingContent = { Text("Submitted: ${patientCase.visitDate}") },
                    trailingContent = {
                        Badge(containerColor = when(patientCase.status) {
                            "Reviewed" -> Color(0xFF22C55E)
                            "Referred" -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        }) {
                            Text(patientCase.status, color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CaseResponseDialog(
    viewModel: PatientViewModel,
    patientCase: PatientCase,
    onDismiss: () -> Unit
) {
    var showFollowUpForm by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dermatologist Response: #${patientCase.patientId}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (patientCase.status == "Pending") {
                    Text("This case is still pending review.", color = Color.Gray)
                } else {
                    ResponseItem("Status", patientCase.status, if(patientCase.status == "Reviewed") Color(0xFF22C55E) else Color(0xFFEF4444))
                    Divider(Modifier.padding(vertical = 8.dp))
                    ResponseItem("Primary Diagnosis", patientCase.diagnosis ?: "N/A")
                    ResponseItem("Differential Diagnoses", patientCase.differentialDiagnosis ?: "N/A")
                    ResponseItem("Recommended Treatment", patientCase.treatmentType ?: "N/A")
                    ResponseItem("Feedback", patientCase.feedback ?: "No feedback.")

                    if (patientCase.updateFeedback != null) {
                        Divider(Modifier.padding(vertical = 8.dp))
                        Text(
                            "Follow-up Feedback:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF208090)
                        )
                        Text(patientCase.updateFeedback)
                    }

                    if (!showFollowUpForm && !patientCase.isUpdatePending) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showFollowUpForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Progress Update")
                        }
                    }

                    if (patientCase.isUpdatePending) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Progress update pending review...",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (showFollowUpForm) {
                        FollowUpForm(
                            onSubmit = { stage, outcome ->
                                viewModel.submitFollowUpUpdate(
                                    patientCase.id,
                                    stage,
                                    outcome
                                ) { success ->
                                    if (success) {
                                        showFollowUpForm = false
                                        onDismiss()
                                    }
                                }
                            },
                            onCancel = { showFollowUpForm = false }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun FollowUpForm(onSubmit: (String, String) -> Unit, onCancel: () -> Unit) {
    var selectedStage by remember { mutableStateOf("") }
    var selectedOutcome by remember { mutableStateOf("") }

    val stages = listOf("Day 30", "Day 60", "Day 90", "Day 180")
    val outcomes = listOf("Healed", "Improving", "No Change", "Worsening", "Other")

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Text("Follow-up Update", fontWeight = FontWeight.Bold)
        DropdownField("Follow-up Stage *", stages, selectedStage, { selectedStage = it })
        DropdownField("Treatment Outcome *", outcomes, selectedOutcome, { selectedOutcome = it })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (selectedStage.isNotBlank() && selectedOutcome.isNotBlank()) onSubmit(
                        selectedStage,
                        selectedOutcome
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("Submit") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
    }
}

@Composable
fun ResponseItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}
