# Release Notes


## v1.5.1 - Jan 9th, 2023
Features:

- Updated SDK to version 1.8.1
- Added support for different push message categories.
- Sensitive screens are protected from recording.

Compatibility:

- Dependencies updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
        * 'com.google.gms:google-services:**4.3.14**'
        * 'androidx.navigation:navigation-safe-args-gradle-plugin:**2.5.3**'
    * In the 'build.gradle' file at the **app** level:
        * 'com.google.code.gson:gson:**2.9.0**'
- Dependencies added:
    * In the 'build.gradle' file at the **app** level:
        * 'com.pingidentity.pingonemfa:android-sdk:1.8.1'
- Dependencies removed from the project:
    * In the 'build.gradle' file at the **app** level:
        * 'org.slf4j:slf4j-api:1.7.30'
        * 'com.github.tony19:logback-android:2.0.0'
        * 'com.madgag.spongycastle:core:1.58.0.0'
        * 'com.madgag.spongycastle:bcpkix-jdk15on:1.58.0.0'
        * 'com.google.android.gms:play-services-vision:20.1.3'
        * 'com.google.android.gms:play-services-safetynet:18.0.1'
        * 'org.bitbucket.b_c:jose4j:0.7.9'
        * 'com.appmattus.certificatetransparency:certificatetransparency-android:1.0.0'

    
## v1.4.2 - Aug 31st, 2022
Features:

- Updated SDK to version 1.7.2.
- Added support for alphanumeric pairing key.
- Bug fixes and performance improvements.

Compatibility:

- Android **target** SDK version is updated to API level 33 (Android 13)
- Dependencies updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
        * 'com.android.tools.build:gradle:**7.2.2**'
        * 'com.google.gms:google-services:**4.3.13**'
        * 'androidx.navigation:navigation-safe-args-gradle-plugin:**2.5.1**'
    * In the 'build.gradle' file at the **app** level:
        * 'com.google.code.gson:gson:**2.9.0**'


## v1.4.1 - June 13th, 2022
Features:

- Updated SDK to version 1.7.1.

Compatibility:

- Dependencies are updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
        * 'com.android.tools.build:gradle:**7.1.2**'
        * 'androidx.navigation:navigation-safe-args-gradle-plugin:**2.4.1**'
    * In the 'build.gradle' file at the **app** level:
        * 'com.google.mlkit:barcode-scanning:**17.0.2**'
        * 'com.google.android.gms:play-services-safetynet:**18.0.1**'
        * 'com.google.code.gson:gson:**2.8.9**'
        * 'org.bitbucket.b_c:jose4j:**0.7.9**'
        * 'androidx.camera:camera-lifecycle:**1.0.2**'
        * 'androidx.camera:camera-view:**1.0.0-alpha30**'


## v1.4.0 - April 25th, 2022
Features:

- Updated SDK to version 1.7.0.
- Added support for authentication using QR Code scanning or manual typing of an authentication code
- Added Certificate Transparency mechanism to protect against mis-issued certificates
- The JWT signature validation updated to use more strong EC algorithm.

Compatibility:
- Minimal Android version is updated to 26 (Android 8)
- Dependencies are updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
        * 'com.android.tools.build:gradle:**7.1.1**'
        * 'com.google.gms:google-services:**4.3.10**'
    * In the 'build.gradle' file at the **app** level:
        * 'com.google.mlkit:barcode-scanning:**17.0.0**'
        * 'androidx.camera:camera-camera2:**1.0.2**'
        * 'androidx.camera:camera-lifecycle:**1.0.2**'
        * 'androidx.camera:camera-view:**1.0.0-alpha30**'
- Dependencies added:
    * In the 'build.gradle' file at the **app** level:
        * 'com.appmattus.certificatetransparency:certificatetransparency-android:1.0.0'


## v1.3.0 - August 1st, 2021
Features:

- Updated SDK to version 1.6.0.
- Added device integrity validation for threat protection.


Compatibility:
- Dependencies are updated to their latest versions:
    * In the 'build.gradle' file at the **project** level:
        * 'com.android.tools.build:gradle:**4.2.1**'
        * 'com.google.gms:google-services:**4.3.8**'
        * 'androidx.navigation:navigation-safe-args-gradle-plugin:**2.3.5**'
    * In the 'build.gradle' file at the **app** level:
        * Migrated to FireBase BOM:
          implementation platform('com.google.firebase:firebase-bom:26.3.0')
          implementation 'com.google.firebase:firebase-core'
          implementation 'com.google.firebase:firebase-messaging'
        * 'com.google.android.gms:play-services-vision:**20.1.3**'
        * 'androidx.navigation:navigation-fragment:**2.3.5**'
        * 'androidx.navigation:navigation-ui:**2.3.5**'
        * Migrated to Google Machine Learning Barcode scanner:
          implementation 'com.google.mlkit:barcode-scanning:16.1.2'
        * 'androidx.camera:camera-core:**1.0.0-beta06**'
        * 'androidx.camera:camera-camera2:**1.0.0-beta06**'
        * 'androidx.camera:camera-lifecycle:**1.0.0-beta06**'
        * 'androidx.camera:camera-view:**1.0.0-alpha13**'

Bug fixes:
- Fixed the issue where the application was generating new passcodes in the background.


## v1.2.0 - April 6th, 2021
Features:

- Updated SDK to version 1.4.0.
- Added one time passcode in users screen.

Dependencies Updated:
* com.android.tools.build:gradle:4.0.1
* com.google.gms:google-services:4.3.4
* com.google.firebase:firebase-messaging:20.3.0
* androidx.appcompat:appcompat:1.2.0
* androidx.constraintlayout:constraintlayout:1.1.3

### Known issues
- After pairing, the app generates new passcodes even when running in the background.


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
- 
