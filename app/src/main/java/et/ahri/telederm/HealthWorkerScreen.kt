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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val currentUser by authViewModel.currentUser.collectAsState()
    
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
                0 -> SubmitCaseTab(patientViewModel, currentUser?.email ?: "unknown")
                1 -> MyCasesTab(patientViewModel, currentUser?.email ?: "unknown", cases)
                2 -> UserSettingsTab(authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubmitCaseTab(viewModel: PatientViewModel, userEmail: String) {
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
    var lesionTypeOther by remember { mutableStateOf("") }
    
    var numLesions by remember { mutableStateOf("") }

    val lesionLocations = listOf("Face", "Arm", "Leg", "Trunk", "Hand", "Foot", "Scalp", "Neck", "Other")
    var selectedLocations by remember { mutableStateOf(setOf<String>()) }
    var lesionLocationOther by remember { mutableStateOf("") }
    
    var lesionSize by remember { mutableStateOf("") }
    var painLevel by remember { mutableStateOf("") }
    
    var prevTreatment by remember { mutableStateOf("") }
    var prevTreatmentOther by remember { mutableStateOf("") }
    
    var associatedSymptoms by remember { mutableStateOf("") }
    var comorbidities by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    var capturedImages by remember { mutableStateOf(listOf<Uri>()) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var enlargedImageUri by remember { mutableStateOf<String?>(null) }
    
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Submission Error") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                Button(onClick = { errorDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

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

    if (enlargedImageUri != null) {
        EnlargedImageDialog(imageUrl = enlargedImageUri.toString(), onDismiss = { enlargedImageUri = null })
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
            
            Column(modifier = Modifier.fillMaxWidth()) {
                DropdownField(
                    label = "Lesion Type *",
                    options = listOf("Papule", "Nodule", "Ulcer", "Plaque", "Pustule", "Other"),
                    selectedOption = lesionType,
                    onOptionSelected = { selectedVal: String -> 
                        lesionType = selectedVal 
                        if (selectedVal != "Other") lesionTypeOther = ""
                    }
                )
                if (lesionType == "Other") {
                    OutlinedTextField(
                        value = lesionTypeOther,
                        onValueChange = { lesionTypeOther = it },
                        label = { Text("Specify Other Lesion Type *") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
            
            OutlinedTextField(
                value = numLesions,
                onValueChange = { numLesions = it },
                label = { Text("Number of Lesions *") },
                placeholder = { Text("e.g., 1, 3, 5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

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
            if (selectedLocations.contains("Other")) {
                OutlinedTextField(
                    value = lesionLocationOther,
                    onValueChange = { lesionLocationOther = it },
                    label = { Text("Specify Other Location *") },
                    modifier = Modifier.fillMaxWidth()
                )
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

            Column(modifier = Modifier.fillMaxWidth()) {
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
                    onOptionSelected = { selectedVal: String -> 
                        prevTreatment = selectedVal 
                        if (selectedVal != "Other") prevTreatmentOther = ""
                    }
                )
                if (prevTreatment == "Other") {
                    OutlinedTextField(
                        value = prevTreatmentOther,
                        onValueChange = { prevTreatmentOther = it },
                        label = { Text("Specify Other Previous Treatment") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

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
                Button(onClick = { handleTakePhotoClick() }, enabled = !isSubmitting) { Text("📷 Take Photo") }
                OutlinedButton(onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }, enabled = !isSubmitting) { Text("📁 Pick Images") }
            }

            if (capturedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages) { uri ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                                    .clickable { enlargedImageUri = uri.toString() },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { capturedImages = capturedImages - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isSubmitting) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Text("Uploading images and submitting case...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = {
                        if (patientId.isNotBlank() && phoneNumber.length == 13) {
                            isSubmitting = true
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
                                lesionTypeOther = if (lesionType == "Other") lesionTypeOther else null,
                                numLesions = numLesions,
                                lesionLocation = selectedLocations.joinToString(", "),
                                lesionLocationOther = if (selectedLocations.contains("Other")) lesionLocationOther else null,
                                lesionSize = lesionSize,
                                painLevel = painLevel,
                                prevTreatment = prevTreatment,
                                prevTreatmentOther = if (prevTreatment == "Other") prevTreatmentOther else null,
                                associatedSymptoms = associatedSymptoms,
                                comorbidities = comorbidities,
                                additionalNotes = additionalNotes
                            )
                            viewModel.submitCase(userEmail, newCase, capturedImages) { success, error ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "✓ Case $patientId submitted!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Reset fields
                                    patientId = ""; patientAge = ""; patientSex = ""; healthFacility = ""; healthFacilityType = ""
                                    healthWorkerType = ""; selectedRegion = ""; selectedZone = ""; selectedWoreda = ""; selectedKebele = ""
                                    phoneNumber = "+251"; consentObtained = false; selectedLocations = emptySet()
                                    capturedImages = emptyList(); lesionDuration = ""; exposureHistory = ""; lesionType = ""
                                    lesionTypeOther = ""; prevTreatmentOther = ""; lesionLocationOther = ""
                                    numLesions = ""; lesionSize = ""; painLevel = ""; prevTreatment = ""; associatedSymptoms = ""
                                    comorbidities = ""; additionalNotes = ""
                                } else {
                                    errorDialogMessage = error ?: "Submission Failed"
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCasesTab(viewModel: PatientViewModel, userEmail: String, cases: List<PatientCase>) {
    var selectedCase by remember { mutableStateOf<PatientCase?>(null) }
    var filterStatus by remember { mutableStateOf("All") }

    val filteredCases =
        if (filterStatus == "All") cases else cases.filter { it.status == filterStatus }

    if (selectedCase != null) {
        CaseResponseDialog(
            viewModel,
            userEmail = userEmail,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseResponseDialog(
    viewModel: PatientViewModel,
    userEmail: String,
    patientCase: PatientCase,
    onDismiss: () -> Unit
) {
    var showFollowUpForm by remember { mutableStateOf(false) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Case Details: #${patientCase.patientId}") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("📋 Submitted Information", fontWeight = FontWeight.Bold, color = Color(0xFF208090))
                    ResponseItem("Patient", "${patientCase.age} ${patientCase.ageUnit}, ${patientCase.sex}")
                    ResponseItem("Facility", "${patientCase.facility} (${patientCase.facilityType})")
                    ResponseItem("HW Type", patientCase.healthWorkerType)
                    ResponseItem("Location", patientCase.residence)
                    ResponseItem("Visit Date", patientCase.visitDate)
                    ResponseItem("Phone", patientCase.phoneNumber)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🔍 Clinical Details", fontWeight = FontWeight.Bold, color = Color(0xFF208090))
                    ResponseItem("Lesion Type", if (patientCase.lesionType == "Other") "Other: ${patientCase.lesionTypeOther ?: ""}" else patientCase.lesionType)
                    ResponseItem("Location", if (patientCase.lesionLocation.contains("Other")) "${patientCase.lesionLocation} (${patientCase.lesionLocationOther ?: ""})" else patientCase.lesionLocation)
                    ResponseItem("Duration", patientCase.lesionDuration)
                    ResponseItem("Exposure", patientCase.exposureHistory)
                    ResponseItem("Symptoms", patientCase.associatedSymptoms)
                    ResponseItem("Comorbidities", patientCase.comorbidities)
                    ResponseItem("Prev treatment", if (patientCase.prevTreatment == "Other") "Other: ${patientCase.prevTreatmentOther ?: ""}" else patientCase.prevTreatment)
                    ResponseItem("Notes", patientCase.additionalNotes)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🖼️ Submitted Images", fontWeight = FontWeight.Bold)
                    if (patientCase.images.isNotEmpty()) {
                        val imageUrls = patientCase.images.split(",")
                        LazyRow(modifier = Modifier.height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(imageUrls) { url ->
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { enlargedImageUri = url }
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))

                    Text("🩺 Dermatologist Response", fontWeight = FontWeight.Bold, color = Color(0xFF208090))
                    if (patientCase.status == "Pending") {
                        Text("This case is still pending review.", color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        ResponseItem("Status", patientCase.status, if(patientCase.status == "Reviewed") Color(0xFF22C55E) else Color(0xFFEF4444))
                        ResponseItem("Primary Diagnosis", if (patientCase.diagnosis == "Other") "Other: ${patientCase.diagnosisOther ?: ""}" else patientCase.diagnosis ?: "N/A")
                        ResponseItem("Differential Diagnoses", patientCase.differentialDiagnosis ?: "N/A")
                        ResponseItem("Certainty", patientCase.certainty ?: "N/A")
                        ResponseItem("Lab Needed", if(patientCase.labConfirmationNeeded) "Yes: ${patientCase.labTests ?: ""}" else "No")
                        ResponseItem("Recommended Treatment", if (patientCase.treatmentType == "Other") "Other: ${patientCase.treatmentTypeOther ?: ""}" else patientCase.treatmentType ?: "N/A")
                        ResponseItem("Dosage & Duration", if (patientCase.dosageDuration == "Other") "Other: ${patientCase.dosageDurationOther ?: ""}" else patientCase.dosageDuration ?: "N/A")
                        ResponseItem("Follow-up Interval", "${patientCase.followUpInterval ?: ""} days")
                        ResponseItem("Referral Needed", if(patientCase.isReferral) "Yes: ${patientCase.referralReason ?: ""}" else "No")
                        ResponseItem("Feedback", patientCase.feedback ?: "No feedback.")

                        // Display all independent follow-ups
                        if (patientCase.followUps != null && patientCase.followUps!!.isNotEmpty()) {
                            patientCase.followUps!!.toSortedMap().forEach { (stage, data) ->
                                Divider(Modifier.padding(vertical = 8.dp))
                                Text("📈 Follow-up: $stage", fontWeight = FontWeight.Bold, color = Color(0xFF208090))
                                ResponseItem("Treatment Outcome", if(data["outcome"] == "Other") "Other: ${data["outcomeOther"] ?: ""}" else data["outcome"] ?: "")
                                ResponseItem("Dermatologist Feedback", data["feedback"] ?: "Pending review...")
                            }
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

                        if (showFollowUpForm) {
                            FollowUpForm(
                                onSubmit = { stage, outcome, outcomeOther ->
                                    viewModel.submitFollowUpUpdateExtended(
                                        userEmail,
                                        patientCase.docId,
                                        patientCase.patientId,
                                        stage,
                                        outcome,
                                        outcomeOther
                                    ) { success, error ->
                                        if (success) {
                                            showFollowUpForm = false
                                            onDismiss()
                                        } else {
                                            errorDialogMessage = error ?: "Connection error"
                                        }
                                    }
                                },
                                onCancel = { showFollowUpForm = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUpForm(onSubmit: (String, String, String?) -> Unit, onCancel: () -> Unit) {
    var selectedStage by remember { mutableStateOf("") }
    var selectedOutcome by remember { mutableStateOf("") }
    var outcomeOther by remember { mutableStateOf("") }

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
        DropdownField("Treatment Outcome *", outcomes, selectedOutcome, { 
            selectedOutcome = it 
            if (it != "Other") outcomeOther = ""
        })
        
        if (selectedOutcome == "Other") {
            OutlinedTextField(
                value = outcomeOther,
                onValueChange = { outcomeOther = it },
                label = { Text("Specify Other Outcome *") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (selectedStage.isNotBlank() && selectedOutcome.isNotBlank()) {
                        onSubmit(selectedStage, selectedOutcome, if (selectedOutcome == "Other") outcomeOther else null)
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Submit") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
    }
}
