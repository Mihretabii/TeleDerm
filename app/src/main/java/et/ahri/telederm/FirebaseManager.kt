package et.ahri.telederm

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import et.ahri.telederm.data.AuditLog
import et.ahri.telederm.data.PatientCase
import et.ahri.telederm.data.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseManager {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private const val USERS_COLLECTION = "users"
    private const val CASES_COLLECTION = "patient_cases"
    private const val LOGS_COLLECTION = "audit_logs"
    
    private const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/dvhldw6t1/image/upload"
    private const val CLOUDINARY_PRESET = "patient_image"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private suspend fun ensureAuth() {
        if (auth.currentUser == null) {
            try {
                Log.d("FirebaseManager", "No active Firebase session, signing in anonymously...")
                auth.signInAnonymously().await()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Firebase Auth anonymous sign-in failed", e)
            }
        }
    }

    // --- Firebase Auth Methods ---

    suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun updatePassword(newPassword: String) {
        auth.currentUser?.updatePassword(newPassword)?.await()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun getCurrentUser() = auth.currentUser

    // --- Firestore User Management ---

    suspend fun saveUser(user: User) {
        try {
            Log.d("FirebaseManager", "Saving user data to Firestore: ${user.email}")
            firestore.collection(USERS_COLLECTION).document(user.email).set(user).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving user", e)
            throw e
        }
    }

    suspend fun getUser(email: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION).document(email).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching user", e)
            null
        }
    }

    suspend fun updateUserApproval(email: String, isApproved: Boolean) {
        try {
            firestore.collection(USERS_COLLECTION).document(email)
                .update("isApproved", isApproved)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating approval", e)
            throw e
        }
    }

    fun getPendingUsers(): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = try {
                    snapshot?.toObjects(User::class.java) ?: emptyList()
                } catch (e: Exception) { emptyList() }
                val pending = users.filter { it.role != "admin" && !it.isApproved }
                trySend(pending)
            }
        awaitClose { subscription.remove() }
    }
    
    fun getNonAdminUsers(): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = try {
                    snapshot?.toObjects(User::class.java) ?: emptyList()
                } catch (e: Exception) { emptyList() }
                val nonAdmins = users.filter { it.role != "admin" }
                trySend(nonAdmins)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteUser(email: String) {
        try {
            firestore.collection(USERS_COLLECTION).document(email).delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting user document", e)
        }
    }

    // --- Image Upload (Cloudinary) ---

    suspend fun uploadImage(contentResolver: ContentResolver, uri: Uri): String {
        var lastException: Exception? = null
        for (attempt in 1..2) {
            try {
                Log.d("FirebaseManager", "Opening stream for Cloudinary upload: $uri (Attempt $attempt)")
                val inputStream = contentResolver.openInputStream(uri) 
                    ?: throw IOException("Could not open input stream for image")
                
                val bytes = inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) throw IOException("Image file is empty")
                
                return suspendCancellableCoroutine { continuation ->
                    val mediaType = "image/jpeg".toMediaTypeOrNull()
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "image_${System.currentTimeMillis()}.jpg",
                            bytes.toRequestBody(mediaType))
                        .addFormDataPart("upload_preset", CLOUDINARY_PRESET)
                        .build()

                    val request = Request.Builder()
                        .url(CLOUDINARY_URL)
                        .post(requestBody)
                        .build()

                    okHttpClient.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("FirebaseManager", "Cloudinary upload network failure: ${e.message}")
                            continuation.resumeWithException(e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            if (response.isSuccessful && responseBody != null) {
                                try {
                                    val json = JSONObject(responseBody)
                                    val imageUrl = json.getString("secure_url")
                                    Log.d("FirebaseManager", "Cloudinary upload successful: $imageUrl")
                                    continuation.resume(imageUrl)
                                } catch (e: Exception) {
                                    Log.e("FirebaseManager", "Error parsing Cloudinary response: ${e.message}")
                                    continuation.resumeWithException(e)
                                }
                            } else {
                                val errorDetail = responseBody ?: "No response body"
                                Log.e("FirebaseManager", "Cloudinary rejected request: ${response.code} - $errorDetail")
                                if (response.code in 400..499) {
                                    continuation.resumeWithException(Exception("Cloudinary config error (${response.code}). Check your preset settings."))
                                } else {
                                    continuation.resumeWithException(IOException("Cloudinary server error: ${response.code}"))
                                }
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("FirebaseManager", "Cloudinary upload attempt $attempt failed: ${e.message}")
                if (attempt < 2) delay(1000)
            }
        }
        throw lastException ?: Exception("Cloudinary upload failed")
    }

    // --- Case Management ---

    suspend fun submitCase(patientCase: PatientCase) {
        try {
            ensureAuth()
            // We use the object directly to include all fields, respect PropertyName annotations
            firestore.collection(CASES_COLLECTION).add(patientCase).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error submitting case to Firestore", e)
            throw e
        }
    }

    fun getAllCases(): Flow<List<PatientCase>> = callbackFlow {
        val subscription = firestore.collection(CASES_COLLECTION)
            .orderBy("visitDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val cases = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(PatientCase::class.java)?.apply { docId = doc.id }
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(cases)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateCase(caseId: String, updates: Map<String, Any>) {
        try {
            ensureAuth()
            firestore.collection(CASES_COLLECTION).document(caseId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating case", e)
            throw e
        }
    }

    // --- Audit Logs ---

    suspend fun logAction(log: AuditLog) {
        try {
            ensureAuth()
            firestore.collection(LOGS_COLLECTION).add(log).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error logging action", e)
        }
    }

    fun getAllLogs(): Flow<List<AuditLog>> = callbackFlow {
        val subscription = firestore.collection(LOGS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(AuditLog::class.java) ?: emptyList()
                trySend(logs)
            }
        awaitClose { subscription.remove() }
    }
}
