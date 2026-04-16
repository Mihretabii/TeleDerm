package et.ahri.telederm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import et.ahri.telederm.data.PatientCase
import et.ahri.telederm.data.User
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    authViewModel: AuthViewModel,
    patientViewModel: PatientViewModel,
    onLogout: () -> Unit
) {
    val cases by patientViewModel.allCases.collectAsState(initial = emptyList())
    val pendingUsers by authViewModel.pendingUsers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Analytics", "User Management", "Settings")

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            authViewModel.loadUsers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Admin Dashboard") },
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
                0 -> AnalyticsTab(cases)
                1 -> UserManagementTab(authViewModel, pendingUsers, allUsers)
                2 -> AdminSettingsTab(authViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTab(cases: List<PatientCase>) {
    val context = LocalContext.current
    var selectedCaseForView by remember { mutableStateOf<PatientCase?>(null) }

    if (selectedCaseForView != null) {
        CaseDetailDialog(
            patientCase = selectedCaseForView!!,
            onDismiss = { selectedCaseForView = null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("System Overview", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Total Cases", cases.size.toString(), Modifier.weight(1f))
            StatCard(
                "Pending",
                cases.count { it.status == "Pending" }.toString(),
                Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                "Reviewed",
                cases.count { it.status == "Reviewed" }.toString(),
                Modifier.weight(1f)
            )
            StatCard(
                "Referred",
                cases.count { it.status == "Referred" }.toString(),
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        AnalyticsSection(
            "🌍 Geography Overview", listOf(
            "Region" to cases.groupBy { it.region },
            "Zone" to cases.groupBy { it.zone },
            "Woreda" to cases.groupBy { it.woreda }
        ))

        AnalyticsSection(
            "🏥 Clinical & Facility Overview", listOf(
            "Facility Type" to cases.groupBy { it.facilityType },
            "Lesion Type" to cases.groupBy { it.lesionType },
            "Primary Diagnosis" to cases.groupBy { it.diagnosis ?: "Not Reviewed" },
            "Diagnostic Certainty" to cases.groupBy { it.certainty ?: "N/A" },
            "Lab Tests Done" to cases.groupBy { it.labTests ?: "None" },
            "Recommended Treatment" to cases.groupBy { it.treatmentType ?: "N/A" },
            "Referral Needed" to cases.groupBy { if (it.isReferral) "Yes" else "No" },
            "Treatment Outcome" to cases.groupBy { it.treatmentOutcome ?: "No Update" }
        ))

        Spacer(modifier = Modifier.height(24.dp))
        Text("📥 Data Management", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { exportData(context, cases, "csv") },
                modifier = Modifier.weight(1f)
            ) { Text("CSV") }
            Button(
                onClick = { exportData(context, cases, "json") },
                modifier = Modifier.weight(1f)
            ) { Text("JSON") }
            Button(
                onClick = { exportData(context, cases, "txt") },
                modifier = Modifier.weight(1f)
            ) { Text("Report") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("All Patient Cases", style = MaterialTheme.typography.titleLarge)
        cases.forEach { patientCase ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { selectedCaseForView = patientCase }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "ID: ${patientCase.patientId}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Badge(
                            containerColor = when (patientCase.status) {
                                "Reviewed" -> Color(0xFF22C55E)
                                "Referred" -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            }
                        ) { Text(patientCase.status, color = Color.White) }
                    }
                    Text("${patientCase.facility} | ${patientCase.visitDate}")
                }
            }
        }
    }
}

@Composable
fun UserManagementTab(viewModel: AuthViewModel, pendingUsers: List<User>, allUsers: List<User>) {
    var showAddUserDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAddUserDialog) {
        AddUserDialog(onDismiss = { showAddUserDialog = false }, onUserAdded = { user ->
            viewModel.adminCreateUser(user) { success, message ->
                Toast.makeText(context, message ?: "Response", Toast.LENGTH_SHORT).show()
                if (success) showAddUserDialog = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("User Management", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAddUserDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add User")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingUsers.isNotEmpty()) {
            Text(
                "🔔 Pending Approvals",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF59E0B)
            )
            pendingUsers.forEach { user ->
                UserItem(
                    user,
                    onApprove = { viewModel.approveUser(user) },
                    onRemove = { viewModel.removeUser(user) })
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("👥 All Users", style = MaterialTheme.typography.titleLarge)
        if (allUsers.isEmpty()) {
            Text(
                "No other users found.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            allUsers.forEach { user ->
                UserItem(user, onRemove = { viewModel.removeUser(user) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserItem(user: User, onApprove: (() -> Unit)? = null, onRemove: () -> Unit) {
    OutlinedCard(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.fullName, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Role: ${user.role.replace("_", " ").uppercase()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (!user.isApproved && onApprove != null) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("Approve")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AdminSettingsTab(viewModel: AuthViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    var fullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var sex by remember { mutableStateOf(currentUser?.sex ?: "") }
    var newPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Admin Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            label = "Sex",
            options = listOf("Male", "Female"),
            selectedOption = sex,
            onOptionSelected = { sex = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.updateProfile(fullName, sex) { success ->
                if (success) Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(32.dp))

        Text("Change Password", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (newPassword.length >= 6) {
                viewModel.changePassword(newPassword) { success ->
                    if (success) {
                        Toast.makeText(
                            context,
                            "Password updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        newPassword = ""
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Password must be at least 6 characters.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Password")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(onDismiss: () -> Unit, onUserAdded: (User) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("health_worker") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Sex",
                    options = listOf("Male", "Female"),
                    selectedOption = sex,
                    onOptionSelected = { sex = it })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownField(
                    label = "Role",
                    options = listOf("health_worker", "dermatologist"),
                    selectedOption = role,
                    onOptionSelected = { role = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    onUserAdded(User(email, fullName, sex, password, role, isApproved = true))
                }
            }) { Text("Create User") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AnalyticsSection(title: String, groupings: List<Pair<String, Map<String, List<PatientCase>>>>) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    groupings.forEach { (label, data) ->
        val sortedData = data.toList().sortedByDescending { it.second.size }
        val total = sortedData.sumOf { it.second.size }

        OutlinedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (sortedData.isEmpty() || (sortedData.size == 1 && sortedData[0].first.isEmpty())) {
                    Text("No data available", style = MaterialTheme.typography.bodySmall)
                } else {
                    sortedData.take(5).forEach { (key, list) ->
                        val displayKey = if (key.isEmpty()) "Unknown" else key
                        val percent =
                            if (total > 0) (list.size.toFloat() / total * 100).toInt() else 0
                        DiagnosisRow(displayKey, list.size.toString(), "$percent%")
                    }
                    if (sortedData.size > 5) {
                        Text(
                            "... and ${sortedData.size - 5} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun exportData(context: Context, cases: List<PatientCase>, format: String) {
    if (cases.isEmpty()) {
        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
        return
    }
    val fileName = "telederm_export_${System.currentTimeMillis()}.$format"
    val content = when (format) {
        "csv" -> generateCsv(cases)
        "json" -> generateJson(cases)
        else -> generateReport(cases)
    }
    try {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (format == "json") "application/json" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Data"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun generateCsv(cases: List<PatientCase>): String {
    val header =
        "PatientID,VisitDate,Age,AgeUnit,Sex,FacilityType,Region,Zone,Woreda,Status,Diagnosis,Certainty,Treatment,Outcome\n"
    return header + cases.joinToString("\n") {
        "\"${it.patientId}\",\"${it.visitDate}\",\"${it.age}\",\"${it.ageUnit}\",\"${it.sex}\",\"${it.facilityType}\",\"${it.region}\",\"${it.zone}\",\"${it.woreda}\",\"${it.status}\",\"${it.diagnosis ?: ""}\",\"${it.certainty ?: ""}\",\"${it.treatmentType ?: ""}\",\"${it.treatmentOutcome ?: ""}\""
    }
}

private fun generateJson(cases: List<PatientCase>): String {
    return "[\n" + cases.joinToString(",\n") {
        """  {
    "id": "${it.patientId}",
    "region": "${it.region}",
    "diagnosis": "${it.diagnosis ?: ""}",
    "status": "${it.status}"
  }"""
    } + "\n]"
}

private fun generateReport(cases: List<PatientCase>): String {
    return "TELE-DERM SYSTEM REPORT\nTotal Cases: ${cases.size}\n\n" + 
        cases.joinToString("\n---\n") {
            "ID: ${it.patientId} | Status: ${it.status} | Diagnosis: ${it.diagnosis}"
        }
}

@Composable
fun DiagnosisRow(label: String, count: String, percent: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(count, modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
        Text(
            percent,
            modifier = Modifier.width(60.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun CaseDetailDialog(patientCase: PatientCase, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Case Details: #${patientCase.patientId}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Patient: ${patientCase.age} ${patientCase.ageUnit}, ${patientCase.sex}")
                Text("Facility: ${patientCase.facility} (${patientCase.facilityType})")
                Text("Location: ${patientCase.region}, ${patientCase.zone}, ${patientCase.woreda}, ${patientCase.kebele}")
                Divider(Modifier.padding(vertical = 8.dp))
                Text("Diagnosis: ${patientCase.diagnosis ?: "Pending"}")
                Text("Lab Tests: ${patientCase.labTests ?: "None"}")
                Text("Outcome: ${patientCase.treatmentOutcome ?: "No follow-up yet"}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
