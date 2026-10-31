# Whole-app screen catalogue (P1a-04)

Every navigation destination the catalogued banking app declares, named at screen level and grouped by the P1a-03 feature area. Each row is one node in `navigation-graph.json`; the edges between them live in that file. Built by static analysis only — the app was not run.

- **Method:** Compose navigation destinations (`NavHost` / `composable` / `dialog` / `navigation`) and the declared activities from the P1a-03 inventory, recovered from the decompiled code with the REA MCP server. Screen names come from each destination's own Compose content function, or an activity's own UI; obfuscated route labels are never used as names. No execution, no network, no string decryption.
- **Sanitization:** semantic names and evidence IDs only (see `README.md`). The node-ID → real route/class map is in the git-ignored `local/evidence/findings/navgraph-private.md`.
- **Labels:** `Recovered` (precise code location), `Inferred` (reasoned from a view-model or surrounding content), `Unknown` (obfuscated/computed/server-driven, with the reason). Nothing is `Observed` — the app was not run.
- **IDs:** `F-001`/`F-002`/`F-066` reuse the P1a-03 inventory IDs (launcher, internal router, dashboard-menu hub). Every other screen is `S-###` (stable, assigned in area then name order).

**Totals:** 485 nodes (266 activities, 199 Compose destinations, 19 nested sub-graphs, 1 server-driven hub) and 590 edges. By label: Recovered 352, Inferred 109, Unknown 24, Observed 0.

## F-001 — Launch

| ID | Name | Label | Basis |
|---|---|---|---|
| F-001 | Launch: splash | Recovered | activity; name kept through obfuscation |

## F-002 — Router

| ID | Name | Label | Basis |
|---|---|---|---|
| F-002 | Router: dispatcher | Recovered | activity; name kept through obfuscation |

## F-014 — Home

| ID | Name | Label | Basis |
|---|---|---|---|
| S-026 | Home: main | Recovered | activity; name kept through obfuscation |

## F-015 — Login

| ID | Name | Label | Basis |
|---|---|---|---|
| S-027 | Login | Recovered | activity; name kept through obfuscation |
| S-028 | Login: add account first run | Recovered | activity; name kept through obfuscation |
| S-029 | Login: default account first run | Recovered | activity; name kept through obfuscation |

## F-016 — Sign-up

| ID | Name | Label | Basis |
|---|---|---|---|
| S-030 | Sign-up: attach view model to activity lifecycle | Recovered | Compose destination; route and screen recovered |
| S-031 | Sign-up: forget identity info | Recovered | Compose destination; route and screen recovered |
| S-032 | Sign-up: forget success result | Recovered | Compose destination; route and screen recovered |
| S-033 | Sign-up: identity auth registration | Inferred | activity; feature read from its view-model |
| S-034 | Sign-up: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-035 | Sign-up: referral register | Inferred | activity; feature read from its view-model |
| S-036 | Sign-up: register agreement | Recovered | Compose destination; route and screen recovered |
| S-037 | Sign-up: register auth methods | Recovered | Compose destination; route and screen recovered |
| S-038 | Sign-up: register auth methods | Recovered | Compose destination; route and screen recovered |
| S-039 | Sign-up: register card auth | Recovered | Compose destination; route and screen recovered |
| S-040 | Sign-up: register card auth | Recovered | Compose destination; route and screen recovered |
| S-041 | Sign-up: register identity info | Recovered | Compose destination; route and screen recovered |
| S-042 | Sign-up: register national card | Recovered | Compose destination; route and screen recovered |
| S-043 | Sign-up: register national card | Recovered | Compose destination; route and screen recovered |
| S-044 | Sign-up: register or forget | Recovered | activity; named from its own screen content |
| S-045 | Sign-up: register stepper | Recovered | Compose destination; route and screen recovered |
| S-046 | Sign-up: register user pass | Recovered | Compose destination; route and screen recovered |
| S-047 | Sign-up: register user pass | Recovered | Compose destination; route and screen recovered |

## F-017 — Intro

| ID | Name | Label | Basis |
|---|---|---|---|
| S-048 | Intro | Recovered | activity; named from its own screen content |
| S-049 | Intro: welcome | Recovered | activity; named from its own screen content |

## F-018 — Activation

| ID | Name | Label | Basis |
|---|---|---|---|
| S-050 | Activation | Recovered | activity; named from its own screen content |

## F-019 — Identity check

| ID | Name | Label | Basis |
|---|---|---|---|
| S-051 | Identity check: identity authentication | Inferred | activity; feature read from its view-model |

## F-020 — Account opening

| ID | Name | Label | Basis |
|---|---|---|---|
| S-052 | Account opening: account branch selection | Recovered | Compose destination; route and screen recovered |
| S-053 | Account opening: additional information | Recovered | Compose destination; route and screen recovered |
| S-054 | Account opening: contact information | Recovered | Compose destination; route and screen recovered |
| S-055 | Account opening: create account | Recovered | activity; named from its own screen content |
| S-056 | Account opening: create account agreement | Recovered | Compose destination; route and screen recovered |
| S-057 | Account opening: create account in app | Recovered | activity; named from its own screen content |
| S-058 | Account opening: create account in app agreement | Inferred | Compose destination; screen recovered, route obfuscated |
| S-059 | Account opening: create account in app result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-060 | Account opening: create account stepper | Recovered | Compose destination; route and screen recovered |
| S-061 | Account opening: identity docs | Recovered | Compose destination; route and screen recovered |
| S-062 | Account opening: information | Inferred | Compose destination; screen recovered, route obfuscated |

## F-021 — Profile

| ID | Name | Label | Basis |
|---|---|---|---|
| S-063 | Profile: account management | Recovered | activity; name kept through obfuscation |
| S-064 | Profile: additional information | Recovered | activity; named from its own screen content |
| S-065 | Profile: edit user | Inferred | activity; feature read from its view-model |
| S-066 | Profile: familiar destinations | Recovered | activity; name kept through obfuscation |
| S-067 | Profile: fetch account | Recovered | activity; name kept through obfuscation |
| S-068 | Profile: fetch card | Recovered | activity; name kept through obfuscation |
| S-069 | Profile: invitation | Recovered | activity; named from its own screen content |

## F-022 — Settings

| ID | Name | Label | Basis |
|---|---|---|---|
| S-025 | Settings: app settings menu | Recovered | activity; content/caller identity recovered in FR-05 (PF-ROUTES) |
| S-070 | Settings | Recovered | activity; named from its own screen content |
| S-071 | Settings: notification settings | Recovered | activity; named from its own screen content |
| S-072 | Settings: service settings | Recovered | activity; named from its own screen content |

## F-023 — Storefront settings

| ID | Name | Label | Basis |
|---|---|---|---|
| S-073 | Storefront settings: storefront service settings | Recovered | activity; named from its own screen content |

## F-024 — What's new

| ID | Name | Label | Basis |
|---|---|---|---|
| S-074 | What's new: changelog list | Inferred | activity; feature read from its view-model |

## F-025 — Notifications

| ID | Name | Label | Basis |
|---|---|---|---|
| S-075 | Notifications | Recovered | activity; named from its own screen content |

## F-026 — Support

| ID | Name | Label | Basis |
|---|---|---|---|
| S-076 | Support: chatbot ask question | Recovered | activity; named from its own screen content |
| S-077 | Support: faq | Recovered | activity; named from its own screen content |
| S-078 | Support: faq no login | Recovered | activity; named from its own screen content |
| S-079 | Support: suggestion | Recovered | activity; named from its own screen content |
| S-080 | Support: ticket list | Recovered | activity; named from its own screen content |
| S-081 | Support: view or send message | Recovered | activity; named from its own screen content |
| S-082 | Support: view ticket | Recovered | activity; name kept through obfuscation |

## F-027 — Shortcuts

| ID | Name | Label | Basis |
|---|---|---|---|
| S-083 | Shortcuts: shortcut | Recovered | activity; named from its own screen content |
| S-084 | Shortcuts: shortcut home | Inferred | Compose destination; screen recovered, route obfuscated |

## F-028 — Accounts

| ID | Name | Label | Basis |
|---|---|---|---|
| S-085 | Accounts: account list | Recovered | activity; name kept through obfuscation |

## F-029 — Statement

| ID | Name | Label | Basis |
|---|---|---|---|
| S-014 | Statement: statement details | Recovered | activity; content/caller identity recovered in FR-02 (AI-STATEMENT) |
| S-016 | Statement: statement list | Recovered | activity; content/caller identity recovered in FR-02 (AI-STATEMENT) |
| S-086 | Statement: request statement | Recovered | activity; named from its own screen content |

## F-030 — Transactions

| ID | Name | Label | Basis |
|---|---|---|---|
| S-087 | Transactions: destination group transfer | Recovered | activity; named from its own screen content |
| S-088 | Transactions: group transfer destination info | Recovered | activity; named from its own screen content |
| S-089 | Transactions: group transfer history | Recovered | activity; named from its own screen content |
| S-090 | Transactions: transaction info | Recovered | activity; name kept through obfuscation |
| S-091 | Transactions: transaction list | Inferred | activity; feature read from its view-model |

## F-031 — Receipts

| ID | Name | Label | Basis |
|---|---|---|---|
| S-092 | Receipts: bill receipt | Recovered | activity; name kept through obfuscation |
| S-093 | Receipts: card transfer receipt | Recovered | activity; name kept through obfuscation |
| S-094 | Receipts: charge receipt | Recovered | activity; name kept through obfuscation |
| S-095 | Receipts: charity receipt | Recovered | activity; name kept through obfuscation |
| S-096 | Receipts: group transfer receipt | Recovered | activity; name kept through obfuscation |
| S-097 | Receipts: iban transfer receipt | Recovered | activity; named from its own screen content |
| S-098 | Receipts: internal transfer receipt | Recovered | activity; named from its own screen content |

## F-032 — Wallet

| ID | Name | Label | Basis |
|---|---|---|---|
| S-099 | Wallet: bill by wallet confirmation | Recovered | activity; named from its own screen content |
| S-100 | Wallet: buy with wallet | Recovered | activity; named from its own screen content |
| S-101 | Wallet: buy with wallet confirmation | Recovered | activity; named from its own screen content |
| S-102 | Wallet: charge by wallet | Recovered | activity; named from its own screen content |
| S-103 | Wallet: charge by wallet confirmation | Recovered | activity; named from its own screen content |
| S-104 | Wallet: charge wallet | Recovered | activity; named from its own screen content |
| S-105 | Wallet: charge wallet confirmation | Recovered | activity; named from its own screen content |
| S-106 | Wallet: de charge wallet | Recovered | activity; named from its own screen content |
| S-107 | Wallet: de charge wallet confirmation | Recovered | activity; named from its own screen content |
| S-108 | Wallet: fast payment qr scanner | Recovered | activity; named from its own screen content |
| S-109 | Wallet: pay bill by wallet | Recovered | activity; named from its own screen content |
| S-110 | Wallet: wallet history menu | Recovered | activity; named from its own screen content |
| S-111 | Wallet: wallet main | Recovered | activity; named from its own screen content |
| S-112 | Wallet: wallet qr | Recovered | activity; named from its own screen content |
| S-113 | Wallet: wallet receipt | Recovered | activity; named from its own screen content |
| S-114 | Wallet: wallet statement details | Recovered | activity; named from its own screen content |
| S-115 | Wallet: wallet statement list | Recovered | activity; named from its own screen content |
| S-116 | Wallet: wallet to wallet | Recovered | activity; named from its own screen content |
| S-117 | Wallet: wallet to wallet confirmation | Recovered | activity; named from its own screen content |

## F-033 — Cards

| ID | Name | Label | Basis |
|---|---|---|---|
| S-012 | Cards: card information | Recovered | activity; content/caller identity recovered in FR-03 (CI-S012) |
| S-118 | Cards: block card without source picker | Recovered | activity; name kept through obfuscation |
| S-119 | Cards: bon card balance | Recovered | activity; named from its own screen content |
| S-120 | Cards: card balance without source picker | Recovered | activity; name kept through obfuscation |
| S-121 | Cards: card block auth | Inferred | activity; feature read from its view-model |
| S-122 | Cards: card block no login 2 | Recovered | activity; named from its own screen content |
| S-123 | Cards: card color | Recovered | activity; named from its own screen content |
| S-124 | Cards: card issue first | Recovered | activity; named from its own screen content |
| S-125 | Cards: card list | Recovered | activity; name kept through obfuscation |
| S-126 | Cards: card management | Recovered | activity; name kept through obfuscation |
| S-127 | Cards: card modify first | Recovered | activity; named from its own screen content |
| S-128 | Cards: card operation confirm | Recovered | activity; named from its own screen content |
| S-129 | Cards: card operation select address | Recovered | activity; named from its own screen content |
| S-130 | Cards: card status visibility | Recovered | activity; named from its own screen content |
| S-131 | Cards: credit card detail | Recovered | activity; named from its own screen content |
| S-132 | Cards: credit card detail confirmation | Recovered | activity; named from its own screen content |
| S-133 | Cards: credit card detail receipt | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |
| S-134 | Cards: credit card file loan summary | Recovered | activity; named from its own screen content |
| S-135 | Cards: get or change pass | Recovered | activity; named from its own screen content |
| S-136 | Cards: second pass activation | Recovered | activity; named from its own screen content |

## F-034 — Wallet card

| ID | Name | Label | Basis |
|---|---|---|---|
| S-137 | Wallet card: attach view model to activity lifecycle | Recovered | Compose destination; route and screen recovered |
| S-138 | Wallet card: wallet card issue | Recovered | activity; named from its own screen content |
| S-139 | Wallet card: wallet card issue confirm | Recovered | Compose destination; route and screen recovered |
| S-140 | Wallet card: wallet card issue send type | Recovered | Compose destination; route and screen recovered |
| S-141 | Wallet card: wallet card set or change pin | Recovered | activity; named from its own screen content |

## F-035 — Cheques

| ID | Name | Label | Basis |
|---|---|---|---|
| S-142 | Cheques: cheque book | Inferred | activity; feature read from its view-model |
| S-143 | Cheques: cheque book issue | Recovered | activity; named from its own screen content |
| S-144 | Cheques: cheque book issue confirmation | Recovered | activity; named from its own screen content |
| S-145 | Cheques: cheque issuance inquiry | Recovered | activity; named from its own screen content |
| S-146 | Cheques: cheque list | Inferred | activity; feature read from its view-model |
| S-147 | Cheques: cheque menu | Recovered | activity; named from its own screen content |
| S-148 | Cheques: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-149 | Cheques: track my cheques | Inferred | activity; feature read from its view-model |
| S-150 | Cheques: track transferred cheques | Inferred | activity; feature read from its view-model |
| S-151 | Cheques: transfer digital cheque | Recovered | activity; named from its own screen content |
| S-152 | Cheques: transfer digital cheque confirmation | Recovered | activity; named from its own screen content |
| S-153 | Cheques: transferred cheques report | Inferred | activity; feature read from its view-model |

## F-036 — Chequebook

| ID | Name | Label | Basis |
|---|---|---|---|
| S-154 | Chequebook: card board | Inferred | activity; feature read from its view-model |
| S-155 | Chequebook: request details | Inferred | activity; feature read from its view-model |
| S-156 | Chequebook: request details | Inferred | activity; feature read from its view-model |

## F-037 — Digital cheque

| ID | Name | Label | Basis |
|---|---|---|---|
| S-157 | Digital cheque: activate with hamun | Recovered | activity; named from its own screen content |
| S-158 | Digital cheque: digital cheque cartable detail | Recovered | activity; named from its own screen content |
| S-159 | Digital cheque: digital cheque cartable list | Recovered | activity; named from its own screen content |

## F-038 — Cheque payment

| ID | Name | Label | Basis |
|---|---|---|---|
| S-160 | Cheque payment: agent | Recovered | activity; named from its own screen content |
| S-161 | Cheque payment: cheque-id pay cheque | Inferred | activity; feature read from its view-model |
| S-162 | Cheque payment: cheque-id pay cheque | Inferred | activity; feature read from its view-model |
| S-163 | Cheque payment: info pay cheque | Inferred | activity; feature read from its view-model |
| S-164 | Cheque payment: inquiry type pay cheque | Inferred | activity; feature read from its view-model |
| S-165 | Cheque payment: issue pay cheque | Inferred | activity; feature read from its view-model |
| S-166 | Cheque payment: made 21 validation result | Recovered | activity; named from its own screen content |
| S-167 | Cheque payment: operation pay cheque | Inferred | activity; feature read from its view-model |
| S-168 | Cheque payment: pay cheque history detail | Recovered | activity; named from its own screen content |
| S-169 | Cheque payment: pay cheque history list | Recovered | activity; named from its own screen content |
| S-170 | Cheque payment: preview pay cheque | Inferred | activity; feature read from its view-model |
| S-171 | Cheque payment: receiver pay cheque | Inferred | activity; feature read from its view-model |
| S-172 | Cheque payment: transfer pay cheque | Inferred | activity; feature read from its view-model |

## F-039 — Transfer

| ID | Name | Label | Basis |
|---|---|---|---|
| S-173 | Transfer: account transfer confirmation | Recovered | activity; name kept through obfuscation |
| S-174 | Transfer: anonymous card transfer | Recovered | activity; name kept through obfuscation |
| S-175 | Transfer: card to card without source | Recovered | activity; name kept through obfuscation |
| S-176 | Transfer: card transfer confirmation | Recovered | activity; named from its own screen content |
| S-177 | Transfer: continuous execution | Recovered | activity; named from its own screen content |
| S-178 | Transfer: continuous history | Recovered | activity; named from its own screen content |
| S-179 | Transfer: continuous history details | Recovered | activity; named from its own screen content |
| S-180 | Transfer: continuous transfer menu | Recovered | activity; named from its own screen content |
| S-181 | Transfer: continuous transfer request details | Recovered | activity; named from its own screen content |
| S-182 | Transfer: excel dest list | Recovered | activity; named from its own screen content |
| S-183 | Wage advance: request receipt | Recovered | activity; content/caller identity recovered in FR-09 (GT-RECOVERY); area/batch move pending owner approval |
| S-184 | Transfer: group transfer dest list | Recovered | activity; named from its own screen content |
| S-185 | Transfer: group transfer errors | Recovered | activity; named from its own screen content |
| S-186 | Transfer: group transfer file pick | Recovered | activity; named from its own screen content |
| S-187 | Transfer: group transfer first | Recovered | activity; named from its own screen content |
| S-188 | Transfer: group transfer validation | Recovered | activity; named from its own screen content |
| S-189 | Transfer: group transfer verify | Recovered | activity; named from its own screen content |
| S-190 | Transfer: group transfer waiting | Recovered | activity; name kept through obfuscation |
| S-191 | Transfer: interbank transfer | Recovered | activity; named from its own screen content |
| S-192 | Transfer: internal transfer | Recovered | activity; named from its own screen content |
| S-193 | Transfer: menu transfer | Recovered | activity; named from its own screen content |
| S-194 | Transfer: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-195 | Transfer: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |

## F-040 — Saved destinations

| ID | Name | Label | Basis |
|---|---|---|---|
| S-196 | Saved destinations: familiar destination | Recovered | activity; named from its own screen content |
| S-197 | Saved destinations: familiar destination management | Recovered | Compose destination; route and screen recovered |
| S-198 | Saved destinations: familiar destination picker | Recovered | Compose destination; route and screen recovered |

## F-041 — Recurring transfers

| ID | Name | Label | Basis |
|---|---|---|---|
| S-199 | Recurring transfers: repetitive transactions | Recovered | activity; named from its own screen content |
| S-200 | Recurring transfers: repetitive transactions home | Recovered | Compose destination; route/caller identity recovered in FR-10 (RT-RECOVERY); S-199 is its host |

## F-042 — IBAN conversion

| ID | Name | Label | Basis |
|---|---|---|---|
| S-201 | IBAN conversion: convert iban login | Recovered | activity; name kept through obfuscation |
| S-202 | IBAN conversion: convert iban no login | Recovered | activity; name kept through obfuscation |
| S-203 | IBAN conversion: convert iban no login pay | Recovered | activity; name kept through obfuscation |
| S-204 | IBAN conversion: convert iban no login pay without source picker | Recovered | activity; name kept through obfuscation |

## F-043 — Bills

| ID | Name | Label | Basis |
|---|---|---|---|
| S-205 | Bills: bill card info | Recovered | activity; named from its own screen content |
| S-206 | Bills: bill card info no login | Recovered | activity; named from its own screen content |
| S-207 | Bills: bill confirmation | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |
| S-208 | Bills: bill payment by typed card (no source picker) | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |
| S-209 | Bills: bill payment from account | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |
| S-210 | Bills: bill payment by card | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |

## F-044 — Mobile top-up

| ID | Name | Label | Basis |
|---|---|---|---|
| S-211 | Mobile top-up: charge by account | Inferred | activity; feature read from its view-model |
| S-212 | Mobile top-up: charge by account confirmation | Inferred | activity; feature read from its view-model |
| S-213 | Mobile top-up: charge by card | Inferred | activity; feature read from its view-model |
| S-214 | Mobile top-up: charge by card | Inferred | activity; feature read from its view-model |
| S-215 | Mobile top-up: charge card info | Recovered | activity; named from its own screen content |
| S-216 | Mobile top-up: charge card info no login | Recovered | activity; named from its own screen content |

## F-045 — Balance top-up

| ID | Name | Label | Basis |
|---|---|---|---|
| S-217 | Balance top-up: gateway receipt | Inferred | activity; feature read from its view-model |
| S-218 | Balance top-up: gateway result | Inferred | activity; feature read from its view-model |
| S-219 | Balance top-up: increase account amount | Recovered | activity; named from its own screen content |
| S-220 | Balance top-up: increase account balance | Recovered | activity; named from its own screen content |
| S-221 | Balance top-up: increase account balance amount in flow | Recovered | Compose destination; route and screen recovered |
| S-222 | Balance top-up: increase account balance amount standalone | Recovered | Compose destination; route and screen recovered |
| S-223 | Balance top-up: increase account balance waiting in flow | Recovered | Compose destination; route and screen recovered |
| S-224 | Balance top-up: increase account balance waiting standalone | Recovered | Compose destination; route and screen recovered |
| S-225 | Balance top-up: increase balance gateway | Recovered | Compose destination; route and screen recovered |
| S-226 | Balance top-up: increase balance gateway | Recovered | Compose destination; route and screen recovered |
| S-227 | Balance top-up: increase balance gateway | Recovered | Compose destination; route and screen recovered |
| S-228 | Balance top-up: increase balance gateway | Recovered | Compose destination; route and screen recovered |
| S-229 | Balance top-up: increase wallet balance | Recovered | activity; named from its own screen content |
| S-230 | Balance top-up: increase wallet balance amount in flow | Recovered | Compose destination; route and screen recovered |
| S-231 | Balance top-up: increase wallet balance amount standalone | Recovered | Compose destination; route and screen recovered |
| S-232 | Balance top-up: increase wallet balance receipt | Recovered | Compose destination; route and screen recovered |
| S-233 | Balance top-up: increase wallet balance receipt | Recovered | Compose destination; route and screen recovered |
| S-234 | Balance top-up: increase wallet balance result | Recovered | Compose destination; route and screen recovered |
| S-235 | Balance top-up: increase wallet balance waiting standalone | Recovered | Compose destination; route and screen recovered |
| S-236 | Balance top-up: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-237 | Balance top-up: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |

## F-046 — Road toll

| ID | Name | Label | Basis |
|---|---|---|---|
| S-238 | Road toll: active plaque inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-239 | Road toll: active plaque inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-240 | Road toll: annual toll confirmation | Inferred | Compose destination; screen recovered, route obfuscated |
| S-241 | Road toll: annual toll inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-242 | Road toll: annual toll inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-243 | Road toll: car violation confirmation | Inferred | Compose destination; screen recovered, route obfuscated |
| S-244 | Road toll: car violation inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-245 | Road toll: car violation inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-246 | Road toll: contact list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-247 | Road toll: driving license inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-248 | Road toll: driving license list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-249 | Road toll: free way toll confirmation | Inferred | Compose destination; screen recovered, route obfuscated |
| S-250 | Road toll: free way toll inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-251 | Road toll: free way toll inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-252 | Road toll: inquiry history | Inferred | Compose destination; screen recovered, route obfuscated |
| S-253 | Road toll: menu list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-254 | Road toll: menu list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-255 | Road toll: negative point inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-256 | Road toll: plaque list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-257 | Road toll: pollution area inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-258 | Road toll: pollution area inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-259 | Road toll: priority back handler | Inferred | Compose destination; screen recovered, route obfuscated |
| S-260 | Road toll: priority back handler | Inferred | Compose destination; screen recovered, route obfuscated |
| S-261 | Road toll: receipt | Inferred | activity; feature read from its view-model |
| S-262 | Road toll: screen | Recovered | activity; named from its own screen content |
| S-263 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-264 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-265 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-266 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-267 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-268 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-269 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-270 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-271 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-272 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-273 | Road toll: sub-flow | Recovered | nested navigation sub-graph |
| S-274 | Road toll: traffic toll confirmation | Inferred | Compose destination; screen recovered, route obfuscated |
| S-275 | Road toll: traffic toll inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-276 | Road toll: traffic toll inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-277 | Road toll: transfer tax confirmation | Inferred | Compose destination; screen recovered, route obfuscated |
| S-278 | Road toll: transfer tax inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-279 | Road toll: transfer tax inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-280 | Road toll: vehicle doc inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-281 | Road toll: vehicle doc inquiry result | Inferred | Compose destination; screen recovered, route obfuscated |

## F-047 — Donations

| ID | Name | Label | Basis |
|---|---|---|---|
| S-282 | Donations: charity confirmation | Recovered | activity; name kept through obfuscation |
| S-283 | Donations: charity picker | Recovered | activity; name kept through obfuscation |
| S-284 | Donations: charity transfer | Recovered | activity; name kept through obfuscation |

## F-048 — Loans

| ID | Name | Label | Basis |
|---|---|---|---|
| S-285 | Loans: agreement | Recovered | activity; named from its own screen content |
| S-286 | Loans: customer info content | Recovered | Compose destination; route and screen recovered |
| S-287 | Loans: hub request | Recovered | activity; named from its own screen content |
| S-288 | Loans: loan | Recovered | activity; named from its own screen content |
| S-289 | Loans: loan child | Recovered | activity; named from its own screen content |
| S-290 | Loans: loan child agreement | Recovered | Compose destination; route and screen recovered |
| S-291 | Loans: loan child calculate | Inferred | Compose destination; screen recovered, route obfuscated |
| S-292 | Loans: loan child condition details | Recovered | Compose destination; route and screen recovered |
| S-293 | Loans: loan child conditions | Recovered | Compose destination; route and screen recovered |
| S-294 | Loans: loan child customer info | Inferred | Compose destination; screen recovered, route obfuscated |
| S-295 | Loans: loan child guarantor documents | Inferred | Compose destination; screen recovered, route obfuscated |
| S-296 | Loans: loan child guarantor info | Inferred | Compose destination; screen recovered, route obfuscated |
| S-297 | Loans: loan child guarantor inquiry | Recovered | Compose destination; route and screen recovered |
| S-298 | Loans: loan child inquiry | Recovered | Compose destination; route and screen recovered |
| S-299 | Loans: loan child prevalidate | Recovered | Compose destination; route and screen recovered |
| S-300 | Loans: loan child receipt | Inferred | activity; feature read from its view-model |
| S-301 | Loans: loan child select promissory | Recovered | Compose destination; route and screen recovered |
| S-302 | Loans: loan company list | Recovered | activity; named from its own screen content |
| S-303 | Loans: loan company receipt | Inferred | activity; feature read from its view-model |
| S-304 | Loans: loan company req | Recovered | activity; named from its own screen content |
| S-305 | Loans: loan company req confirmation | Recovered | activity; named from its own screen content |
| S-306 | Loans: loan detail | Recovered | activity; named from its own screen content |
| S-307 | Loans: loan group | Recovered | Compose destination; route and screen recovered |
| S-308 | Loans: loan group calculate | Recovered | Compose destination; route and screen recovered |
| S-309 | Loans: loan group detail | Recovered | Compose destination; route and screen recovered |
| S-310 | Loans: loan marriage | Recovered | activity; named from its own screen content |
| S-311 | Loans: loan marriage agreement | Recovered | Compose destination; route and screen recovered |
| S-312 | Loans: loan marriage calculate | Inferred | Compose destination; screen recovered, route obfuscated |
| S-313 | Loans: loan marriage condition | Recovered | Compose destination; route and screen recovered |
| S-314 | Loans: loan marriage condition details | Recovered | Compose destination; route and screen recovered |
| S-315 | Loans: loan marriage confirm | Recovered | Compose destination; route and screen recovered |
| S-316 | Loans: loan marriage customer info | Inferred | Compose destination; screen recovered, route obfuscated |
| S-317 | Loans: loan marriage guarantor info | Inferred | Compose destination; screen recovered, route obfuscated |
| S-318 | Loans: loan marriage guarantor inquiry | Recovered | Compose destination; route and screen recovered |
| S-319 | Loans: loan marriage inquiry | Recovered | Compose destination; route and screen recovered |
| S-320 | Loans: loan marriage prevalidate | Recovered | Compose destination; route and screen recovered |
| S-321 | Loans: loan marriage receipt | Inferred | activity; feature read from its view-model |
| S-322 | Loans: loan marriage select promissory | Recovered | Compose destination; route and screen recovered |
| S-323 | Loans: loan marriage spouse info | Inferred | Compose destination; screen recovered, route obfuscated |
| S-324 | Loans: loan menu | Recovered | activity; named from its own screen content |
| S-325 | Loans: loan others | Recovered | activity; named from its own screen content |
| S-326 | Loans: loan others confirmation | Recovered | Compose destination; route and screen recovered |
| S-327 | Loans: loan others details | Recovered | Compose destination; route and screen recovered |
| S-328 | Loans: loan others entry | Recovered | Compose destination; route and screen recovered |
| S-329 | Loans: loan others receipt | Inferred | activity; feature read from its view-model |
| S-330 | Loans: loan payment confirmation | Inferred | activity; feature read from its view-model |
| S-331 | Loans: loan payment confirmation | Recovered | activity; named from its own screen content |
| S-332 | Loans: loan promissory draft | Recovered | Compose destination; route and screen recovered |
| S-333 | Loans: loan promissory guarantor inquiry | Recovered | Compose destination; route and screen recovered |
| S-334 | Loans: loan promissory select promissory | Recovered | Compose destination; route and screen recovered |
| S-335 | Loans: loan promissory set guarantor | Recovered | Compose destination; route and screen recovered |
| S-336 | Loans: loan request confirmation | Recovered | activity; named from its own screen content |
| S-337 | Loans: loan request receipt | Inferred | activity; feature read from its view-model |
| S-338 | Loans: loan summary | Recovered | activity; named from its own screen content |
| S-339 | Loans: welcome request | Recovered | activity; named from its own screen content |

## F-049 — Promissory notes

| ID | Name | Label | Basis |
|---|---|---|---|
| S-340 | Promissory notes: attach view model to activity lifecycle | Recovered | Compose destination; route and screen recovered |
| S-341 | Promissory notes: my promissory note details | Recovered | Compose destination; route and screen recovered |
| S-342 | Promissory notes: my promissory notes entry | Recovered | Compose destination; route and screen recovered |
| S-343 | Promissory notes: my promissory notes result | Recovered | Compose destination; route and screen recovered |
| S-344 | Promissory notes: promissory | Recovered | activity; named from its own screen content |
| S-345 | Promissory notes: promissory guarantee address | Recovered | Compose destination; route and screen recovered |
| S-346 | Promissory notes: promissory guarantee agreement | Recovered | Compose destination; route and screen recovered |
| S-347 | Promissory notes: promissory guarantee announcement | Recovered | Compose destination; route and screen recovered |
| S-348 | Promissory notes: promissory guarantee info | Recovered | Compose destination; route and screen recovered |
| S-349 | Promissory notes: promissory guarantee list | Recovered | Compose destination; route and screen recovered |
| S-350 | Promissory notes: promissory guarantee prevalidate | Recovered | Compose destination; route and screen recovered |
| S-351 | Promissory notes: promissory guarantee result | Recovered | Compose destination; route and screen recovered |
| S-352 | Promissory notes: promissory issue address | Recovered | Compose destination; route and screen recovered |
| S-353 | Promissory notes: promissory issue amount entry | Recovered | Compose destination; route and screen recovered |
| S-354 | Promissory notes: promissory issue draft | Recovered | Compose destination; route and screen recovered |
| S-355 | Promissory notes: promissory issue entry | Recovered | Compose destination; route and screen recovered |
| S-356 | Promissory notes: promissory issue payment | Recovered | Compose destination; route and screen recovered |
| S-357 | Promissory notes: promissory issue prevalidate | Recovered | Compose destination; route and screen recovered |
| S-358 | Promissory notes: promissory issue result | Recovered | Compose destination; route and screen recovered |
| S-359 | Promissory notes: promissory main | Recovered | Compose destination; route and screen recovered |
| S-360 | Promissory notes: sub-flow | Recovered | nested navigation sub-graph |
| S-361 | Promissory notes: sub-flow | Recovered | nested navigation sub-graph |
| S-362 | Promissory notes: sub-flow | Recovered | nested navigation sub-graph |

## F-050 — Guarantees

| ID | Name | Label | Basis |
|---|---|---|---|
| S-363 | Guarantees: guarantee | Recovered | activity; named from its own screen content |
| S-364 | Guarantees: guarantee detail | Recovered | activity; named from its own screen content |
| S-365 | Guarantees: guarantee list | Recovered | activity; named from its own screen content |

## F-051 — Digital signature

| ID | Name | Label | Basis |
|---|---|---|---|
| S-366 | Digital signature: digital sign detail | Inferred | Compose destination; screen recovered, route obfuscated |
| S-367 | Digital signature: digital sign issue pending case | Inferred | Compose destination; screen recovered, route obfuscated |
| S-368 | Digital signature: digital sign issue pending case | Inferred | Compose destination; screen recovered, route obfuscated |
| S-369 | Digital signature: digital sign issue pending case | Inferred | Compose destination; screen recovered, route obfuscated |
| S-370 | Digital signature: digital sign lists | Inferred | Compose destination; screen recovered, route obfuscated |
| S-371 | Digital signature: digital sign result | Inferred | Compose destination; screen recovered, route obfuscated |
| S-372 | Digital signature: menu list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-373 | Digital signature: priority back handler | Inferred | Compose destination; screen recovered, route obfuscated |
| S-374 | Digital signature: sign issue | Recovered | activity; named from its own screen content |
| S-375 | Digital signature: sign issue | Inferred | Compose destination; screen recovered, route obfuscated |
| S-376 | Digital signature: sign issue conditions | Inferred | Compose destination; screen recovered, route obfuscated |
| S-377 | Digital signature: sign issue information | Inferred | Compose destination; screen recovered, route obfuscated |
| S-378 | Digital signature: sign issue sign | Inferred | Compose destination; screen recovered, route obfuscated |
| S-379 | Digital signature: sign issue verify agreement | Inferred | Compose destination; screen recovered, route obfuscated |
| S-380 | Digital signature: sign list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-381 | Digital signature: sign video | Inferred | Compose destination; screen recovered, route obfuscated |
| S-382 | Digital signature: signed doc list | Inferred | Compose destination; screen recovered, route obfuscated |

## F-052 — Brokerage

| ID | Name | Label | Basis |
|---|---|---|---|
| S-383 | Brokerage: broker | Recovered | activity; named from its own screen content |
| S-384 | Brokerage: broker agreement | Recovered | Compose destination; route and screen recovered |
| S-385 | Brokerage: broker inquiry result | Recovered | Compose destination; route and screen recovered |
| S-386 | Brokerage: broker list | Recovered | Compose destination; route and screen recovered |
| S-387 | Brokerage: broker user pass | Recovered | Compose destination; route and screen recovered |
| S-388 | Brokerage: fund detail | Recovered | Compose destination; route and screen recovered |
| S-389 | Brokerage: fund issue | Recovered | activity; named from its own screen content |
| S-390 | Brokerage: fund issue confirmation | Recovered | activity; named from its own screen content |
| S-391 | Brokerage: fund management | Recovered | Compose destination; route and screen recovered |
| S-392 | Brokerage: fund revoke | Recovered | Compose destination; route and screen recovered |
| S-393 | Brokerage: fund revoke confirmation | Recovered | Compose destination; route and screen recovered |
| S-394 | Brokerage: order list | Recovered | Compose destination; route and screen recovered |

## F-053 — Crowdfunding

| ID | Name | Label | Basis |
|---|---|---|---|
| S-395 | Crowdfunding: crowd funding | Recovered | activity; named from its own screen content |
| S-396 | Crowdfunding: crowd funding agreement | Recovered | Compose destination; route and screen recovered |
| S-397 | Crowdfunding: crowd funding confirm | Recovered | activity; named from its own screen content |
| S-398 | Crowdfunding: crowd funding details | Recovered | Compose destination; route and screen recovered |
| S-399 | Crowdfunding: crowd funding enter amount | Recovered | activity; named from its own screen content |
| S-400 | Crowdfunding: crowd funding plans | Recovered | Compose destination; route and screen recovered |

## F-054 — Insurance

| ID | Name | Label | Basis |
|---|---|---|---|
| S-401 | Insurance: insurance installments | Recovered | activity; named from its own screen content |
| S-402 | Insurance: insurance list | Recovered | activity; named from its own screen content |
| S-403 | Insurance: insurance menu | Recovered | activity; named from its own screen content |
| S-404 | Insurance: insurance payment confirmation | Recovered | activity; named from its own screen content |
| S-405 | Insurance: insurance payment receipt | Recovered | activity; identity and callers recovered in FR-11 (BP-RECOVERY) |

## F-055 — Credit product

| ID | Name | Label | Basis |
|---|---|---|---|
| S-406 | Credit product: guide | Inferred | Compose destination; screen recovered, route obfuscated |
| S-407 | Credit product: main | Inferred | Compose destination; screen recovered, route obfuscated |
| S-408 | Credit product: onboarding | Inferred | Compose destination; screen recovered, route obfuscated |
| S-409 | Credit product: screen | Recovered | activity; named from its own screen content |

## F-056 — Wage advance

| ID | Name | Label | Basis |
|---|---|---|---|
| S-410 | Wage advance: activation account picker | Recovered | Compose destination; route and screen recovered |
| S-411 | Wage advance: activation agreement | Recovered | Compose destination; route and screen recovered |
| S-412 | Wage advance: activation conditions | Recovered | Compose destination; route and screen recovered |
| S-413 | Wage advance: activation credit rating | Recovered | Compose destination; route and screen recovered |
| S-414 | Wage advance: dashboard | Recovered | Compose destination; route and screen recovered |
| S-415 | Wage advance: entry | Recovered | Compose destination; route and screen recovered |
| S-416 | Wage advance: history | Recovered | Compose destination; route and screen recovered |
| S-417 | Wage advance: request agreement | Recovered | Compose destination; route and screen recovered |
| S-418 | Wage advance: request confirmation | Recovered | Compose destination; route and screen recovered |
| S-419 | Wage advance: request enter amount | Recovered | Compose destination; route and screen recovered |
| S-420 | Wage advance: screen | Recovered | activity; named from its own screen content |
| S-421 | Wage advance: settlement enter amount | Recovered | Compose destination; route and screen recovered |
| S-422 | Wage advance: settlement receipt | Inferred | activity; feature read from its view-model |
| S-423 | Wage advance: sub-flow | Recovered | nested navigation sub-graph |
| S-424 | Wage advance: sub-flow | Recovered | nested navigation sub-graph |
| S-425 | Wage advance: sub-flow | Recovered | nested navigation sub-graph |

## F-057 — Loyalty club

| ID | Name | Label | Basis |
|---|---|---|---|
| S-426 | Loyalty club: coin transaction history | Recovered | Compose destination; route and screen recovered |
| S-427 | Loyalty club: coin transfer confirmation | Recovered | Compose destination; route and screen recovered |
| S-428 | Loyalty club: coin transfer entry | Recovered | Compose destination; route and screen recovered |
| S-429 | Loyalty club: coin transfer inquiry | Recovered | Compose destination; route and screen recovered |
| S-430 | Loyalty club: coin transfer request list | Recovered | Compose destination; route and screen recovered |
| S-431 | Loyalty club: detail | Recovered | Compose destination; route and screen recovered |
| S-432 | Loyalty club: home | Recovered | Compose destination; route and screen recovered |
| S-433 | Loyalty club: screen | Recovered | activity; named from its own screen content |

## F-058 — Finance manager

| ID | Name | Label | Basis |
|---|---|---|---|
| S-434 | Finance manager: budget | Recovered | Compose destination; route and screen recovered |
| S-435 | Finance manager: budget detail | Recovered | Compose destination; route and screen recovered |
| S-436 | Finance manager: main | Recovered | Compose destination; route and screen recovered |
| S-437 | Finance manager: screen | Recovered | activity; named from its own screen content |
| S-438 | Finance manager: story group | Recovered | Compose destination; route and screen recovered |
| S-439 | Finance manager: story list | Recovered | Compose destination; route and screen recovered |

## F-059 — Inquiries

| ID | Name | Label | Basis |
|---|---|---|---|
| S-440 | Inquiries: detail item bounced cheque | Recovered | activity; named from its own screen content |
| S-441 | Inquiries: detail item samat | Recovered | activity; named from its own screen content |
| S-442 | Inquiries: inquiries fee | Recovered | activity; named from its own screen content |
| S-443 | Inquiries: inquiries menu | Recovered | activity; named from its own screen content |
| S-444 | Inquiries: inquiry result bounced cheque | Recovered | activity; named from its own screen content |
| S-445 | Inquiries: inquiry result samat | Recovered | activity; named from its own screen content |
| S-446 | Inquiries: national all banks guarantee inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-447 | Inquiries: national all banks guarantee inquiry result | Recovered | Compose destination; route and screen recovered |
| S-448 | Inquiries: national guarantee inquiry | Inferred | Compose destination; screen recovered, route obfuscated |
| S-449 | Inquiries: national guarantee inquiry result | Recovered | Compose destination; route and screen recovered |
| S-450 | Inquiries: national inquiry guarantee | Recovered | activity; named from its own screen content |
| S-451 | Inquiries: national inquiry guarantee result | Recovered | activity; named from its own screen content |
| S-452 | Inquiries: not success result rate | Recovered | activity; named from its own screen content |
| S-453 | Inquiries: rate result | Recovered | activity; named from its own screen content |

## F-060 — Referral

| ID | Name | Label | Basis |
|---|---|---|---|
| S-454 | Referral: advocacy history | Recovered | activity; named from its own screen content |
| S-455 | Referral: advocacy history detail | Recovered | activity; named from its own screen content |
| S-456 | Referral: advocacy menu | Recovered | activity; named from its own screen content |
| S-457 | Referral: advocacy service plan confirmation | Recovered | activity; named from its own screen content |
| S-458 | Referral: advocacy service plan list | Recovered | activity; named from its own screen content |
| S-459 | Referral: proxy agreement | Inferred | activity; feature read from its view-model |
| S-460 | Referral: proxy list | Inferred | activity; feature read from its view-model |
| S-461 | Referral: service plan agreement | Inferred | activity; feature read from its view-model |

## F-061 — Agents

| ID | Name | Label | Basis |
|---|---|---|---|
| S-462 | Agents: agent history | Recovered | activity; named from its own screen content |

## F-062 — Addresses

| ID | Name | Label | Basis |
|---|---|---|---|
| S-463 | Addresses: add address | Recovered | activity; named from its own screen content |
| S-464 | Addresses: address list | Recovered | activity; named from its own screen content |

## F-063 — Postal letters

| ID | Name | Label | Basis |
|---|---|---|---|
| S-465 | Postal letters: postal letter | Recovered | activity; named from its own screen content |
| S-466 | Postal letters: postal letter list | Inferred | Compose destination; screen recovered, route obfuscated |
| S-467 | Postal letters: postal letter select address | Inferred | Compose destination; screen recovered, route obfuscated |
| S-468 | Postal letters: postal letter sending type | Inferred | Compose destination; screen recovered, route obfuscated |

## F-064 — Merchant service

| ID | Name | Label | Basis |
|---|---|---|---|
| S-469 | Merchant service: business information | Recovered | Compose destination; route and screen recovered |
| S-470 | Merchant service: change account | Recovered | Compose destination; route and screen recovered |
| S-471 | Merchant service: deactivate terminal | Recovered | Compose destination; route and screen recovered |
| S-472 | Merchant service: inform | Recovered | Compose destination; route and screen recovered |
| S-473 | Merchant service: installation information | Recovered | Compose destination; route and screen recovered |
| S-474 | Merchant service: menu | Recovered | Compose destination; route and screen recovered |
| S-475 | Merchant service: request confirmation | Recovered | Compose destination; route and screen recovered |
| S-476 | Merchant service: request tracking list | Recovered | Compose destination; route and screen recovered |
| S-477 | Merchant service: required documents | Recovered | Compose destination; route and screen recovered |
| S-478 | Merchant service: sales terminal | Recovered | activity; named from its own screen content |
| S-479 | Merchant service: screen | Recovered | activity; named from its own screen content |
| S-480 | Merchant service: sub-flow | Recovered | nested navigation sub-graph |
| S-481 | Merchant service: sub-flow | Recovered | nested navigation sub-graph |
| S-482 | Merchant service: terminal list | Recovered | Compose destination; route and screen recovered |

## F-066 — Dashboard menu

| ID | Name | Label | Basis |
|---|---|---|---|
| F-066 | Dashboard menu: tile grid | Inferred | server-driven menu hub (F-066) |

## Unassigned

| ID | Name | Label | Basis |
|---|---|---|---|
| S-001 | Unassigned: chooser image | Recovered | activity; named from its own screen content |
| S-002 | Unassigned: menu list | Recovered | activity; named from its own screen content |
| S-003 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-004 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-005 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-006 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-007 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-008 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-009 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-010 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-011 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-013 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-015 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-017 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-018 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-019 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-020 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-021 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-022 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-023 | Unassigned: obfuscated screen | Unknown | activity; obfuscated, feature unresolved |
| S-024 | Unassigned: qr code scanner | Recovered | activity; named from its own screen content |

## Navigation edges

The graph has **590 edges**: Recovered 545, Inferred 3, Unknown 42. **43** are `dynamic` (destination server-driven or built from a computed/encrypted route); these carry `to: null`. The server-driven dashboard menu (F-066) is one hub node whose out-edge is `dynamic` because the backend chooses the tiles and where each leads.

**Gap:** 139 further `navigate`/launch call sites were found but their source screen could not be attributed statically — the reverse call path dead-ends in shared, obfuscated glue code. These are recorded, not guessed: no edge is emitted for them, so the affected destinations may show no inbound edge. Most such destinations are feature activities that the server-driven dashboard menu (F-066) opens.

