<img src="art/ic_launcher-web.png" width="100" height="100">  

PhoneProfilesPlus
=================

[![version](https://img.shields.io/badge/version-5.3.1-blue)](https://github.com/henrichg/PhoneProfilesPlus/releases/tag/5.3.1)
[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/henrichg/PhoneProfilesPlus/blob/master/LICENSE)
[![Crowdin](https://badges.crowdin.net/phoneprofilesplus/localized.svg)](https://crowdin.com/project/phoneprofilesplus)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=AF5QK49DMAL2U&currency_code=EUR)

__[Google Play release](https://play.google.com/store/apps/details?id=sk.henrichg.phoneprofilesplus)__  
Latest version is 4.2.0.3 and will never by upgraded.  
Reason: Google restrictions:  
1. Android - All Wi-Fi related functions not working since Android 10.  
2. Google Play - Google require (currently) Android 10+ for applications. In application exists gradle configuration parameter:  
   `targetSdkVersion targetSdk`  
   and in PPP must be target sdk = 28. Android 10 is 29.

In stores, in which is currently deployed PPP, restriction about target sdk does not apply.

__[GitHub release (direct download)](https://github.com/henrichg/PhoneProfilesPlus/releases/latest/download/PhoneProfilesPlus.apk)__

__Another sources of PhoneProfilesPlus (PPP):__

Use keyword "PhoneProfilesPlus" for search this application in these stores.

__Galaxy Store (for Samsung devices)__

__[Huawei AppGallery PPP release](https://appgallery.cloud.huawei.com/ag/n/app/C104501059?channelId=PhoneProfilesPlus+application&id=957ced9f0ca648df8f253a3d1460051e&s=79376612D7DD2C824692C162FB2F957A7AEE81EE1471CDC58034CD5106DAB009&detailType=0&v=&callType=AGDLINK&installType=0000)__  
__[Huawei AppGallery application (download)](https://consumer.huawei.com/en/mobileservices/appgallery/)__

__[APKPure PPP release](https://apkpure.com/p/sk.henrichg.phoneprofilesplus)__  
__[APKPure application (download)](https://apkpure.com/apkpure/com.apkpure.aegon)__

__[F-Droid PPP release](https://apt.izzysoft.de/fdroid/index/apk/sk.henrichg.phoneprofilesplus)__
&nbsp;&nbsp;&nbsp;_[How to add F-Droid repository to F-Droid application](https://apt.izzysoft.de/fdroid/index/info)_  
__[F-Droid application (download)](https://www.f-droid.org/)__

---

__Android application - manually and by event triggered change of device settings like ringer mode, sounds, Wifi, Bluetooth, launcher wallpaper, ...__

__This application is for configuration of device for life situations (at home, at work, in car, sleep, outside, ...) using Profiles.__

__In it is also possibility to automatically activate Profiles by Events.__

[Privacy Policy](https://henrichg.github.io/PhoneProfilesPlus/privacy_policy.html)

_**** Please report me bugs, comments and suggestions to my e-mail: <henrich.gron@gmail.com>. Speed up the especially bug fixes. Thank you very much. ****_

_*** Please help me with translation, thank you: <https://crowdin.com/project/phoneprofilesplus> ***_


##### (HELP) How to grant (G1) permission - for profile parameters that require this permission
- [Show it](docs/grant_g1_permission.md)

##### (HELP) How to disable Wi-Fi scan throttling - useful for Wi-Fi scanning
- [Show it](docs/wifi_scan_throttling.md)

##### Features
- [Show it](docs/ppp_features.md)

##### Screenshots
- [[1]](art/_base/phoneScreenshots/01.png),
[[2]](art/_base/phoneScreenshots/02.png),
[[3]](art/_base/phoneScreenshots/03.png),
[[4]](art/_base/phoneScreenshots/04.png),
[[5]](art/_base/phoneScreenshots/05.png),
[[6]](art/_base/phoneScreenshots/06.png),
[[7]](art/_base/phoneScreenshots/07.png),
[[8]](art/_base/phoneScreenshots/08.png),
[[9]](art/_base/phoneScreenshots/09.png),
[[10]](art/_base/phoneScreenshots/10.png),
[[11]](art/_base/phoneScreenshots/11.png),
[[12]](art/_base/phoneScreenshots/12.png),
[[13]](art/_base/phoneScreenshots/13.png)

##### Supported Android versions

- From Android 7.0
- minSdkVersion = 24
- targetSdkVersion = 28
- compiledSdkVersion = 31

##### Required external libs - open-source

- AndroidX library: appcompat, preferences, gridlayout, cardview, recyclerview, viewpager2, constraintlayout, workmanager - https://developer.android.com/jetpack/androidx/versions
- Google Material components - https://github.com/material-components/material-components-android
- google-gson - https://code.google.com/p/google-gson/
- ACRA - https://github.com/ACRA/acra
- guava - https://github.com/google/guava
- osmdroid - https://github.com/osmdroid/osmdroid
- TapTargetView - https://github.com/KeepSafe/TapTargetView
- doki - https://github.com/DoubleDotLabs/doki
- dashclock - https://github.com/romannurik/dashclock
- DexMaker - https://github.com/linkedin/dexmaker
- volley - https://github.com/google/volley
- ExpandableLayout - https://github.com/skydoves/ExpandableLayout
- SmoothBottomBar - https://github.com/ibrahimsn98/SmoothBottomBar
- RootTools (as module, code modified) - https://github.com/Stericson/RootTools
- RootShell (as module, code modified) - https://github.com/Stericson/RootShell
- time-duration-picker (as module, code modified) - https://github.com/svenwiegand/time-duration-picker
- android-betterpickers (as module, code modified) - https://github.com/code-troopers/android-betterpickers
- AndroidClearChroma (as module, code modified) - https://github.com/Kunzisoft/AndroidClearChroma
- RecyclerView-FastScroll (as module, code modified) - https://github.com/jahirfiquitiva/RecyclerView-FastScroll (original repository: https://github.com/timusus/RecyclerView-FastScroll)
- RelativePopupWindow (only modified class RelativePopupWindow) - https://github.com/kakajika/RelativePopupWindow
- SunriseSunset (only modified class SunriseSunset) - https://github.com/caarmen/SunriseSunset
- android-hidden-api (downloaded android.jar copied into folder \<android-sdk\>/android-XX) - https://github.com/Reginer/aosp-android-jar (original repository: https://github.com/anggrayudi/android-hidden-api)
- FreeReflection (only code from https://github.com/tiann/FreeReflection/blob/master/library/src/main/java/me/weishu/reflection/BootstrapClass.java) - https://github.com/tiann/FreeReflection
- NoobCameraFlash (as module, code modified) - https://github.com/Abhi347/NoobCameraFlash
- AutoStarter (only modified class AutoStartPermissionHelper.kt) - https://github.com/judemanutd/AutoStarter
- ToastCompat (as module, code modified) - https://github.com/PureWriter/ToastCompat

##### Required external libs - not open-source

- Samsung Look - http://developer.samsung.com/galaxy/edge
