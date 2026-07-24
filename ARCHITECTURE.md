# Calling Bell - Architecture Overview

Calling Bell is designed to be a fast, real-time application using a centralized state model mediated by Socket.IO.

## System Architecture

### 1. Web Frontend (`/frontend`)
*   **Framework:** React 19 + Vite
*   **State Management:** React local state (`useState`, `useEffect`) tracking Socket connection state.
*   **Audio/Haptics Engine:** Uses HTML5 Audio with a robust `window.AudioContext` synthesizer fallback (ensuring sounds play securely on restricted mobile browsers) and the `navigator.vibrate` API.
*   **Component Structure:**
    *   `App.jsx`: Global controller for Themes, Socket connection, and User Limits logic.
    *   `PairingScreen.jsx`: Handles room generation and name entry.
    *   `BellScreen.jsx`: The primary interactive UI. Manages Intentions, Audio selection, and Ring history.
    *   `PremiumModal.jsx`: Paywall simulation/UI logic for the PRO upgrade.

### 2. Mobile Client (`/android-app`)
*   **Framework:** Kotlin + Jetpack Compose
*   **Network:** `io.socket:socket.io-client` for Android to connect to the backend node server.
*   **UI Components:** Replicates the Web UI layout natively using Compose `Column`, `Row`, and `LazyRow` for intention selections.
*   **Hardware Integration:** Utilizes native Android `Vibrator` and `VibratorManager` (for SDK 31+) to trigger waveform haptics (e.g., heartbeat pulse), alongside `RingtoneManager` for system-level audio cues.

### 3. Backend Server (`/backend`)
*   **Framework:** Node.js, Express, Socket.IO
*   **Data Store:** In-memory `Map()` (`roomStates`). *Note: In a full production setup, this would be migrated to Redis or PostgreSQL for persistent pairings and scaling across multiple server instances.*
*   **Responsibilities:**
    *   Room routing and connection lifecycle.
    *   Free tier limitation enforcement (max 3 users).
    *   Ring broadcast payload routing (including Intentions, Sender Name).
    *   Session Ring History tracking (last 20 rings).

## Event Protocol (Socket.IO)
- `join_room` -> Client joins room. Payload: `{ roomCode, userName, isUserPremium }`.
- `room_status` <- Server broadcasts user count & premium state.
- `room_full_premium_required` <- Server rejects 4th user on free tier.
- `ring_bell` -> Client rings bell. Payload: `{ roomCode, intention, sound, senderName }`.
- `receive_ring` <- Server broadcasts ring to room. Payload: `{ senderName, intention, sound }`.
- `ring_history_sync` <- Server pushes history array to clients.
