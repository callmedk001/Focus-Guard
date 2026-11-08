# DK FOCUS GUARD — PHASE 11: PHYSICAL DEVICE VALIDATION & RELEASE READINESS MANUAL
**Target Device**: Oppo A3x 5G (`CPH2681`)  
**Operating System**: Android 14 (API 34) / ColorOS 14.0.1  
**Package**: `com.dk.focusguard`  
**Application Version**: `1.0.0` (VersionCode `1`)  
**APK Build Target**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 1. DEVICE ONBOARDING & ADB HARNESS SETUP

### Step 1.1: Enable Developer Options on ColorOS 14
1. On your Oppo A3x 5G, open **Settings**.
2. Scroll to the bottom and select **About device**.
3. Tap **Version** (Radio & kernel version).
4. Tap **Version No.** rapidly **7 times** until the toast banner announces: *"You are now in Developer mode!"*
5. If prompted, enter your device lockscreen PIN.

### Step 1.2: Enable USB Debugging
1. Return to **Settings** > **Additional settings** (or **System settings**).
2. Tap **Developer options**.
3. Toggle **USB debugging** to **ON**.
4. (Optional but recommended for automated permission checks): Toggle **Disable permission monitoring** to **ON** if you wish to run extended ADB shell configuration commands.

### Step 1.3: Connect Phone to Windows & Authorize
1. Connect your Oppo phone to your PC via a USB cable.
2. When prompted on the phone, choose **File Transfer / Android Auto** mode.
3. A popup will appear on your phone: *"Allow USB debugging?"*
4. Check **Always allow from this computer** and tap **Allow**.

### Step 1.4: Verify ADB Connection (PowerShell / Command Prompt)
Run the following copy-paste command in PowerShell:
```powershell
adb devices -l
```
**Expected Output**:
```text
List of devices attached
<device-serial>       device product:CPH2681 model:CPH2681 device:OP5B1CL1
```

---

## 2. INSTALLATION & COLOROS HARDENING

### Step 2.1: Install the Latest Build via ADB
```powershell
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```
**Expected Output**:
```text
Performing Streamed Install
Success
```

### Step 2.2: Launch the Application
```powershell
adb shell "am start -n com.dk.focusguard/.MainActivity"
```

### Step 2.3: Configure ColorOS 14 Battery & Auto-Launch Settings (CRITICAL)
Oppo's ColorOS terminates background processes aggressively. Configure the following to ensure daemon persistence:
1. Open **Settings** > **Apps** > **App management** > **DK FOCUS GUARD**.
2. Tap **Battery usage**:
   - Enable **Allow foreground activity**.
   - Enable **Allow background activity**.
   - Enable **Allow auto-launch**.
3. Tap **More settings** > **Battery optimization** (if prompted) and set to **Don't optimize**.
4. **Lock App in Recent Tasks**:
   - Swipe up from the bottom of the screen to reveal the Recent Apps carousel.
   - Tap the three dots (or pull down on the DK FOCUS GUARD card).
   - Tap **Lock**. A small padlock icon will appear on the card, preventing ColorOS from sweeping the process during memory cleanup.

---

## 3. PERMISSIONS & SYSTEM PRIVACY CONFIGURATION

### Step 3.1: Grant Accessibility Service Permission
1. Open phone **Settings** > **Additional settings** > **Accessibility**.
2. Tap **Downloaded apps** (or **Installed services**).
3. Tap **DK Focus Guard**.
4. Toggle the switch to **ON**.
5. When the ColorOS system security warning appears, tap **Allow**.

### Step 3.2: Grant VPN Consent
1. Open **DK FOCUS GUARD**.
2. On the Dashboard or Protection screen, tap **Enable Protection** (or toggle **Adult Website Protection** to ARMED).
3. The Android system dialog appears: *"Connection request: DK FOCUS GUARD wants to set up a VPN connection that allows it to monitor network traffic..."*
4. Tap **OK**. (A key icon will appear in your status bar).

### Step 3.3: Set Private DNS to "Off" (CRITICAL FOR DNS FILTERING)
Android 14 routes DNS-over-TLS (DoT, port 853) directly to cloud providers if Private DNS is set to Automatic. To allow local UDP port 53 DNS filtering:
1. Go to **Settings** > **Connection & sharing** > **Private DNS**.
2. Select **Off** (instead of Automatic or Designated Private DNS).

---

## 4. PHYSICAL DEVICE TEST PROCEDURES

### Test Suite 1: YouTube Shorts Shield
*Device status: `com.google.android.youtube` is installed on your device.*

#### Scenario 1.1: Shorts Interception
1. Open DK FOCUS GUARD, verify **Protection Active** (or Shorts switch is ARMED).
2. Open the **YouTube** app.
3. Tap the **Shorts** tab on the bottom navigation bar.
4. **Expected Result**: Within 1–2 seconds, the Accessibility Engine intercepts the Shorts container, triggers a system back action returning you to the previous screen, and displays the **CYBER INTERRUPT** overlay.
5. In DK FOCUS GUARD Dashboard, the **Blocked** counter increments by 1.

#### Scenario 1.2: Normal Video Playback Regression
1. Open YouTube, search for any video (e.g. "Tech News").
2. Play the video in standard widescreen mode.
3. Pause, play, scrub, open comments, view channel description.
4. **Expected Result**: NO interruption. Playback is completely smooth.

---

### Test Suite 2: Instagram Reels Guard
*Device status: `com.instagram.android` must be installed from Google Play Store.*

#### Scenario 2.1: Reels Tab Interception
1. Install Instagram from Google Play Store and log in.
2. In DK FOCUS GUARD, verify **Instagram Reels** switch is ARMED.
3. Open Instagram and tap the center **Reels** tab.
4. **Expected Result**: Reels container is intercepted, back navigation fires, and the **CYBER INTERRUPT** overlay appears.
5. In DK FOCUS GUARD Dashboard, the **Blocked** counter increments by 1.

#### Scenario 2.2: Direct Messages (DMs), Stories, and Feed Posts
1. Open Instagram Direct Messages (Inbox) and view chats.
2. View friend Stories from the top carousel.
3. Scroll normal photo posts on the home feed.
4. **Expected Result**: NO interruption. DMs, Stories, and photos remain 100% accessible.

---

### Test Suite 3: Local VPN Adult Website Filtering & Safe Search

> [!NOTE]
> Safe testing protocol: Do NOT visit explicit adult websites. Use safe non-explicit test domains or the Custom Domains feature.

#### Scenario 3.1: Custom Domain Filtering (Safe Test)
1. In DK FOCUS GUARD > **Protection** screen, tap **Manage Custom Domains**.
2. Add `example-blocked-test.com` and tap **Add Domain**.
3. Open **Google Chrome** on your phone and browse to `http://example-blocked-test.com`.
4. **Expected Result**: The connection fails immediately with `ERR_NAME_NOT_RESOLVED` or `ERR_CONNECTION_REFUSED` (0.0.0.0 sinkhole). The browser cannot resolve the domain.
5. In DK FOCUS GUARD, the **Blocked** telemetry counter increments.

#### Scenario 3.2: Critical Google Services Regression Test
1. Ensure the VPN filter is ACTIVE.
2. Open **Gmail**: verify incoming messages sync and attachments load.
3. Open **Google Drive**: verify files and folders display.
4. Open **Google Play Store**: browse and update apps.
5. **Expected Result**: All Google services load normally without SSL handshake errors.

#### Scenario 3.3: Normal Web Browsing
1. Open Chrome and visit:
   - `https://en.wikipedia.org`
   - `https://github.com`
   - `https://stackoverflow.com`
2. **Expected Result**: All pages load rapidly and normally through the protected upstream resolver (`1.1.1.1`).

---

### Test Suite 4: Automated Focus Schedules & Overnight Enforcement

#### Scenario 4.1: Immediate Schedule Window
1. Open DK FOCUS GUARD > **Focus** screen.
2. Note current device time (e.g. 14:30).
3. Set **Start Time** to 2 minutes in the future (e.g. 14:32) and **End Time** to 5 minutes in the future (e.g. 14:35).
4. Tap **Save Defense Schedule**.
5. Lock the phone. Wait until 14:32.
6. Unlock phone and open DK FOCUS GUARD.
7. **Expected Result**: Dashboard displays **SCHEDULE ACTIVE** (Cyan badge). Persistent notification displays `DK FOCUS GUARD — SCHEDULE ACTIVE`. Defenses are actively armed.
8. Wait until 14:35.
9. **Expected Result**: Protection automatically transitions out of scheduled mode and restores manual preferences.

---

### Test Suite 5: Emergency Defense Override

#### Scenario 5.1: 5-Minute Emergency Override
1. In DK FOCUS GUARD, tap the Emergency Override trigger (or Disarm in Strict Mode).
2. Enter your secure PIN.
3. Select **5 Minutes** duration and confirm.
4. **Expected Result**:
   - Dashboard reflects **OVERRIDE ACTIVE** (Yellow badge).
   - Notification displays `DK FOCUS GUARD — OVERRIDE ACTIVE (5m remaining)`.
   - YouTube Shorts and Instagram Reels are temporarily permitted without interruption.
5. Wait for the 5-minute timer to elapse (or tap **Cancel Override**).
6. **Expected Result**: Override automatically expires, defenses re-arm, and notification restores to `DEFENSE ARMED`.

---

### Test Suite 6: Device Reboot & Service Recovery

1. Ensure protection is ARMED or a focus schedule is active.
2. Restart your Oppo A3x 5G.
3. Unlock the device after boot.
4. **Expected Result**:
   - `BootRecoveryReceiver` receives `ACTION_BOOT_COMPLETED`.
   - Protection state is re-evaluated against saved database preferences.
   - Persistent notification appears in notification shade.
   - Local VPN restarts cleanly if adult filtering was armed.
   - No crash dialogs, no battery warning, no infinite restart loops.

---

## 5. REAL-TIME LOGGING & DIAGNOSTICS WITH ADB

Run the following commands to monitor real-time diagnostic output:

### Stream All DK Focus Guard Logs
```powershell
adb logcat -v time -s FocusGuardVpnService:V FocusGuardAccessibilityService:V UnifiedProtectionManager:V YouTubeInterceptionEngine:V InstagramReelsInterceptionEngine:V BootRecoveryReceiver:V
```

### Inspect Live Local Database Block Events
```powershell
adb shell "run-as com.dk.focusguard sqlite3 /data/data/com.dk.focusguard/databases/dk_focus_guard_database 'SELECT id, category, eventStatus, datetime(timestamp/1000, \"unixepoch\", \"localtime\") FROM block_events ORDER BY id DESC LIMIT 10;'"
```

### Check Process State & Memory Usage
```powershell
adb shell "dumpsys meminfo com.dk.focusguard | grep -E 'TOTAL|Native Heap|Dalvik Heap'"
```

