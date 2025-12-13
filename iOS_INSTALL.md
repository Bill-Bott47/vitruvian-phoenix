# iOS Installation Guide

This guide explains how to install Vitruvian Phoenix on your iPhone without the App Store.

## Prerequisites

- iPhone running iOS 14.0 or later
- A computer (Mac or Windows PC)
- Apple ID (free)
- The `VitruvianPhoenix.ipa` file from [GitHub Releases](../../releases)

---

## Option 1: AltStore (Recommended)

AltStore is a free app that lets you sideload apps using your Apple ID.

### Step 1: Install AltServer on Your Computer

**Mac:**
1. Download AltServer from [altstore.io](https://altstore.io/)
2. Open the downloaded file and drag AltServer to your Applications folder
3. Launch AltServer - it will appear in your menu bar

**Windows:**
1. Download AltServer from [altstore.io](https://altstore.io/)
2. Run the installer
3. Launch AltServer - it will appear in your system tray

### Step 2: Install AltStore on Your iPhone

1. Connect your iPhone to your computer with a USB cable
2. **Trust** the computer on your iPhone if prompted
3. On Mac: Click the AltServer icon in the menu bar → Install AltStore → Select your iPhone
4. On Windows: Click the AltServer icon in the system tray → Install AltStore → Select your iPhone
5. Enter your Apple ID and password when prompted
6. AltStore will appear on your iPhone home screen

### Step 3: Install Vitruvian Phoenix

1. Download the `VitruvianPhoenix.ipa` file to your iPhone (via Safari, Files app, or AirDrop)
2. Open AltStore on your iPhone
3. Go to the **My Apps** tab
4. Tap the **+** button in the top left
5. Select the `VitruvianPhoenix.ipa` file
6. Wait for installation to complete

### Step 4: Trust the App

1. Go to **Settings → General → VPN & Device Management**
2. Tap your Apple ID under "Developer App"
3. Tap **Trust** and confirm

The app is now ready to use!

---

## Keeping the App Active (Important!)

Apps installed with a free Apple ID expire after **7 days**. To keep Vitruvian Phoenix working:

### Automatic Refresh (Recommended)

AltStore can refresh apps automatically in the background:

1. Keep AltServer running on your computer
2. Make sure your iPhone and computer are on the **same WiFi network**
3. AltStore will refresh apps automatically when conditions are right

**Tip:** Enable "Refresh All on WiFi" in AltStore settings.

### Manual Refresh

If automatic refresh isn't working:

1. Connect your iPhone to your computer via USB
2. Make sure AltServer is running
3. Open AltStore on your iPhone
4. Go to **My Apps** tab
5. Tap **Refresh All**

### What Happens If It Expires?

- The app icon will still be there but won't open
- **Your workout data is NOT deleted** - it's safely stored on your device
- Simply refresh via AltStore and everything will be restored

---

## Option 2: Sideloadly

Sideloadly is an alternative to AltStore with a simpler interface.

### Step 1: Install Sideloadly

1. Download from [sideloadly.io](https://sideloadly.io/)
2. Install and launch the application

### Step 2: Install the App

1. Connect your iPhone to your computer via USB
2. Open Sideloadly
3. Drag the `VitruvianPhoenix.ipa` file into Sideloadly (or click to browse)
4. Select your iPhone from the device dropdown
5. Enter your Apple ID and click **Start**
6. Enter your Apple ID password when prompted
7. Wait for installation to complete

### Step 3: Trust the App

1. Go to **Settings → General → VPN & Device Management**
2. Tap your Apple ID under "Developer App"
3. Tap **Trust** and confirm

### Refreshing with Sideloadly

Unlike AltStore, Sideloadly doesn't have automatic refresh. Every 7 days:

1. Connect your iPhone to your computer
2. Open Sideloadly
3. Re-install the IPA file (your data will be preserved)

---

## Troubleshooting

### "Unable to Install" Error

- Make sure your Apple ID doesn't have 2FA issues - try generating an app-specific password at [appleid.apple.com](https://appleid.apple.com/)
- Try a different Apple ID
- Ensure iTunes/Apple Devices is installed (Windows)

### App Crashes on Launch

- Make sure you trusted the developer certificate in Settings
- Try reinstalling the app

### AltStore Won't Refresh

- Ensure your iPhone and computer are on the same WiFi network
- Check that AltServer is running on your computer
- Try a manual refresh via USB connection

### "Your session has expired"

- Open AltStore and sign in again with your Apple ID
- This can happen if you changed your Apple ID password

---

## Data & Privacy

- All workout data is stored **locally on your device**
- Your Apple ID is only used by Apple's signing servers - we never see it
- Refreshing/reinstalling preserves all your data as long as you don't delete the app

---

## FAQ

**Q: Will I lose my workout history when I refresh?**
A: No. Refreshing re-signs the app without deleting data. Your history is safe.

**Q: Can I use a throwaway Apple ID?**
A: Yes. Any valid Apple ID works for sideloading.

**Q: What if I have a paid Apple Developer account?**
A: Apps will last 1 year instead of 7 days before needing refresh.

**Q: Is this legal?**
A: Yes. Sideloading apps you have legitimate access to is legal. Apple allows it.

---

## Need Help?

If you run into issues, please open an issue on our [GitHub repository](../../issues).
