# DK FOCUS GUARD — PROTECTION RELIABILITY TESTING GUIDE
## Continuous YouTube Shorts Blocking & System-Wide Adult Content Filtering
**Target Device**: Oppo A3x 5G (`CPH2681`)  
**Operating System**: Android 14 / ColorOS 14.0.0  
**Application Package**: `com.dk.focusguard`  
**Security Status**: All 3 modules armed (Shorts, Reels, Adult Filter)

---

## 1. PRE-TEST VERIFICATION CHECKLIST

Ensure the following configuration on your Oppo A3x 5G:

| Checkpoint | Target State | Verification Method |
|---|---|---|
| **USB Debugging** | Enabled | `adb devices` shows `AUQKQ46XZPEQNN9H device` |
| **Accessibility Service** | Enabled & Bound | Settings > Additional Settings > Accessibility > DK Focus Guard |
| **VPN Profile** | Permitted & Active | Status bar shows key/shield icon when Adult Blocker is enabled |
| **Battery Optimization** | Whitelisted | App info > Battery usage > Allow background activity & Allow auto-launch |
| **Private DNS** | Off or Automatic | Settings > Connection & sharing > Private DNS (Do not set custom DoT provider) |

---

## 2. PROBLEM 1 VERIFICATION: CONTINUOUS YOUTUBE SHORTS BLOCKING

### Objective
Verify that YouTube Shorts are blocked **continuously** without temporary failures, infinite loops, or closing the entire YouTube app. YouTube must remain open on the Home feed.

### Test Cases

#### Test 1.1: Clicking a Short from YouTube Home Feed
1. Open the YouTube app.
2. Scroll to the "Shorts" shelf or tap any video marked as a Short.
3. **Expected Behavior**:
   - The Short is detected within 50ms.
   - Focus Guard immediately triggers safe navigation (clicks Navigate Up `<` or dispatches Back).
   - The user is returned to the YouTube Home feed.
   - A brief notification/toast appears: `YouTube Shorts blocked — Focus Guard`.
   - The YouTube app stays running and does **not** close.

#### Test 1.2: Tapping the "Shorts" Bottom Navigation Tab
1. From inside YouTube, tap the dedicated "Shorts" icon on the bottom navigation bar.
2. **Expected Behavior**:
   - Focus Guard's multi-signal classifier detects the Shorts container / player.
   - Multi-tier navigation triggers: clicks the "Home" tab or re-launches the YouTube Home Intent.
   - YouTube transitions directly to the Home tab.
   - Shorts video does not play.

#### Test 1.3: Swiping to Next Short (Continuous Scrolling Resistance)
1. If a Short begins to render, rapidly swipe vertically to the next Short.
2. **Expected Behavior**:
   - The reduced 750ms cooldown window immediately catches the new Short.
   - Repeated playback is prevented.
   - The user is redirected back to the YouTube Home tab.

#### Test 1.4: Background Persistence (After 15-30 Minutes of Phone Use)
1. Lock your Oppo phone screen or use other applications (e.g. WhatsApp, Camera, Chrome) for 15–30 minutes.
2. Open YouTube and tap a Short.
3. **Expected Behavior**:
   - The Accessibility Service remains bound (`PROC_STATE_PERSISTENT`).
   - ColorOS does not kill the service because DK Focus Guard is on the battery idle whitelist (`dumpsys deviceidle whitelist`).
   - Shorts are blocked immediately, exactly as in Test 1.1.

### ADB Telemetry Command
Monitor live YouTube detection events in real time:
```powershell
adb logcat -v time -s FocusGuardA11y
```
Look for:
```
D FocusGuardA11y: Inspecting node for com.google.android.youtube (shortsEnabled=true)
D FocusGuardA11y: YouTube outcome: INTERRUPTED_SUCCESS (Shorts session successfully interrupted)
I FocusGuardA11y: Exiting Shorts inside YouTube (keeping YouTube open)
```

---

## 3. PROBLEM 2 VERIFICATION: SYSTEM-WIDE ADULT CONTENT FILTERING

### Objective
Verify that adult websites are filtered across **all browsers** (Google Chrome, Microsoft Edge, Mozilla Firefox, Brave) in both **Normal** and **Incognito / Private** browsing modes, while ordinary websites load at full speed.

### Two-Tier Filtering Architecture Under Test:
1. **Tier 1 (Local DNS Engine)**: 160+ top adult domains and all adult TLDs (`.xxx`, `.adult`, `.porn`, `.sex`) are sinkholed locally to `0.0.0.0` within 1ms.
2. **Tier 2 (Upstream Cloudflare Family DNS)**: Permitted queries are forwarded to `1.1.1.3` / `1.0.0.3` (Cloudflare 1.1.1.1 for Families) and CleanBrowsing `185.228.168.168`, which automatically sinkhole millions of adult websites upstream.
3. **DoH Bypass Shield**: Bootstrap domains (`dns.google`, `cloudflare-dns.com`, etc.) are intercepted so Chrome and Edge fall back to the protected system DNS in Incognito mode.

### Test Cases

#### Test 2.1: Chrome Normal Browsing Mode
1. Open Google Chrome.
2. Enter a safe test adult domain (or search for adult sites).
3. **Expected Behavior**:
   - The browser fails to resolve the domain.
   - Chrome displays `ERR_NAME_NOT_RESOLVED` or `ERR_CONNECTION_REFUSED` (0.0.0.0).
   - The adult website does **not** load.

#### Test 2.2: Chrome Incognito Mode
1. In Chrome, open a new **Incognito tab**.
2. Navigate to an adult domain or adult search term.
3. **Expected Behavior**:
   - Because DoH bootstrap endpoints are sinkholed by DK Focus Guard, Chrome's Secure DNS fails over to the system VPN filter.
   - The adult website fails to load in Incognito mode.

#### Test 2.3: SafeSearch Enforcement (Google & Bing)
1. Open Google.com or Bing.com in any browser.
2. Enter an explicit search term.
3. **Expected Behavior**:
   - Google queries resolve to SafeSearch VIP (`216.239.38.120`).
   - Bing queries resolve to Strict SafeSearch VIP (`204.79.197.220`).
   - Search results display with SafeSearch locked on; explicit images/videos are omitted.

#### Test 2.4: Ordinary Websites Load Normally
1. Open `google.com`, `wikipedia.org`, `github.com`, `amazon.com`, `news.google.com`.
2. **Expected Behavior**:
   - All standard non-adult websites resolve instantly via Cloudflare Family DNS (`1.1.1.3`).
   - Web pages load completely with zero broken layouts, zero delays, and full network speed.

### ADB Telemetry Command
Monitor live DNS filtering events in real time:
```powershell
adb logcat -v time | Select-String "VPN_DNS_FILTER|ADULT_WEB|FocusGuard"
```

---

## 4. COLOROS 14 PERSISTENCE TUNING (OPPO SPECIFIC)

To ensure ColorOS 14 never suspends Focus Guard during deep sleep:

1. **Auto-Launch & Background Activity**:
   - Open **Settings** > **Apps** > **App management** > **DK Focus Guard**.
   - Tap **Battery usage**.
   - Enable **Allow background activity**.
   - Enable **Allow auto-launch**.
2. **App Lock in Recent Apps**:
   - Swipe up and hold to open the Recent Apps overview.
   - Tap the three dots `⋮` above DK Focus Guard card.
   - Select **Lock** (a small padlock icon appears).
   - This prevents ColorOS memory cleaner from closing DK Focus Guard.
3. **Disable Private DNS Custom Host**:
   - Open **Settings** > **Connection & sharing** > **Private DNS**.
   - Ensure it is set to **Off** or **Auto** (not set to an external DoT hostname that bypasses UDP port 53).

---

## 5. SUMMARY OF TEST COMMANDS

```powershell
# Check ADB connection to Oppo A3x 5G
adb devices

# Reinstall debug APK
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

# Launch Focus Guard
adb shell am start -n com.dk.focusguard/.MainActivity

# Check running Accessibility service status
adb shell "dumpsys accessibility | grep -A 2 -B 2 'FocusGuardAccessibilityService'"

# Stream live logcat of YouTube Shorts interception
adb logcat -s FocusGuardA11y
```

