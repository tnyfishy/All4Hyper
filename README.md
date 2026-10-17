# All4Hyper

An LSPosed module (libxposed API 102) with a Material 3 Expressive settings app for Xiaomi HyperOS. It does two things, each on its own tab:

- **Security Center:** lifts its charging, power, permission and network limits, one switch at a time.
- **Google:** long-press power → Gemini / Assistant, long-press the navigation handle → Circle to Search, live translate, and the GMS China restriction removed.

## Security Center (tabs Charging and System)

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

Every switch applies immediately; Security Center reads the setting on each call.

> ⚠️ Lifting AppOps restrictions and install verification lowers the device's protection. Turn on only what you need. Switching off thermal throttling and charge protection is at your own risk.

## Google (tab Google)

| Feature | Works in (LSPosed scope) |
|---|---|
| Long-press power opens Gemini / Assistant | System framework |
| Long-press the navigation handle opens Circle to Search (also the side swipe-and-hold; optional short vibration) | System framework, XiaoAi |
| Remove the GMS China restriction, Quick Share and its control center tile included | System framework, System UI |
| Live translate in Circle to Search (tap Translate and keep translating as you scroll) | Google app |

All four start **off**. The tab also shows each feature's scopes (✓ present, ⚠ missing), a button that requests the missing apps, and the state of the companion module.

**Companion module.** The features depend on the module XGoogle ships (id `coogle_companion`): it makes the Google app a system app, declares Circle to Search and its live translate to the system, and sets the power key and the navigation handle to Google on every boot. All4Hyper installs it through root (KernelSU / Magisk / APatch) from the tab, and it needs a reboot. The copy bundled here adds a switch for the power key, so an XGoogle companion (1.3.1) shows as *update available*.

- Turning the power or navigation switch off hands the key back to the one you had before (the companion saved it); turning it on gives it to Google again.
- Install the companion with a switch off and its first boot leaves that key alone.
- After turning on a system-level switch, reboot. Choose **Google** as the digital assistant in Settings → Apps → Default apps.
- Do not turn on the same features in XGoogle at the same time; both modules would hook the same places.

## Requirements

- Xiaomi HyperOS (Android 15+, tested target Android 17), LSPosed with API 102, root. The Google features target the China ROM and need the Google app installed.
- The Security Center hooks target obfuscated classes (`hg.a`, `jh.p`, `ng.c` ...). A Security Center update can rename them; a missing target is logged (`All4Hyper`) and only that hook is skipped.

## Install

1. Install the APK.
2. LSPosed → Modules → enable **All4Hyper**. Its recommended scope: System framework, System UI, XiaoAi, Google, Security Center (apps can be requested from the app; System framework is ticked by hand).
3. Open All4Hyper, grant root, turn on what you want. For the Google tab, install the companion first and reboot.
4. For Security Center switches, tap **Restart Security Center** once.

By default the night charge hold is blocked and the common charge limit is unlimited; everything else is off.

## Credits and licence

GPL-3.0 (see `LICENSE` at the repository root and `NOTICE.txt`). The Google features and the companion come from [XGoogle](https://github.com/Leekin2/XGoogle) by Leekin, itself a fork of Coogle by zopulus, with hooks derived from ONextBox. Live translate uses [DexKit](https://github.com/LuckyPray/DexKit).

The Security Center hooks' targets (class and method names, which call does what) were found by reading [MeowPower](https://github.com/Xposed-Modules-Repo/io.github.yuhj319.meowpower) by yuhj319. MeowPower has no licence and its app shows a support page; this project re-implements the same hooks from scratch rather than copying its code. If you find them useful, consider supporting the original author.

The floating navigation bar is inspired by [NzHelper](https://github.com/bug-bit/NzHelper); no NzHelper code is used.

## How it was checked

- The module builds, and the APK contains the LSPosed entry files (`META-INF/xposed/*`), the module class, DexKit's native library and the companion ZIP.
- The hook logic runs on a JVM against a fake LSPosed framework and fake system classes with the same names and signatures:
  - Security Center hooks: 108 checks (defaults, master switch, argument rewriting, what is dropped and what passes).
  - Google wiring: 54 checks over six scenarios (every switch off, off HyperOS, power, navigation, GMS, live translate): which hooks get installed for which switch, and that they follow the switch at run time.
- The root side runs the real Kotlin code through a fake `su`/`settings`: 20 checks on switching the keys and reading the companion state, plus the real `boot-completed.sh` (5 checks on the markers).
- **Not tested:** any of it on a real HyperOS device (class names come from MeowPower and XGoogle, not from your firmware), installing the companion with a real root manager, and the Compose UI on a device.

## Building

JDK 17+, Android SDK Platform 37 and Build Tools 36.0.0. Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`), then:

```
python3 build-companion.py   # only after changing companion/
./gradlew assembleRelease
```

The APK is `app/build/outputs/apk/release/app-release.apk`, signed with the building machine's debug key; updates must use the same key.
