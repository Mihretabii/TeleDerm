package et.ahri.telederm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) 
    @ColumnInfo(name = "id") 
    @get:Exclude @set:Exclude
    var id: Int = 0,

    @ColumnInfo(name = "userEmail") 
    @get:PropertyName("userEmail") @set:PropertyName("userEmail")
    var userEmail: String = "",

    @ColumnInfo(name = "action") 
    @get:PropertyName("action") @set:PropertyName("action")
    var action: String = "",

    @ColumnInfo(name = "details") 
    @get:PropertyName("details") @set:PropertyName("details")
    var details: String = "",

    @ColumnInfo(name = "timestamp") 
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)
