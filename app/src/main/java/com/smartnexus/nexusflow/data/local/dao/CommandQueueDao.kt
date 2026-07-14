package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.QueuedCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandQueueDao {
    @Query("SELECT * FROM command_queue ORDER BY queued_at ASC")
    fun getQueuedCommandsFlow(): Flow<List<QueuedCommandEntity>>

    @Query("SELECT * FROM command_queue ORDER BY queued_at ASC")
    fun getQueuedCommands(): List<QueuedCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCommand(command: QueuedCommandEntity): Long

    @Delete
    fun deleteCommand(command: QueuedCommandEntity): Int

    @Query("DELETE FROM command_queue WHERE id = :id")
    fun deleteCommandById(id: String): Int
}
