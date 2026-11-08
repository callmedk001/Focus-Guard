# DK FOCUS GUARD

**Personal Digital Discipline & Behavioral Focus Shield for Android**

DK FOCUS GUARD is an open-source, privacy-first Android application designed to eliminate algorithmic micro-distractions and enforce digital discipline. It features real-time, in-app interception of short-form video feeds and local encrypted DNS filtering to block adult content, without requiring root access, cloud accounts, or third-party servers.

---

## 1. Project Title
**DK FOCUS GUARD** (`com.dk.focusguard`)

---

## 2. Short Description
A native Android application built with Jetpack Compose that intercepts YouTube Shorts, blocks Instagram Reels, and filters adult web domains locally via Android Accessibility and VpnService APIs.

---

## 3. Project Overview
Modern social platforms utilize algorithmic vertical short-form video feeds optimized for endless scrolling, leading to involuntary digital habit loops and cognitive fragmentation. 

DK FOCUS GUARD acts as a personal firewall against algorithmic micro-distractions. Rather than locking users completely out of valuable productivity and communication tools, Focus Guard operates with precision:
- Allows normal YouTube videos, subscriptions, search, and long-form education while instantly exiting vertical Shorts feeds.
- Allows Instagram direct messaging, photo feed browsing, and search while exiting vertical Reels feeds.
- Encrypts and filters DNS queries locally via an on-device Android `VpnService` to block adult domains without tunneling user traffic to any external VPN server.

All telemetry, state enforcement, and domain classification occur 100% on-device.

---

## 4. Main Features
- **In-App Vertical Feed Interception**: Automatically detects and exits short-form video players inside target apps, redirecting back to the main content feed rather than abruptly killing the application.
- **Local DNS Protection Shield**: Intercepts port 53 UDP DNS requests on a loopback VPN interface to filter adult domains and enforce Google/Bing/YouTube Safe Search without inspecting HTTPS payloads.
- **PIN-Secured Settings & Strict Mode**: Hardware-backed biometric authentication and PBKDF2-derived master PIN protection to prevent impulsive disabling of shields.
- **Timed Emergency Override**: Configurable emergency temporary suspension protocol (e.g., 5 to 60 minutes) that auto-resumes focus protection when the timer expires.
- **Automated Focus Schedules**: Schedule defense windows by time and day of week (e.g., Mon–Fri 09:00–17:00) using Android `AlarmManager`.
- **Cybersecurity Command Center UI**: Modern, high-contrast dark cyberpunk interface built entirely with Jetpack Compose Material 3.
- **Zero Cloud Footprint**: Operates without user accounts, analytics trackers, remote databases, or external telemetry servers.

---

## 5. Three Dedicated Protection Modules

### Module 1: YouTube Shorts Shield
- **Target Package**: `com.google.android.youtube`
- **Mechanism**: Inspects visible accessibility hierarchy for Shorts-specific Litho/Compose containers (`reel_watch_fragment_root`, `reel_recycler`, `reel_time_bar`).
- **Enforcement Action**: Executes an in-app `ACTION_CLICK` on the YouTube "Home" or "Subscriptions" tab node (falling back to `GLOBAL_ACTION_BACK`).
- **Result**: Closes the Short immediately while keeping YouTube open for productive, long-form viewing.

### Module 2: Instagram Reels Guard
- **Target Package**: `com.instagram.android`
- **Mechanism**: Evaluates tab bar selection indicators and Reels view identifiers (`clips_video_container`, `reels_viewer`) with negative scoring for Direct Messages, Stories, and Profiles to eliminate false positives.
- **Enforcement Action**: Executes an in-app `ACTION_CLICK` on the Instagram "Feed" tab or triggers an accessibility `GLOBAL_ACTION_BACK`.
- **Result**: Closes the Reels player while keeping Instagram messages and profile browsing fully accessible.

### Module 3: Adult Website Blocker & Safe Search Engine
- **Mechanism**: Local Android `VpnService` capturing device DNS packets on `10.1.10.1`.
- **Filtering Engine**: Matches requested hostnames against a local Trie-backed blocklist and wildcard pattern matcher.
- **Response**: Returns standard `NXDOMAIN` (or `0.0.0.0`) responses for blocked adult sites while routing legitimate queries upstream to secure public DNS resolvers (`1.1.1.1` / `8.8.8.8`).
- **Safe Search**: Rewrites standard DNS queries for Google, Bing, and DuckDuckGo to their official SafeSearch CNAME equivalents.

> **Scope Note**: TikTok, Facebook, and other applications are completely excluded from this project. Focus Guard strictly targets the three modules listed above.

---

## 6. Screenshots

| Dashboard (Control Center) | Protection Shields | Automated Focus |
|:---:|:---:|:---:|
| *(Screenshot Placeholder: `docs/screenshots/dashboard.png`)* | *(Screenshot Placeholder: `docs/screenshots/protection.png`)* | *(Screenshot Placeholder: `docs/screenshots/focus.png`)* |

| Live Accessibility Status | PIN Authentication | DNS Protection |
|:---:|:---:|:---:|
| *(Screenshot Placeholder: `docs/screenshots/accessibility.png`)* | *(Screenshot Placeholder: `docs/screenshots/pin_auth.png`)* | *(Screenshot Placeholder: `docs/screenshots/vpn_status.png`)* |

---

## 7. Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose (BOM 2024.10.00) with Material 3
- **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence**: Room Database 2.6.1 (SQLite with KSP compiler)
- **Key-Value Store**: AndroidX DataStore Preferences 1.1.1
- **Concurrency**: Kotlin Coroutines 1.9.0 & StateFlow / SharedFlow reactive streams
- **Dependency Processing**: Google KSP (Kotlin Symbol Processing) 2.0.21-1.0.28
- **Platform APIs**:
  - `AccessibilityService` (UI hierarchy inspection and non-intrusive action dispatch)
  - `VpnService` (Local loopback DNS packet processor)
  - `AlarmManager` (Exact focus schedule transitions)
  - `BiometricPrompt` & `KeyStore` (Hardware biometric authentication)

---

## 8. Project Architecture

The project follows a reactive, un-directional data flow (UDF) architecture:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (DashboardScreen, ProtectionScreen, FocusScreen)     │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                       ViewModels                       │
│ (DashboardViewModel, ProtectionViewModel, etc.)        │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                    Domain Repositories                 │
│ (ProtectionSettingsRepo, FocusScheduleRepo, etc.)      │
└─────────────▲────────────────────────────▲─────────────┘
              │                            │
┌─────────────┴─────────────┐┌─────────────┴─────────────┐
│    Room SQLite Database   ││    DataStore Preferences  │
│  (Schedules, BlockEvents) ││  (PIN hash, Active Rules) │
└───────────────────────────┘└───────────────────────────┘
              ▲                            ▲
              │                            │
┌─────────────┴────────────────────────────┴─────────────┐
│                   System Background Engines            │
│  - FocusGuardAccessibilityService                      │
│  - FocusGuardVpnService                                │
│  - UnifiedProtectionManager                            │
└────────────────────────────────────────────────────────┘
```

---

## 9. Folder Structure

```
Focus Guard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dk/focusguard/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/          # Room DB, DAOs, and Entities
│   │   │   │   │   ├── preferences/    # DataStore preference managers
│   │   │   │   │   └── repository/     # Concrete repository implementations
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/          # Core domain models and enums
│   │   │   │   │   └── repository/     # Repository interfaces
│   │   │   │   ├── receivers/          # BootRecoveryReceiver & ScheduleAlarmReceiver
│   │   │   │   ├── security/           # PBKDF2 PIN encryption & Biometric managers
│   │   │   │   ├── services/
│   │   │   │   │   ├── accessibility/  # FocusGuardAccessibilityService & Classifiers
│   │   │   │   │   └── vpn/            # FocusGuardVpnService & DnsPacketProcessor
│   │   │   │   ├── ui/
│   │   │   │   │   ├── accessibility/  # Accessibility setup UI
│   │   │   │   │   ├── components/     # Reusable cyber cards, buttons, badges
│   │   │   │   │   ├── dashboard/      # Main command center screen
│   │   │   │   │   ├── focus/          # Focus timer and schedule management
│   │   │   │   │   ├── navigation/     # Bottom navigation bar & nav graph
│   │   │   │   │   ├── protection/     # Protection shield toggles & cards
│   │   │   │   │   ├── security/       # PIN lock, unlock, and setup screens
│   │   │   │   │   ├── settings/       # App preferences & strictness settings
│   │   │   │   │   ├── statistics/     # Usage metrics and block event graphs
│   │   │   │   │   └── theme/          # Cyberpunk high-contrast color scheme
│   │   │   │   └── utils/              # DateTime, Permission, and network utilities
│   │   │   ├── res/                    # Drawables, mipmaps, strings, XML configs
│   │   │   └── AndroidManifest.xml     # Application manifest & service registrations
│   │   └── test/                       # 156 comprehensive JVM unit tests
│   └── build.gradle.kts                # Module build script and dependencies
├── docs/                               # Device testing and validation documentation
├── gradle/                             # Gradle wrapper and version catalog (libs.versions.toml)
├── .gitignore                          # Git exclusions file
├── build.gradle.kts                    # Root project build configuration
├── gradle.properties                   # JVM heap and AndroidX flags
├── gradlew / gradlew.bat               # Gradle wrapper scripts
├── README.md                           # Public repository documentation
└── settings.gradle.kts                 # Project repositories and module definitions
```

---

## 10. System Requirements
- **Android OS**: Android 8.0 (Oreo / API Level 26) or higher.
- **Target Platform**: Optimized and validated on Android 14 (API Level 34 / 35).
- **Architecture Support**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- **System Services**:
  - Android Accessibility Framework.
  - Android VpnService framework.

---

## 11. Installation Instructions

### Option A: Install Pre-Built Debug APK via ADB
1. Enable **Developer Options** and **USB Debugging** on your Android device.
2. Connect your phone via USB.
3. Open a terminal and run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Option B: Build from Source
See [How to Build the APK](#12-how-to-build-the-apk) below.

---

## 12. How to Build the APK

### Prerequisites
- JDK 17 (Java Development Kit) installed and set in `JAVA_HOME`.
- Android SDK with Platform 35 and Build-Tools installed.

### Build Steps
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd "Focus Guard"
   ```
2. Create or verify `local.properties` with your Android SDK path:
   ```properties
   sdk.dir=/path/to/android-sdk
   ```
3. Run the unit test suite:
   ```bash
   # Linux / macOS
   ./gradlew testDebugUnitTest

   # Windows (PowerShell)
   .\gradlew.bat testDebugUnitTest
   ```
4. Build the debug APK:
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug

   # Windows (PowerShell)
   .\gradlew.bat assembleDebug
   ```
5. The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 13. How to Install the APK

Once built, deploy the APK to a connected device or emulator:
```bash
# Verify connection
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.dk.focusguard/.MainActivity
```

---

## 14. First-Time Application Setup
1. Launch **DK FOCUS GUARD** from your app drawer.
2. The Dashboard displays a **Setup Required** status banner.
3. Tap **Grant Permissions** or navigate to the **Protection** tab.
4. Enable the **Accessibility Service** for short-form video feed interception.
5. Enable the **VPN Protection Shield** for adult website filtering.
6. (Optional) Set up a Master PIN under **Settings** to protect your focus configuration from impulsive changes.

---

## 15. How to Enable Accessibility Permission
To intercept YouTube Shorts and Instagram Reels, Focus Guard requires Accessibility access:
1. Inside the app, navigate to **Protection** > tap **Enable Accessibility**.
2. Android will open **Settings** > **Accessibility**.
3. Under *Downloaded apps* (or *Installed services*), locate **DK Focus Guard Detection Engine**.
4. Toggle the service **ON** and confirm the standard Android system dialog.
5. Return to Focus Guard; the Accessibility Engine card will immediately display **Active & Monitoring**.

---

## 16. How to Configure VPN Protection
To filter adult websites locally:
1. On the **Protection** screen, find the **Adult Web Filter** card.
2. Toggle the switch to **ON**.
3. When prompted with Android's *Connection request* dialog (*"DK Focus Guard wants to set up a VPN connection"*), tap **OK**.
4. The key icon will appear in your device status bar, indicating active local DNS protection.
5. (Optional) Toggle **Safe Search** to enforce filtered search results on Google, Bing, and YouTube.
6. (Optional) Tap **Custom Domains** to add or import your own personal blacklist rules.

---

## 17. How to Configure Focus Schedules
Automate your focus defense during deep work or study hours:
1. Tap the **Focus** tab on the bottom navigation bar.
2. Toggle **Focus Defense Mode** to **ON**.
3. Set your desired **Start Time** (e.g., `09:00`) and **End Time** (e.g., `17:00`).
4. Select the active weekdays (e.g., `Mon`, `Tue`, `Wed`, `Thu`, `Fri`).
5. Focus Guard automatically arms all active shields during the scheduled window and disarms them when the window concludes.

---

## 18. How to Use Emergency Override
If you legitimately need access during a locked focus session:
1. Navigate to **Settings** > **Emergency Override**.
2. Authenticate using your master PIN or biometric verification.
3. Select an override duration (e.g., 5, 15, or 30 minutes).
4. Protection temporarily pauses and displays an amber countdown pill on the Dashboard.
5. When the countdown completes, protection automatically resumes full enforcement.

---

## 19. How to Configure Android Private DNS for DNS Filtering
Modern Android versions (Android 9+) support **Private DNS** (DNS-over-TLS), which can bypass local IPv4 DNS proxies if set to an external DoT provider.
- For optimal DNS filtering, keep Android's system **Private DNS** setting set to **Automatic** or **Off** in **Settings > Network & internet > Private DNS**.
- Focus Guard forwards permitted queries upstream to secure public resolvers (`1.1.1.1` and `8.8.8.8`).

---

## 20. ColorOS / OEM Battery & Auto-Launch Guidance
OEM Android skins (such as Oppo/Realme ColorOS, Xiaomi MIUI/HyperOS, and OnePlus OxygenOS) aggressively terminate background services unless explicitly configured:
1. Open **Settings** > **Apps** > **App management** > **DK FOCUS GUARD**.
2. Tap **Battery usage**:
   - Enable **Allow background activity**.
   - Enable **Allow auto-launch**.
3. Open the **Recent Apps** overview, tap the three dots on DK Focus Guard, and select **Lock** to prevent accidental task clearing.

---

## 21. Privacy and Security Information
- **Zero Keystroke Logging**: The Accessibility Service does not request `canRequestFilterKeyEvents` and never logs or reads user keystrokes, passwords, or payment info.
- **No Payload Inspection**: The VPN service operates solely as a UDP DNS resolver on port 53. It does NOT terminate TLS, install CA certificates, or inspect HTTPS website content.
- **Cryptographic PIN Storage**: Master PINs are hashed using **PBKDF2WithHmacSHA256** with 65,536 iterations and a 16-byte random salt. Constant-time byte comparisons are enforced to prevent timing attacks. Plaintext PINs are zeroized from memory immediately after hashing.
- **No Analytics / Telemetry**: No third-party SDKs (Firebase, Google Analytics, Facebook SDK, Crashlytics) are included in this project.

---

## 22. Known Limitations
- **Web-based Shorts/Reels**: The Accessibility Service monitors native YouTube and Instagram applications. Shorts viewed within third-party web browsers are not intercepted via accessibility (they can be mitigated via DNS or custom app blocks).
- **Split-Screen Mode**: In Android split-screen or picture-in-picture mode, accessibility node focus may vary depending on active window focus.
- **Private DNS Conflict**: If an external Private DNS (DoT) profile is active in Android system settings, encrypted TLS DNS traffic bypasses local UDP port 53 VPN interception.

---

## 23. Troubleshooting

| Issue | Cause | Resolution |
| :--- | :--- | :--- |
| **Shorts are playing without exiting** | Accessibility Service was stopped by Android OS battery manager | Re-enable the Accessibility Service in Android Settings and grant background execution permission. |
| **Cannot return to Home from Protection** | Fixed in Phase 12 release | Tapping Home or the `←` back arrow now cleanly restores the root dashboard. |
| **Adult sites not blocked** | VPN permission not granted or Private DNS is enabled | Verify the VPN key icon is present in the status bar and ensure Android Private DNS is set to "Off" or "Automatic". |
| **Forgot Master PIN** | Security lockout active | Wait for the lockout timer to expire, or clear app data via Android Settings (resets local configuration). |

---

## 24. Development Setup
1. Clone the repository into your preferred workspace.
2. Open the project in **Android Studio Ladybug (2024.2.1)** or newer.
3. Allow Gradle to synchronize dependencies.
4. Set Android Studio's Gradle JDK to **JDK 17**.
5. Ensure a physical device or emulator running API 26+ is available for testing.

---

## 25. Testing Instructions
The repository contains 156 unit tests verifying classifiers, DNS processors, cryptographic managers, Room DAOs, and policy engines.

```bash
# Run unit tests with Gradle
./gradlew testDebugUnitTest

# Run specific classifier test
./gradlew testDebugUnitTest --tests "com.dk.focusguard.YouTubeShortsClassifierTest"
```

---

## 26. GitHub Contribution Instructions
Contributions that enhance the reliability, performance, and privacy of Focus Guard are welcome.

### Contribution Rules:
1. **Preserve Scope**: Strictly maintain the three defined modules (YouTube Shorts, Instagram Reels, Adult Website Blocker). Do NOT submit PRs adding TikTok or arbitrary apps.
2. **Privacy First**: Never add third-party tracking, analytics, ad networks, or cloud dependencies.
3. **Test Coverage**: All architectural changes must include accompanying JVM unit tests.
4. **Code Style**: Follow official Kotlin coding conventions (`kotlin.code.style=official`).

### Workflow:
1. Fork the repository and create your feature branch: `git checkout -b feature/my-enhancement`.
2. Commit your changes: `git commit -m "feat: enhance reels detection accuracy"`.
3. Push to your fork: `git push origin feature/my-enhancement`.
4. Open a Pull Request with a detailed description of verified hardware testing.

---

## 27. License
*(License placeholder: Project license has not yet been selected. All rights reserved by the author. Do not distribute without permission.)*

