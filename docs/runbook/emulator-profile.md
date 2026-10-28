# Emulator Profile (P1a-02)

One Android emulator profile for the investigation and, later, the replica. This step installed
the SDK, created the AVD and booted it bare to check the profile. No app was installed, nothing was
signed in to, and the guest network was restricted. Running our own app and watching its traffic
is a separate step the owner decides (the API-map fork).

`<repo>` below is `/root/projects/assistant`. Recorded on 28 Oct 2026.

## 1. The profile

| Setting | Value | Set by |
|---|---|---|
| API level | 35 (Android 15), `system-images;android-35;google_apis;x86_64` revision 9 | The system image |
| Device | Pixel 7 (`pixel_7`, OEM Google, 6.31-inch) | `avdmanager create avd -d pixel_7` |
| Resolution | 1080 × 2400 | `hw.lcd.width`, `hw.lcd.height` in `config.ini`, from the device profile |
| Density | 420 dpi | `hw.lcd.density` in `config.ini`, from the device profile |
| Locale | `fa-IR` | `emulator -change-locale fa-IR` on the first boot; kept in the AVD's user data |
| Font scale | 1.0 | `adb shell settings put system font_scale 1.0`; kept in the AVD's user data |
| Input method | Gboard 14.2.09, preinstalled in the image (`com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME`), with two subtypes enabled: Persian `fa_IR` (selected) and "Alphabet" `und-Latn` QWERTY for Latin. The globe key switches between them. | Gboard enables both subtypes itself once the locale is `fa-IR`; nothing else was changed |
| Hardware keyboard | None (`hw.keyboard=no`), as on the phone | `config.ini`, from the device profile |
| Guest network | Restricted: QEMU user-mode networking with `restrict=on` | Emulator boot options (section 5) |

`config.ini` has no key for locale, font scale or input method (the emulator's own list of keys is
`emulator/lib/hardware-properties.ini`), so those three are set inside Android and live in the
AVD's user data, not in `config.ini`.

**API level.** The app is `minSdk 24`, `targetSdk 37` (P1a-01, Observed). The issue assumed API 37
is not a released Android version and asked for 35, else 34. API 35 is available, so it is used.
However, the SDK index of 28 Oct 2026 also lists stable (`channel-0`) `google_apis` x86_64 images
for API 36, 36.1 and 37.0. Whether to move the profile to API 37, to match `targetSdk`, is for the
managers to decide.

## 2. Where the files are

| What | Path | In git |
|---|---|---|
| Android SDK | `<repo>/local/toolchain/android-sdk/` (`cmdline-tools/latest`, `platform-tools`, `emulator`, `system-images/android-35/google_apis/x86_64`, `licenses`) | No |
| Host libraries for the emulator | `<repo>/local/toolchain/emulator-hostlibs/` (section 4) | No |
| JDK used by `sdkmanager`/`avdmanager` | `<repo>/local/toolchain/jdk/` (Temurin 21.0.13, from P0-04) | No |
| AVD | `~/.android/avd/ragbot_investigation.avd/` and `~/.android/avd/ragbot_investigation.ini` (`/root/.android/avd/`) | No |
| Downloaded zips and indexes | `<repo>/local/scratch/p1a02/repo/` (safe to delete) | No |

## 3. Download source and verification

From this server `dl.google.com` answers, but every Android path under it returns 404, and
`developer.android.com` returns 403 (probe of 28 Oct 2026, recorded on issue #28). After that stop,
the instruction for this task was changed on 28 Oct 2026 to allow a mirror, overriding the issue's
"do not look for mirrors". The packages came from the Tencent Cloud mirror of the SDK
repository, `https://mirrors.cloud.tencent.com/AndroidSDK/`, which serves Google's repository
files and indexes.

How the files were checked:

1. The mirror's indexes (`repository2-4.xml`, `sys-img/google_apis/sys-img2-4.xml`) give each
   package's SHA-1 and size.
2. Those values were compared with nixpkgs' `pkgs/development/mobile/androidenv/repo.json`
   (commit `d206ccbcf9b8`, 9 Oct 2026), which a bot builds from `dl.google.com` directly. Every
   package below matched in SHA-1 and size.
3. Each downloaded zip was checked with `sha1sum -c` and its size, and `sdkmanager` checked the
   SHA-1 again when it installed it.
4. Google's download page, which publishes the SHA-256 of the command-line tools zip, could not be
   reached (403 from this server and from the agent's fetch tool). The SHA-256 values below were
   computed here and are recorded for later checks; they were not compared with a published value.

## 4. Installed versions

| Package | Version | File | Size (bytes) | SHA-1 (Google index) | SHA-256 (computed) |
|---|---|---|---|---|---|
| `cmdline-tools;latest` (installed as `cmdline-tools/latest`) | 22.0 | `commandlinetools-linux-15859902_latest.zip` | 181833628 | `040d3996a65543d22ec4bf73e4c37aa37a8d4af4` | `4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583` |
| `platform-tools` | 37.0.1 (adb 1.0.41) | `platform-tools_r37.0.1-linux.zip` | 9054187 | `477254aa5f903c15cf51001717bdf347fb6b53e0` | `d230f13842f60f782a8645f9c813f8f845bf36089ea7289f28c48f17979313f1` |
| `emulator` | 37.2.12 (build 16428233) | `emulator-linux_x64-16428233.zip` | 349654171 | `cd7362ea55dfb86a418958138dc396e74165dd01` | `b08fc43d8608d2955f607f1b287beb525041e730086bdca3af152accac3af9c1` |
| `system-images;android-35;google_apis;x86_64` | 9 | `sys-img/google_apis/x86_64-35_r09.zip` | 1738815903 | `0103e6dab21290c4b9d16550a3ce99476f884eef` | `c67b9ba0ff5bc0eb6d046871bfa228af14d4d47b02f0cdae94f048e511b7566e` |

All four use the `android-sdk-license`, accepted with `yes | sdkmanager --licenses`; the accepted
licenses are in `local/toolchain/android-sdk/licenses/`.

**Why cmdline-tools 22.0, not 23.0.** 23.0 is the newest, but its `sdkmanager` is only a shim over
the new Android CLI, and the `android` binary it calls is a launcher that downloads the CLI itself
on first run from `dl.google.com/android/cli/latest/...`, with no pinned version or checksum. That
path is blocked here, and an unpinned download does not fit this setup. 22.0 is the newest release
whose `sdkmanager` and `avdmanager` are the Java tools. (23.0's zip also matched its SHA-1
`e025545c62a8e64c7559119566a569fb1dec5f60`; it is not installed.)

**Host libraries.** The emulator's QEMU needs `libpulse.so.0`, which this host does not have.
Installing it would change the system packages, so instead ten Ubuntu 24.04 packages were fetched
with `apt-get download` (download only; each `.deb` matched the SHA-256 in apt's signed index) and
unpacked with `dpkg-deb -x` into `local/toolchain/emulator-hostlibs/root/`. The emulator finds them
through `LD_LIBRARY_PATH` (section 5). Packages: `libpulse0` 1:16.1+dfsg1-2ubuntu10.1,
`libsndfile1` 1.2.2-1ubuntu5.24.04.1, `libasyncns0` 0.8-6build4, `libflac12t64` 1.4.3+ds-2.1ubuntu2,
`libogg0` 1.3.5-3build1, `libvorbis0a` 1.3.7-1build3, `libvorbisenc2` 1.3.7-1build3, `libopus0`
1.4-1build1, `libmpg123-0t64` 1.32.5-1ubuntu1.1, `libmp3lame0` 3.100-6build1.

## 5. Commands

Environment, for every command below:

```sh
export ANDROID_HOME=<repo>/local/toolchain/android-sdk
export JAVA_HOME=<repo>/local/toolchain/jdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
H=<repo>/local/toolchain/emulator-hostlibs/root/usr/lib/x86_64-linux-gnu
export LD_LIBRARY_PATH=$H:$H/pulseaudio
```

Install. The zips and indexes were downloaded with `curl` from the mirror into
`local/scratch/p1a02/repo/`, laid out like the repository, and checked as in section 3;
`sdkmanager` then installed from that folder:

```sh
mkdir -p $ANDROID_HOME/cmdline-tools
unzip -q commandlinetools-linux-15859902_latest.zip -d <empty-dir>
mv <empty-dir>/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
export SDK_TEST_BASE_URL=file://<repo>/local/scratch/p1a02/repo/
yes | sdkmanager --licenses
sdkmanager --install platform-tools emulator "system-images;android-35;google_apis;x86_64"
```

`sdkmanager` warns that it is deprecated and that it cannot read the other system-image indexes
(only `google_apis` is in the local folder); both are expected.

Create the AVD:

```sh
echo no | avdmanager create avd -n ragbot_investigation -k "system-images;android-35;google_apis;x86_64" -d pixel_7
```

`echo no` declines a custom hardware profile. `avdmanager` prints `Could not load devices from
.../x86_64/devices.xml`, an optional device list this image does not ship; the AVD is created from
`pixel_7` anyway. No `config.ini` value was edited by hand. The values that define the profile:
`hw.device.name=pixel_7`, `hw.device.manufacturer=Google`, `hw.lcd.width=1080`,
`hw.lcd.height=2400`, `hw.lcd.density=420`, `hw.keyboard=no`, `hw.ramSize=2G`, `hw.cpu.ncore=4`,
`abi.type=x86_64`, `tag.id=google_apis`, `PlayStore.enabled=no`.

Boot it bare (first boot sets the locale):

```sh
emulator -avd ragbot_investigation -no-window -gpu swiftshader_indirect -no-snapshot -no-boot-anim \
  -no-metrics -change-locale fa-IR \
  -wifi-user-mode-options restrict=on -network-user-mode-options restrict=on
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
```

`restrict=on` is QEMU's option to isolate the guest: its packets are not routed beyond the host
(section 6 records what was observed). API 31 and later use
`-wifi-user-mode-options` for Wi-Fi and `-network-user-mode-options` for the radio, so both are set.
`-no-metrics` stops the emulator itself sending usage data. adb does not use the guest network.

After the first boot, make the font scale explicit (it already read 1.0):

```sh
adb shell settings put system font_scale 1.0
```

Later boots are the same command without `-change-locale fa-IR`. Shut down with `adb emu kill`.

## 6. Boot verification

The host has KVM (`/dev/kvm` present, CPU flag `vmx`; `emulator -accel-check`: "KVM (version 12)
is installed and usable"). The AVD was booted bare, headless, three times on 28 Oct 2026:
first with `-change-locale fa-IR`, then twice without it to check that the settings stay.

| Check | Command | Result |
|---|---|---|
| Boot completes | `getprop sys.boot_completed` | `1` after 63 s (first boot) and 53 s (second boot) |
| Android version | `getprop ro.build.version.release` / `ro.build.version.sdk` | 15 / 35 (build `AE3A.240806.043`) |
| Locale | `getprop persist.sys.locale`; `am get-config` | `fa-IR`; configuration `fa-rIR`, `ldrtl` (right-to-left) |
| Resolution | `wm size` | `Physical size: 1080x2400` |
| Density | `wm density` | `Physical density: 420` |
| Font scale | `settings get system font_scale` | `1.0` |
| Input method | `settings get secure default_input_method` / `enabled_input_methods` / `selected_input_method_subtype` | Gboard; subtypes `-2105124344` (Persian `fa_IR`) and `-1097284911` (Alphabet, Latin); Persian selected |
| No app installed | `pm list packages -3` | Empty |
| Guest network | `dumpsys connectivity` | One network (cellular), evaluated but never validated; Wi-Fi enabled, not connected |

All values were the same on the second boot, without `-change-locale`. Android's own connectivity
check never succeeded, which fits the `restrict=on` isolation, though it does not prove it alone:
Google's hosts may refuse this server anyway.

Emulator log notes, none of which stopped the boot: the emulator's host process tried its update
check and a feature-flag download from Google, and both failed (it is the emulator binary, not an
Android app); `pulseaudio: Failed to initialize PA context` (no sound server on the host, expected
headless).

## 7. ABI note

The host is x86_64. The app ships `arm64-v8a` and `armeabi-v7a` native libraries, including
TensorFlow Lite (P1a-01, Observed). This image reports:

| Property | Value |
|---|---|
| `ro.product.cpu.abilist` | `x86_64,arm64-v8a` |
| `ro.product.cpu.abilist32` | empty |
| `ro.dalvik.vm.native.bridge` | `libndk_translation.so` |
| `ro.enable.native.bridge.exec` | `1` |

So the x86_64 image has ARM translation for 64-bit ARM code: our app could install and use its
`arm64-v8a` libraries through it. It offers no 32-bit ABI, so `armeabi-v7a` would not be used.
Whether the app's native code, TensorFlow Lite in particular, runs correctly and fast enough under
translation is not tested here. An `arm64-v8a` system image is not a way round this on this host:
the emulator runs ARM64 images only on ARM64 hosts (not tried here). This note is recorded, not
resolved; running our app is a separate step.
