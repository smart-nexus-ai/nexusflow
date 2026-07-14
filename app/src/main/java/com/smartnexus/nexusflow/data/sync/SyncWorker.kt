package com.smartnexus.nexusflow.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val commandQueueProcessor: CommandQueueProcessor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val drainSuccess = commandQueueProcessor.processQueue()
            if (drainSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
