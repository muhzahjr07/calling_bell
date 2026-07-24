# Calling Bell - System Architecture Overview

Calling Bell is a native Android application engineered for ultra-fast, local Wi-Fi and push notification household ringing with haptic waveforms and delivery tracking.

## System Architecture

```
                               ┌────────────────────────────────────────┐
                               │           Calling Bell App             │
                               │      (Kotlin + Jetpack Compose)        │
                               └──────────────────┬─────────────────────┘
                                                  │
                                   ┌──────────────┴──────────────┐
                                   ▼                             ▼
                    ┌────────────────────────────┐  ┌───────────────────────────┐
                    │    RingListenerService     │  │   SharedPrefsManager      │
                    │   (UDP & FCM Foreground)   │  │   (JSON SharedPreferences)│
                    └──────────────┬─────────────┘  └───────────────────────────┘
                                   │
                    ┌──────────────┴─────────────┐
                    ▼                            ▼
      ┌───────────────────────────┐  ┌───────────────────────────┐
      │     HeartbeatEventBus     │  │       RingEventBus        │
      │   (Member Discovery Bus)  │  │   (Ring Payload Bus)      │
      └───────────────────────────┘  └───────────────────────────┘
```

### 1. Presentation & UI Layer (`MainActivity.kt`)
* **Framework:** Jetpack Compose + Navigation Compose.
* **Aesthetics:** Midnight Celestial dark theme featuring glassmorphic bento cards, gradient borders, and responsive Compose layouts.
* **Component Hierarchy:**
  - `AppNavigator()`: Global router handling notification permissions, active room FCM subscriptions, theme management, and `DeliveryAckState` pop-up overlays.
  - `DashboardScreen()`: Main dashboard displaying selected room info, target member selection roster (scrollable for > 3 members), ring intentions, and central bell trigger button.
  - `RoomsScreen()`: Room management screen featuring **Scan Available Rooms** (`🔍`) dialog and duplicate code validation with **✨ Generate Code** helper.
  - `RoomScreen()`: Active room lobby with **Scan Members** (`📡`) heartbeat trigger and member selection.
  - `SettingsScreen()`: Chime configuration including Master Volume slider, Default Chime Sound selector, Haptic Intensity slider, **Custom Messages Manager** with `🗑️` delete options, and direct system Permission shortcuts.
  - `ProScreen()`: Risk Tech PRO subscription page unlocking unlimited rooms, members, custom messages, and custom audio.

### 2. Networking & Service Layer
* **Foreground Service (`RingListenerService.kt`)**: Runs in the Android foreground listening on UDP port `3001` for local Wi-Fi broadcast packets and Firebase Cloud Messaging (FCM) topics for active room codes.
* **Event Busses (`HeartbeatEventBus.kt` & `RingEventBus.kt`)**: Reactive event streams powered by Kotlin `SharedFlow` for real-time member online/offline discovery and ring payload processing.

### 3. Local Data Persistence (`SharedPrefsManager.kt`)
* **Storage Engine:** Android `SharedPreferences` serialized as JSON arrays.
* **Entities Managed:**
  - `USER_NAME` & `IS_PRO` flags.
  - `ROOMS_LIST` (Room entities with nested Member lists).
  - `PRO_CUSTOM_INTENTIONS` (Custom ring reasons with user-selected emojis and labels, editable and deletable).
  - `AppSettings` (Volume, vibration toggle, haptics intensity, theme ID, default chime ID).

### 4. Hardware Integration (`Utils.kt`)
* **Audio Synthesizer & Player:** `MediaPlayer` handling tone playback (`classic`, `chime`, `urgent`, `gentle`, `horn`).
* **Haptics Waveforms:** Native Android `Vibrator` and `VibratorManager` executing dynamic haptic vibration patterns scaled by user intensity preferences.
