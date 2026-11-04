package com.jmtube.app.sync2.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.jmtube.app.data.profile.ProfileFiles
import com.jmtube.app.data.profile.StoreScope
import com.jmtube.app.sync2.store.LocalStore
import java.io.File
import javax.inject.Singleton

/** One copy per device, shared by every profile: the group belongs to the device, not to a profile. */
@Module
@InstallIn(SingletonComponent::class)
object LinkedDatabaseModule {
    @Provides
    @Singleton
    fun provideLinkedDatabase(
        @ApplicationContext context: Context,
    ): LinkedDatabase =
        Room.databaseBuilder(context, LinkedDatabase::class.java, "linked_devices")
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideLocalStore(
        @ApplicationContext context: Context,
        db: LinkedDatabase,
    ): LocalStore = LocalStore(store(context), db.held())

    @Provides
    @Singleton
    fun provideKeptBatches(
        @ApplicationContext context: Context,
        db: LinkedDatabase,
    ): KeptBatches = KeptBatches(File(store(context), "kept"), db.profiles())

    private fun store(context: Context) = ProfileFiles.file(context, StoreScope.DEVICE, "linked_store")
}
