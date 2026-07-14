package com.smartnexus.nexusflow.app.di

import android.content.Context
import androidx.room.Room
import com.smartnexus.nexusflow.data.local.NexusFlowDatabase
import com.smartnexus.nexusflow.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NexusFlowDatabase {
        return Room.databaseBuilder(
            context,
            NexusFlowDatabase::class.java,
            "nexusflow_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideAppConfigDao(db: NexusFlowDatabase): AppConfigDao = db.appConfigDao()

    @Provides
    @Singleton
    fun provideDeviceDao(db: NexusFlowDatabase): DeviceDao = db.deviceDao()

    @Provides
    @Singleton
    fun provideRelayStateDao(db: NexusFlowDatabase): RelayStateDao = db.relayStateDao()

    @Provides
    @Singleton
    fun provideScheduleDao(db: NexusFlowDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    @Singleton
    fun provideSceneDao(db: NexusFlowDatabase): SceneDao = db.sceneDao()

    @Provides
    @Singleton
    fun provideAutomationRuleDao(db: NexusFlowDatabase): AutomationRuleDao = db.automationRuleDao()

    @Provides
    @Singleton
    fun provideCommandQueueDao(db: NexusFlowDatabase): CommandQueueDao = db.commandQueueDao()

    @Provides
    @Singleton
    fun provideRelayRuntimeDao(db: NexusFlowDatabase): RelayRuntimeDao = db.relayRuntimeDao()

    @Provides
    @Singleton
    fun provideBillingCycleDao(db: NexusFlowDatabase): BillingCycleDao = db.billingCycleDao()
}
