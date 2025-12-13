import SwiftUI
import shared

/// Main ContentView that hosts the Compose Multiplatform UI
struct ContentView: View {
    var body: some View {
        ComposeView()
            // Ignore safe area - Compose Multiplatform handles safe areas internally
            // This allows the Compose UI to extend edge-to-edge like Android
            .ignoresSafeArea(.all)
    }
}

/// UIViewControllerRepresentable wrapper for Compose Multiplatform
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed - Compose handles its own state and safe area insets
    }
}

#Preview {
    ContentView()
}
