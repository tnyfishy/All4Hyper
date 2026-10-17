# All4Hyper

An LSPosed module (libxposed API 102) with a Material 3 Expressive settings app for Xiaomi HyperOS. It hooks **Security Center** (`com.miui.securitycenter`) to lift its charging, power, permission and network limits, one switch at a time.

## What it can do

**Charging**
- Block night charge protection (the 80% overnight hold): entry, apply and state, as two layers
- Block every protection strategy, or just the common ones; override the charge limit (50–100%, 100 = none)
- Fast charge: stock / force on / force off; bypass charging: stock / block start / block stop
- Block pogo dock protection; turn off silent wireless charging
- Fake the battery health level (1–4); replace the health percentage text on the charge protection page

**Power and performance**
- Block power mode switching and super power saving
- No refresh rate / brightness / screen timeout throttling when saving power; no thermal throttling

**Permissions and background**
- Lift private AppOps restrictions (install, chain launch, device ID, screen recording, accessibility, AI screen control)
- Keep background apps on one-tap clean-up; force allow auto-start

**Network and install**
- Lift per-app Wi-Fi / mobile blocking and background restriction; keep data on past the limit; lift the hotspot traffic limit
- Skip Security Center's install verification step

Every switch applies immediately; Security Center reads the setting on each call. The app can also restart Security Center through root (`am force-stop`) and shows LSPosed, scope and root status on the home screen.

> ⚠️ Lifting AppOps restrictions and install verification lowers the device's protection. Turn on only what you need. Switching off thermal throttling and charge protection is at your own risk.

## Requirements

- Xiaomi HyperOS (Android 15+), LSPosed with API 102, root (for the restart button)
- Targets Security Center's obfuscated classes (`hg.a`, `jh.p`, `ng.c` ...). A Security Center update can rename them; a missing target is logged (`All4Hyper`) and only that hook is skipped.

## Install

1. Install the APK.
2. LSPosed → Modules → enable **All4Hyper** with **Security Center** in its scope (the app can request it for you).
3. Open All4Hyper, grant root, turn on what you want, then tap **Restart Security Center** once.

By default the night charge hold is blocked and the common charge limit is unlimited; everything else is off.

## Credits and licence

The hook targets (class and method names, which call does what) were found by reading [MeowPower](https://github.com/Xposed-Modules-Repo/io.github.yuhj319.meowpower) by yuhj319. MeowPower has no licence and its app shows a support page; this project re-implements the same hooks from scratch rather than copying its code. If you find it useful, consider supporting the original author.

The settings UI follows the Material 3 guidelines, with the floating navigation bar inspired by [NzHelper](https://github.com/bug-bit/NzHelper) (GPL-3.0); no NzHelper code is used.

## How it was checked

- The module builds, and the APK contains the LSPosed entry files (`META-INF/xposed/*`) and the module class.
- The hook logic was run on a JVM against a fake LSPosed framework and fake Security Center classes with the same names and signatures: 108 checks pass (defaults, master switch, argument rewriting, which calls are dropped and which are let through).
- **Not tested:** the hooks on a real HyperOS device (class names come from MeowPower, not from your Security Center build), and the Compose UI on a device.

## Building

JDK 17+, Android SDK Platform 37 and Build Tools 36.0.0. Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`), then:

```
./gradlew assembleRelease
```

The APK is `app/build/outputs/apk/release/app-release.apk`, signed with the building machine's debug key; updates must use the same key.
