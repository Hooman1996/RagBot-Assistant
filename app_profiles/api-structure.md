# Whole-app API-structure map (P1a-05)

The third whole-app map: the HTTP calls the app makes, grouped by the P1a-03 feature area, with each call's method, parameter shapes and request/response model shapes. Built by static analysis of the catalogued app's Retrofit service interfaces; the app was not run. It sits beside `feature-inventory.md` (P1a-03) and `navigation-graph.json` (P1a-04) as structured data (`api-structure.json`) the later Python prototype can load.

- **Method:** the 34 Retrofit/OkHttp service interfaces recovered from the decompiled code (REA MCP server) — HTTP-method annotation, `@Url`/`@Header`/`@Body`/`@Query`/`@Part` parameters, and the Kotlin request/response model types. Real method/model names come from each class's Kotlin `@Metadata`, decoded offline. No execution, no network.
- **Sanitization:** semantic names and `E-###`/`M-###` IDs only (see `README.md`). Real service/method/model classes, header names and the path/base-URL recovery attempt are in the git-ignored `local/evidence/findings/api-private.md`.
- **Labels:** `Recovered` (method, params and models read directly from the typed interface + metadata), `Inferred` (reasoned from a caller/model), `Unknown` (identity/type not statically resolvable, with the reason). Nothing is `Observed` — the app was not run.
- **Paths are not here.** Every call takes its URL via Retrofit `@Url`, built at runtime from encrypted constants; no path or host literal appears at the interface. See the decryption note in `api-private.md`.

**Totals:** 520 endpoints and 60 models across 24 feature-area groups. Endpoints by label: Recovered 445, Inferred 0, Unknown 75, Observed 0. Methods: GET 210, POST 270, PUT 29, DELETE 11. Models by label: Recovered 59, Unknown 1.

## F-015 — Login (6)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-130 | login with refresh token | POST | bearer | M-003 → M-029 | Recovered |
| E-131 | normal login | POST | none | M-003 → M-029 | Recovered |
| E-132 | register | POST | none | M-003 → M-029 | Recovered |
| E-133 | is vip | GET | none | — → M-029 | Recovered |
| E-134 | activation | POST | none | M-013 → M-011 | Recovered |
| E-135 | inquiry referral | POST | bearer | M-002 → M-029 | Recovered |

## F-022 — Settings (32)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-369 | search facilities | POST | bearer | M-002 → M-029 | Recovered |
| E-370 | search anonymous facilities | PUT | bearer | M-002 → M-029 | Recovered |
| E-371 | disable facility with login | POST | bearer | M-002 → M-029 | Recovered |
| E-372 | enable facility with login | POST | none | M-002 → M-029 | Recovered |
| E-373 | disable facility anonymous | POST | bearer | M-002 → M-029 | Recovered |
| E-374 | enable facility anonymous | POST | bearer | M-002 → M-029 | Recovered |
| E-375 | disable facility biometric | POST | none | M-002 → M-029 | Recovered |
| E-376 | enable facility biometric | PUT | none | M-002 → M-029 | Recovered |
| E-377 | ask no login | POST | bearer | M-002 → M-029 | Recovered |
| E-378 | de active biometric | DELETE | bearer | — → M-029 | Recovered |
| E-379 | ask no active | POST | none | M-014 → M-058 | Recovered |
| E-380 | get all ticket list | POST | none | M-002 → M-029 | Recovered |
| E-381 | view ticket thread | POST | none | — → M-029 | Recovered |
| E-382 | get all ticket list no login | PUT | none | M-002 → M-029 | Recovered |
| E-383 | enable biometric | POST | bearer | M-002 → M-029 | Recovered |
| E-384 | get all ticket list no active | POST | bearer | M-019 → M-040 | Recovered |
| E-385 | ticket req otp | GET | bearer | — → M-029 | Recovered |
| E-386 | view ticket thread no login | POST | none | M-002 → M-029 | Recovered |
| E-387 | ask | POST | bearer | M-002 → M-029 | Recovered |
| E-388 | view ticket thread no active | POST | bearer | M-018 → M-032 | Recovered |
| E-389 | close ticket | POST | none | M-002 → M-029 | Recovered |
| E-390 | close ticket no login | PUT | bearer | M-002 → M-029 | Recovered |
| E-391 | ticket req otp no active | POST | none | — → — | Recovered |
| E-392 | close ticket no active | POST | none | M-016 → M-029 | Recovered |
| E-393 | get fa qs | PUT | none | M-002 → M-029 | Recovered |
| E-394 | reply ticket | POST | none | M-002 → M-029 | Recovered |
| E-395 | get fa qs no login | PUT | none | M-012 → M-030 | Recovered |
| E-396 | ticket verify otp | POST | bearer | M-002 → M-029 | Recovered |
| E-397 | ticket verify otp no active | POST | bearer | M-020 → M-041 | Recovered |
| E-398 | edit ticket no login | POST | none | M-002 → M-029 | Recovered |
| E-399 | edit ticket no active | POST | bearer | M-017 → M-058 | Recovered |
| E-400 | send suggestion | POST | bearer | M-002 → M-029 | Recovered |

## F-025 — Notifications (16)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-185 | activate auth notification in setting | PUT | bearer | M-002 → M-029 | Recovered |
| E-186 | inquiry notification in setting | PUT | bearer | M-002 → M-029 | Recovered |
| E-187 | de activate notification in setting | POST | bearer | M-002 → M-029 | Recovered |
| E-188 | activate notification | POST | bearer | M-002 → — | Recovered |
| E-189 | get notification item list | POST | bearer | M-002 → M-029 | Recovered |
| E-190 | get notification unseen count | GET | bearer | — → M-049 | Recovered |
| E-191 | activate auth notification in login | POST | none | M-002 → — | Recovered |
| E-192 | inquiry notification in home | POST | none | M-002 → M-048 | Recovered |
| E-193 | get notification type list | GET | bearer | — → M-029 | Recovered |
| E-194 | get notification unseen count new | GET | bearer | — → M-029 | Recovered |
| E-195 | delete notification | POST | bearer | M-002 → M-029 | Recovered |
| E-196 | de activate notification in login | POST | bearer | M-002 → — | Recovered |
| E-197 | de activate notification in home | POST | bearer | M-002 → — | Recovered |
| E-198 | set seen notification | POST | bearer | M-002 → M-029 | Recovered |
| E-199 | seen all notifications | POST | bearer | M-002 → M-029 | Recovered |
| E-200 | delete all notifications | POST | bearer | M-002 → M-029 | Recovered |

## F-028 — Accounts (43)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-001 | get source accounts | GET | bearer | — → M-029 | Recovered |
| E-002 | get default account balance | GET | bearer | — → M-029 | Recovered |
| E-003 | get transaction source account | GET | bearer | — → M-029 | Recovered |
| E-004 | get transaction source accounts new arch | GET | bearer | — → M-029 | Recovered |
| E-005 | get transaction individual source accounts new arch | GET | bearer | — → M-029 | Recovered |
| E-006 | get account info with connected cards | GET | bearer | — → M-007 | Recovered |
| E-007 | download file | GET | bearer | — → M-029 | Recovered |
| E-008 | get source accounts | GET | bearer | — → M-009 | Recovered |
| E-009 | proxy account agreement | GET | bearer | — → M-029 | Recovered |
| E-010 | account info | GET | bearer | — → M-004 | Recovered |
| E-011 | trace proxy account | GET | bearer | — → M-029 | Recovered |
| E-012 | accepting accounts | GET | bearer | — → M-009 | Recovered |
| E-013 | account info new | GET | bearer | — → M-004 | Recovered |
| E-014 | get transaction source accounts | POST | bearer | — → M-009 | Recovered |
| E-015 | get active accounts | GET | bearer | — → M-004 | Recovered |
| E-016 | unfreeze account | POST | bearer | M-002 → — | Recovered |
| E-017 | cancel advocacy | PUT | bearer | M-002 → M-029 | Recovered |
| E-018 | get agent source accounts | POST | bearer | — → M-009 | Recovered |
| E-019 | add accounts for activation | POST | bearer | M-002 → — | Recovered |
| E-020 | get account balance | GET | bearer | — → M-029 | Recovered |
| E-021 | proxy account pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-022 | set default account | GET | bearer | — → M-023 | Recovered |
| E-023 | get agents | GET | bearer | — → M-029 | Recovered |
| E-024 | register advocacy | POST | bearer | M-002 → M-029 | Recovered |
| E-025 | accept proxy account | POST | bearer | M-002 → M-029 | Recovered |
| E-026 | get condition info | GET | bearer | — → M-059 | Recovered |
| E-027 | get default account balance | GET | bearer | — → M-023 | Recovered |
| E-028 | send otp advocacy | POST | bearer | M-002 → M-029 | Recovered |
| E-029 | micro transaction | POST | bearer | M-002 → M-029 | Recovered |
| E-030 | get active accounts for management | GET | bearer | — → M-004 | Recovered |
| E-031 | freeze account | GET | bearer | — → M-007 | Recovered |
| E-032 | convert account to iban | POST | bearer | M-002 → M-050 | Recovered |
| E-033 | get account info call type | GET | bearer | — → M-029 | Recovered |
| E-034 | remove agent | POST | bearer | M-002 → M-029 | Recovered |
| E-035 | get advocacy active plans | GET | bearer | — → M-029 | Recovered |
| E-036 | get active accounts for management | GET | bearer | — → M-029 | Recovered |
| E-037 | de active account | GET | bearer | — → M-007 | Recovered |
| E-038 | trace advocacy | GET | bearer | — → M-029 | Recovered |
| E-039 | get account balance | PUT | bearer | — → M-023 | Recovered |
| E-040 | get request list advocacy | GET | bearer | — → M-029 | Recovered |
| E-041 | source accounts with balance | GET | bearer | — → M-038 | Recovered |
| E-042 | get wallet source account | GET | bearer | — → M-009 | Recovered |
| E-043 | get default account info | GET | bearer | — → M-004 | Recovered |

## F-030 — Transactions (13)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-237 | internal trace | GET | bearer | — → M-031 | Recovered |
| E-238 | paya trace | GET | bearer | — → M-031 | Recovered |
| E-239 | satna trace | GET | bearer | — → M-031 | Recovered |
| E-240 | pol trace | GET | bearer | — → M-031 | Recovered |
| E-241 | download internal attachment | GET | bearer | — → M-022 | Recovered |
| E-242 | download bill attachment | GET | bearer | — → M-022 | Recovered |
| E-243 | download charity attachment | GET | bearer | — → M-022 | Recovered |
| E-244 | download paya attachment | GET | bearer | — → M-022 | Recovered |
| E-245 | transactions trace | POST | bearer | M-002 → M-029 | Recovered |
| E-246 | download satna attachment | GET | bearer | — → M-022 | Recovered |
| E-247 | download pol attachment | GET | bearer | — → M-022 | Recovered |
| E-307 | get login history | GET | bearer | — → M-029 | Recovered |
| E-308 | get activation history | GET | bearer | — → M-029 | Recovered |

## F-032 — Wallet (18)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-094 | add wallet note | POST | bearer | M-002 → M-029 | Recovered |
| E-095 | charge pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-096 | send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-097 | get wallet info | GET | bearer | — → M-056 | Recovered |
| E-098 | get wallet list | GET | bearer | — → M-029 | Recovered |
| E-099 | wallet charge transfer | POST | bearer | M-002 → M-029 | Recovered |
| E-100 | trace wallet history | GET | bearer | — → M-055 | Recovered |
| E-101 | get balance | GET | bearer | — → M-029 | Recovered |
| E-102 | wallet to wallet prevalidate | POST | bearer | M-002 → M-029 | Recovered |
| E-103 | export wallet transactions | GET | bearer | — → M-029 | Recovered |
| E-104 | get wallet source transaction | GET | bearer | — → M-029 | Recovered |
| E-105 | wallet to wallet transfer | POST | bearer | M-002 → M-029 | Recovered |
| E-106 | pre validate bill by wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-107 | trace wallet transfer | GET | bearer | — → M-029 | Recovered |
| E-108 | charge by wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-109 | get wallet transactions | GET | bearer | — → M-029 | Recovered |
| E-110 | bill by wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-111 | pre validate charge by wallet | POST | bearer | M-002 → M-029 | Recovered |

## F-033 — Cards (50)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-044 | send otp by pan | GET | bearer | — → — | Recovered |
| E-045 | card balance | POST | bearer | M-002 → M-023 | Recovered |
| E-046 | get customer cards list | GET | bearer | — → M-008 | Recovered |
| E-047 | get customer cards list v2 old arch | PUT | bearer | — → M-026 | Recovered |
| E-048 | card info | GET | bearer | — → M-031 | Recovered |
| E-049 | get customer cards list | GET | bearer | — → M-029 | Recovered |
| E-050 | card transfer second step | POST | bearer | M-002 → M-029 | Recovered |
| E-051 | harim | POST | bearer | M-002 → M-029 | Recovered |
| E-052 | get customer cards list with balance | GET | bearer | — → M-060 | Recovered |
| E-053 | issue pin otp | POST | bearer | M-002 → M-029 | Recovered |
| E-054 | get new cards | GET | bearer | — → M-026 | Recovered |
| E-055 | trace card transfer | GET | bearer | — → M-031 | Recovered |
| E-056 | card transfer step | POST | bearer | M-002 → M-025 | Recovered |
| E-057 | set attachment card transfer | POST | bearer | — → M-029 | Recovered |
| E-058 | change pin otp | POST | bearer | M-002 → M-029 | Recovered |
| E-059 | convert iban to card | GET | bearer | — → M-043 | Recovered |
| E-060 | get show card | GET | bearer | — → M-029 | Recovered |
| E-061 | verify otp | POST | bearer | M-002 → M-029 | Recovered |
| E-062 | issue pin | POST | bearer | M-002 → M-029 | Recovered |
| E-063 | set default card | GET | bearer | — → — | Recovered |
| E-064 | card transfer step | POST | bearer | M-002 → M-031 | Recovered |
| E-065 | card info with balance | GET | bearer | — → M-029 | Recovered |
| E-066 | change pin | POST | bearer | M-002 → M-029 | Recovered |
| E-067 | delete card | DELETE | bearer | — → — | Recovered |
| E-068 | block card | POST | bearer | M-002 → — | Recovered |
| E-069 | trace | GET | bearer | — → M-029 | Recovered |
| E-070 | send otp for credit card detail | POST | bearer | M-002 → M-029 | Recovered |
| E-071 | disable card | POST | bearer | — → — | Recovered |
| E-072 | card info with balance | GET | bearer | — → M-024 | Recovered |
| E-073 | set attachment card transfer | POST | bearer | M-002 → M-033 | Recovered |
| E-074 | activate second pin req otp c | POST | bearer | M-002 → M-029 | Recovered |
| E-075 | get credit card file loan summary list | GET | bearer | — → M-029 | Recovered |
| E-076 | card customer info | GET | bearer | — → M-008 | Recovered |
| E-077 | activate second pin verify otp c | POST | bearer | M-002 → M-029 | Recovered |
| E-078 | confirmation received card | GET | bearer | — → M-005 | Recovered |
| E-079 | card block c | POST | bearer | M-002 → M-029 | Recovered |
| E-080 | get credit card detail | GET | bearer | — → M-029 | Recovered |
| E-081 | is show card | POST | bearer | M-002 → M-029 | Recovered |
| E-082 | harim with identification code | POST | none | M-002 → M-029 | Recovered |
| E-083 | harim2 with identification code | POST | none | M-002 → M-029 | Recovered |
| E-084 | card transfer step | POST | none | M-002 → M-025 | Recovered |
| E-085 | harim2 with identification code | POST | none | M-002 → M-033 | Recovered |
| E-086 | harim2 with identification code bill | POST | none | M-002 → M-029 | Recovered |
| E-087 | rade harim | POST | none | M-002 → — | Recovered |
| E-088 | card balance | POST | none | M-002 → M-023 | Recovered |
| E-089 | card transfer step | POST | none | M-002 → M-031 | Recovered |
| E-090 | get customer cards list | GET | none | — → — | Recovered |
| E-091 | card transfer second step | POST | none | M-002 → M-029 | Recovered |
| E-092 | set attachment card transfer | POST | none | M-002 → M-029 | Recovered |
| E-093 | block card c | POST | none | M-002 → M-029 | Recovered |

## F-035 — Cheques (21)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-265 | pre validate cheque book issue | POST | bearer | M-002 → M-029 | Recovered |
| E-266 | get cheque books | GET | bearer | — → M-029 | Recovered |
| E-267 | get cheque accounts | GET | bearer | — → M-009 | Recovered |
| E-268 | get cheque book page counts | GET | bearer | — → M-029 | Recovered |
| E-269 | calculate fee cheque book issue | POST | bearer | M-002 → M-029 | Recovered |
| E-270 | digital cheque cheque book trace | POST | bearer | M-002 → M-029 | Recovered |
| E-271 | get cheques | GET | bearer | — → M-029 | Recovered |
| E-272 | trace request cheque book issue | GET | bearer | — → M-029 | Recovered |
| E-273 | send otp cheque book issue | POST | bearer | M-002 → M-029 | Recovered |
| E-274 | cheque inquiry | GET | bearer | — → M-029 | Recovered |
| E-275 | digital cheque activation trace | GET | bearer | — → M-029 | Recovered |
| E-276 | confirm request cheque book issue | POST | bearer | M-002 → M-029 | Recovered |
| E-277 | transferred cheques report | GET | bearer | — → M-029 | Recovered |
| E-278 | digital cheque deactivation | GET | bearer | — → M-029 | Recovered |
| E-279 | digital cheque activation | POST | bearer | M-002 → M-029 | Recovered |
| E-280 | get cheque individual accounts | GET | bearer | — → M-009 | Recovered |
| E-281 | digital cheque trace | GET | bearer | — → M-029 | Recovered |
| E-282 | digital cheque transfer sayad cheque | POST | bearer | M-002 → M-029 | Recovered |
| E-283 | digital cheque activation inquiry | GET | bearer | — → M-029 | Recovered |
| E-284 | get super agent source accounts | GET | bearer | — → M-009 | Recovered |
| E-285 | trace digital cheque digital cheque | POST | bearer | M-002 → M-029 | Recovered |

## F-038 — Cheque payment (23)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-214 | inquiry receivers | POST | bearer | M-002 → M-029 | Recovered |
| E-215 | get pay check | GET | bearer | — → M-029 | Recovered |
| E-216 | pay cheque issue | POST | bearer | M-002 → M-029 | Recovered |
| E-217 | get digital cheque check | GET | bearer | — → M-029 | Recovered |
| E-218 | reason pay cheque | GET | bearer | — → M-029 | Recovered |
| E-219 | trace digital cheque | POST | bearer | M-002 → M-029 | Recovered |
| E-220 | pay cheque history | POST | bearer | M-002 → M-029 | Recovered |
| E-221 | get reason thresholds | GET | bearer | — → M-029 | Recovered |
| E-222 | accept pay cheque | POST | bearer | M-002 → M-029 | Recovered |
| E-223 | agent terms and condition | GET | bearer | — → — | Recovered |
| E-224 | inquiry by holder | GET | bearer | — → M-029 | Recovered |
| E-225 | transfer pay cheque | POST | bearer | M-002 → M-029 | Recovered |
| E-226 | digital cheque history | POST | bearer | M-002 → M-029 | Recovered |
| E-227 | get assigned cheque | GET | bearer | — → M-029 | Recovered |
| E-228 | pay cheque pre validate | GET | bearer | — → M-029 | Recovered |
| E-229 | digital cheque update | POST | bearer | M-002 → M-029 | Recovered |
| E-230 | agent inquiry | POST | bearer | M-002 → M-029 | Recovered |
| E-231 | digital cheque pre validate | GET | bearer | — → M-029 | Recovered |
| E-232 | pay cheque super agent issue | POST | bearer | M-002 → M-029 | Recovered |
| E-233 | add agent | POST | bearer | M-002 → M-029 | Recovered |
| E-234 | get signer | GET | bearer | — → M-029 | Recovered |
| E-235 | inquiry cartable | GET | bearer | — → M-029 | Recovered |
| E-236 | inquiry made | POST | bearer | M-002 → M-029 | Recovered |

## F-039 — Money transfer (29)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-145 | get attachment | GET | bearer | — → M-029 | Recovered |
| E-146 | delete destination | DELETE | bearer | — → M-029 | Recovered |
| E-147 | edit destination | PUT | bearer | M-002 → M-029 | Recovered |
| E-148 | send otp transfer group | POST | bearer | M-002 → M-029 | Recovered |
| E-149 | last five transaction history | GET | bearer | — → M-029 | Recovered |
| E-150 | inquiry pre validate | DELETE | bearer | — → M-029 | Recovered |
| E-151 | transfer group pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-152 | transfer | POST | bearer | M-002 → M-029 | Recovered |
| E-153 | destinations details | GET | bearer | — → M-029 | Recovered |
| E-154 | transfer trace batch | GET | bearer | — → M-029 | Recovered |
| E-155 | delete transaction group | GET | bearer | — → M-029 | Recovered |
| E-156 | attachment transfer group | POST | bearer | — → M-029 | Recovered |
| E-157 | destinations | POST | bearer | M-002 → M-029 | Recovered |
| E-158 | result pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-159 | group delete | POST | bearer | M-002 → M-029 | Recovered |
| E-160 | download trace file | GET | bearer | — → M-029 | Recovered |
| E-161 | registry transfer group | GET | bearer | — → M-029 | Recovered |
| E-162 | transfer trace | GET | bearer | — → M-031 | Recovered |
| E-163 | inquiry transfer group | POST | bearer | — → M-029 | Recovered |
| E-164 | get file template | DELETE | bearer | M-001 → M-029 | Recovered |
| E-296 | get continuous frequency | GET | bearer | — → M-029 | Recovered |
| E-297 | get continuous execution date | PUT | bearer | M-002 → M-029 | Recovered |
| E-298 | add continuous | POST | bearer | M-002 → M-046 | Recovered |
| E-299 | get transaction | GET | bearer | — → M-029 | Recovered |
| E-300 | get attachment | GET | bearer | — → M-029 | Recovered |
| E-301 | delete transaction | DELETE | bearer | — → M-029 | Recovered |
| E-302 | get continuous history | POST | bearer | M-002 → M-029 | Recovered |
| E-303 | add track transaction | GET | bearer | — → M-045 | Recovered |
| E-304 | cancel transaction | POST | bearer | M-002 → M-029 | Recovered |

## F-040 — Saved destinations (5)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-112 | add familiar dest card no login | POST | bearer | M-002 → M-029 | Recovered |
| E-113 | get familiar dests no login | GET | bearer | — → M-010 | Recovered |
| E-114 | get familiar dests | GET | bearer | — → M-029 | Recovered |
| E-115 | add familiar dest card | POST | bearer | M-002 → M-029 | Recovered |
| E-116 | add familiar dest card no login | POST | none | M-002 → M-010 | Recovered |

## F-042 — IBAN conversion (6)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-250 | get radeh fee | GET | none | — → M-053 | Recovered |
| E-251 | convert card to iban | POST | none | M-002 → M-052 | Recovered |
| E-252 | convert iban to card | GET | none | — → M-043 | Recovered |
| E-253 | convert account to iban | POST | none | M-002 → M-050 | Recovered |
| E-254 | convert iban to account | GET | none | — → M-042 | Recovered |
| E-255 | convert card to account | POST | none | M-002 → M-051 | Recovered |

## F-043 — Bills (9)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-256 | pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-257 | pre validate no login | POST | none | M-002 → M-029 | Recovered |
| E-258 | trace bill login | GET | none | — → M-031 | Recovered |
| E-259 | bill attachment login | POST | bearer | M-002 → M-029 | Recovered |
| E-260 | bill attachment no login | POST | none | M-002 → M-029 | Recovered |
| E-261 | bill otp | POST | bearer | M-002 → M-029 | Recovered |
| E-262 | trace bill no login | GET | bearer | — → M-031 | Recovered |
| E-263 | bill transfer | POST | bearer | M-002 → M-029 | Recovered |
| E-264 | bill transfer card no login | POST | bearer | M-002 → M-029 | Recovered |

## F-044 — Mobile top-up (10)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-286 | trace | GET | bearer | — → M-031 | Recovered |
| E-287 | pre validate | POST | none | M-002 → M-029 | Recovered |
| E-288 | pre validate no login | POST | none | M-002 → M-029 | Recovered |
| E-289 | get charge configs | GET | none | — → M-029 | Recovered |
| E-290 | request otp | POST | bearer | M-002 → M-029 | Recovered |
| E-291 | trace no login | GET | none | — → M-031 | Recovered |
| E-292 | verify otp | POST | bearer | M-002 → M-029 | Recovered |
| E-293 | get charge configs no login | GET | bearer | — → M-029 | Recovered |
| E-294 | verify card | POST | bearer | M-002 → M-029 | Recovered |
| E-295 | verify card no login | POST | bearer | M-002 → M-029 | Recovered |

## F-045 — Balance top-up / gateway (2)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-248 | call with body | POST | bearer | M-002 → M-029 | Recovered |
| E-249 | call with query | GET | bearer | — → M-029 | Recovered |

## F-046 — Road toll (13)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-201 | inquiry free way toll | POST | bearer | M-002 → M-029 | Recovered |
| E-202 | pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-203 | get plaque letter list | GET | bearer | — → M-029 | Recovered |
| E-204 | verify otp for payment | POST | bearer | M-002 → M-029 | Recovered |
| E-205 | get plaque list | GET | bearer | — → M-029 | Recovered |
| E-206 | save plaque | POST | bearer | M-002 → M-029 | Recovered |
| E-207 | delete plaque | GET | bearer | — → M-029 | Recovered |
| E-208 | inquiry traffic toll | POST | bearer | M-002 → M-029 | Recovered |
| E-209 | send otp for payment | GET | bearer | — → M-029 | Recovered |
| E-210 | inquiry annual toll | POST | bearer | M-002 → M-029 | Recovered |
| E-211 | trace road toll | DELETE | bearer | — → M-029 | Recovered |
| E-212 | inquiry transfer tax | POST | bearer | M-002 → M-029 | Recovered |
| E-213 | get inquiry history | POST | bearer | M-002 → M-029 | Recovered |

## F-047 — Donations (6)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-136 | charity step | POST | bearer | M-002 → M-031 | Recovered |
| E-137 | set attachment charity | POST | bearer | M-002 → — | Recovered |
| E-138 | get all charities | GET | bearer | — → M-021 | Recovered |
| E-139 | trace charity | GET | bearer | — → M-031 | Recovered |
| E-140 | charity otp | POST | bearer | M-002 → — | Recovered |
| E-141 | charity transfer step | POST | bearer | M-002 → M-031 | Recovered |

## F-048 — Loans (60)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-309 | get trace id | GET | bearer | — → M-044 | Recovered |
| E-310 | get loan pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-311 | get loan summary | POST | bearer | M-002 → M-029 | Recovered |
| E-312 | get loan otp | POST | bearer | M-002 → M-029 | Recovered |
| E-313 | micro calculate | GET | bearer | — → M-029 | Recovered |
| E-314 | get loan pay | POST | bearer | M-002 → M-029 | Recovered |
| E-315 | micro pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-316 | micro loan get data to be signed | POST | bearer | M-002 → M-029 | Recovered |
| E-317 | company loan get data to be signed | POST | bearer | M-002 → M-029 | Recovered |
| E-318 | hub loan get data to be signed | POST | bearer | M-002 → M-029 | Recovered |
| E-319 | welcome loan get data to be signed | POST | bearer | M-002 → M-029 | Recovered |
| E-320 | micro loan sign | POST | bearer | M-002 → M-029 | Recovered |
| E-321 | company loan sign | POST | bearer | M-002 → M-029 | Recovered |
| E-322 | micro loan agreement | GET | bearer | — → M-029 | Recovered |
| E-323 | hub loan sign | POST | bearer | M-002 → M-029 | Recovered |
| E-324 | micro trace loan | GET | bearer | — → M-029 | Recovered |
| E-325 | welcome loan sign | POST | bearer | M-002 → M-029 | Recovered |
| E-326 | get loan company list | GET | bearer | — → M-029 | Recovered |
| E-327 | loan company agreement | GET | bearer | — → M-044 | Recovered |
| E-328 | micro loan send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-329 | get loan detail | POST | bearer | M-002 → M-029 | Recovered |
| E-330 | get loan marriage conditions | GET | bearer | — → M-029 | Recovered |
| E-331 | get payback accounts | GET | bearer | — → M-029 | Recovered |
| E-332 | micro loan grant | POST | bearer | M-002 → M-029 | Recovered |
| E-333 | welcome pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-334 | get loan child conditions | GET | bearer | — → M-029 | Recovered |
| E-335 | trace company loan | GET | bearer | — → M-029 | Recovered |
| E-336 | loan company calculate | POST | bearer | M-002 → M-029 | Recovered |
| E-337 | welcome loan send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-338 | get loan marriage history | GET | bearer | — → M-029 | Recovered |
| E-339 | get loan child agreement | GET | bearer | — → M-029 | Recovered |
| E-340 | loan company pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-341 | welcome loan grant | POST | bearer | M-002 → M-029 | Recovered |
| E-342 | loan company send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-343 | loan company grant | POST | bearer | M-002 → M-029 | Recovered |
| E-344 | get loan marriage info | POST | bearer | M-002 → M-029 | Recovered |
| E-345 | loan marriage send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-346 | loan child send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-347 | get loan child info | POST | bearer | M-002 → M-029 | Recovered |
| E-348 | get loan child history | GET | bearer | — → M-029 | Recovered |
| E-349 | loan marriage validate account | GET | bearer | — → M-029 | Recovered |
| E-350 | loan marriage initial info | POST | bearer | M-002 → M-029 | Recovered |
| E-351 | hub pre validate | POST | bearer | M-002 → M-029 | Recovered |
| E-352 | loan child validate account | GET | bearer | — → M-029 | Recovered |
| E-353 | loan child decline request | POST | bearer | M-002 → M-029 | Recovered |
| E-354 | loan child initial info | POST | bearer | M-002 → M-029 | Recovered |
| E-355 | loan child inquiry customer guarantor | POST | bearer | M-002 → M-029 | Recovered |
| E-356 | loan child get central bank trace id list | GET | bearer | — → M-029 | Recovered |
| E-357 | get loan marriage address | POST | bearer | M-002 → M-029 | Recovered |
| E-358 | hub loan send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-359 | get loan child address | POST | bearer | M-002 → M-029 | Recovered |
| E-360 | loan marriage register customer info | POST | bearer | M-002 → M-029 | Recovered |
| E-361 | loan child register customer info | POST | bearer | M-002 → M-029 | Recovered |
| E-362 | loan marriage get central bank trace id list | GET | bearer | — → M-029 | Recovered |
| E-363 | loan marriage register spouse info | POST | bearer | M-002 → M-029 | Recovered |
| E-364 | get loan marriage jobs | GET | bearer | — → M-029 | Recovered |
| E-365 | loan marriage register guarantor info | POST | bearer | M-002 → M-029 | Recovered |
| E-366 | loan marriage trace | GET | bearer | — → M-029 | Recovered |
| E-367 | loan child register guarantor info | POST | bearer | M-002 → M-029 | Recovered |
| E-368 | hub loan grant | POST | bearer | M-002 → M-029 | Recovered |

## F-050 — Guarantees (2)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-305 | get customer companies info | GET | bearer | — → M-029 | Recovered |
| E-306 | get guarantee file by customer | POST | bearer | M-002 → M-029 | Recovered |

## F-051 — Digital signature (13)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-117 | get inquiry | POST | bearer | — → M-029 | Recovered |
| E-118 | revoke certificate | GET | bearer | — → M-029 | Recovered |
| E-119 | get revoke sign video expression | GET | bearer | — → M-029 | Recovered |
| E-120 | call with body | POST | bearer | M-002 → M-029 | Recovered |
| E-121 | inquiry revoke sign | GET | bearer | — → M-029 | Recovered |
| E-122 | get revoke sign status | GET | bearer | — → M-029 | Recovered |
| E-123 | complete csr | GET | bearer | — → M-029 | Recovered |
| E-124 | revoke sign | GET | bearer | — → M-029 | Recovered |
| E-125 | certificate status | POST | bearer | M-002 → M-029 | Recovered |
| E-126 | call with query | GET | bearer | — → M-029 | Recovered |
| E-127 | upload file | POST | bearer | — → M-029 | Recovered |
| E-128 | get all certificates | POST | bearer | M-002 → M-029 | Recovered |
| E-129 | sign issue agreement | GET | bearer | — → — | Recovered |

## F-054 — Insurance (9)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-176 | get insurance requests | GET | bearer | — → M-029 | Recovered |
| E-177 | export insurance | GET | bearer | — → M-029 | Recovered |
| E-178 | pre validate installment payment | POST | bearer | M-002 → M-029 | Recovered |
| E-179 | trace insurance payment history | GET | bearer | — → M-031 | Recovered |
| E-180 | get insurance list | GET | bearer | — → M-029 | Recovered |
| E-181 | insurance payment send otp | POST | bearer | M-002 → M-029 | Recovered |
| E-182 | trace insurance payment | GET | bearer | — → M-029 | Recovered |
| E-183 | insurance payment | POST | bearer | M-002 → M-029 | Recovered |
| E-184 | submit insurance request | POST | bearer | M-002 → M-029 | Recovered |

## F-057 — Loyalty club (3)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-142 | get loyalty score | GET | bearer | — → M-029 | Recovered |
| E-143 | get score table by id | POST | bearer | M-002 → M-029 | Recovered |
| E-144 | get loyalty transaction score | GET | bearer | — → M-029 | Recovered |

## F-059 — Inquiries (11)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-165 | send inquiries otp | POST | bearer | M-002 → M-029 | Recovered |
| E-166 | inquiries bounced cheque with account | POST | bearer | M-002 → M-029 | Recovered |
| E-167 | inquiry bounced cheque with wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-168 | inquiries credit rating with account | POST | bearer | M-002 → M-029 | Recovered |
| E-169 | inquiries credit rating with wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-170 | get html | GET | bearer | — → — | Recovered |
| E-171 | inquiries credit report with account | POST | bearer | M-002 → M-029 | Recovered |
| E-172 | inquiries credit report with wallet | POST | bearer | M-002 → M-029 | Recovered |
| E-173 | trace last inquiry rate | GET | bearer | — → M-029 | Recovered |
| E-174 | get inquiry garanty information | POST | bearer | M-002 → M-029 | Recovered |
| E-175 | get inquiry guaranty authenticity | POST | bearer | M-002 → M-029 | Recovered |

## F-000 — Cross-cutting / infra (no single feature area) (120)

| ID | Name | Method | Auth | Request → Response | Label |
|---|---|---|---|---|---|
| E-401 | get user tasks | POST | bearer | M-002 → M-029 | Recovered |
| E-402 | cancel process | GET | bearer | — → M-029 | Recovered |
| E-403 | act | POST | bearer | M-002 → M-029 | Recovered |
| E-404 | request otp | GET | bearer | — → M-029 | Recovered |
| E-405 | revoke act | POST | bearer | M-002 → M-029 | Recovered |
| E-406 | request otp for charge | GET | bearer | — → M-029 | Recovered |
| E-407 | pay | POST | bearer | M-002 → M-029 | Recovered |
| E-408 | request otp for bill | GET | bearer | — → M-029 | Recovered |
| E-409 | get process tasks | POST | bearer | M-002 → M-029 | Recovered |
| E-410 | pay for charge | POST | bearer | M-002 → M-029 | Recovered |
| E-411 | request otp for loan | GET | bearer | — → M-029 | Recovered |
| E-412 | pay for loan | POST | bearer | M-002 → M-029 | Recovered |
| E-413 | request otp for cheque book issue | GET | bearer | — → M-029 | Recovered |
| E-414 | pay for bill | POST | bearer | M-002 → M-029 | Recovered |
| E-415 | pay for cheque book issue | POST | bearer | M-002 → M-029 | Recovered |
| E-416 | request otp for road toll transaction | GET | bearer | — → M-029 | Recovered |
| E-417 | pay for road toll transaction | POST | bearer | M-002 → M-029 | Recovered |
| E-418 | get | PUT | bearer | — → M-029 | Recovered |
| E-419 | get raw text | GET | bearer | — → M-029 | Recovered |
| E-420 | post | PUT | bearer | M-002 → M-029 | Recovered |
| E-421 | get raw | DELETE | bearer | — → M-029 | Recovered |
| E-422 | post with identification code | POST | bearer | M-003 → M-029 | Recovered |
| E-423 | post | GET | bearer | — → M-029 | Recovered |
| E-424 | download | GET | bearer | — → — | Recovered |
| E-425 | put | POST | bearer | — → M-029 | Recovered |
| E-426 | put | POST | bearer | M-002 → M-029 | Recovered |
| E-427 | delete | GET | bearer | — → M-029 | Recovered |
| E-428 | multipart | POST | bearer | — → M-029 | Recovered |
| E-429 | get | DELETE | none | — → M-006 | Recovered |
| E-430 | post | POST | none | M-002 → M-006 | Recovered |
| E-431 | get raw text | POST | none | — → — | Recovered |
| E-432 | post with identification code | POST | none | M-003 → M-006 | Recovered |
| E-433 | get raw | GET | none | — → — | Recovered |
| E-434 | post | GET | none | — → M-006 | Recovered |
| E-435 | app version history | POST | bearer | M-002 → M-029 | Recovered |
| E-436 | exchange digital signature | POST | bearer | M-002 → M-029 | Recovered |
| E-437 | trace id | GET | none | — → M-044 | Recovered |
| E-438 | save auth card call | PUT | bearer | M-002 → M-029 | Recovered |
| E-439 | get fee list | GET | bearer | — → M-029 | Recovered |
| E-440 | welcome message | GET | bearer | — → M-029 | Recovered |
| E-441 | add to familiar dest | POST | bearer | M-002 → M-029 | Recovered |
| E-442 | get home banners | GET | bearer | — → M-029 | Recovered |
| E-443 | referred list | POST | bearer | M-002 → M-029 | Recovered |
| E-444 | loan marriage upload document | POST | bearer | — → M-029 | Recovered |
| E-445 | get app version | GET | none | — → M-027 | Recovered |
| E-446 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-447 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-448 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-449 | post req encrypted | POST | none | M-002 → — | Unknown |
| E-450 | delete call | DELETE | bearer | — → — | Unknown |
| E-451 | get res financial transaction | GET | bearer | — → M-031 | Unknown |
| E-452 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-453 | get res financial transaction | GET | bearer | — → M-031 | Unknown |
| E-454 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-455 | get trace id | GET | none | — → M-044 | Unknown |
| E-456 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-457 | get res financial transaction | GET | bearer | — → M-031 | Unknown |
| E-458 | post res financial transaction | POST | bearer | M-002 → M-031 | Unknown |
| E-459 | get res financial transaction | GET | bearer | — → M-031 | Unknown |
| E-460 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-461 | post res radeh send otp | POST | bearer | M-002 → M-054 | Unknown |
| E-462 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-463 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-464 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-465 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-466 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-467 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-468 | get res radeh fee | GET | bearer | — → M-053 | Unknown |
| E-469 | get res limit | GET | bearer | — → M-036 | Unknown |
| E-470 | post res financial transaction | POST | bearer | M-002 → M-031 | Unknown |
| E-471 | post res statement | POST | bearer | M-002 → M-039 | Unknown |
| E-472 | get iban to account res | GET | bearer | — → M-042 | Unknown |
| E-473 | post call | POST | bearer | — → — | Unknown |
| E-474 | put req encrypted with identification code | PUT | none | M-003 → — | Unknown |
| E-475 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-476 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-477 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-478 | post res encrypted | POST | bearer | — → M-029 | Unknown |
| E-479 | get call | GET | bearer | — → — | Unknown |
| E-480 | post familiar dest | POST | bearer | M-002 → M-010 | Unknown |
| E-481 | post req encrypted with identification code | POST | none | M-003 → — | Unknown |
| E-482 | put res encrypted | PUT | bearer | — → M-029 | Unknown |
| E-483 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-484 | get branch | GET | bearer | — → M-057 | Unknown |
| E-485 | get res account check status | GET | bearer | — → M-047 | Unknown |
| E-486 | post res enable biometric | POST | bearer | M-002 → M-028 | Unknown |
| E-487 | post call | POST | bearer | — → — | Unknown |
| E-488 | post req encrypted | POST | bearer | M-002 → — | Unknown |
| E-489 | post req encrypted with identification code | POST | none | M-003 → — | Unknown |
| E-490 | get res photo | GET | bearer | — → M-037 | Unknown |
| E-491 | post call | POST | bearer | — → — | Unknown |
| E-492 | put req encrypted with identification code | PUT | none | M-003 → — | Unknown |
| E-493 | delete res encrypted | DELETE | bearer | — → M-029 | Unknown |
| E-494 | post res key exchange | POST | none | M-015 → M-035 | Unknown |
| E-495 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-496 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-497 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-498 | get familiar dest | GET | bearer | — → M-010 | Unknown |
| E-499 | get call | GET | none | — → — | Unknown |
| E-500 | get call | GET | bearer | — → — | Unknown |
| E-501 | post req encrypted | POST | bearer | M-002 → — | Unknown |
| E-502 | post call | POST | bearer | — → — | Unknown |
| E-503 | post req encrypted with identification code | POST | none | M-003 → — | Unknown |
| E-504 | put res encrypted | PUT | bearer | M-002 → M-029 | Unknown |
| E-505 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-506 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-507 | get call | GET | bearer | — → — | Unknown |
| E-508 | put familiar dest | PUT | bearer | M-002 → M-010 | Unknown |
| E-509 | get res encrypted | GET | bearer | — → M-029 | Unknown |
| E-510 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-511 | post call | POST | none | — → — | Unknown |
| E-512 | get trace id | GET | none | — → M-044 | Unknown |
| E-513 | post res financial transaction | POST | bearer | M-002 → M-031 | Unknown |
| E-514 | get call | GET | none | — → — | Unknown |
| E-515 | get res financial transaction | GET | bearer | — → M-031 | Unknown |
| E-516 | post res encrypted | POST | bearer | M-002 → M-029 | Unknown |
| E-517 | get branch | GET | none | — → M-057 | Unknown |
| E-518 | get call | GET | bearer | — → — | Unknown |
| E-519 | post res iban inquiry | POST | bearer | M-002 → M-034 | Unknown |
| E-520 | put req encrypted | PUT | bearer | M-002 → — | Unknown |

## Models

| ID | Name | Fields | Label |
|---|---|---|---|
| M-001 | file ext type | payload:string | Recovered |
| M-002 | req encrypted | payload:string | Recovered |
| M-003 | req encrypted with identification code | payload:string | Recovered |
| M-004 | account | accountActive:boolean, accountDescription:string, accountNo:string, accountNo2:string, accountOpenDate:string, accountOwnerShipType:object, accountOwnerShipT… | Recovered |
| M-005 | res confirmation card | accountNumber:string, firstname:string, lastname:string, gender:boolean, cardColor:string, textColor:string | Recovered |
| M-006 | res encrypted | — | Unknown |
| M-007 | account with cards | accountClientStatus:boolean, accountDesc:string, accountNo:string, accountTypeDeposit:string, accountTypeDesc:string, balance:number, color:string, currency:… | Recovered |
| M-008 | card and account | accountClientStatus:boolean, accountDesc:string, accountNo:string, accountTypeDeposit:string, accountTypeDesc:string, balance:number, color:string, currency:… | Recovered |
| M-009 | source account | accountClientStatus:boolean, accountDesc:string, accountNo:string, accountTypeDeposit:string, accountTypeDesc:string, balance:number, color:string, currency:… | Recovered |
| M-010 | familiar dest | bank:object, bankId:integer, desc:string, dest:string, id:long, isMine:boolean, name:string, orderPos:integer, type:object | Recovered |
| M-011 | res otp | isOtpRequired:boolean, otpInfo:object | Recovered |
| M-012 | pagination req | offset:integer, count:integer | Recovered |
| M-013 | req activation | identificationCode:string, otp:string, refNo:string | Recovered |
| M-014 | req ask no login | traceId:string, question:string, mobileNo:string, uuid:string | Recovered |
| M-015 | req shared key | customerPublicKey:string, identificationCode:string, refNo:string | Recovered |
| M-016 | req ticketing close | id:long, uuid:string, mobileNumber:string, traceId:string | Recovered |
| M-017 | req ticketing edit | id:long, uuid:string, mobileNumber:string, traceId:string, question:string | Recovered |
| M-018 | req ticketing history | uuid:string, mobileNumber:string, traceId:string | Recovered |
| M-019 | req ticketing pagination | offset:integer, count:integer, uuid:string, mobileNumber:string | Recovered |
| M-020 | req verify otp | otp:string, identificationCode:string, referenceNumber:string | Recovered |
| M-021 | charity | id:long, version:integer, name:string, desc:string, key:string, accountNo:string, website:string, icon:integer | Recovered |
| M-022 | res attachment | attachment:string | Recovered |
| M-023 | res balance | actualBalance:number, availableBalance:number, currency:string, currencyDescription:string, date:string, ledgerBalance:number, pledgeAmount:number | Recovered |
| M-024 | res balance card | cards:array, bankName:string | Recovered |
| M-025 | res card transfer step | bankName:string, cardNumber:string, deposit:string, depositOwners:string | Recovered |
| M-026 | res cards | bonCard:object, creditCard:object, debitCard:object, giftCard:object, walletCard:object | Recovered |
| M-027 | res client spec | appVersionStatus:object, descList:array, downloadLinks:object, latestVersion:string, releaseDate:string, settings:map | Recovered |
| M-028 | res enable biometric | biometricContext:string | Recovered |
| M-029 | res encrypted | payload:string | Recovered |
| M-030 | res fa qs | startRow:integer, endRow:integer, totalRows:long, data:array | Recovered |
| M-031 | res financial transaction | transactionId:string, status:object, statusDesc:string, receipt:object, centralBankReferenceCode:string, transactionCode:string, score:long, traceId:string, … | Recovered |
| M-032 | res get ticket no active | questionList:array | Recovered |
| M-033 | res harim | destAccountName:string, destAccountNo:string, rrn:string, stan:string | Recovered |
| M-034 | res iban inquiry | accountOwners:array, description:object, transferAllowed:boolean | Recovered |
| M-035 | res key exchange | serverPublicKey:string | Recovered |
| M-036 | res limit | groupId:integer, id:integer, itemName:string, itemValue:long | Recovered |
| M-037 | res photo | photo:string | Recovered |
| M-038 | res source account list | accountClientStatus:boolean, accountDesc:string, accountNo:string, accountTypeDeposit:string, accountTypeDesc:string, balance:number, color:string, currency:… | Recovered |
| M-039 | res statement | actualBalance:number, amount:number, branchCode:string, transactionCode:string, branchName:string, creditDebit:string, creditDebitDescription:string, finance… | Recovered |
| M-040 | res ticket list | end_row:integer, startRow:integer, totalRows:integer, data:array | Recovered |
| M-041 | res ticket verify otp | referenceNumber:string, uuid:string | Recovered |
| M-042 | iban to account res | accountDes:string, accountNo:string, iban:string, bankName:string | Recovered |
| M-043 | iban to card res | cards:array, bankName:string | Recovered |
| M-044 | trace id | payload:string | Recovered |
| M-045 | res add track transaction | transactionType:string, transactionTypeDescription:string, amount:string, attachmentDesc:string, attachmentFileName:string, createdDate:string, destination:s… | Recovered |
| M-046 | res continuous add | transactionType:string, transactionTypeDescription:string, amount:string, cancelCount:integer, createdDate:string, destination:string, endDate:string, source… | Recovered |
| M-047 | res account check status | accountActivationStatus:integer, status:object | Recovered |
| M-048 | res inquiry notification | activationType:string, fcmValid:boolean | Recovered |
| M-049 | res notification unseen count | unseenCount:integer | Recovered |
| M-050 | res account to iban | bankName:string, deposit:string, depositOwners:string, iban:string | Recovered |
| M-051 | res card to account | bankName:string, cardNumber:string, deposit:string, depositOwners:string | Recovered |
| M-052 | res card to iban | bankName:string, card:string, depositOwners:string, iban:string | Recovered |
| M-053 | res radeh fee | feeAmount:long | Recovered |
| M-054 | res radeh send otp | mobileNumber:string | Recovered |
| M-055 | res wallet transfer receipt | amount:long, description:string, destination:string, destinationDescription:string, destinationWalletName:string, source:string, sourceWithoutMask:string, so… | Recovered |
| M-056 | wallet info | fullName:string, walletAccountNumber:string | Recovered |
| M-057 | branch | address:string, branchServices:array, code:string, fax:string, lat:number, lng:number, name:string, phone:array, postalCode:string, province:string, region:s… | Recovered |
| M-058 | ticket | id:long, caption:string, question:string, traceId:string, status:object, lastModifiedDate:string | Recovered |
| M-059 | account withdraw condition | fromAmount:string, toAmount:string, conditionNo:integer, expireDate:string, minSign:integer, conditionDesc:string, currency:string, currencyDescription:strin… | Recovered |
| M-060 | card with balance res | bonCard:object, creditCard:object, debitCard:object, giftCard:object, walletCard:object | Recovered |

