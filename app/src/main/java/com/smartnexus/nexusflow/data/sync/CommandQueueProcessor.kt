package com.smartnexus.nexusflow.data.sync

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.smartnexus.nexusflow.data.local.dao.CommandQueueDao
import com.smartnexus.nexusflow.data.remote.firebase.FirebaseAuthService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandQueueProcessor @Inject constructor(
    private val commandQueueDao: CommandQueueDao,
    private val authService: FirebaseAuthService
) {
    suspend fun processQueue(): Boolean {
        val currentUser = authService.getCurrentUser() ?: return false
        val userId = currentUser.uid
        val commands = commandQueueDao.getQueuedCommands()
        if (commands.isEmpty()) return true

        Log.d("CommandQueueProcessor", "Draining ${commands.size} queued commands...")
        var allSuccess = true

        for (command in commands) {
            try {
                // Update remote RTDB state via commands/ path
                val timestamp = System.currentTimeMillis().toString()
                val commandPayload = mapOf(
                    "relay_id" to command.relayId,
                    "action" to command.action,
                    "source" to "app",
                    "executed" to false,
                    "created_at" to System.currentTimeMillis()
                )
                FirebaseDatabase.getInstance()
                    .getReference("commands/${command.deviceId}/$timestamp")
                    .setValue(commandPayload)
                    .await()

                // Delete from local Room queue on success
                commandQueueDao.deleteCommand(command)
                Log.d("CommandQueueProcessor", "Successfully drained command: ${command.id}")
            } catch (e: Exception) {
                Log.e("CommandQueueProcessor", "Failed to drain command: ${command.id}", e)
                allSuccess = false
            }
        }
        return allSuccess
    }
}
