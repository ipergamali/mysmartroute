package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus

@Dao
interface TransferRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: TransferRequestEntity): Long

    @Query("SELECT * FROM transfer_requests")
    fun getAll(): Flow<List<TransferRequestEntity>>

    @Query("UPDATE transfer_requests SET status = :status WHERE requestNumber = :requestNumber")
    suspend fun updateStatus(requestNumber: Int, status: RequestStatus)

    @Query("UPDATE transfer_requests SET driverId = :driverId, status = :status WHERE requestNumber = :requestNumber")
    suspend fun assignDriver(requestNumber: Int, driverId: String, status: RequestStatus)

    @Query("UPDATE transfer_requests SET firebaseId = :firebaseId WHERE requestNumber = :requestNumber")
    suspend fun setFirebaseId(requestNumber: Int, firebaseId: String)

    @Query("SELECT * FROM transfer_requests WHERE requestNumber = :requestNumber")
    suspend fun getRequestByNumber(requestNumber: Int): TransferRequestEntity?

    @Query("SELECT * FROM transfer_requests WHERE passengerId = :passengerId")
    fun getRequestsForPassenger(passengerId: String): Flow<List<TransferRequestEntity>>

    @Query("SELECT * FROM transfer_requests WHERE driverId = :driverId")
    fun getRequestsForDriver(driverId: String): Flow<List<TransferRequestEntity>>

    @Query("DELETE FROM transfer_requests WHERE driverId = :driverId")
    suspend fun deleteForDriver(driverId: String)

    @Query("DELETE FROM transfer_requests WHERE passengerId = :passengerId")
    suspend fun deleteForPassenger(passengerId: String)
}
