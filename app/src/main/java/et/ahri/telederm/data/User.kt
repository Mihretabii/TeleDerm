package et.ahri.telederm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
@Entity(tableName = "users")
data class User(
    @PrimaryKey 
    @ColumnInfo(name = "email")
    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @ColumnInfo(name = "fullName")
    @get:PropertyName("fullName")
    @set:PropertyName("fullName")
    var fullName: String = "",

    @ColumnInfo(name = "sex")
    @get:PropertyName("sex")
    @set:PropertyName("sex")
    var sex: String = "",

    @ColumnInfo(name = "passwordHash")
    @get:PropertyName("passwordHash")
    @set:PropertyName("passwordHash")
    var passwordHash: String = "",

    @ColumnInfo(name = "role")
    @get:PropertyName("role")
    @set:PropertyName("role")
    var role: String = "", // "health_worker", "dermatologist", "admin"

    @ColumnInfo(name = "isApproved")
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false
)
