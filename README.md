# FitLink - Fitness Tracker with Trainer Support

FitLink is an Android application developed using **Kotlin** and **Jetpack Compose** that helps users follow a consistent fitness routine. Trainers assign workouts (with instructional videos), users complete and record those workouts, and the app calculates calories burned using a simple MET-based formula. It includes real-time chat between users and trainers, a progress dashboard, and push notifications.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Step-by-Step Setup & Execution](#step-by-step-setup--execution)
  - [1. Clone the Repository](#1-clone-the-repository)
  - [2. Open Project in Android Studio](#2-open-project-in-android-studio)
  - [3. Configure Firebase](#3-configure-firebase)
  - [4. Set Up Cloudinary](#4-set-up-cloudinary)
  - [5. Add API Keys to `local.properties`](#5-add-api-keys-to-localproperties)
  - [6. Build and Run](#6-build-and-run)
- [Firestore Security Rules & Indexes](#firestore-security-rules--indexes)
- [Testing Guide](#testing-guide)
  - [Test Authentication](#test-authentication)
  - [Test Workout Assignment & Completion](#test-workout-assignment--completion)
  - [Test Video Playback](#test-video-playback)
  - [Test Real-Time Chat](#test-real-time-chat)
  - [Test Dashboard & Calorie Calculation](#test-dashboard--calorie-calculation)
  - [Test Push Notifications (FCM)](#test-push-notifications-fcm)
- [Troubleshooting](#troubleshooting)
- [User Guide (Mobile App)](#user-guide-mobile-app)
- [Building a Release APK](#building-a-release-apk)
- [Credits & License](#credits--license)

## Features

- **User Authentication** – Firebase Email/Password or Google Sign-In.
- **Workout Assignment** – Trainers can assign workouts with exercise videos.
- **Video Learning** – Watch instructional videos hosted on Cloudinary.
- **Workout Logging** – Mark workouts as completed; calories are auto-calculated (MET × weight × duration).
- **Progress Dashboard** – View workout history and basic trends (weekly completion rate, total calories).
- **Real-Time Chat** – Users and trainers communicate directly via Firestore.
- **Push Notifications** – Reminders and updates using Firebase Cloud Messaging (FCM).

## Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Backend & Storage**: Firebase Authentication, Cloud Firestore
- **Media Hosting**: Cloudinary (video & image uploads)
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Language**: Kotlin
- **Architecture**: MVVM (ViewModel + StateFlow), Repository pattern

## Prerequisites

- **Android Studio** Ladybug | 2024.2.1 or newer
- **JDK 11** or higher (included with Android Studio)
- **Firebase account** (free tier)
- **Cloudinary account** (free tier)
- **Android device** (physical or emulator) running API 24+ (Android 7.0)
- **Google Services** dependency (google-services.json)
- **Internet connection** for API calls and media streaming

## Step-by-Step Setup & Execution

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/fitlink-android.git
cd fitlink-android
```
Step 2: Open in Android Studio
Open Android Studio
Click Open Project
Select the FitLink folder
Step 3: Setup Firebase
Go to Firebase Console
Create a new project
Add Android app (use package name)
Download google-services.json
Place it inside:
app/
Step 4: Sync Project
Click Sync Now in Android Studio
Step 5: Run the App
Connect Android device OR use emulator
Click Run ▶️
🧪 How to Test the App
Test Authentication
Sign up with email and password
Login and verify access
Test Workout Feature
Trainer assigns workout
User views and marks it complete
Test Chat
Send messages between user and trainer
Check real-time updates
Test Notifications
Trigger notification (new workout or message)
Verify push notification received
Test Media
Upload workout video/image
Check if it displays correctly
📱 How It Works (Simple Flow)
User logs in using Firebase Authentication
Trainer assigns workouts with videos
User completes workouts and tracks progress
Calories are calculated using a simple formula
Chat is used for communication
Data is stored in Firestore
Media is stored in Cloudinary
