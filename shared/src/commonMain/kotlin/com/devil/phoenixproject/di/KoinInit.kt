package com.devil.phoenixproject.di

import com.devil.phoenixproject.data.migration.MigrationManager
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(commonModule, platformModule)
}

/**
 * Helper function for iOS that doesn't require lambda parameter.
 * Call this from Swift: KoinKt.doInitKoin()
 */
fun doInitKoin() = initKoin {}

/**
 * Helper function for iOS to run migrations after Koin initialization.
 * Call this from Swift: KoinKt.runMigrations()
 * This mirrors Android's VitruvianApp.onCreate() migration call.
 */
fun runMigrations() {
    try {
        val koin = GlobalContext.get()
        val migrationManager = koin.get<MigrationManager>()
        migrationManager.checkAndRunMigrations()
    } catch (e: Exception) {
        // Log error but don't crash - migrations are best effort
        co.touchlab.kermit.Logger.e(e) { "Failed to run migrations on iOS" }
    }
}
