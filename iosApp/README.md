# Vitruvian Phoenix - iOS App

iOS application for controlling Vitruvian Trainer workout machines via BLE.

## Prerequisites

- macOS with Xcode 15 or later
- iOS device with Bluetooth LE support (iOS 14+)
- CocoaPods (optional, for dependency management)

## Building the Shared Framework

Before opening in Xcode, build the shared Kotlin Multiplatform framework:

```bash
# From the project root directory
./gradlew :shared:assembleXCFramework
```

The framework will be built at:
`shared/build/XCFrameworks/release/shared.xcframework`

## Xcode Project Setup

### Option 1: Create New Xcode Project

1. Open Xcode and create a new iOS App project:
   - Product Name: `VitruvianPhoenix`
   - Team: Your development team
   - Organization Identifier: `com.example.vitruvianredux`
   - Interface: SwiftUI
   - Language: Swift

2. Add the shared framework:
   - In Xcode, select the project in the navigator
   - Select the target → General → Frameworks, Libraries, and Embedded Content
   - Click "+" and choose "Add Other" → "Add Files"
   - Navigate to `shared/build/XCFrameworks/release/shared.xcframework`
   - Ensure "Embed & Sign" is selected

3. Copy Swift files:
   - Copy `VitruvianPhoenix/VitruvianPhoenixApp.swift` to your project
   - Copy `VitruvianPhoenix/ContentView.swift` to your project
   - Copy `VitruvianPhoenix/Info.plist` to your project (or merge the keys)

### Option 2: Use the Provided Project Files

The `VitruvianPhoenix/` directory contains the Swift source files:

- `VitruvianPhoenixApp.swift` - App entry point with Koin initialization
- `ContentView.swift` - SwiftUI wrapper for Compose Multiplatform UI
- `Info.plist` - App configuration with BLE permissions

## Key Features

### Bluetooth Permissions

The `Info.plist` includes required BLE permission strings:
- `NSBluetoothAlwaysUsageDescription` - Required for BLE scanning/connection
- `NSBluetoothPeripheralUsageDescription` - Legacy iOS 12 support

### CoreBluetooth Integration

The iOS BLE implementation (`IosBleRepository.kt`) uses CoreBluetooth to:
- Scan for Vitruvian devices (names starting with "Vee_" or "VIT")
- Connect via Nordic UART Service (NUS)
- Parse real-time workout metrics
- Handle rep notifications

## Architecture

```
iosApp/
├── README.md                       # This file
└── VitruvianPhoenix/
    ├── VitruvianPhoenixApp.swift  # @main entry point
    ├── ContentView.swift          # Compose UI wrapper
    └── Info.plist                 # App configuration

shared/src/iosMain/
├── kotlin/com/example/vitruvianredux/
│   ├── MainViewController.kt      # Compose UI entry point
│   ├── Platform.ios.kt           # Platform info
│   ├── data/
│   │   ├── local/DriverFactory.ios.kt    # SQLDelight driver
│   │   └── repository/IosBleRepository.kt # CoreBluetooth BLE
│   ├── di/PlatformModule.ios.kt  # Koin DI module
│   ├── domain/model/
│   │   ├── PlatformUtils.ios.kt  # Time utilities
│   │   └── UUIDGeneration.ios.kt # UUID generation
│   ├── presentation/components/
│   │   ├── CompactNumberPicker.ios.kt   # Wheel picker
│   │   ├── HapticFeedbackEffect.ios.kt  # Haptic feedback
│   │   └── VideoPlayer.ios.kt           # Video player stub
│   └── util/CsvExporter.ios.kt   # CSV export & sharing
```

## Testing on Device

1. Connect your iOS device
2. Select your device as the run destination
3. Build and run from Xcode
4. Grant Bluetooth permission when prompted
5. The app will scan for Vitruvian devices

## Troubleshooting

### Framework Not Found

If you get "No such module 'shared'" error:
1. Rebuild the framework: `./gradlew :shared:assembleXCFramework`
2. Clean Xcode build folder: Product → Clean Build Folder
3. Verify framework path in Build Settings → Framework Search Paths

### Bluetooth Not Working

- Ensure "bluetooth-le" capability is in UIRequiredDeviceCapabilities
- Test on a real device (Simulator doesn't support BLE)
- Check that Bluetooth is enabled on the device

### Koin Initialization and Migrations

The app initializes Koin and runs migrations in `VitruvianPhoenixApp.init()`:
```swift
KoinKt.doInitKoin()
KoinKt.runMigrations()
```

This must be called before any Compose UI is rendered. Migrations run automatically on app startup, matching Android behavior.

### BLE Permission Handling

The app includes a BLE permission UI component (`RequireBlePermissions`) that:
- Shows guidance screens before BLE is first used
- Provides instructions for enabling permissions in Settings if denied
- Matches Android's permission handling UX

On iOS, CoreBluetooth automatically requests permission when BLE scanning starts, but the UI provides guidance beforehand.

### Background Execution

The app supports background BLE execution via `UIBackgroundModes` with `bluetooth-central` in `Info.plist`. This allows BLE connections to persist when the app is backgrounded, similar to Android's foreground service.

## Development Notes

- The Compose Multiplatform UI is identical across Android, iOS, and Desktop
- Platform-specific code is in `shared/src/iosMain/`
- All business logic is shared via the `shared` module
- The SwiftUI wrapper is minimal - just hosts the Compose view
