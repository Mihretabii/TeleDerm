package et.ahri.telederm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import et.ahri.telederm.data.AppDatabase
import et.ahri.telederm.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _authState = MutableStateFlow<AuthState>(AuthState.Login)
    val authState: StateFlow<AuthState> = _authState

    private val _pendingUsers = MutableStateFlow<List<User>>(emptyList())
    val pendingUsers: StateFlow<List<User>> = _pendingUsers

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    init {
        seedAdminAccount()
    }

    private fun seedAdminAccount() {
        viewModelScope.launch {
            val adminEmail = "teledermadmin@gmail.com"
            val adminPassword = "admin123"

            val oldEmails = listOf("admin@telederm.et", "admin@ahri.gov.et")
            oldEmails.forEach { email ->
                val oldAdmin = userDao.getUserByEmail(email)
                if (oldAdmin != null && oldAdmin.role == "admin") {
                    userDao.deleteUser(oldAdmin)
                }
            }

            val existingAdmin = userDao.getUserByEmail(adminEmail)
            if (existingAdmin == null) {
                userDao.registerUser(
                    User(
                        email = adminEmail,
                        fullName = "System Admin",
                        sex = "Male",
                        passwordHash = adminPassword,
                        role = "admin",
                        isApproved = true
                    )
                )
            }
        }
    }

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()

                val user = userDao.getUserByEmail(trimmedEmail)
                if (user != null && user.passwordHash == trimmedPassword) {
                    if (user.role != "admin" && !user.isApproved) {
                        onResult(
                            false,
                            "Your account is pending admin approval. Please contact the administrator."
                        )
                    } else {
                        _currentUser.value = user
                        onResult(true, null)
                    }
                } else {
                    onResult(false, "Invalid email or password")
                }
            } catch (e: Exception) {
                onResult(false, "Login failed: ${e.message}")
            }
        }
    }

    fun register(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (!isPasswordValid(user.passwordHash)) {
                onResult(false, "Password must be at least 6 characters.")
                return@launch
            }
            try {
                val cleanedUser = user.copy(
                    email = user.email.trim(),
                    passwordHash = user.passwordHash.trim(),
                    isApproved = false
                )

                val existingUser = userDao.getUserByEmail(cleanedUser.email)
                if (existingUser != null) {
                    onResult(false, "Email already registered.")
                    return@launch
                }

                userDao.registerUser(cleanedUser)
                _authState.value = AuthState.Login
                onResult(true, "Registration successful! Your account is pending admin approval.")
            } catch (e: Exception) {
                onResult(false, "Registration failed: ${e.message}")
            }
        }
    }

    fun adminCreateUser(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val existingUser = userDao.getUserByEmail(user.email.trim())
                if (existingUser != null) {
                    onResult(false, "Account is already created with this email.")
                    return@launch
                }
                userDao.registerUser(
                    user.copy(
                        email = user.email.trim(),
                        passwordHash = user.passwordHash.trim()
                    )
                )
                loadUsers()
                onResult(true, "User created successfully.")
            } catch (e: Exception) {
                onResult(false, "Creation failed: ${e.message}")
            }
        }
    }

    fun updateProfile(fullName: String, sex: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val updatedUser = user.copy(fullName = fullName, sex = sex)
                userDao.updateUser(updatedUser)
                _currentUser.value = updatedUser
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _pendingUsers.value = userDao.getPendingUsers()
            _allUsers.value = userDao.getAllNonAdminUsers()
        }
    }

    fun approveUser(user: User) {
        viewModelScope.launch {
            userDao.updateUser(user.copy(isApproved = true))
            loadUsers()
        }
    }

    fun removeUser(user: User) {
        viewModelScope.launch {
            userDao.deleteUser(user)
            loadUsers()
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            val trimmedPassword = newPassword.trim()
            if (user != null && trimmedPassword.length >= 6) {
                val updatedUser = user.copy(passwordHash = trimmedPassword)
                userDao.updateUser(updatedUser)
                _currentUser.value = updatedUser
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?, String?) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            val user = userDao.getUserByEmail(trimmedEmail)
            if (user != null) {
                // Generate a temporary 8-character password
                val tempPassword = UUID.randomUUID().toString().take(8).uppercase()
                val updatedUser = user.copy(passwordHash = tempPassword)
                userDao.updateUser(updatedUser)
                onResult(true, "Success", tempPassword)
            } else {
                onResult(false, "No account found with that email address.", null)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Login
    }
}

sealed class AuthState {
    object Login : AuthState()
    object Register : AuthState()
    object ForgotPassword : AuthState()
}
