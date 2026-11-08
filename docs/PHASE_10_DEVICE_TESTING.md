# DK FOCUS GUARD — PHASE 10: REAL DEVICE VALIDATION & TESTING GUIDE
**Target Device**: Oppo A3x 5G  
**OS**: Android 14 / ColorOS 14  
**Package**: `com.dk.focusguard`  
**Application Name**: DK FOCUS GUARD  
**Build Target**: Debug APK (`app-debug.apk`)

---

## 1. PRE-FLIGHT SETUP & INSTALLATION

### A. Installing via ADB
Connect your Oppo A3x 5G via USB with **USB Debugging** enabled in Developer Options:
```bash
# Verify connection
adb devices

# Install the debug build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### B. Oppo ColorOS 14 Specific Configuration (CRITICAL)
Oppo's ColorOS aggressively suspends background processes, which will terminate Accessibility and VPN daemons unless explicitly whitelisted:
1. **Allow Auto-Launch & Background Activity**:
   - Go to **Settings** > **Apps** > **App management** > **DK FOCUS GUARD** > **Battery usage**.
   - Enable **Allow foreground activity**, **Allow background activity**, and **Allow auto-launch**.
2. **Disable Battery Optimization**:
   - In **Battery usage**, ensure background optimization is set to **Don't optimize** (or "Unrestricted").
3. **Lock App in Recent Tasks**:
   - Open DK FOCUS GUARD, swipe up to view the Recent Tasks list.
   - Tap the three dots (or pull down on the app card) and tap **Lock**. A lock icon will appear on the card, preventing ColorOS task cleaner from killing the app process.

---

## 2. PERMISSIONS SETUP CHECKLIST

| Permission | How to Enable on ColorOS 14 | Why It's Needed |
| :--- | :--- | :--- |
| **Accessibility Service** | Settings > Additional settings > Accessibility > Downloaded apps > **DK Focus Guard** > Toggle **ON** | Detects active YouTube Shorts and Instagram Reels UI containers to trigger back navigation. |
| **VPN Service** | Prompted automatically inside the app when toggling Protection ON. Tap **OK** on system dialog. | Creates a local DNS filter interface (`10.0.0.2`) to sinkhole explicit domains. |
| **Notifications** | Prompted automatically on first launch (Android 13+). | Displays the persistent, low-priority defense status notification. |
| **Usage Access (Optional)** | Settings > Security & privacy > More security settings > Apps with usage access > **DK Focus Guard** > Allow | Tracks foreground usage metrics and application telemetry. |

> [!IMPORTANT]
> **Android 14 Private DNS Limitation**:
> Android 14 enables "Private DNS" (DNS-over-TLS on port 853) by default. If your network or device has Private DNS enabled, DNS queries will bypass the local port 53 UDP VPN loop!
> - Go to **Settings** > **Connection & sharing** > **Private DNS**.
> - Set Private DNS to **Off** during local DNS filtering tests.

---

## 3. TEST CASES & EXECUTION PROCEDURE

### Test Suite 1: YouTube Shorts Shield
*Goal: Intercept continuous vertical short-form scrolling while leaving normal widescreen videos, search, and home feed completely functional.*

#### Test 1.1: Shorts Tab Interception
1. Open DK FOCUS GUARD, verify **Protection Active** (or YouTube Shorts switch is ARMED).
2. Open the official **YouTube** app.
3. Tap the **Shorts** tab on the bottom navigation bar.
4. **Expected Result**: Within 1-2 seconds, the Accessibility Engine intercepts the Shorts container, executes a system back action to return to the previous screen, and displays the **CYBER INTERRUPT** overlay.
5. In DK FOCUS GUARD Dashboard, the **Blocked** counter increments by 1.

#### Test 1.2: Normal YouTube Video Regression Test
1. Open YouTube, search for any long-form video (e.g. "Android Kotlin Tutorial").
2. Tap the video to enter full-screen landscape or standard widescreen portrait playback.
3. Pause, play, scrub the timeline, open comments, and browse related videos.
4. **Expected Result**: NO interruption. Playback is completely smooth and uninterrupted.

#### Test 1.3: YouTube Home Feed & Channel Pages
1. Scroll through the normal YouTube Home feed.
2. Tap on any creator channel page and view playlists.
3. **Expected Result**: Content is fully browsable; classifier maintains confidence below threshold and does not block.

---

### Test Suite 2: Instagram Reels Guard
*Goal: Intercept continuous algorithmic Reels feed while preserving Direct Messages (DMs), regular feed photo/video posts, and Stories.*

#### Test 2.1: Reels Tab Interception
1. Open DK FOCUS GUARD, verify **Instagram Reels** switch is ARMED.
2. Open the **Instagram** app.
3. Tap the **Reels** center tab on the bottom navigation bar.
4. **Expected Result**: Accessibility engine detects `clips_viewer_container` / `clips_video_container`, triggers back navigation, and displays the **CYBER INTERRUPT** overlay.
5. In DK FOCUS GUARD Dashboard, the **Blocked** counter increments by 1.

#### Test 2.2: Direct Messages (DMs) Regression Test
1. In Instagram, open your **Direct Messages (Inbox)**.
2. Open a chat thread, read messages, and return to the inbox.
3. **Expected Result**: Strict DM protection rule prevents any interception. DMs remain 100% private and accessible.

#### Test 2.3: Instagram Stories Regression Test
1. In Instagram Home feed, tap on a friend's **Story** circle at the top.
2. Watch several stories, skip forward, and tap to close.
3. **Expected Result**: NO interruption. Story indicators (`story_viewer_container`, `story_progress_bar`) are explicitly recognized as legitimate friend updates.

#### Test 2.4: Instagram Feed Posts & Comments
1. Scroll through the normal Instagram feed viewing photo posts and single video posts.
2. Open the comment section under a photo.
3. **Expected Result**: Normal browse identifiers prevent false positive interception.

---

### Test Suite 3: Local VPN Adult Website Filtering & Safe Search
*Goal: Block explicit domain resolutions and enforce Safe Search without breaking normal web browsing or critical Google services (Gmail, Google Drive, Google Play).*

> [!NOTE]
> **Safe Testing Protocol**:
> You do **NOT** need to visit explicit adult content to verify DNS filtering. Use safe verification commands or add safe test domains in the Custom Domains dialog.

#### Test 3.1: Custom Domain Filtering (Safe Test)
1. Open DK FOCUS GUARD > **Protection** screen > **Domain Filtering** card.
2. Tap **Manage Custom Domains**, add `example-blocked-test.com`, and tap **Add Domain**.
3. Open **Google Chrome** on your phone and navigate to `http://example-blocked-test.com`.
4. **Expected Result**: The connection fails immediately with `ERR_NAME_NOT_RESOLVED` or `ERR_CONNECTION_REFUSED` (0.0.0.0 sinkhole). The browser cannot resolve the domain.
5. In DK FOCUS GUARD, the **Blocked** telemetry counter increments.

#### Test 3.2: Critical Google Services Regression Test
1. Ensure the VPN filter is ACTIVE.
2. Open **Gmail** and verify emails sync and load attachments.
3. Open **Google Drive** and verify files and folders display.
4. Open **Google Play Store** and browse apps.
5. **Expected Result**: ALL Google services load seamlessly without SSL handshake errors. (The engine strictly excludes non-search Google subdomains from SafeSearch VIP redirection).

#### Test 3.3: Normal Web Browsing Regression Test
1. With VPN ACTIVE, open Chrome and visit:
   - `https://en.wikipedia.org`
   - `https://github.com`
   - `https://stackoverflow.com`
2. **Expected Result**: All pages load rapidly and normally through the protected upstream resolver (`1.1.1.1`).

---

### Test Suite 4: Automated Focus Schedules & Overnight Enforcement
*Goal: Verify automated activation and deactivation, including midnight-crossing overnight schedules.*

#### Test 4.1: Immediate Schedule Window
1. Open DK FOCUS GUARD > **Focus** screen.
2. Note current device time (e.g. 14:15).
3. Set **Start Time** to 2 minutes in the future (e.g. 14:17) and **End Time** to 5 minutes in the future (e.g. 14:20).
4. Ensure today's weekday chip is highlighted. Tap **Save Defense Schedule**.
5. Lock the phone. Wait until 14:17.
6. Unlock phone and open DK FOCUS GUARD.
7. **Expected Result**: Dashboard displays **SCHEDULE ACTIVE** (Cyan badge). Persistent notification displays `DK FOCUS GUARD — SCHEDULE ACTIVE`. Defenses are actively armed.
8. Wait until 14:20.
9. **Expected Result**: Protection automatically transitions out of scheduled mode and restores manual preferences.

#### Test 4.2: Overnight Schedule (Midnight Crossing)
1. Set **Start Time** to `22:00` and **End Time** to `06:00`.
2. Select today's weekday. Tap **Save Defense Schedule**.
3. **Verification**: In unit tests (`FocusScheduleEnforcementTest`), overnight logic verifies both Part 1 ($T \ge 22:00$ today) and Part 2 ($T < 06:00$ tomorrow morning carried from yesterday's schedule).

---

### Test Suite 5: Emergency Defense Override
*Goal: Verify temporary PIN-protected suspension and automatic restoration.*

#### Test 5.1: 5-Minute Emergency Override
1. In DK FOCUS GUARD, tap the Emergency Override trigger (or Disarm in Strict Mode).
2. Enter your secure PIN.
3. Select **5 Minutes** duration and confirm.
4. **Expected Result**:
   - Dashboard immediately reflects **OVERRIDE ACTIVE** (Yellow badge).
   - Notification displays `DK FOCUS GUARD — OVERRIDE ACTIVE (5m remaining)`.
   - YouTube Shorts and Instagram Reels are temporarily permitted without interruption.
5. Wait for the 5-minute timer to elapse (or tap **Cancel Override**).
6. **Expected Result**: Override automatically expires, defenses re-arm, and notification restores to `DEFENSE ARMED`.

---

### Test Suite 6: Device Reboot & Service Recovery
*Goal: Verify auto-recovery across device restarts without infinite crash loops.*

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

## 4. COLLECTING REAL-DEVICE LOGS WITH ADB

If any unexpected behavior occurs, collect logcat telemetry filtered by the DK FOCUS GUARD process:

```bash
# Capture full Focus Guard diagnostic stream
adb logcat -v time -s FocusGuardAccessibilityService:V FocusGuardVpnService:V UnifiedProtectionManager:V YouTubeInterceptionEngine:V InstagramReelsInterceptionEngine:V > device_test_log.txt

# Inspect live block events logged to local Room database
adb shell "run-as com.dk.focusguard sqlite3 /data/data/com.dk.focusguard/databases/dk_focus_guard_database 'SELECT * FROM block_events ORDER BY id DESC LIMIT 10;'"
```

---

## 5. KNOWN PLATFORM LIMITATIONS (DOCUMENTED HONESTLY)

1. **Private DNS (DoT / DoH)**: Android 14's native Private DNS routes over TLS on port 853 directly to the chosen provider. To enforce local port 53 UDP filtering, Private DNS must be set to "Off" in device settings.
2. **Encrypted / In-App DoH Browsers**: If a browser has "Secure DNS" forced with an internal DoH client (e.g. Chrome with custom DoH URL), it may bypass local port 53 DNS. The standard device configuration uses system DNS.
3. **ColorOS Background Aggression**: If ColorOS "Deep Cleanup" or "Super Power Saving" mode is engaged, the OS may terminate accessibility services. Always keep DK FOCUS GUARD locked in Recent Tasks.

