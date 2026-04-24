package et.ahri.telederm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import et.ahri.telederm.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class AuthState {
    object Login : AuthState()
    object Register : AuthState()
    object ForgotPassword : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseManager = FirebaseManager

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _authState = MutableStateFlow<AuthState>(AuthState.Login)
    val authState: StateFlow<AuthState> = _authState

    private val _pendingUsers = MutableStateFlow<List<User>>(emptyList())
    val pendingUsers: StateFlow<List<User>> = _pendingUsers

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    init {
        // ALWAYS check/restore admin account if missing from Firestore
        ensureAdminAccountExists()
        observeUsers()
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val fbUser = firebaseManager.getCurrentUser()
            if (fbUser != null && fbUser.email != null) {
                val userData = firebaseManager.getUser(fbUser.email!!)
                if (userData != null) {
                    _currentUser.value = userData
                }
            }
        }
    }

    private fun ensureAdminAccountExists() {
        viewModelScope.launch {
            try {
                val adminEmail = "teledermahri@gmail.com"
                val existingAdmin = firebaseManager.getUser(adminEmail)
                
                if (existingAdmin == null) {
                    Log.d("AuthViewModel", "Admin record missing from Firestore. Restoring...")
                    firebaseManager.saveUser(User(
                        email = adminEmail,
                        fullName = "System Admin",
                        sex = "Male",
                        passwordHash = "PROTECTED",
                        role = "admin",
                        isApproved = true
                    ))
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Admin restoration failed", e)
            }
        }
    }

    private fun observeUsers() {
        viewModelScope.launch {
            try {
                firebaseManager.getPendingUsers().collectLatest {
                    _pendingUsers.value = it
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Observe pending users failed", e)
            }
        }
        viewModelScope.launch {
            try {
                firebaseManager.getNonAdminUsers().collectLatest {
                    _allUsers.value = it
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Observe non-admin users failed", e)
            }
        }
    }

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }

    private fun mapError(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("badly formatted") -> "incorrect email format"
            msg.contains("incorrect malformed") || msg.contains("invalid credential") || msg.contains("invalid-credential") || msg.contains("auth credential is incorrect") ->
                "Incorrect email or password"
            e is java.io.IOException || 
            msg.contains("network") || 
            msg.contains("connection") || 
            msg.contains("unavailable") ||
            msg.contains("timeout") ||
            msg.contains("stream") ||
            msg.contains("end of file") ||
            e.javaClass.simpleName.contains("Network") -> 
                "Connection error"
            else -> e.message ?: "Authentication failed"
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanedEmail = email.trim().lowercase()
                val trimmedPassword = password.trim()
                
                firebaseManager.login(cleanedEmail, trimmedPassword)
                
                val user = firebaseManager.getUser(cleanedEmail)
                if (user != null) {
                    if (user.role != "admin" && !user.isApproved) {
                        firebaseManager.logout()
                        onResult(false, "Your account is pending admin approval.")
                    } else {
                        _currentUser.value = user
                        onResult(true, null)
                    }
                } else {
                    onResult(false, "User profile not found. Please contact support.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun register(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanedEmail = user.email.trim().lowercase()
                val password = user.passwordHash.trim()
                firebaseManager.register(cleanedEmail, password)
                
                val cleanedUser = user.copy(
                    email = cleanedEmail,
                    passwordHash = "PROTECTED",
                    isApproved = false 
                )
                firebaseManager.saveUser(cleanedUser)
                onResult(true, "Registered successfully! Please wait for admin approval.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration error", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun adminCreateUser(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanedEmail = user.email.trim().lowercase()
                val password = user.passwordHash.trim()
                firebaseManager.register(cleanedEmail, password)
                firebaseManager.saveUser(user.copy(
                    email = cleanedEmail, 
                    passwordHash = "PROTECTED",
                    isApproved = true
                ))
                onResult(true, "User created successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Admin create user error", e)
                onResult(false, mapError(e))
            }
        }
    }

    fun updateProfile(fullName: String, sex: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value
                if (user != null) {
                    val updatedUser = user.copy(fullName = fullName, sex = sex)
                    firebaseManager.saveUser(updatedUser)
                    _currentUser.value = updatedUser
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun approveUser(user: User) {
        viewModelScope.launch {
            try {
                firebaseManager.updateUserApproval(user.email, true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Approve user failed", e)
            }
        }
    }

    fun removeUser(user: User) {
        viewModelScope.launch {
            try {
                firebaseManager.deleteUser(user.email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Remove user failed", e)
            }
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedPassword = newPassword.trim()
                if (trimmedPassword.length >= 6) {
                    firebaseManager.updatePassword(trimmedPassword)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanedEmail = email.trim().lowercase()
                firebaseManager.sendPasswordReset(cleanedEmail)
                onResult(true, "Password reset email sent. Please check your inbox.", null)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Reset password error", e)
                onResult(false, mapError(e), null)
            }
        }
    }

    fun logout() {
        firebaseManager.logout()
        _currentUser.value = null
        _authState.value = AuthState.Login
    }
}
