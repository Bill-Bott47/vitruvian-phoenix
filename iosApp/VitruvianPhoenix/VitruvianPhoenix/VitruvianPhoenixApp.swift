import SwiftUI
import shared

@main
struct VitruvianPhoenixApp: App {

    init() {
        // Initialize Koin for dependency injection
        KoinInitKt.doInitKoin()

        // Run migrations after Koin is initialized (mirrors Android VitruvianApp.onCreate())
        KoinInitKt.runMigrations()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
