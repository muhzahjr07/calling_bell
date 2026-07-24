```bash
npm install
```

node server.jscd frontend

# Calling Bell

Calling Bell is a real-time, multi-platform household bell chime application. It allows you to connect multiple devices (Web and Android) into a shared "room" to ring each other instantly.

With Premium features enabled, it supports unlimited connected devices, custom bell intentions (Coffee Time, Dinner, Urgent Help, etc.), custom sounds, physical haptic feedback, and a real-time activity history log.

## Project Structure

The project is divided into three main components:

- `backend/`: Node.js + Socket.IO server that handles real-time communication, room state, and user limits.
- `frontend/`: React + Vite web application featuring a modern glassmorphism UI, themes, and Web Audio synthesizers.
- `android-app/`: Native Android application built with Kotlin and Jetpack Compose, fully synchronized with the backend.

---

## How to Run the Application

To run the full stack locally, you will need to start both the backend server and the frontend client (and optionally the Android app).

### 1. Run the Backend Server

The backend handles all the Socket.IO connections and room logic.

1. Open a terminal and navigate to the backend directory:

   ```bash
   cd backend
   ```
2. Install dependencies (if you haven't already):

   ```bash
   npm install
   ```
3. Start the server:

   ```bash
   node server.js
   ```

   *The server will start running on port `3001`.*

### 2. Run the Web Frontend

The web app is the primary interface for desktop and iOS users (via browser).

1. Open a **new** terminal and navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. Open your browser and navigate to `http://localhost:5173` (or the URL provided by Vite).

### 3. Run the Android App (Optional)

The native Android app offers a seamless mobile experience with custom vibration patterns.

1. Open **Android Studio**.
2. Select **File > Open** and choose the `android-app` folder from this repository.
3. Allow Gradle to sync and build the project.
4. **Important for Emulators:** The Android app is configured to connect to `http://10.0.2.2:3001` (which maps to your computer's `localhost` port 3001 in the Android Emulator).
   *If you are testing on a **physical Android device**, you must update the socket URL in `MainActivity.kt` to point to your computer's local IP address (e.g., `http://192.168.1.x:3001`).*
5. Click the **Run** button (green play icon) in Android Studio to install and launch the app on your emulator or device.

---

node server.js

## Features

* **Free vs. PRO Tiers:** Free rooms support up to 3 connected devices. Upgrading to PRO unlocks unlimited devices.
* **Ring Intentions:** Select specific reasons for ringing (e.g., Coffee, Dinner, Urgent).
* **Multi-Theme UI:** Switch between Glassmorphism, Cyber Neon, and Midnight Dark themes on the web.
* **Activity Log:** View a real-time history of rings and intentions within your room.
* **Haptics & Audio:** Custom vibration patterns on Android and Web Audio API synthesized fallbacks on the web.

## Tech Stack

* **Backend:** Node.js, Express, Socket.IO
* **Frontend:** React, Vite, Lucide Icons, Vanilla CSS
* **Android:** Kotlin, Jetpack Compose, Socket.IO Client for Android
