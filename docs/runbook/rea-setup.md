# REA Setup (P0-04)

How the REA MCP server and the static-analysis tools it drives were installed on the dev
server, pinned and registered for Claude Code and Codex. Everything lives under the
git-ignored `local/toolchain/`. Nothing here touches the APK: analysis starts in P1a (D-019).

`<repo>` below is `/root/projects/assistant`.

## 1. Pinned tools

| Tool | Version | Source | SHA-256 of the download | Location |
|---|---|---|---|---|
| Node.js | 22.23.4 | `https://nodejs.org/dist/v22.23.4/node-v22.23.4-linux-x64.tar.xz` | `1805bcd35e6815559bd2eb97db438d3fdb0eb3a5e6a977b7c1badca76337e9ce` | `local/toolchain/node/` |
| JDK (Temurin) | 21.0.13+9, includes `jdk.compiler` | GitHub `adoptium/temurin21-binaries`, release `jdk-21.0.13+9`, `OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz` | `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c` | `local/toolchain/jdk/` |
| jadx-headless-mcp | 0.7.1 | GitHub `1013503897/jadx-headless-mcp`, release `v0.7.1`, `jadx-headless-mcp-0.7.1-all.jar` | `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75` | `local/toolchain/jadx/` |
| apktool | 2.12.1 | GitHub `iBotPeaches/Apktool`, release `v2.12.1`, `apktool_2.12.1.jar` | `66cf4524a4a45a7f56567d08b2c9b6ec237bcdd78cee69fd4a59c8a0243aeafa` | `local/toolchain/apktool/` |
| Ghidra | 12.1.4 | GitHub `NationalSecurityAgency/ghidra`, release `Ghidra_12.1.4_build`, `ghidra_12.1.4_PUBLIC_20260921.zip` | `ddac49f903da9d5bac833e5cc79395098b9c33cfd3279be5f31bd00387d2d4db` | `local/toolchain/ghidra/ghidra_12.1.4_PUBLIC/` |
| rea-agents | 6.8.0 | `registry.npmjs.org`, repository `git+https://github.com/morluto/rea.git` | npm integrity `sha512-9QDeRnGqEDzABZyr04eLnBXBv/c6I9/TWu/6M0MD4pEfkySI/BIgwTODIbAReX3C1x6hjw9+Jlx4A2yUSPZ9ww==` | `local/toolchain/rea/` |

Each hash was checked against the publisher's value: Node.js against the release's
`SHASUMS256.txt`, the JDK against its `.sha256.txt`, Ghidra against the SHA-256 in its release
notes, jadx-headless-mcp against the hash pinned in issue #5, and apktool against the asset
digest GitHub shows on the release page (the release notes give no separate hash).

The system Node (22.23.1) is too old for REA and is left alone. The commercial REA backends
(IDA, Hopper, JEB) need paid licences and are not installed. Dynamic tools (adb, emulator,
Frida) belong to P1a-02.

## 2. Install

Run from `<repo>`. No sudo, no global npm install, no change to the system Node or a conda env.

```shell
T=$PWD/local/toolchain

# Node.js
tar -xJf node-v22.23.4-linux-x64.tar.xz -C $T/node --strip-components=1

# JDK
tar -xzf OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz -C $T/jdk --strip-components=1
$T/jdk/bin/java --list-modules | grep jdk.compiler

# jadx-headless-mcp is used as downloaded. apktool gets a launcher (section 3)
$T/jdk/bin/java -jar $T/apktool/apktool_2.12.1.jar --version    # 2.12.1

# Ghidra
unzip -q $T/ghidra/ghidra_12.1.4_PUBLIC_20260921.zip -d $T/ghidra
$T/ghidra/ghidra_12.1.4_PUBLIC/support/analyzeHeadless          # prints usage

# REA
PATH=$T/node/bin:/usr/bin:/bin npm install --prefix $T/rea rea-agents@6.8.0
```

## 3. Environment

REA, and every agent registration, uses exactly these values:

| Variable | Value |
|---|---|
| `PATH` | `<repo>/local/toolchain/node/bin:<repo>/local/toolchain/jdk/bin:/usr/bin:/bin` |
| `JAVA_HOME` | `<repo>/local/toolchain/jdk` |
| `REA_JADX_MCP_JAR` | `<repo>/local/toolchain/jadx/jadx-headless-mcp-0.7.1-all.jar` |
| `GHIDRA_INSTALL_DIR` | `<repo>/local/toolchain/ghidra/ghidra_12.1.4_PUBLIC` |
| `REA_APKTOOL_COMMAND` | `<repo>/local/toolchain/apktool/apktool` |

- `REA_APKTOOL_COMMAND` must be an executable launcher, not the jar. Save this as
  `local/toolchain/apktool/apktool` and `chmod +x` it. Run on its own, it prints `2.12.1`.

  ```sh
  #!/bin/sh
  # apktool launcher for REA (REA_APKTOOL_COMMAND), using the project JDK.
  dir=$(cd "$(dirname "$0")" && pwd)
  exec "$dir/../jdk/bin/java" -jar "$dir/apktool_2.12.1.jar" "$@"
  ```
- When apktool decodes an APK it may keep framework files in `~/.local/share/apktool/`, outside
  the checkout.
- `ps` (procps) must be on `PATH`; Ghidra and JADX need it. It is `/usr/bin/ps`.
- Ghidra keeps per-user settings in `~/.config/ghidra/`, outside the checkout.

## 4. Readiness

```shell
T=$PWD/local/toolchain
env -i HOME=$HOME PATH=$T/node/bin:$T/jdk/bin:/usr/bin:/bin JAVA_HOME=$T/jdk \
  REA_JADX_MCP_JAR=$T/jadx/jadx-headless-mcp-0.7.1-all.jar \
  GHIDRA_INSTALL_DIR=$T/ghidra/ghidra_12.1.4_PUBLIC \
  REA_APKTOOL_COMMAND=$T/apktool/apktool \
  $T/rea/node_modules/.bin/rea doctor --json
```

The `node`, `jadx`, `ghidra` and `ghidra-java` checks must pass. REA's `inspect_apktool_client`
tool, which checks only the launcher, reports version 2.12.1. `doctor` still reports
`healthy: false` because these stay unconfigured on purpose: `hopper`, `ida-registration`,
`kuna-installation` and `skill:identity` (the REA skill comes with P0-07). The `registration:*`
checks report `config_drift` because the registrations in §5 are made by hand, not by
`rea setup`; this is expected.

`rea setup` is not run. Its dry run (`rea setup --dry-run --json --client claude_code --client codex`)
would write user-scope registrations without `PATH` and install the REA skill into
`~/.agents/skills/` and `~/.claude/skills/`, so the registrations below are made by hand.

## 5. Register the MCP server

Both registrations live in each agent's own user config; nothing is written into the repository.
On the dev server neither CLI is on `PATH`; use the binaries bundled with the VS Code extensions
(`~/.vscode-server/extensions/anthropic.claude-code-*/resources/native-binary/claude` and
`~/.vscode-server/extensions/openai.chatgpt-*/bin/linux-x86_64/codex`).

Claude Code, local scope (stored in `~/.claude.json` under this project):

```shell
T=$PWD/local/toolchain
claude mcp add --scope local rea \
  -e PATH=$T/node/bin:$T/jdk/bin:/usr/bin:/bin \
  -e JAVA_HOME=$T/jdk \
  -e REA_JADX_MCP_JAR=$T/jadx/jadx-headless-mcp-0.7.1-all.jar \
  -e GHIDRA_INSTALL_DIR=$T/ghidra/ghidra_12.1.4_PUBLIC \
  -e REA_APKTOOL_COMMAND=$T/apktool/apktool \
  -- $T/rea/node_modules/.bin/rea mcp
```

Codex, appended to `~/.codex/config.toml`:

```toml
[mcp_servers.rea]
command = "/root/projects/assistant/local/toolchain/rea/node_modules/.bin/rea"
args = ["mcp"]
startup_timeout_sec = 30

[mcp_servers.rea.env]
PATH = "/root/projects/assistant/local/toolchain/node/bin:/root/projects/assistant/local/toolchain/jdk/bin:/usr/bin:/bin"
JAVA_HOME = "/root/projects/assistant/local/toolchain/jdk"
REA_JADX_MCP_JAR = "/root/projects/assistant/local/toolchain/jadx/jadx-headless-mcp-0.7.1-all.jar"
GHIDRA_INSTALL_DIR = "/root/projects/assistant/local/toolchain/ghidra/ghidra_12.1.4_PUBLIC"
REA_APKTOOL_COMMAND = "/root/projects/assistant/local/toolchain/apktool/apktool"
```

Check: `claude mcp list` shows `rea ... ✔ Connected`, and `codex mcp list` shows `rea` enabled.

## 6. Check

`python tools/project_checks.py rea-environment` reads the installed versions and jar hashes
under `local/toolchain/` and fails if a tool is missing or differs from section 1. Where
`local/toolchain/` is absent (CI), it reports "not applicable".
