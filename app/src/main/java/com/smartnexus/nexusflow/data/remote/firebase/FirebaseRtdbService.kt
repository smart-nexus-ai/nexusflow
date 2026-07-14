package com.smartnexus.nexusflow.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import com.smartnexus.nexusflow.data.local.entity.AppConfigEntity

@Singleton
class FirebaseRtdbService @Inject constructor() {
    private val db: FirebaseDatabase get() = FirebaseDatabase.getInstance()

    fun observeAppConfig(): Flow<AppConfigEntity?> = callbackFlow {
        val ref = db.getReference("app_config")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val maintenanceMode = snapshot.child("maintenance_mode").getValue(Boolean::class.java) ?: false
                    val maintenanceMessage = snapshot.child("maintenance_message").getValue(String::class.java)
                    val latestVersionCode = snapshot.child("latest_version_code").getValue(Int::class.java) ?: 1
                    val updateType = snapshot.child("update_type").getValue(String::class.java)
                    val termsUrl = snapshot.child("terms_url").getValue(String::class.java)
                    val entity = AppConfigEntity(
                        id = 1,
                        maintenanceMode = maintenanceMode,
                        maintenanceMessage = maintenanceMessage,
                        latestVersionCode = latestVersionCode,
                        updateType = updateType,
                        termsUrl = termsUrl,
                        fetchedAt = System.currentTimeMillis()
                    )
                    trySend(entity)
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeUserDevices(userId: String): Flow<DataSnapshot> = callbackFlow {
        val ref = db.getReference("users/$userId/devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun updateRelayState(userId: String, deviceId: String, relayId: String, isOn: Boolean) {
        val ref = db.getReference("users/$userId/devices/$deviceId/relays/$relayId/isOn")
        ref.setValue(isOn).await()
    }

    suspend fun updateDeviceLastSeen(userId: String, deviceId: String, lastSeen: Long) {
        val ref = db.getReference("users/$userId/devices/$deviceId/lastSeen")
        ref.setValue(lastSeen).await()
    }
}
