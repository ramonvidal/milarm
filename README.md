# Milarm

An Android alarm clock that also buzzes your **Xiaomi Smart Band 10**, with a **wake-up check**
that rings again if you go back to sleep.

## Features

- Alarms with time, repeat days, label and on/off switch
- Sound modes: **ringtone + vibrate**, **vibrate only**, or **band only** (phone stays silent)
- Full-screen alarm over the lock screen with **Snooze** and **Dismiss**
- While ringing, a fresh notification is posted every 30 s. Mi Fitness forwards each one to the
  band, so your wrist keeps buzzing until you dismiss.
- **Wake-up check** (per alarm): after you dismiss, the phone watches its motion sensors for
  *X* minutes (default 5)
  - Movement found (walking with the phone, using it, unlocking it): done, quietly
  - No movement: "Are you up?" (buzzes the band); tap **I'm up** within 30 s
  - No answer: the alarm rings again in full. Repeats up to 3 rounds (configurable).
- Alarms survive reboots, clock changes and battery saver (`AlarmManager.setAlarmClock`)

## Getting the band to buzz

There is no custom Bluetooth code. The band buzzes through Mi Fitness notification forwarding.

1. In **Mi Fitness → your band → App notifications**, turn on **Milarm**.
2. Some Mi Fitness versions only forward notifications while the phone screen is off.

## Wake-up check: what counts as movement

Movement is detected **by the phone**, because Mi Fitness does not share live band data.
It passes when any of these happen after you dismiss:

- 15+ steps (needs the *Physical activity* permission)
- about 20 s of real motion (rolling over in bed doesn't count)
- unlocking the phone

If you leave the phone on the nightstand while you walk around, it will ask "Are you up?". One tap
on the phone settles it.

## Install

On your phone, open
<https://github.com/ramonvidal/milarm/releases/latest/download/Milarm.apk> and install it.
Allow "install unknown apps" when asked. On first launch, grant the permissions listed on the main
screen.

## Updates

Every push to `main` publishes a release tagged `v0.<build>`. The app checks the release page
every 6 hours (when opened) and shows **"Milarm 0.x is available – Update"** at the top of the
alarm list. **⋮ → Check for updates** checks right away. Tapping **Update** downloads the APK and
hands it to Android's installer. The first time, Android asks you to allow Milarm to install apps.

### Signing key

Android only installs an update signed with the same key as the installed app, so every
published build is signed with one fixed key (`milarm-release.keystore`, alias and passwords
`milarm`). It is never committed: CI writes it from the repository secret
`SIGNING_KEYSTORE_BASE64` (the keystore file, base64-encoded), and skips publishing a release
without it. Keep a copy of the keystore somewhere safe: losing it means the next build can't
update installed copies. To build a signed APK locally, put the keystore in `app/` (it's
git-ignored).

## Build

Requires JDK 17 and the Android SDK (Android Studio works out of the box).

```bash
./gradlew test assembleDebug   # APK in app/build/outputs/apk/debug/
```

The alarm logic lives in the plain-Kotlin `core` module and can be tested without an Android SDK:

```bash
./gradlew -PcoreOnly=true :core:test
```

## Project layout

```
core/   pure Kotlin: Alarm model, next ring time, wake-up check detector, alarm state machine (+ tests)
app/    Android app
  alarm/  scheduler, receivers, AlarmService (ringing, band alerts, wake-up check), notifications
  data/   alarm storage
  update/ release check, download and install of new builds
  ui/     alarm list, alarm editor, ringing screen, permissions, update banner
```
