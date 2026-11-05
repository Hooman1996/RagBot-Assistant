# Graph validation sheet

For the owner to fill in (Stage 2, D-021). Our side runs no observation pass on the real app.

**How to use it.** For each row, write the real app's taps in **Real-app taps** and mark
**Owner checked**. When a row marked as a demo path has real taps, it can become a real Home link in
the replica.

**Owner checked** holds `yes` when the owner confirmed the row, and `not found in the real app` when
the owner looked for the move and could not find it. A not-found row is not a tick: the replica keeps
its control and edge, and only this sheet records the answer. An empty cell means not answered yet.

**Columns.**

- **Type:** `Replica route` is a move the replica's own screens make. `Demo path` comes from the demo
  menu on Home and claims nothing about the real app. `User-only step` needs the user's OTP, card
  secret or payment, so the executor never takes it.
- **Control:** the replica's test tags, in tap order. `*` stands for a row.
- **Graph:** what `navigation-graph.json` records for this move. `None` means no recorded edge, not
  that the move is wrong.

## Trace these first

The graph records a launch from Home for these screens, but the control is Unknown. **Replica** says
what came of each trace. A link was added only where the taps and landing page match what the replica
screen shows.

| Screen | Name | Real-app taps from Home | Owner checked | Replica |
|---|---|---|---|---|
| S-193 | Transfer: menu transfer | Home → انتقال وجه→ lands on: form with انتقال وجه داخلی، انتقال وجه بین بانکی، انتقال وجه گروهی، انتقال وجه خودکار | yes | Real link: `menu-transfer` |
| S-187 | Transfer: group transfer first | Home → انتقال وجه → انتقال وجه گروهی→ lands on: form with   مبلغ، ورود دستی مقصد ها، بارگذاری فایل مقصد ها | yes | Real route: `menu-transfer > transfer-group` |
| S-178 | Transfer: continuous history | Home → انتقال وجه →انتقال وجه خودکار→لیست درخواست ها | yes | Real route: `menu-transfer > transfer-continuous > recurring-history`. Note 1 |
| S-089 | Transactions: group transfer history | Home → سوابق→سوابق انتقال وجه گروهی→ lands on: form with  group transaction history | yes | Demo path kept. Note 2 |
| S-209 | Bills: bill payment from account | Home →قبض → lands on: form with شناسه قبض and شناسه پرداخت | yes | Real link: `menu-bill` |
| S-211 | Mobile top-up: charge by account | Home →افزایش موجودی→ lands on: form with the amount for افزایش موجودی | yes | No link added. Note 3 |
| S-283 | Donations: charity picker | Home →خیریه→ lands on: a page with different charity foundations | yes | Real link: `menu-charity` |
| S-338 | Loans: loan summary | Home →تسهیلات→ lands on: a page with different loans | yes | Real link: `menu-loan` |

**Notes.**

1. **S-178.** The last tap, `لیست درخواست ها`, has no label of that name in the replica. Its S-180
   menu says `سوابق انتقال وجه خودکار`, and S-178 lists the requests under that title. Open question
   for the owner.
2. **S-089.** The replica has no `سوابق` menu screen for the middle step, and this issue adds no
   screens, so the Home tile stays unavailable and the demo path stays.
3. **S-211.** The replica's S-211 is the mobile charge form (`خرید شارژ`: phone and operator). The
   owner's taps and amount form match S-222, which `home-increase` already opens. Open question for
   the owner. The S-211 demo path stays.
4. **S-086.** The Home tile `menu-statement` has the same label as the owner's tap, but the taps name
   no landing page and the replica has two statement screens: S-016 (`صورتحساب`) and S-086
   (`درخواست صورتحساب`). The demo path stays until the owner says which one it opens.
5. **S-463.** The owner's taps reach the address list (S-464). `address-add` is the replica's next
   step from that list.
6. **S-071.** The replica's settings menu had no entry for this screen, so `settings-notifications`
   was added to match the owner's taps. The screen itself is unchanged.

## Every edge (240)

| From | To | Control | Type | Graph | Real-app taps | Owner checked |
|---|---|---|---|---|---|---|
| S-026 Home: main | S-111 Wallet: wallet main | `menu-wallet` | Replica route | None |  | yes |
| S-026 Home: main | S-077 Support: faq | `menu-support` | Replica route | Recovered |  |  |
| S-026 Home: main | S-077 Support: faq | `header-SUPPORT` | Replica route | Recovered |  |  |
| S-026 Home: main | S-147 Cheques: cheque menu | `menu-cheque` | Replica route | Recovered |  |  |
| S-026 Home: main | S-154 Chequebook: card board | `menu-cartable` | Replica route | Recovered |  |  |
| S-026 Home: main | S-253 Road toll: menu list | `menu-vehicle` | Replica route | None |  | yes |
| S-026 Home: main | S-075 Notifications | `header-NOTIFICATIONS` | Replica route | Recovered |  |  |
| S-026 Home: main | S-084 Shortcuts: shortcut home | `header-SEARCH` | Replica route | None |  | not found in the real app |
| S-026 Home: main | S-200 Recurring transfers: repetitive transactions home | `header-SHORTCUTS` | Replica route | None |  | not found in the real app |
| S-026 Home: main | S-222 Balance top-up: increase account balance amount standalone | `home-increase` | Replica route | None |  | yes |
| S-026 Home: main | S-063 Profile: account management | `expand-account > home-account-management` | Replica route | None |  | yes |
| S-026 Home: main | S-085 Accounts: account list | `control-ACCOUNT > account-list` | Replica route | None |  | yes |
| S-026 Home: main | S-016 Statement: statement list | `control-ACCOUNT > account-statement` | Replica route | None |  | yes |
| S-026 Home: main | S-063 Profile: account management | `control-ACCOUNT > account-management` | Replica route | None |  | yes |
| S-026 Home: main | S-193 Transfer: menu transfer | `menu-transfer` | Replica route | Recovered | Home → انتقال وجه | yes |
| S-026 Home: main | S-209 Bills: bill payment from account | `menu-bill` | Replica route | Recovered | Home →قبض | yes |
| S-026 Home: main | S-283 Donations: charity picker | `menu-charity` | Replica route | Recovered | Home →خیریه | yes |
| S-026 Home: main | S-338 Loans: loan summary | `menu-loan` | Replica route | Recovered | Home →تسهیلات | yes |
| S-026 Home: main | S-012 Cards: card information | `control-CARD > card-kind-DEBIT > card-action-info` | Replica route | None |  | yes |
| S-026 Home: main | S-125 Cards: card list | `control-CARD > card-kind-DEBIT > card-action-list` | Replica route | None |  |  |
| S-026 Home: main | S-124 Cards: card issue first | `control-CARD > card-kind-DEBIT > card-action-issue` | Replica route | Recovered |  |  |
| S-026 Home: main | S-127 Cards: card modify first | `control-CARD > card-kind-DEBIT > card-action-extend` | Replica route | None |  | yes |
| S-026 Home: main | S-127 Cards: card modify first | `control-CARD > card-kind-DEBIT > card-action-replicate` | Replica route | None |  | yes |
| S-026 Home: main | S-127 Cards: card modify first | `control-CARD > card-kind-DEBIT > card-action-recolor` | Replica route | None |  | yes |
| S-026 Home: main | S-121 Cards: card block auth | `control-CARD > card-kind-DEBIT > card-action-block` | Replica route | None |  | yes |
| S-026 Home: main | S-135 Cards: get or change pass | `control-CARD > card-kind-DEBIT > card-action-pin` | Replica route | None |  | yes |
| S-026 Home: main | S-136 Cards: second pass activation | `control-CARD > card-kind-DEBIT > card-action-activation` | Replica route | None |  | yes |
| S-026 Home: main | S-131 Cards: credit card detail | `control-CARD > card-kind-CREDIT > card-action-credit` | Replica route | None |  | yes |
| S-026 Home: main | S-134 Cards: credit card file loan summary | `control-CARD > card-kind-CREDIT > card-action-loan` | Replica route | None |  | yes |
| S-026 Home: main | S-091 Transactions: transaction list | `control-CARD > card-kind-CREDIT > card-action-history` | Replica route | None |  | yes |
| S-026 Home: main | S-121 Cards: card block auth | `control-CARD > card-kind-CREDIT > card-action-block` | Replica route | None |  | yes |
| S-026 Home: main | S-119 Cards: bon card balance | `control-CARD > card-kind-BON > card-action-bon` | Replica route | None |  | yes |
| S-026 Home: main | S-121 Cards: card block auth | `control-CARD > card-kind-BON > card-action-block` | Replica route | None |  | yes |
| S-026 Home: main | S-012 Cards: card information | `control-CARD > card-kind-GIFT > card-action-info` | Replica route | None |  | not found in the real app |
| S-026 Home: main | S-140 Wallet card: wallet card issue send type | `control-CARD > card-kind-WALLET > card-action-wallet-issue` | Replica route | None |  | yes |
| S-026 Home: main | S-141 Wallet card: wallet card set or change pin | `control-CARD > card-kind-WALLET > card-action-wallet-pin` | Replica route | None |  | yes |
| S-026 Home: main | S-065 Profile: edit user | `control-PROFILE > profile-edit` | Replica route | None |  | yes |
| S-026 Home: main | S-064 Profile: additional information | `control-PROFILE > profile-additional` | Replica route | None |  | yes |
| S-026 Home: main | S-063 Profile: account management | `control-PROFILE > profile-accounts` | Replica route | None |  | yes |
| S-026 Home: main | S-126 Cards: card management | `control-PROFILE > profile-cards` | Replica route | None |  | yes |
| S-026 Home: main | S-464 Addresses: address list | `control-PROFILE > profile-addresses` | Replica route | None |  | yes |
| S-026 Home: main | S-025 Settings: app settings menu | `control-PROFILE > profile-settings` | Replica route | None |  | yes |
| S-026 Home: main | S-069 Profile: invitation | `control-PROFILE > profile-invite` | Replica route | None |  | yes |
| S-026 Home: main | S-074 What's new: changelog list | `control-PROFILE > profile-versions` | Replica route | None |  | yes |
| S-026 Home: main | S-086 Statement: request statement | `demo-menu > demo-path-statement` | Demo path, not a real-app route | None | Home →صورتحساب | yes |
| S-026 Home: main | S-120 Cards: card balance without source picker | `demo-menu > demo-path-S-120` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-118 Cards: block card without source picker | `demo-menu > demo-path-S-118` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-122 Cards: card block no login 2 | `demo-menu > demo-path-S-122` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-076 Support: chatbot ask question | `demo-menu > demo-path-S-076` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-078 Support: faq no login | `demo-menu > demo-path-S-078` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-079 Support: suggestion | `demo-menu > demo-path-S-079` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-080 Support: ticket list | `demo-menu > demo-path-S-080` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-082 Support: view ticket | `demo-menu > demo-path-S-082` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-066 Profile: familiar destinations | `demo-menu > demo-path-S-066` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-197 Saved destinations: familiar destination management | `demo-menu > demo-path-S-197` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-198 Saved destinations: familiar destination picker | `demo-menu > demo-path-S-198` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-174 Transfer: anonymous card transfer | `demo-menu > demo-path-S-174` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-175 Transfer: card to card without source | `demo-menu > demo-path-S-175` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-093 Receipts: card transfer receipt | `demo-menu > demo-path-S-093` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-098 Receipts: internal transfer receipt | `demo-menu > demo-path-S-098` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-097 Receipts: iban transfer receipt | `demo-menu > demo-path-S-097` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-089 Transactions: group transfer history | `demo-menu > demo-path-S-089` | Demo path, not a real-app route | Recovered |  |  |
| S-026 Home: main | S-096 Receipts: group transfer receipt | `demo-menu > demo-path-S-096` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-210 Bills: bill payment by card | `demo-menu > demo-path-S-210` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-208 Bills: bill payment by typed card (no source picker) | `demo-menu > demo-path-S-208` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-092 Receipts: bill receipt | `demo-menu > demo-path-S-092` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-403 Insurance: insurance menu | `demo-menu > demo-path-S-403` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-405 Insurance: insurance payment receipt | `demo-menu > demo-path-S-405` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-133 Cards: credit card detail receipt | `demo-menu > demo-path-S-133` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-211 Mobile top-up: charge by account | `demo-menu > demo-path-S-211` | Demo path, not a real-app route | Recovered |  |  |
| S-026 Home: main | S-213 Mobile top-up: charge by card | `demo-menu > demo-path-S-213` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-214 Mobile top-up: charge by typed card (no source picker) | `demo-menu > demo-path-S-214` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-094 Receipts: charge receipt | `demo-menu > demo-path-S-094` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-095 Receipts: charity receipt | `demo-menu > demo-path-S-095` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-113 Wallet: wallet receipt | `demo-menu > demo-path-S-113` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-201 IBAN conversion: convert iban login | `demo-menu > demo-path-S-201` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-202 IBAN conversion: convert iban no login | `demo-menu > demo-path-S-202` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-203 IBAN conversion: convert iban no login pay | `demo-menu > demo-path-S-203` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-204 IBAN conversion: convert iban no login pay without source picker | `demo-menu > demo-path-S-204` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-324 Loans: loan menu | `demo-menu > demo-path-S-324` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-307 Loans: loan group | `demo-menu > demo-path-S-307` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-261 Road toll: receipt | `demo-menu > demo-path-S-261` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-221 Balance top-up: increase account balance amount in flow | `demo-menu > demo-path-S-221` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-230 Balance top-up: increase wallet balance amount in flow | `demo-menu > demo-path-S-230` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-157 Digital cheque: activate with hamun | `demo-menu > demo-path-digital-active` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-100 Wallet: buy with wallet | `demo-menu > demo-path-wallet-buy-qr` | Demo path, not a real-app route | None |  |  |
| S-016 Statement: statement list | S-014 Statement: statement details | `statement-*` | Replica route | Recovered |  |  |
| S-063 Profile: account management | S-067 Profile: fetch account | `fetch-accounts` | Replica route | Recovered |  |  |
| S-091 Transactions: transaction list | S-090 Transactions: transaction info | `history-*` | Replica route | Recovered |  |  |
| S-126 Cards: card management | S-068 Profile: fetch card | `fetch-cards` | Replica route | Recovered |  |  |
| S-131 Cards: credit card detail | S-132 Cards: credit card detail confirmation | `credit-pay > credit-amount-pay > credit-source-*` | Replica route | Recovered |  |  |
| S-464 Addresses: address list | S-463 Addresses: add address | `address-add` | Replica route | None | Home →پروفایل→مدیریت آدرس | yes |
| S-129 Cards: card operation select address | S-463 Addresses: add address | `add-address` | Replica route | Recovered |  |  |
| S-129 Cards: card operation select address | S-123 Cards: card color | `address-ok` | Replica route | Recovered |  |  |
| S-123 Cards: card color | S-128 Cards: card operation confirm | `color-ok` | Replica route | Recovered |  |  |
| S-140 Wallet card: wallet card issue send type | S-137 Wallet card: attach view model to activity lifecycle | `wallet-type-ok` | Replica route | None |  |  |
| S-137 Wallet card: attach view model to activity lifecycle | S-463 Addresses: add address | `wallet-add-address` | Replica route | None |  |  |
| S-137 Wallet card: attach view model to activity lifecycle | S-139 Wallet card: wallet card issue confirm | `wallet-address-ok` | Replica route | None |  |  |
| S-124 Cards: card issue first | S-129 Cards: card operation select address | `first-ok` | Replica route | Recovered |  |  |
| S-124 Cards: card issue first | S-123 Cards: card color | `first-ok` | Replica route | Recovered |  |  |
| S-127 Cards: card modify first | S-129 Cards: card operation select address | `first-ok` | Replica route | Recovered |  |  |
| S-127 Cards: card modify first | S-123 Cards: card color | `first-ok` | Replica route | Recovered |  |  |
| S-025 Settings: app settings menu | S-070 Settings | `settings-biometric` | Replica route | Recovered |  |  |
| S-025 Settings: app settings menu | S-072 Settings: service settings | `settings-services` | Replica route | Recovered |  |  |
| S-025 Settings: app settings menu | S-073 Storefront settings: storefront service settings | `settings-storefront` | Replica route | Recovered |  |  |
| S-025 Settings: app settings menu | S-130 Cards: card status visibility | `settings-cards` | Replica route | None |  |  |
| S-025 Settings: app settings menu | S-071 Settings: notification settings | `settings-notifications` | Replica route | None | Home →پروفایل →تنظیمات →تنظیمات اعلان | yes |
| S-193 Transfer: menu transfer | S-192 Transfer: internal transfer | `transfer-internal` | Replica route | None |  |  |
| S-193 Transfer: menu transfer | S-191 Transfer: interbank transfer | `transfer-interbank` | Replica route | None |  |  |
| S-193 Transfer: menu transfer | S-187 Transfer: group transfer first | `transfer-group` | Replica route | None |  |  |
| S-193 Transfer: menu transfer | S-180 Transfer: continuous transfer menu | `transfer-continuous` | Replica route | None |  |  |
| S-180 Transfer: continuous transfer menu | S-181 Transfer: continuous transfer request details | `recurring-new` | Replica route | None |  |  |
| S-180 Transfer: continuous transfer menu | S-178 Transfer: continuous history | `recurring-history` | Replica route | None |  |  |
| S-178 Transfer: continuous history | S-181 Transfer: continuous transfer request details | `continuous-new` | Replica route | Recovered |  |  |
| S-178 Transfer: continuous history | S-179 Transfer: continuous history details | `continuous-*` | Replica route | Recovered |  |  |
| S-181 Transfer: continuous transfer request details | S-177 Transfer: continuous execution | `schedule-continue > schedule-type-*` | Replica route | Recovered |  |  |
| S-186 Transfer: group transfer file pick | S-182 Transfer: excel dest list | `group-file-apply` | Replica route | Recovered |  |  |
| S-185 Transfer: group transfer errors | S-190 Transfer: group transfer waiting | `group-remove-rejected` | Replica route | Recovered |  |  |
| S-089 Transactions: group transfer history | S-087 Transactions: destination group transfer | `group-history-*-destinations` | Replica route | None |  |  |
| S-087 Transactions: destination group transfer | S-088 Transactions: group transfer destination info | `group-destination-*` | Replica route | Recovered |  |  |
| S-177 Transfer: continuous execution | S-192 Transfer: internal transfer | `schedule-execution-continue` | Replica route | Recovered |  |  |
| S-177 Transfer: continuous execution | S-191 Transfer: interbank transfer | `schedule-execution-continue` | Replica route | Recovered |  |  |
| S-200 Recurring transfers: repetitive transactions home | S-176 Transfer: card transfer confirmation | `repetitive-*-repeat` | Replica route | None |  |  |
| S-200 Recurring transfers: repetitive transactions home | S-173 Transfer: account transfer confirmation | `repetitive-*-repeat` | Replica route | None |  |  |
| S-200 Recurring transfers: repetitive transactions home | S-212 Mobile top-up: charge confirmation | `repetitive-*-repeat` | Replica route | None |  |  |
| S-174 Transfer: anonymous card transfer | S-176 Transfer: card transfer confirmation | `card-transfer-review` | Replica route | Recovered |  |  |
| S-175 Transfer: card to card without source | S-176 Transfer: card transfer confirmation | `card-transfer-review` | Replica route | Recovered |  |  |
| S-192 Transfer: internal transfer | S-173 Transfer: account transfer confirmation | `account-transfer-continue` | Replica route | Recovered |  |  |
| S-191 Transfer: interbank transfer | S-173 Transfer: account transfer confirmation | `account-transfer-continue` | Replica route | Recovered |  |  |
| S-187 Transfer: group transfer first | S-184 Transfer: group transfer dest list | `group-continue` | Replica route | Recovered |  |  |
| S-187 Transfer: group transfer first | S-186 Transfer: group transfer file pick | `group-continue` | Replica route | Recovered |  |  |
| S-184 Transfer: group transfer dest list | S-190 Transfer: group transfer waiting | `group-accept` | Replica route | Recovered |  |  |
| S-182 Transfer: excel dest list | S-190 Transfer: group transfer waiting | `group-accept` | Replica route | Recovered |  |  |
| S-190 Transfer: group transfer waiting | S-188 Transfer: group transfer validation | `group-refresh` | Replica route | Recovered |  |  |
| S-190 Transfer: group transfer waiting | S-189 Transfer: group transfer verify | `group-refresh` | Replica route | Recovered |  |  |
| S-188 Transfer: group transfer validation | S-185 Transfer: group transfer errors | `group-validation-continue` | Replica route | Recovered |  |  |
| S-188 Transfer: group transfer validation | S-190 Transfer: group transfer waiting | `group-validation-continue` | Replica route | Recovered |  |  |
| S-209 Bills: bill payment from account | S-207 Bills: bill confirmation | `bill-continue` | Replica route | Recovered |  |  |
| S-210 Bills: bill payment by card | S-207 Bills: bill confirmation | `bill-continue` | Replica route | Recovered |  |  |
| S-208 Bills: bill payment by typed card (no source picker) | S-207 Bills: bill confirmation | `bill-continue` | Replica route | Recovered |  |  |
| S-207 Bills: bill confirmation | S-205 Bills: bill card info | `bill-accept` | User-only step | Recovered |  |  |
| S-207 Bills: bill confirmation | S-206 Bills: bill card info no login | `bill-accept` | User-only step | Recovered |  |  |
| S-211 Mobile top-up: charge by account | S-212 Mobile top-up: charge confirmation | `topup-continue` | Replica route | Recovered |  |  |
| S-213 Mobile top-up: charge by card | S-212 Mobile top-up: charge confirmation | `topup-continue` | Replica route | Recovered |  |  |
| S-214 Mobile top-up: charge by typed card (no source picker) | S-212 Mobile top-up: charge confirmation | `topup-continue` | Replica route | Recovered |  |  |
| S-212 Mobile top-up: charge confirmation | S-215 Mobile top-up: charge card info | `topup-accept` | User-only step | Recovered |  |  |
| S-212 Mobile top-up: charge confirmation | S-216 Mobile top-up: charge card info no login | `topup-accept` | User-only step | Recovered |  |  |
| S-403 Insurance: insurance menu | S-402 Insurance: insurance list | `insurance-list` | Replica route | None |  |  |
| S-402 Insurance: insurance list | S-401 Insurance: insurance installments | `insurance-policy-*` | Replica route | Recovered |  |  |
| S-401 Insurance: insurance installments | S-404 Insurance: insurance payment confirmation | `insurance-pay > insurance-source-*` | Replica route | Recovered |  |  |
| S-283 Donations: charity picker | S-284 Donations: charity transfer | `charity-*` | Replica route | Recovered |  |  |
| S-284 Donations: charity transfer | S-282 Donations: charity confirmation | `charity-continue` | Replica route | Recovered |  |  |
| S-324 Loans: loan menu | S-338 Loans: loan summary | `loan-current` | Replica route | None |  |  |
| S-324 Loans: loan menu | S-307 Loans: loan group | `loan-groups` | Replica route | None |  |  |
| S-338 Loans: loan summary | S-306 Loans: loan detail | `loan-*` | Replica route | Recovered |  |  |
| S-306 Loans: loan detail | S-331 Loans: loan payment confirmation | `loan-review > loan-source-* > loan-source-confirm` | Replica route | Recovered |  |  |
| S-307 Loans: loan group | S-309 Loans: loan group detail | `loan-group-*` | Replica route | None |  |  |
| S-201 IBAN conversion: convert iban login | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-202 IBAN conversion: convert iban no login | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-111 Wallet: wallet main | S-106 Wallet: de charge wallet | `wallet-action-AUTHENTICATED_WALLET_DECHARGE` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-116 Wallet: wallet to wallet | `wallet-action-AUTHENTICATED_WALLET_W2W` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-104 Wallet: charge wallet | `wallet-action-AUTHENTICATED_WALLET_CHARGE > wallet-charge-AUTHENTICATED_WALLET_CHARGE_ACCOUNT` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-231 Balance top-up: increase wallet balance amount standalone | `wallet-action-AUTHENTICATED_WALLET_CHARGE > wallet-charge-AUTHENTICATED_WALLET_CHARGE_IPG` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-115 Wallet: wallet statement list | `wallet-menu-AUTHENTICATED_WALLET_STATEMENT` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-110 Wallet: wallet history menu | `wallet-menu-AUTHENTICATED_WALLET_TRANSACTION_HISTORY` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-112 Wallet: wallet qr | `wallet-menu-AUTHENTICATED_WALLET_RECEIVE_BARCODE` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-100 Wallet: buy with wallet | `wallet-menu-AUTHENTICATED_WALLET_BUY_WITH_WALLET` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-109 Wallet: pay bill by wallet | `wallet-menu-AUTHENTICATED_WALLET_PAY_BILL` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-102 Wallet: charge by wallet | `wallet-menu-AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_CHARGE_TRANSACTION_HISTORY` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_DE_CHARGE_TRANSACTION_HISTORY` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_W2W_TRANSACTION_HISTORY` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_BUY_TRANSACTION_HISTORY` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE_HISTORY` | Replica route | None |  |  |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_PAY_BILL_HISTORY` | Replica route | None |  |  |
| S-115 Wallet: wallet statement list | S-114 Wallet: wallet statement details | `wallet-row-*` | Replica route | Recovered |  |  |
| S-116 Wallet: wallet to wallet | S-117 Wallet: wallet to wallet confirmation | `wallet-op-continue` | Replica route | Recovered |  |  |
| S-100 Wallet: buy with wallet | S-101 Wallet: buy with wallet confirmation | `wallet-op-continue` | Demo path, not a real-app route | Recovered |  |  |
| S-106 Wallet: de charge wallet | S-107 Wallet: de charge wallet confirmation | `wallet-op-continue` | Replica route | Recovered |  |  |
| S-104 Wallet: charge wallet | S-105 Wallet: charge wallet confirmation | `wallet-op-continue` | Replica route | Recovered |  |  |
| S-102 Wallet: charge by wallet | S-103 Wallet: charge by wallet confirmation | `wallet-op-continue` | Replica route | Recovered |  |  |
| S-109 Wallet: pay bill by wallet | S-099 Wallet: bill by wallet confirmation | `wallet-op-continue` | Replica route | Recovered |  |  |
| S-222 Balance top-up: increase account balance amount standalone | S-228 Balance top-up: increase balance gateway | `funding-confirm` | User-only step | None |  |  |
| S-228 Balance top-up: increase balance gateway | S-224 Balance top-up: increase account balance waiting standalone | `funding-outcome-*` | User-only step | None |  |  |
| S-224 Balance top-up: increase account balance waiting standalone | S-217 Balance top-up: gateway receipt | `funding-fetch` | User-only step | None |  |  |
| S-221 Balance top-up: increase account balance amount in flow | S-227 Balance top-up: increase balance gateway | `funding-confirm` | User-only step | None |  |  |
| S-227 Balance top-up: increase balance gateway | S-223 Balance top-up: increase account balance waiting in flow | `funding-outcome-*` | User-only step | None |  |  |
| S-223 Balance top-up: increase account balance waiting in flow | S-232 Balance top-up: increase wallet balance receipt | `funding-fetch` | User-only step | None |  |  |
| S-231 Balance top-up: increase wallet balance amount standalone | S-225 Balance top-up: increase balance gateway | `funding-confirm` | User-only step | None |  |  |
| S-225 Balance top-up: increase balance gateway | S-235 Balance top-up: increase wallet balance waiting standalone | `funding-outcome-*` | User-only step | None |  |  |
| S-235 Balance top-up: increase wallet balance waiting standalone | S-113 Wallet: wallet receipt | `funding-fetch` | User-only step | None |  |  |
| S-230 Balance top-up: increase wallet balance amount in flow | S-226 Balance top-up: increase balance gateway | `funding-confirm` | User-only step | None |  |  |
| S-226 Balance top-up: increase balance gateway | S-234 Balance top-up: increase wallet balance result | `funding-outcome-*` | User-only step | None |  |  |
| S-234 Balance top-up: increase wallet balance result | S-233 Balance top-up: increase wallet balance receipt | `funding-fetch` | User-only step | None |  |  |
| S-147 Cheques: cheque menu | S-142 Cheques: cheque book | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_CHEQUEBOOK` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-143 Cheques: cheque book issue | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_PAPER_CHECKBOOK` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-143 Cheques: cheque book issue | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_DIGITAL_CHECKBOOK` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-145 Cheques: cheque issuance inquiry | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRACE_ISSUANCE_CHECKBOOK` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-149 Cheques: track my cheques | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_MY_CHEQUE_TRACE` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-150 Cheques: track transferred cheques | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_FOLLOWUP` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-148 Cheques: transferred cheques report filter | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_REPORT` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-148 Cheques: transferred cheques report filter | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_KARAFARIN_TRANSFERRED_CHEQUES_REPORT` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-157 Digital cheque: activate with hamun | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD_ACTIVATION` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-151 Cheques: transfer digital cheque | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFER_DIGITAL_CHEQUE` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-159 Digital cheque: digital cheque cartable list | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD_CARTABLE_DIGITAL_CHEQUE` | Replica route | None |  |  |
| S-154 Chequebook: card board | S-155 Chequebook: request details | `cardboard-*` | Replica route | Recovered |  |  |
| S-154 Chequebook: card board | S-156 Chequebook: request details | `cardboard-*` | Replica route | Recovered |  |  |
| S-142 Cheques: cheque book | S-146 Cheques: cheque list | `cheque-book-*` | Replica route | Recovered |  |  |
| S-143 Cheques: cheque book issue | S-144 Cheques: cheque book issue confirmation | `cheque-issue-continue` | Replica route | Recovered |  |  |
| S-148 Cheques: transferred cheques report filter | S-153 Cheques: transferred cheques report | `cheque-report-apply` | Replica route | Recovered |  |  |
| S-159 Digital cheque: digital cheque cartable list | S-158 Digital cheque: digital cheque cartable detail | `digital-*` | Replica route | Recovered |  |  |
| S-158 Digital cheque: digital cheque cartable detail | S-151 Cheques: transfer digital cheque | `digital-transfer` | Replica route | Recovered |  |  |
| S-151 Cheques: transfer digital cheque | S-152 Cheques: transfer digital cheque confirmation | `digital-continue` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_ANNUAL_TOLL` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_TRAFFIC_TOLL` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_FREEWAY_TOLL` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_TRANSFER_TOLL` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_RAHVAR_TOLL` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_VEHICLE_DOC_INQUIRY` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-256 Road toll: plaque list | `vehicle-menu-AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-246 Road toll: contact list | `vehicle-menu-AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-246 Road toll: contact list | `vehicle-menu-AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-248 Road toll: driving license list | `vehicle-menu-AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-254 Road toll: menu list | `vehicle-menu-AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY` | Replica route | None |  |  |
| S-256 Road toll: plaque list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-246 Road toll: contact list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-248 Road toll: driving license list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-241 Road toll: annual toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_ANNUAL_TOLL > vehicle-new` | Replica route | None |  |  |
| S-241 Road toll: annual toll inquiry | S-242 Road toll: annual toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-242 Road toll: annual toll inquiry result | S-240 Road toll: annual toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-275 Road toll: traffic toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_TRAFFIC_TOLL > vehicle-new` | Replica route | None |  |  |
| S-275 Road toll: traffic toll inquiry | S-276 Road toll: traffic toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-276 Road toll: traffic toll inquiry result | S-274 Road toll: traffic toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-250 Road toll: free way toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_FREEWAY_TOLL > vehicle-new` | Replica route | None |  |  |
| S-250 Road toll: free way toll inquiry | S-251 Road toll: free way toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-251 Road toll: free way toll inquiry result | S-249 Road toll: free way toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-257 Road toll: pollution area inquiry | `vehicle-menu-AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY > vehicle-new` | Replica route | None |  |  |
| S-257 Road toll: pollution area inquiry | S-258 Road toll: pollution area inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-078 Support: faq no login | S-081 Support: view or send message | `faq-support > support-mobile > support-identity-continue > support-otp > support-otp-continue` | User-only step | Recovered |  |  |
| S-080 Support: ticket list | S-082 Support: view ticket | `ticket-*` | Replica route | Recovered |  |  |
