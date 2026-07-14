package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getAppConfigFlow(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getAppConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppConfig(config: AppConfigEntity): Long
}
