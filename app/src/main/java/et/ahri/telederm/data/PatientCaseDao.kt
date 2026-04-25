package et.ahri.telederm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PatientCaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(patientCase: PatientCase)

    @Update
    suspend fun updateCase(patientCase: PatientCase)

    @Query("SELECT * FROM patient_cases ORDER BY visitDate DESC")
    suspend fun getAllCases(): List<PatientCase>

    @Query("SELECT * FROM patient_cases WHERE patientId = :patientId LIMIT 1")
    suspend fun getCaseById(patientId: String): PatientCase?
}
