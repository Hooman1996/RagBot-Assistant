# Agent Skills (P0-07)

The skills the owner chose in issue #20, installed for Claude Code and Codex. They live in the
git-ignored `.agents/skills/`, which Codex reads directly; Claude Code reads the same folder
through the git-ignored symlink `.claude/skills -> ../.agents/skills`. Each skill was copied
whole from its source at the commit below, and every file was read before it was installed.

`<repo>` below is `/root/projects/assistant`.

## 1. Installed skills

| Skill | Source repository | Path inside it | Commit | License |
|---|---|---|---|---|
| `r8-analyzer` | `github.com/android/skills` | `performance/r8-analyzer` | `45fd1739a4dbca5452c3e936570797f7f6261cd7` | Apache-2.0 |
| `navigation-3` | `github.com/android/skills` | `navigation/navigation-3` | `45fd1739a4dbca5452c3e936570797f7f6261cd7` | Apache-2.0 |
| `testing-setup` | `github.com/android/skills` | `testing/testing-setup` | `45fd1739a4dbca5452c3e936570797f7f6261cd7` | Apache-2.0 |
| `android-intent-security` | `github.com/android/skills` | `security/android-intent-security` | `45fd1739a4dbca5452c3e936570797f7f6261cd7` | Apache-2.0 |
| `jetpack-compose-performance` | `github.com/android/skills` | `jetpack-compose/jetpack-compose-performance` | `45fd1739a4dbca5452c3e936570797f7f6261cd7` | Apache-2.0 |
| `android-emulator-qa` | `github.com/openai/plugins` | `plugins/test-android-apps/skills/android-emulator-qa` | `0722921d5542fc593105c27bd52630babd8b8c2a` | Apache-2.0 |
| `fastapi` | `github.com/fastapi/fastapi` | `fastapi/.agents/skills/fastapi` | `79cab5bed28b24d92c185d236feb33f5783dfeb3` | MIT |
| `async-python-patterns` | `github.com/wshobson/agents` | `plugins/python-development/skills/async-python-patterns` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `python-testing-patterns` | `github.com/wshobson/agents` | `plugins/python-development/skills/python-testing-patterns` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `debugging-strategies` | `github.com/wshobson/agents` | `plugins/developer-essentials/skills/debugging-strategies` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `openapi-spec-generation` | `github.com/wshobson/agents` | `plugins/documentation-generation/skills/openapi-spec-generation` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `postgresql-table-design` | `github.com/wshobson/agents` | `plugins/database-design/skills/postgresql-table-design` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `rag-implementation` | `github.com/wshobson/agents` | `plugins/llm-application-dev/skills/rag-implementation` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `embedding-strategies` | `github.com/wshobson/agents` | `plugins/llm-application-dev/skills/embedding-strategies` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `llm-evaluation` | `github.com/wshobson/agents` | `plugins/llm-application-dev/skills/llm-evaluation` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `vector-index-tuning` | `github.com/wshobson/agents` | `plugins/llm-application-dev/skills/vector-index-tuning` | `0874a27b17a22e2b7c2e2bcd725768e0803cad06` | MIT |
| `langgraph-fundamentals` | `github.com/langchain-ai/langchain-skills` | `config/skills/langgraph-fundamentals` | `126c384d8552dcb476b226c244172186cc3f4328` | MIT |
| `langgraph-persistence` | `github.com/langchain-ai/langchain-skills` | `config/skills/langgraph-persistence` | `126c384d8552dcb476b226c244172186cc3f4328` | MIT |
| `langgraph-human-in-the-loop` | `github.com/langchain-ai/langchain-skills` | `config/skills/langgraph-human-in-the-loop` | `126c384d8552dcb476b226c244172186cc3f4328` | MIT |
| `reverse-engineer-anything` | npm `rea-agents` 6.8.0, installed in `local/toolchain/rea/` (P0-04) | `node_modules/rea-agents/skills/reverse-engineer-anything` | npm 6.8.0 (no git commit in the package) | MIT |

The Jetpack Compose skill: `jetpack-compose/` holds no single general UI skill, so
`jetpack-compose-performance` was picked. The others there cover adaptive layouts, XML
migration and inking.

Where a repository keeps its license only at the root, that file was copied into the skill's
folder. `android-emulator-qa` ships its own `LICENSE.txt`.

The four RAG skills were already on the server. They exist in `wshobson/agents` and were
reinstalled from there; the server copies matched those files.

## 2. Notes for agents using them

- `android-emulator-qa` writes its examples to `/tmp/...`. Use `local/scratch/` instead.
- `postgresql-table-design` recommends `NUMERIC` for money. Rule 6 wins: integers with a unit.
- The RAG and evaluation examples use cloud services (Pinecone, Voyage, OpenAI, Cohere,
  LangSmith). Our stack is the local TEI, Qdrant and vLLM; do not copy those clients in.
- `reverse-engineer-anything` suggests `npx -y rea-agents@6.8.0 ...` for repairs. Use the
  installed `local/toolchain/rea/node_modules/.bin/rea` instead, as its own text prefers.
- Install commands in skills (`pip`, `npm install -g`, `android sdk`) are not run without an
  approved issue.

## 3. Removed

Moved, not deleted, to `local/scratch/removed-skills/`:

| Skill | On the server before P0-07 |
|---|---|
| `fastapi-templates` | Yes, moved |
| `python-observability` | Yes, moved |
| `python-performance-optimization` | Yes, moved |
| `ragbot-inference-benchmark`, `ragbot-load-test`, `ragbot-performance-audit`, `design-taste-frontend`, `frontend-design`, `grafana-dashboards`, `prometheus-configuration`, `supabase-postgres-best-practices` | No, nothing to move |

The previous copies of the kept skills are in `local/scratch/old-skills/`.

## 4. Not installed, and why

From issue #20:

- `limrun-android-emulator`: a cloud emulator; the APK would be uploaded to a third party.
- `firebase-apk-scanner`: probes the target app's live backend (no production calls).
- `clerk-android`: Clerk sign-in SDK, unrelated.
- `argent-android-emulator-setup`: needs the Argent MCP and targets React Native.
- `android-clean-architecture`: heavy multi-module architecture, against rule 8.
- `appium/skills`: very few installs; adb-based skills cover the emulator.
- `new-silvermoon/awesome-android-agent-skills`: overlaps Google's official skills.
- `flutter/agent-plugins`: only if P1a shows the app is built with Flutter.

Found while installing:

- `android-cli` (`android/skills`): its `SKILL.md` installs the CLI with
  `curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash`.
- `static-analysis` (`trailofbits/skills`): no skill has this name. It is a plugin folder that
  holds three skills: `semgrep`, `codeql` and `sarif-parsing`.
- `insecure-defaults` (`trailofbits/skills`): no skill has this name. It is a plugin folder
  with a command and references but no `SKILL.md`.

## 5. Reinstall

Run from `<repo>`. Fetch each source at its pinned commit into `local/scratch/`:

```shell
fetch() {  # fetch <owner/repo> <commit>
  d=local/scratch/skill-sources/${1/\//-}
  git init -q $d && git -C $d fetch -q --depth 1 https://github.com/$1 $2 && git -C $d checkout -q FETCH_HEAD
}
fetch android/skills 45fd1739a4dbca5452c3e936570797f7f6261cd7
fetch openai/plugins 0722921d5542fc593105c27bd52630babd8b8c2a
fetch fastapi/fastapi 79cab5bed28b24d92c185d236feb33f5783dfeb3
fetch wshobson/agents 0874a27b17a22e2b7c2e2bcd725768e0803cad06
fetch langchain-ai/langchain-skills 126c384d8552dcb476b226c244172186cc3f4328
```

Read every file of a skill, then copy its whole folder (the path in §1) to
`.agents/skills/<name>/`, plus the root license where the table says so:

```shell
S=local/scratch/skill-sources
cp -a $S/android-skills/performance/r8-analyzer .agents/skills/
cp $S/android-skills/LICENSE.txt .agents/skills/r8-analyzer/
# ...the same for each row of §1
cp -a local/toolchain/rea/node_modules/rea-agents/skills/reverse-engineer-anything .agents/skills/
cp local/toolchain/rea/node_modules/rea-agents/LICENSE .agents/skills/reverse-engineer-anything/
```

Link Claude Code to the same folder and check both paths are ignored:

```shell
ln -s ../.agents/skills .claude/skills
git check-ignore -v .agents/skills .claude/skills
```

## 6. Check

Use the CLI binaries from the VS Code extensions (`docs/runbook/rea-setup.md` §5), run from
`<repo>`:

- Claude Code: `claude -p "Reply OK" --output-format stream-json --verbose --max-turns 1`.
  The `init` event's `skills` list holds all 20 names.
- Codex: `codex debug prompt-input` (no model call). Its skill list holds all 20 names under
  the root `/root/projects/assistant/.agents/skills`.

Checked on 27 Oct 2026: both list all 20, and none of the removed skills.
