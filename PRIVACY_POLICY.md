# Privacy Policy & Terms of Service for Calling Bell

**Effective Date:** July 27, 2026  
**Developer:** Risk Technologies  
**App Name:** Calling Bell  
**Package Name:** `calling.bell.risktech`  
**Website:** https://sites.google.com/view/risk-tech/home  

---

## 1. Privacy Policy

Risk Technologies ("we", "us", or "our") respects your privacy. This Privacy Policy explains how **Calling Bell** collects, uses, and safeguards information when you use our mobile application.

### A. Information We Collect and How It Is Used

1. **Local Wi-Fi Network & Device Permissions**
   - **Permissions Used:** `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NEARBY_WIFI_DEVICES`, `ACCESS_FINE_LOCATION`.
   - **Purpose:** These permissions enable local UDP broadcast communication so devices connected to the same home or office Wi-Fi network can discover each other and deliver instant chimes without latency.
   - **Location Data Note:** While Android system permissions require location access for Wi-Fi scanning on certain Android versions, **Calling Bell does not collect, record, track, or share your physical location.**

2. **Notifications & Ring Alerts**
   - **Permissions Used:** `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`.
   - **Purpose:** Used strictly to trigger audio chimes, haptic vibration alerts, and full-screen incoming ring notifications when a household or room member rings the bell.

3. **Firebase Cloud Messaging (FCM) & Remote Rings**
   - **Purpose:** When operating outside local Wi-Fi (e.g. via mobile cellular data), Calling Bell uses Google Firebase Cloud Messaging to route encrypted room ring alerts. Rooms use anonymous codes; no personal identity or contact lists are collected or stored on external servers.

4. **In-App Purchases (Google Play Billing)**
   - **Purpose:** Calling Bell offers a $2.99 One-Time Lifetime PRO upgrade. All payment transactions are processed entirely and securely by **Google Play Billing**. Risk Technologies does not access, process, or store credit cards, debit cards, or financial details.

### B. Third-Party Services

Calling Bell integrates standard Google services to enable core functionality:
- **Google Play Services & Firebase Cloud Messaging (FCM)** (Push Notification Delivery)
- **Google Play Billing Library** (In-App Purchase Processing)

These services handle data according to Google's Privacy Policy: https://policies.google.com/privacy

### C. Data Retention & Security

Calling Bell operates on a **privacy-first, local-first architecture**:
- All room lists, custom chime sounds, and user settings are stored locally on your device (`SharedPreferences`).
- No personal user profiles, tracking pixels, or advertising identifiers are created, stored, or sold.

### D. Children’s Privacy

Calling Bell does not knowingly collect or solicit personal data from children under 13. The application is designed for family and household use without requiring personal registration or account creation.

### E. Contact Us

If you have questions regarding this Privacy Policy, please contact us at:
- **Developer:** Risk Technologies
- **Website:** https://sites.google.com/view/risk-tech/home

---

## 2. Terms of Service

By downloading or using **Calling Bell**, you agree to the following terms:

1. **License & Usage:** Risk Technologies grants you a personal, non-exclusive, non-transferable license to use Calling Bell on your personal devices.
2. **Freemium & PRO Purchase:** Basic room communication (up to 3 members per room) is free. Unlocking unlimited rooms, custom intentions, and custom audio requires a one-time $2.99 PRO purchase processed via Google Play. Purchases are subject to Google Play Store refund policies.
3. **Disclaimer of Warranties:** Calling Bell is provided "as is" without warranties of any kind. While engineered for high reliability, network interruptions or device power saver modes may affect alert delivery. Calling Bell is not intended for life-safety emergency signaling.
4. **Modifications:** We reserve the right to update this policy as necessary to comply with Android system requirements or Google Play Store policies.
