package et.ahri.telederm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "fullName") val fullName: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "passwordHash") val passwordHash: String,
    @ColumnInfo(name = "role") val role: String, // "health_worker", "dermatologist", "admin"
    @ColumnInfo(name = "isApproved") val isApproved: Boolean = false
)
