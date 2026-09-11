package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.SyncQueueItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueItem(item: SyncQueueItemEntity)

    @Update
    suspend fun updateItem(item: SyncQueueItemEntity)

    @Query("SELECT * FROM sync_queue WHERE profileId = :profileId AND status = :status ORDER BY createdAt ASC")
    fun getItemsByStatus(profileId: String, status: String): Flow<List<SyncQueueItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllQueueItems(profileId: String): Flow<List<SyncQueueItemEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE profileId = :profileId AND status = 'PENDING'")
    fun getPendingCount(profileId: String): Flow<Int>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM sync_queue WHERE profileId = :profileId AND status = 'SYNCED'")
    suspend fun clearSyncedItems(profileId: String)
}
