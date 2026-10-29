#!/usr/bin/env bash
# Source from the repository root on the development server.
export JAVA_HOME="$PWD/local/toolchain/jdk"
export ANDROID_HOME="$PWD/local/toolchain/android-sdk"
export GRADLE_USER_HOME="$PWD/local/toolchain/gradle-home"
export TMPDIR="$PWD/local/scratch/replica-foundation"
export ANDROID_USER_HOME="$PWD/local/toolchain/replica-android-user"
export ANDROID_AVD_HOME=/root/.android/avd
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$TMPDIR"
mkdir -p "$TMPDIR" "$GRADLE_USER_HOME" "$ANDROID_USER_HOME"
if [ -f "$GRADLE_USER_HOME/init.d/replica-mirror.gradle" ]; then
  export REPLICA_MAVEN_CACHE="file://$PWD/local/scratch/replica-foundation/maven"
fi
if [ ! -f "$PWD/local/toolchain/replica-debug.keystore" ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$PWD/local/toolchain/replica-debug.keystore" \
    -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'
fi
