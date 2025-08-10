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
    suspend fun insert(request: TransferRequestEntity)

    @Query("UPDATE transfer_requests SET status = :status WHERE requestNumber = :requestNumber")
    suspend fun updateStatus(requestNumber: Int, status: RequestStatus)

    @Query("UPDATE transfer_requests SET driverId = :driverId WHERE requestNumber = :requestNumber")
    suspend fun assignDriver(requestNumber: Int, driverId: String)

    @Query("SELECT * FROM transfer_requests WHERE passengerId = :passengerId")
    fun getRequestsForPassenger(passengerId: String): Flow<List<TransferRequestEntity>>

    @Query("SELECT * FROM transfer_requests WHERE driverId = :driverId")
    fun getRequestsForDriver(driverId: String): Flow<List<TransferRequestEntity>>
}
