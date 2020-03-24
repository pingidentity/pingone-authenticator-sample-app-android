# Authenticator sample app

This sample application demonstrates an authenticator-only application that uses PingOne for Customers mobile SDK. An Android developer can easily build a branded and customized Authenticator application using this sample.

## What’s in the sample app?

  - Full native application written in Android, compatible with all Android devices from Android 5 and above.
  - Integration with PingOne SDK version 1.2.0.
  - UI customization can be done easily to get your company flavor on the app.
  - All app texts can be easily localized and modified in one file.

## Features

  - Pairing flow using QR code scannig or manual input.
  - Override usernames locally, or use names from PingOne directory.
  - Authentication flow with push notifications using biometric recognition to approve authentication.
  - Send logs option to track customers issues with support ID.

## Prerequisites

The Authenticator sample app requires Android Studio 3.5 or above to compile and run.

To set up your application for  working with push messages in Android refer to [PingOne mobile SDK iOS readMe].

## Installation

1. Clone this repository to Android.
2. Go to the **Target General** setting and update the **Display Name** and **Bundle Identifier** with your app's values.
3. To update the UI, replace the following images in the `Assets.xcassets` folder:
    - Splash screen image: `launch_image`
    - Banner logo in navigation bar: `logo_navbar`
    - App Icon: `AppIcon`
    ##### Note: 
    It is mandatory to replace these images before submitting the app to Google Play, in order to create a unique app complying with Google's restrictions. For more information, refer to [Google Play Impersonation and Intellectual Property Guidelines].

4. If needed, update the `Localizable.strings` file, to customize any string in the app.
5. Build and run the project.
##### Note: For further understanding the code implementation of this app, refer to [Setup a mobile app] using the PingOne SDK sample code.


[Setup a mobile app]: <https://github.com/pingidentity/pingone-customers-mobile-sdk-ios>
[Google Play Impersonation and Intellectual Property Guidelines]:<https://play.google.com/about/ip-impersonation/impersonation/>
[PingOne mobile SDK iOS readMe]:<https://github.com/pingidentity/pingone-customers-mobile-sdk-ios/blob/master/README.md>
