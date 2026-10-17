# All4Hyper

An LSPosed module (libxposed API 102) with a Material 3 settings app for Xiaomi HyperOS. It does three things, each on its own tab:

- **Security Center:** lifts its power, permission and network limits, one switch at a time.
- **Google:** long-press power → Gemini / Assistant, long-press the navigation handle → Circle to Search, live translate, and the GMS China restriction removed.
- **Other:** Power Keeper hooks from HyperCeiler.

## Security Center (tab System)

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

## Power Keeper (tab Other)

| Switch | Works in (LSPosed scope) |
|---|---|
| Lock maximum refresh rate (no per-scene rate switching) | Power Keeper |
| No brightness / refresh rate limits when hot | Power Keeper |
| Per-app high refresh rate | Power Keeper, Mi Settings |
| Keep background apps (Power Keeper's process kill does nothing) | Power Keeper |
| Keep the battery optimization list (no batch restore of Power Keeper's whitelist) | Power Keeper |
| GMS online at all times (network, alarms and push in sleep) | Power Keeper, System framework |

All start **off**. Restart Power Keeper after a change (restart button in the top bar); the GMS switch needs a system restart.

## Google (tab Google)

| Feature | Works in (LSPosed scope) |
|---|---|
| Long-press power opens Gemini / Assistant | System framework |
| Long-press the navigation handle opens Circle to Search (also the side swipe-and-hold; optional short vibration) | System framework, XiaoAi |
| Remove the GMS China restriction, Quick Share and its control center tile included | System framework, System UI |
| Live translate in Circle to Search (tap Translate and keep translating as you scroll) | Google app |

All four start **off**. The tab also shows the state of the companion module, which is re-read on its own while the app is open. Tick each feature's scopes (the table above) in LSPosed.

**Companion module.** The features depend on the module XGoogle ships (id `coogle_companion`): it makes the Google app a system app, declares Circle to Search and its live translate to the system, and sets the power key and the navigation handle to Google on every boot. All4Hyper installs it through root (KernelSU / Magisk / APatch) from the tab, and it needs a reboot. The copy bundled here adds a switch for the power key, so an XGoogle companion (1.3.1) shows as *update available*.

- Turning the power or navigation switch on gives that key to Google at once (no reboot, and without the companion having booted); the key you had before is saved first. Turning it off hands it back. The power switch also makes Google the assistant (the `ASSISTANT` role) when it is not already; the key only opens Google's assistant while Google holds that role.
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
4. For Security Center switches, use **Restart → Security Center** once. The same button restarts System UI, or the whole system (a soft reboot) after confirming.

Everything starts off.

## App

Bottom navigation (Home, System, Google, Settings), Vietnamese by default with English in Settings, and light, dark, AMOLED (pure black) or system theme.

## Credits and licence

GPL-3.0 (see `LICENSE` at the repository root and `NOTICE.txt`). The Google features and the companion come from [XGoogle](https://github.com/Leekin2/XGoogle) by Leekin, itself a fork of Coogle by zopulus, with hooks derived from ONextBox. Live translate uses [DexKit](https://github.com/LuckyPray/DexKit).

The Power Keeper hooks (*Other* tab) are ported from [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler) (AGPL-3.0); see `NOTICE.txt`.

The Security Center hooks' targets (class and method names, which call does what) were found by reading [MeowPower](https://github.com/Xposed-Modules-Repo/io.github.yuhj319.meowpower) by yuhj319. MeowPower has no licence and its app shows a support page; this project re-implements the same hooks from scratch rather than copying its code. If you find them useful, consider supporting the original author.

## How it was checked

- The module builds, and the APK contains the LSPosed entry files (`META-INF/xposed/*`), the module class, DexKit's native library and the companion ZIP.
- The hook logic runs on a JVM against a fake LSPosed framework and fake system classes with the same names and signatures:
  - Security Center hooks: 108 checks (defaults, master switch, argument rewriting, what is dropped and what passes), run before the charging hooks were removed.
  - Google wiring: 54 checks over six scenarios (every switch off, off HyperOS, power, navigation, GMS, live translate): which hooks get installed for which switch, and that they follow the switch at run time.
- The root side runs the real Kotlin code through a fake `su`/`settings`: 20 checks on switching the keys and reading the companion state, plus the real `boot-completed.sh` (5 checks on the markers).
- **Not tested:** any of it on a real HyperOS device (class names come from MeowPower and XGoogle, not from your firmware), installing the companion with a real root manager, and the Compose UI on a device.

## Building

JDK 17+, Android SDK Platform 37 and Build Tools 36.0.0. Set `ANDROID_HOME` (or `sdk.dir` in `local.properties`), then:

```
python3 build-companion.py   # only after changing companion/
./gradlew assembleRelease
```

The APK is `app/build/outputs/apk/release/app-release.apk`, signed with the key in `signing/all4hyper.jks` (the password is in `app/build.gradle`). That key is public, so every build installs over the previous one as long as its `versionCode` is higher; it does not prove who built an APK. Builds before 0.6.1 used a throwaway key per build: uninstall those once.
