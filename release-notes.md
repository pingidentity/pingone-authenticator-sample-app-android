# Release Notes

### v1.2.0 - April 5th, 2021
Features:
* Updated SDK to version 1.4.0.
* Added one time passcode in users screen.
* Deprecated current pairing method and added support to new one with returned object of PairingInfo.

## v1.1.0 - June 18th, 2020
 Features:

 - The PingOne SDK library is updated to v1.3.0
 - Show custom notification content in all authentication flows.

 Compatibility:
 
 - Dependencies are updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
      * 'com.android.tools.build:gradle:**4.0.0**'
      * 'androidx.navigation:navigation-safe-args-gradle-plugin:**2.2.2**'
    * In the 'build.gradle' file at the **app** level:
      * 'org.bitbucket.b_c:jose4j:**0.7.1**'
      * 'com.google.firebase:firebase-messaging:**20.2.0**'
      * 'com.google.android.gms:play-services-vision:**20.1.0**'
      * 'androidx.navigation:navigation-fragment:**2.2.2**'
      * 'androidx.navigation:navigation-ui:**2.2.2**'
      * 'com.google.firebase:firebase-ml-vision:**24.0.3**'
      * 'com.google.firebase:firebase-ml-vision-barcode-model:**16.1.1**'
      * 'androidx.camera:camera-core:**1.0.0-beta05**'
      * 'androidx.camera:camera-camera2:**1.0.0-beta05**'
      * 'androidx.camera:camera-lifecycle:**1.0.0-beta05**'
      * 'androidx.camera:camera-view:**1.0.0-alpha12**'


## v1.0.0 - March 31st, 2020
Features:

- Pairing flow using QR code with camera scan or manual entry.
- Override usernames locally, or use names from PingOne directory.
- Authentication flow with push notifications using biometric recognition to approve authentication.
- Side menu with send logs option to track customer issues with support ID.

