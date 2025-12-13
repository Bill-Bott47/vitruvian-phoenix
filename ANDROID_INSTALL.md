# Android Installation Guide

This guide explains how to install Vitruvian Phoenix on your Android device.

## Prerequisites

- Android phone or tablet running Android 8.0 (Oreo) or later
- Bluetooth Low Energy (BLE) support (most devices from 2015+ have this)
- The `androidApp-debug.apk` file from [GitHub Releases](../../releases)

---

## Installation Steps

### Step 1: Download the APK

1. On your Android device, go to our [GitHub Releases](../../releases) page
2. Download the latest `androidApp-debug.apk` file
3. You may see a warning about downloading APK files - tap **Download anyway**

### Step 2: Enable Installation from Unknown Sources

Android requires permission to install apps from outside the Play Store.

**Android 8.0 and later:**
1. When you try to open the APK, Android will prompt you to allow installation
2. Tap **Settings** when prompted
3. Enable **Allow from this source** for your browser or file manager
4. Go back and try opening the APK again

**Or manually:**
1. Go to **Settings → Apps → Special app access → Install unknown apps**
2. Select your browser (Chrome, Firefox, etc.) or file manager
3. Enable **Allow from this source**

### Step 3: Install the App

1. Open the downloaded `androidApp-debug.apk` file
2. Tap **Install**
3. Wait for installation to complete
4. Tap **Open** or find **Vitruvian Phoenix** in your app drawer

---

## Permissions

When you first launch the app, you'll be asked to grant permissions:

### Bluetooth Permissions
- **Nearby devices** - Required to scan for and connect to your Vitruvian trainer
- Tap **Allow** when prompted

### Location Permission
- On Android 11 and below, Bluetooth scanning requires location permission
- This is an Android requirement - the app does not track your location
- Tap **Allow** when prompted

**Note:** If you deny permissions, the app cannot connect to your trainer. You can always grant permissions later in Settings → Apps → Vitruvian Phoenix → Permissions.

---

## Updating the App

When a new version is released:

1. Download the new APK from GitHub Releases
2. Open and install it - Android will update the existing app
3. **Your workout data is preserved** during updates

---

## Troubleshooting

### "App not installed" Error

- Check that you have enough storage space
- Uninstall any previous version and try again
- Make sure the APK downloaded completely (check file size matches the release)

### Can't Find the Trainer

- Ensure Bluetooth is enabled on your device
- Make sure you granted Bluetooth/Nearby devices permission
- On Android 11 and below, ensure Location is enabled (required for BLE scanning)
- Move closer to your Vitruvian trainer
- Try turning your trainer off and on again

### App Crashes on Launch

- Make sure your device is running Android 8.0 or later
- Try clearing app data: Settings → Apps → Vitruvian Phoenix → Storage → Clear data
- Report the issue on GitHub with your device model and Android version

### Bluetooth Permission Denied

1. Go to **Settings → Apps → Vitruvian Phoenix → Permissions**
2. Enable **Nearby devices** (Android 12+) or **Location** (Android 11 and below)
3. Restart the app

---

## Data & Privacy

- All workout data is stored **locally on your device**
- Location permission is only used for Bluetooth scanning (Android requirement) - we don't track your location
- No data is sent to any server
- Uninstalling the app will delete your workout history

### Backup Your Data

Currently, workout data is stored in the app's internal database. If you want to preserve your data:

- Don't uninstall the app when updating - install over the existing version
- Android's built-in backup may include app data (varies by device)

---

## FAQ

**Q: Why isn't this on the Play Store?**
A: This is a community project to keep Vitruvian trainers functional. We're distributing directly to users who need it.

**Q: Is it safe to install APKs?**
A: APKs from trusted sources are safe. Our releases are built from the open-source code in this repository. You can verify or build it yourself.

**Q: Will this work on my tablet?**
A: Yes, as long as it has Bluetooth Low Energy support and runs Android 8.0+.

**Q: Why does it need location permission?**
A: Android requires location permission for Bluetooth scanning on Android 11 and below. This is a platform limitation, not something we can change. The app never accesses your actual location.

**Q: What Vitruvian devices are supported?**
A:
- Vitruvian V-Form Trainer (VIT-200) - devices starting with `Vee_`
- Vitruvian Trainer+ - devices starting with `VIT`

---

## Need Help?

If you run into issues, please open an issue on our [GitHub repository](../../issues).
