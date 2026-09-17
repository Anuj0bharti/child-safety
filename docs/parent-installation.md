# Parent Android Application Installation Guide

## 1. Prerequisites & Device Compatibility
* **Supported Platform**: Android 8.0 (API Level 26) through Android 15 (API Level 35)
* **Tested Form Factors**: Android smartphones, Android Virtual Devices (AVD Pixel 7/8)

---

## 2. Compiling the APK
From the `parent-app` directory:
```powershell
cd parent-app
.\gradlew.bat assembleDebug
```
The compiled APK will be at:
`parent-app/app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Installing on Android Phone or Emulator

### Method A: Direct ADB Installation
Ensure USB debugging is enabled on your phone:
```bash
adb devices
adb install -r parent-app/app/build/outputs/apk/debug/app-debug.apk
```

### Method B: Emulator Installation
If running the `Pixel_7` emulator:
```bash
adb -s emulator-5554 install -r parent-app/app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. First-Time Setup Workflow

1. **Launch App**: Open **Child Safety Parent** on your smartphone.
2. **Account Creation**:
   * Click **New parent? Create Account**.
   * Enter your name, email, phone number, and password.
   * Click **Register**.
3. **Child Registration**:
   * From the dashboard, click **Add Default Child Profile** or register a new child name and age.
4. **Pair Smartwatch**:
   * Click **Pair Watch**.
   * Enter the 6-character pairing code shown on the child's Wear OS watch.
   * Click **Pair Watch With Child**.
5. **Set Up Safe Route Corridor**:
   * Click **Create Route**.
   * Give the route a name (e.g., "Home to School").
   * Choose the safety corridor buffer width (50m, 100m, or 200m).
   * Click **Save & Activate Safe Route**.
6. **Monitor Live Safety**:
   * Click **View Live Map & Route** to monitor real-time GPS locations, route adherence, battery percentage, and incoming warnings.

