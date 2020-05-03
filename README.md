# Authenticator sample app

This sample application demonstrates an authenticator-only application that uses PingOne for Customers mobile SDK. An Android developer can easily build a branded and customized Authenticator application using this sample.

## What’s in the sample app?

  - Full native application written in Android, compatible with all Android devices from Android 5 and above.
  - Integration with PingOne SDK version 1.3.0.
  - UI customization can be done easily to get your company flavor on the app.
  - All app texts can be easily localized and modified in one file.

## Features

  - Pairing flow using QR code scanning or manual input.
  - Override user names locally, or use names from PingOne directory.
  - Authentication flow with push notifications using biometric recognition to approve authentication.
  - Send logs option to track customers issues with support ID.

## Prerequisites

The Authenticator sample app requires Android Studio 3.6 or higher and Android SDK 29 to compile and run.

You should choose a **package name** for your application. A package name uniquely identifies your app on the device and in the Google Play Store. Generally, the package name of an app is in the format `com.example.myapp`, but it’s completely up to the developer to choose the name. *A package name is often referred to as an **applicationId***. For more information refer to [Android applicationId guidelines].

To set up your application for work with push messages in Android, refer to [Firebase project set-up guidelines]. You should register your Android application with the package name you choose, and download the ```google-services.json``` file when prompted. 
This file will be needed in the installation

## Installation

1. Clone this repository in Android Studio.
2. In the **app/build.gradle** file update the **applicationId** value with your package name. For example:
```groovy
android {
    defaultConfig {
        applicationId "com.pingidentity.authenticatorsampleapp"
        ...
    }
    ...
}
```
should be changed to:
```groovy
android {
    defaultConfig {
        applicationId "com.example.myapp" //your package name
        ...
    }
    ...
}
```
3. Copy and paste the ```google-services.json``` file received from Firebase into the ```app``` folder.
4. Click on ```Sync Project with Gradle Files```. At this step you can build and run the app.

## Customization
1. Use the built-in Asset Studio to import your own application icon and override the placeholder with the name ```ic_launcher```.
2. Overwrite the following images:
    * `app/src/main/res/mipmap-hdpi/logo_splash.png` for the splash screen image.
    * `app/src/main/res/mipmap-xxhdpi/logo_horizontal.png` for the logo in the navigation bar.
    ##### Note: 
    It is mandatory to replace these images before submitting the application to Google Play, in order to create a unique app complying with Google's restrictions. For more information, refer to [Google Play Impersonation and Intellectual Property Guidelines].

3. If needed, update the `app/src/main/res/values/strings.xml` file, to customize any string in the application.
##### Note: For further understanding the code implementation of this app, refer to [Setup a mobile app] using the PingOne SDK sample code.


[Setup a mobile app]: <https://github.com/pingidentity/pingone-customers-mobile-sdk-android>
[Firebase project set-up guidelines]:<https://firebase.google.com/docs/android/setup?authuser=0#register-app>
[Google Play Impersonation and Intellectual Property Guidelines]:<https://play.google.com/about/ip-impersonation/impersonation>
[Android applicationId guidelines]:<https://developer.android.com/studio/build/application-id>
[PingOne mobile SDK Android README]:<https://github.com/pingidentity/pingone-customers-mobile-sdk-android/blob/master/README.md>
