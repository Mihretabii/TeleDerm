package et.ahri.telederm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<AuditLog>

    @Query("SELECT * FROM audit_logs WHERE userEmail = :email ORDER BY timestamp DESC")
    suspend fun getLogsByUser(email: String): List<AuditLog>
}
