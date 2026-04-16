package et.ahri.telederm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import et.ahri.telederm.ui.theme.TeleDermTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule Follow-up Reminders (Daily check)
        scheduleFollowUpReminders()
        
        setContent {
            TeleDermTheme {
                val authViewModel: AuthViewModel = viewModel()
                val patientViewModel: PatientViewModel = viewModel()

                // Request Notification Permission for Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val currentUser by authViewModel.currentUser.collectAsState()
                val authState by authViewModel.authState.collectAsState()

                if (currentUser == null) {
                    when (authState) {
                        is AuthState.Login -> LoginScreen(authViewModel)
                        is AuthState.Register -> RegisterScreen(authViewModel)
                        is AuthState.ForgotPassword -> ForgotPasswordScreen(authViewModel)
                    }
                } else {
                    when (currentUser?.role) {
                        "health_worker" -> HealthWorkerScreen(
                            authViewModel,
                            patientViewModel,
                            onLogout = { authViewModel.logout() })

                        "dermatologist" -> DermatologistScreen(
                            authViewModel,
                            patientViewModel,
                            onLogout = { authViewModel.logout() })

                        "admin" -> AdminScreen(
                            authViewModel,
                            patientViewModel,
                            onLogout = { authViewModel.logout() })

                        else -> HealthWorkerScreen(
                            authViewModel,
                            patientViewModel,
                            onLogout = { authViewModel.logout() })
                    }
                }
            }
        }
    }

    private fun scheduleFollowUpReminders() {
        val followUpWorkRequest = PeriodicWorkRequestBuilder<FollowUpWorker>(
            24, TimeUnit.HOURS // Check daily
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "FollowUpReminders",
            ExistingPeriodicWorkPolicy.KEEP,
            followUpWorkRequest
        )
    }
}
