# Try it

This page starts the backend, opens the graph page and the viewer, and runs a task by clicking a
node. Run everything on the server, from `/root/projects/assistant`, unless it says
otherwise.

## 1. Start the emulator

Skip this if it is already running. Check with `adb devices`.

```bash
source android/env.sh
nohup $ANDROID_HOME/emulator/emulator -avd ragbot_investigation -no-window \
  -gpu swiftshader_indirect -no-snapshot -no-boot-anim -no-metrics \
  -wifi-user-mode-options restrict=on -network-user-mode-options restrict=on \
  > local/scratch/emulator.log 2>&1 < /dev/null &
adb -s emulator-5554 wait-for-device
until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
```

## 2. Start the backend

Use a second terminal, and keep it open. Ctrl-C stops the backend.

```bash
cd /root/projects/assistant
conda activate assistant
python -m assistant
```

It listens on `127.0.0.1:8100`, not port 8000, which another service uses.

## 3. Turn on the app's loop

```bash
source android/env.sh
./android/gradlew -p android --no-daemon :app:installDebug
adb -s emulator-5554 shell am start -n org.ragbot.replica/.MainActivity
adb -s emulator-5554 reverse tcp:8100 tcp:8100
adb -s emulator-5554 shell touch /sdcard/Android/data/org.ragbot.replica/files/bridge.enabled
adb -s emulator-5554 shell am force-stop org.ragbot.replica
adb -s emulator-5554 shell am start -n org.ragbot.replica/.MainActivity
```

The first launch creates the folder for the flag file, so the app starts twice. The `adb reverse`
link ends when the emulator stops, so repeat that line after a reboot.

## 4. Start the viewer

```bash
local/toolchain/replica-viewer/start.sh
cat local/scratch/replica-viewer/password.txt
```

## 5. Forward two ports

Both pages listen on the server only. On your own machine, forward them.

- **VS Code Remote SSH:** in the Ports tab, forward `8100` and `6080`, and keep both private.
- **Windows PowerShell:** keep this window open.

```powershell
ssh -N -o ExitOnForwardFailure=yes -L 127.0.0.1:8100:127.0.0.1:8100 -L 127.0.0.1:6080:127.0.0.1:6080 YOUR_SSH_HOST
```

## 6. Open the two pages side by side

- The graph: <http://127.0.0.1:8100/graph>
- The replica: <http://127.0.0.1:6080/vnc.html?autoconnect=1&resize=scale&path=websockify>

The viewer asks for the password from step 4. If VS Code picks other local ports, use those.

## 7. Click a node

On the graph, green is built, grey is not, amber is where the replica is now, and a blue outline
marks the route of your last click. Drag to pan, use the wheel to zoom, and type in the box to find
a screen.

1. **Card to card to رضا.** Find `S-174` and click it. The panel shows the route from where the
   replica is, and three questions. Press **Fill with demo values**, put `رضا` and `250000` in if you
   like, and press **Run**. The amber node follows the replica, and each command and observation
   appears in the panel. It stops on `S-176`, the review, and says so. Only the user confirms.
2. **A bill payment.** Click `S-209`, press **Fill with demo values**, then **Run**. It stops on the
   review, `S-207`.
3. **A screen with a selection.** Click `S-284`. The first question is which charity, a selection
   on the way there rather than a field. Fill and run. The choice travels as `navigate`'s argument.
4. **A node with no questions.** Click `S-085`. The replica just moves there.
5. **A receipt.** Click `S-093`. The panel says only the user's own step leads there, and nothing
   is sent.
6. **A grey node.** The panel says it is not built, and nothing is sent.

The words in the steps are explained in
[every way it can stop](command-lifecycle.md#every-way-it-can-stop).

## Check it from the command line

With the backend running and the loop on, this runs card to card to رضا through the same API and
checks the review:

```bash
source android/env.sh
python tools/bridge_e2e.py
```

To run every feature with its demo values and see how each ended, add `--features`. It takes about
15 minutes and restarts the app between features.

## Stop it

```bash
source android/env.sh
adb -s emulator-5554 shell rm /sdcard/Android/data/org.ragbot.replica/files/bridge.enabled
adb -s emulator-5554 reverse --remove tcp:8100
local/toolchain/replica-viewer/stop.sh
```

Then press Ctrl-C in the backend's terminal. To stop the emulator as well, run
`adb -s emulator-5554 emu kill`.
