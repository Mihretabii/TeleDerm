package et.ahri.telederm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FollowUpWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val firebaseManager = FirebaseManager
            // Fetch cases directly from Firebase for follow-up reminders
            val cases = firebaseManager.getAllCases().first()

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance()

            cases.forEach { patientCase ->
                if (patientCase.status == "Reviewed" || patientCase.status == "Referred") {
                    try {
                        val visitDateStr = patientCase.visitDate
                        val visitDate = sdf.parse(visitDateStr)
                        if (visitDate != null) {
                            val diffInMillis = today.timeInMillis - visitDate.time
                            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                            val intervals = listOf(30, 60, 90, 180)
                            if (intervals.contains(diffInDays)) {
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    "Follow-up Reminder",
                                    "Case #${patientCase.patientId} reached Day $diffInDays. Please review patient progress.",
                                    patientCase.patientId.hashCode() // Use patientId hash since we don't use Room ID anymore
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FollowUpWorker", "Error processing case #${patientCase.patientId}", e)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("FollowUpWorker", "Worker execution failed", e)
            Result.retry()
        }
    }
}
