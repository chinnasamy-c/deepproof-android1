# DeepProof Android App Starter

This Android app is a native Kotlin/Java-style UI starter (Java source) for the DeepProof PDF signature verification backend.

## What it does
- Android PDF picker (multiple selection, up to 25)
- Sends selected PDFs to `POST /api/validate`
- Displays backend statuses and signature details
- Exports CSV and ZIP through backend endpoints
- No PDF verification is faked on-device: the backend must perform actual validation

## Important
This app is not fully usable until you deploy the DeepProof FastAPI backend and enter its HTTPS URL in the app.
Never enter a backend URL you do not trust. Selected PDFs are uploaded to that server.
The app is not an official government service.
The current source needs a small follow-up fix to handle Android's Save Document result for exports; exports are returned by backend and the current version initiates a Save As dialog but does not yet write the returned bytes to the selected URI. Do not rely on in-app export until that is completed.
This starter does not include certificate roots or the production validation policy. Configure and test the backend separately.

## Build APK using GitHub Actions from Android
1. Create a GitHub account and a new repository named `deepproof-android`.
2. Upload all files and folders from this project ZIP into the repository root. Keep `.github/workflows/build-apk.yml`.
3. Open the repository's **Actions** tab and enable Actions if asked.
4. Open **Build DeepProof APK**, press **Run workflow**.
5. Wait for the green check mark.
6. Open the completed workflow run, scroll to **Artifacts**, and download `DeepProof-debug-apk`.
7. Extract the artifact ZIP to get `app-debug.apk`, then open it on your Android phone to install. Android may ask permission to install apps from that browser/file manager.

## Build locally (Android Studio)
Open the project folder in Android Studio, let Gradle sync, then Build > Build APK(s).

## Backend
Deploy the `deepproof_starter` FastAPI project first. Set the backend HTTPS URL in the app. Backend endpoints expected:
- `POST /api/validate`
- `POST /api/export/csv`
- `POST /api/export/zip`

## Project structure
- `app/src/main/java/in/deepproof/app/MainActivity.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/styles.xml`
- `.github/workflows/build-apk.yml`
