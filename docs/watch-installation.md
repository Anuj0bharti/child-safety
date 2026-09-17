# Wear OS Watch Installation Guide

## 1. Target Device & Compatibility
* **Primary Target**: Samsung Galaxy Watch4 LTE 40mm (Wear OS 3.0+ / 4.0 / 5.0)
* **General Compatibility**: Any Wear OS smartwatch running Android API 30+ equipped with location and sensor hardware.

---

## 2. Building the APK
Build the release or debug APK from the root directory:
```powershell
cd watch-app
.\gradlew.bat assembleDebug
```
The output APK is located at:
`watch-app/app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Sideloading onto Samsung Galaxy Watch4

### 3.1 Enable Developer Options on Galaxy Watch4:
1. On the watch, navigate to **Settings** $\rightarrow$ **About watch** $\rightarrow$ **Software info**.
2. Tap **Software version** 7 times continuously until the prompt *"Developer mode turned on"* appears.
3. Return to **Settings** $\rightarrow$ **Developer options**.

### 3.2 Enable Wireless Debugging:
1. In **Developer options**, enable **ADB debugging**.
2. Connect both your PC and Galaxy Watch4 to the **same Wi-Fi network**.
3. Under **Developer options**, scroll to **Wireless debugging** and toggle it **ON**.
4. Note the displayed IP address and port (e.g. `192.168.1.105:5555` or pair port).

### 3.3 Connect via ADB:
From PowerShell or terminal:
```bash
adb connect 192.168.1.105:5555
```
Accept the *"Allow debugging?"* prompt on the watch face.

### 3.4 Install the Watch APK:
```bash
adb install -r watch-app/app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. First-Time Configuration & Permissions
1. Launch the **Child Safety** app from the watch app drawer.
2. Grant all requested permissions when prompted:
   * **Location**: Select *"While using app"* $\rightarrow$ *"Allow all the time"* in app info.
   * **Body Sensors**: Select *"Allow"*.
   * **Notifications**: Select *"Allow"*.
3. Open the **Diagnostics** screen from the bottom chip to verify:
   * GPS: `AVAILABLE`
   * Accelerometer: `AVAILABLE`
   * Gyroscope: `AVAILABLE`
   * Battery: `AVAILABLE`
   * Cellular/LTE: `CONNECTED` or `PHYSICAL TEST REQ`

---

## 5. Pairing Watch with Parent App
1. On the watch home screen, take note of the device identifier (`WATCH-001`) and 6-character pairing code.
2. Open the **Parent Android App** $\rightarrow$ Select Child $\rightarrow$ **Pair Watch**.
3. Enter the pairing code to link the smartwatch.

