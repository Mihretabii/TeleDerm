package et.ahri.telederm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import et.ahri.telederm.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FollowUpWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val caseDao = database.patientCaseDao()
        val cases = caseDao.getAllCases().first()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        cases.forEach { patientCase ->
            // Automated reminders for follow-up (30, 60, 90, 180 days)
            // Reminders are relevant for both Dermatologists and Health Workers
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
                                patientCase.id
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return Result.success()
    }
}
