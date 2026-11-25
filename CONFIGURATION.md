# OSAS BEN TRACKER (Version 1.0) — Setup & Configuration Guide

## 1. Firebase Configuration
Firebase Authentication (Google Sign-In) and Cloud Firestore (`ai-studio-android-osasbent-ecc3abb7-8035-4116-86fb-1246ee568fbf`) are provisioned in `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`, with security rules in `firestore.rules`.

### Optional Missing Configuration in Firebase Console:
- **Passwordless Email-Link Sign-In**: If you want passwordless email-link sign-in in addition to Google Sign-In, enable **Email/Password -> Email link (passwordless sign-in)** in the Firebase Authentication Console and configure an associated Android App Link / Dynamic Link domain.
- **Release Signing Fingerprints**: Before publishing a release `.aab`/APK to Google Play, add your Play App Signing SHA-1 and SHA-256 certificates in **Firebase Console -> Project Settings -> Your Apps (Android)**.

## 2. Map Provider (OpenStreetMap / osmdroid)
- Uses `org.osmdroid:osmdroid-android:6.1.20` with standard OpenStreetMap Mapnik tiles (`© OpenStreetMap contributors`) and scoped-storage caching (`filesDir` / `cacheDir`).
- **Missing Setup for High-Volume Production**: OpenStreetMap's public tile servers are intended for light/test use. Before scaling to production users, configure a commercial OSM tile provider (e.g., MapTiler, Stadia Maps, or Thunderforest) or self-hosted tile server and ensure your `User-Agent` / API key meets their terms.

## 3. Emergency Calling
- **Spain**: Opens the device dialer to `112`.
- **Nigeria**: Defaults to `112` and is user-configurable inside **Settings -> Emergency Contacts**.
- OSAS BEN TRACKER does not automatically dispatch police or emergency agencies.

## 4. Administrative Datasets
- `app/src/main/assets/nigeria_locations.json`: 36 States + FCT (37 total) and all 774 LGAs with coordinates.
- `app/src/main/assets/spain_locations.json`: 17 Autonomous Communities + Ceuta + Melilla (19), 50 Provinces + Ceuta + Melilla (52), and all 8,132 INE municipalities with INE codes and coordinates.
