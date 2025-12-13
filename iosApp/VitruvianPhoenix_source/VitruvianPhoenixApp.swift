import SwiftUI
import shared

@main
struct VitruvianPhoenixApp: App {

    init() {
        // Initialize Koin for dependency injection
        KoinKt.doInitKoin()
        
        // Run migrations after Koin is initialized (mirrors Android VitruvianApp.onCreate())
        KoinKt.runMigrations()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
