package et.ahri.telederm

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import et.ahri.telederm.data.User

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏥 TELE-DERM Login", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password) { success, error ->
                        if (!success) {
                            errorDialogMessage = error ?: "Login failed"
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        TextButton(onClick = { viewModel.setAuthState(AuthState.Register) }) {
            Text("Don't have an account? Register")
        }

        TextButton(onClick = { viewModel.setAuthState(AuthState.ForgotPassword) }) {
            Text("Forgot Password?")
        }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel) {
    var fullName by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("health_worker") }
    val context = LocalContext.current

    val roles = listOf("health_worker", "dermatologist")
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
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

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                viewModel.setAuthState(AuthState.Login)
            },
            title = { Text("Registration Successful") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    viewModel.setAuthState(AuthState.Login)
                }) {
                    Text("Go to Login")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("🏥 Create Account", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it }, 
            label = { Text("Email") }, 
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password") }, 
            visualTransformation = PasswordVisualTransformation(), 
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        DropdownField(
            label = "Role",
            options = roles,
            selectedOption = role,
            onOptionSelected = { role = it }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && sex.isNotBlank()) {
                    viewModel.register(
                        User(
                            email = email,
                            fullName = fullName,
                            sex = sex,
                            passwordHash = password,
                            role = role,
                            isApproved = false
                        )
                    ) { success, message ->
                        if (success) {
                            successMessage = message ?: "Account created successfully! Please wait for admin approval."
                            showSuccessDialog = true
                        } else {
                            errorDialogMessage = message ?: "Registration failed"
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        TextButton(onClick = { viewModel.setAuthState(AuthState.Login) }) {
            Text("Already have an account? Login")
        }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var recoveredEmail by remember { mutableStateOf("") }
    var recoveredPass by remember { mutableStateOf("") }

    val context = LocalContext.current
    
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Password Recovered Successfully") },
            text = {
                Column {
                    Text("A temporary password has been generated for your account.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recipient Email:", style = MaterialTheme.typography.labelSmall)
                    Text(recoveredEmail, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Temporary Password:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        recoveredPass,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Click 'Open Email App' to send this password to the registered email account.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$recoveredEmail")
                        putExtra(Intent.EXTRA_SUBJECT, "TeleDerm Password Recovery")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hello,\n\nYour temporary password for TeleDerm is: $recoveredPass\n\nPlease use this to log in and change your password in the settings.\n\nBest regards,\nTeleDerm System"
                        )
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Choose Email Client"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                    showDialog = false
                    viewModel.setAuthState(AuthState.Login)
                }) {
                    Text("Open Email App")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    viewModel.setAuthState(AuthState.Login)
                }) {
                    Text("Back to Login")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔑 Reset Password", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Enter your registered email to recover your account.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isNotBlank()) {
                    viewModel.resetPassword(email) { success, message, password ->
                        if (success && password != null) {
                            recoveredEmail = email
                            recoveredPass = password
                            showDialog = true
                        } else {
                            errorDialogMessage = message ?: "Failed to recover password"
                        }
                    }
                } else {
                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recover Password")
        }
        TextButton(onClick = { viewModel.setAuthState(AuthState.Login) }) {
            Text("Back to Login")
        }
    }
}
