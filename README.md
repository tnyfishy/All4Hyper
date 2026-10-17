# All4Hyper

A small LSPosed module (libxposed API 102) for Xiaomi HyperOS. For now it has one switch:

- **Lift AppOps restrictions:** Security Center (`com.miui.securitycenter`) can no longer put Xiaomi's private AppOps into a restricted mode: install apps (10048), chain launch (10049), device identifiers (10050), screen recording protection (10041), accessibility restriction (119), AI screen reading/control (10054/10055). Calls that allow an op still go through.

> ⚠️ This also removes protections against accessibility and screen recording abuse. Turn it on only if you trust your installed apps.

## How it works

Security Center applies these ops through its wrapper `com.miui.permcenter.compact.AppOpsUtilsCompat` (`setMode`, `setModeWithXSpace`, `setUidMode`). While the switch is on, a call whose mode (its last argument) is not `MODE_ALLOWED` (0) is dropped. The methods are found by name and by the mode being the last `int` parameter, not by an exact signature.

- The switch is read on every call, so turning it on or off needs no restart.
- Restrictions set **before** you turn it on stay until something changes them.
- Each dropped call is written to the LSPosed log (`All4Hyper`) with its op and app, so a per-op filter can be built from real values later.

The class and method names were identified by reading [MeowPower](https://github.com/Xposed-Modules-Repo/io.github.yuhj319.meowpower); the code here is written from scratch.

## Requirements

- HyperOS (Android 15+, tested target Android 17), LSPosed with API 102.

## Install

1. Install the APK.
2. LSPosed → Modules → enable **All4Hyper**; the scope is **Security Center** (`com.miui.securitycenter`).
3. Force stop Security Center (or reboot) so the hooks load.
4. Open All4Hyper and turn the switch on.

## Building

JDK 17+, Android SDK Platform 37 and Build Tools 36.0.0. Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`), then:

```
./gradlew assembleRelease
```

The APK (`app/build/outputs/apk/release/app-release.apk`) is signed with the building machine's debug key; updates must use the same key.
