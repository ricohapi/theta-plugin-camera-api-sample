# THETA Plug-in: CameraAPI Capture Plugin

Version: 3.1.0

This sample plug-in shows the way of capturing image with Camera API for RICOH THETA V/Z1/X.

## Contents

* [Development Environment](#requirements)
* [Specification of this plugin](#specification)
* [Getting Started](#started)
* [Terms of Service](#terms)
* [Trademark Information](#trademark)

<a name="requirements"></a>

## Development Environment

This sample plug-in has been developed under the following conditions.

#### Camera

* RICOH THETA X  (Version 1.10.1 or later)
* RICOH THETA Z1 (Version 2.10.3 or later)
* RICOH THETA V  (Version 2.40.2 or later)

Tips : How to update your RICOH THETA firmware:
> * [THETA X](https://support.theta360.com/en/manual/x/content/update/update_01.html)
> * [THETA Z1](https://support.theta360.com/en/manual/z1/content/update/update_01.html)
> * [THETA V](https://support.theta360.com/en/manual/v/content/update/update_01.html)


### Development Environment

* Android Studio Chipmunk | 2021.2.1 Patch 2
    * Windows 10 Version 21H2
    * macOS Version 13.2.1 Ventura
* Gradle Plugin Version 4.2.2
* Gradle Version 6.8.3
* Android&trade; SDK (API Level 29)
* compileSdkVersion 29
* minSdkVersion 25
* targetSdkVersion 29
* [RICOH THETA Plug-in Library](https://github.com/ricohapi/theta-plugin-library) (Version 3.1.0) is being imported.

<a name="specification"></a>
## Specification of this plugin
* This plug-in capture still and video by using [Camera API](https://api.ricoh/docs/theta-plugin-reference/camera-api/), [AudioManager API](https://api.ricoh/docs/theta-plugin-reference/audio-manager-api/) and [PluginLibrary](https://github.com/ricohapi/theta-plugin-sdk/tree/master/pluginlibrary).
* After capturing still or video, the JPEG or MP4(+WAV) file is stored as,
    * THETA X: PC*.JPG or MV*.MP4 in DCIM/100_TEST folder.
    * THETA V/Z1 : "yyyyMMddHHmmss".jpg (.mp4 or .wav for video). "yyyyMMddHHmmss" is 20180123123456 when it is 12:34:56 Jan 23, 2018.
* WebAPI can not be used when Camera API is used.
* The .mp4 file includes 4K video and 1ch monaural audio.

* THETA V and THETA Z1 only
    * The .wav file includes 4ch spatial audio as a first-order ambisonics B-format. Only for THETA V and THETA Z1.
    * The metadata of the files (.mp4 and .jpg) which outputted by using CameraAPI will be missed than the case of using WebAPI. (We recommend to use WebAPI instead of CameraAPI.)

<a name="started"></a>
## Getting Started

### THETA X
1. Import this sample project into Android&trade; Studio.
1. Verify that AndroidManifest.xml declears to support only RICOH THETA X.
    * app/src/main/AndroidManifest.xml

        ```xml
        <uses-feature android:name="com.theta360.receptor.x"  android:required="true" />
        <uses-feature android:name="com.theta360.receptor.z1" android:required="true" />
        <uses-feature android:name="com.theta360.receptor.v"  android:required="true" />
        ```

1. Verify that build.gradle set RICOH THETA Plug-in Library as dependencies.
    * app/build.gradle

        ```gradle
        dependencies {
            ...
            implementation 'com.theta360:pluginlibrary:3.1.0'
            ...
        }
        ```

    * build.gradle

        ```gradle
        allprojects {
        repositories {
            ...
            maven { url 'https://github.com/ricohapi/theta-plugin-library/raw/master/repository' }
            ...
        }
        ```

1. Connect RICOH THETA X to PC with USB cable.
1. Execute "Run" in Android&trade; Studio, then APK will be installed to RICOH THETA X automatically and you can debug it.

### THETA V and THETA Z1
1. Open Vysor chrome app to see desktop of the camera.
1. Initial setting for debugging

    At the first time to use this app, app need to be taken permissions to use camera and storage.
    Settings → Apps → "CameraAPI Capture Plugin" → Permissions →
      “Camera”, "Microphone" and “Storage” to be checked (turn ON).

1. Launch "CameraAPI Capture Plugin" app
    (Ignore button on GUI)
1. Press shutter button to take a photo/video
1. Pull JPEG/MP4/WAV by adb
    "adb pull /sdcard/DCIM/yyyyMMddHHmmss.jpg" ("yyyyMMddHHmmss" is 20180123123456 when it is 12:34:56 Jan 23, 2018.)

---

<a name="terms"></a>
## Terms of Service

> You agree to comply with all applicable export and import laws and regulations applicable to the jurisdiction in which the Software was obtained and in which it is used. Without limiting the foregoing, in connection with use of the Software, you shall not export or re-export the Software  into any U.S. embargoed countries (currently including, but necessarily limited to, Crimea – Region of Ukraine, Cuba, Iran, North Korea, Sudan and Syria) or  to anyone on the U.S. Treasury Department’s list of Specially Designated Nationals or the U.S. Department of Commerce Denied Person’s List or Entity List.  By using the Software, you represent and warrant that you are not located in any such country or on any such list.  You also agree that you will not use the Software for any purposes prohibited by any applicable laws, including, without limitation, the development, design, manufacture or production of missiles, nuclear, chemical or biological weapons.

By using the RICOH THETA Plug-in SDK, you are agreeing to the above and the license terms, [LICENSE.txt](LICENSE.txt).

Copyright &copy; 2018 Ricoh Company, Ltd.

<a name="trademark"></a>
## Trademark Information

The names of products and services described in this document are trademarks or registered trademarks of each company.

* Android, Nexus, Google Chrome, Google Play, Google Play logo, Google Maps, Google+, Gmail, Google Drive, Google Cloud Print and YouTube are trademarks of Google Inc.
* Apple, Apple logo, Macintosh, Mac, Mac OS, OS X, AppleTalk, Apple TV, App Store, AirPrint, Bonjour, iPhone, iPad, iPad mini, iPad Air, iPod, iPod mini, iPod classic, iPod touch, iWork, Safari, the App Store logo, the AirPrint logo, Retina and iPad Pro are trademarks of Apple Inc., registered in the United States and other countries. The App Store is a service mark of Apple Inc.
* Bluetooth Low Energy and Bluetooth are trademarks or registered trademarks of US Bluetooth SIG, INC., in the United States and other countries.
* Microsoft, Windows, Windows Vista, Windows Live, Windows Media, Windows Server System, Windows Server, Excel, PowerPoint, Photosynth, SQL Server, Internet Explorer, Azure, Active Directory, OneDrive, Outlook, Wingdings, Hyper-V, Visual Basic, Visual C ++, Surface, SharePoint Server, Microsoft Edge, Active Directory, BitLocker, .NET Framework and Skype are registered trademarks or trademarks of Microsoft Corporation in the United States and other countries. The name of Skype, the trademarks and logos associated with it, and the "S" logo are trademarks of Skype or its affiliates.
* Wi-Fi™, Wi-Fi Certified Miracast, Wi-Fi Certified logo, Wi-Fi Direct, Wi-Fi Protected Setup, WPA, WPA 2 and Miracast are trademarks of the Wi-Fi Alliance.
* The official name of Windows is Microsoft Windows Operating System.
* All other trademarks belong to their respective owners.
