# Graph validation sheet

For the owner to fill in (Stage 2, D-021). Our side runs no observation pass on the real app.

**How to use it.** For each row, write the real app's taps in **Real-app taps** and mark
**Owner checked**. When a row marked as a demo path has real taps, it can become a real Home link in
the replica.

**Owner checked** holds `yes` when the owner confirmed the row, and `not found in the real app` when
the owner looked for the move and could not find it. A not-found row is not a tick: the replica keeps
its control and edge, and only this sheet records the answer. An empty cell means not answered yet.

**Columns.**

- **Type:** `Owner-traced` is a real link the owner confirmed by tapping through the real app.
  `Replica route` is a move the replica's own screens make. `Demo path` comes from the demo
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
| S-178 | Transfer: continuous history | Home → انتقال وجه →انتقال وجه خودکار→لیست درخواست ها | yes | Real route: `menu-transfer > transfer-continuous > recurring-history`, now labelled لیست درخواست ها |
| S-089 | Transactions: group transfer history | Home → سوابق→سوابق انتقال وجه گروهی→ lands on: form with  group transaction history | not found in the real app | Demo path kept. Note 2 |
| S-209 | Bills: bill payment from account | Home →قبض → lands on: form with شناسه قبض and شناسه پرداخت | yes | Real link: `menu-bill` |
| S-211 | Mobile top-up: charge by account | Home → شارژ | yes | Real link: `menu-topup`. Note 3 |
| S-283 | Donations: charity picker | Home →خیریه→ lands on: a page with different charity foundations | yes | Real link: `menu-charity` |
| S-338 | Loans: loan summary | Home → تسهیلات → تسهیلات در جریان و پرداخت | yes | Real route: `menu-loan > loan-current` |

**Notes.**

1. **S-178.** Final answer: S-180 shows ثبت درخواست and لیست درخواست ها. The replica now uses these
   labels, and the transfer menu uses the four real labels.
2. **S-089.** The owner first traced Home → سوابق → سوابق انتقال وجه گروهی, then marked S-089 not
   found in the final list. The later answer stands, so the demo path stays. Home → سوابق opens
   S-091 directly, a Designed-for-mock route. This is one to confirm with the owner.
3. **S-211.** Final answer: S-211 is the mobile charge purchase, Home → شارژ. The first trace
   (Home → افزایش موجودی) was the balance top-up, so it belongs to S-221 and S-222.
4. **S-086.** Final answer: Home → صورتحساب opens the transaction list S-016. The request form S-086
   keeps its designed route, the demo path.
5. **S-463.** The owner's taps reach the address list (S-464). `address-add` is the replica's next
   step from that list.
6. **S-071.** The replica's settings menu had no entry for this screen, so `settings-notifications`
   was added to match the owner's taps. The screen itself is unchanged.
7. **Receipts** (S-093, S-098, S-097, S-096, S-092, S-133, S-095, S-094) appear only after the user
   confirms a transaction. They are results of the user-only confirm, not navigation targets.
8. **S-118 and S-122.** The owner's card picker is the replica's S-121, which already shows that
   prompt. S-118 and S-122 are forms with a typed card number, so the trace does not land on them
   and their demo paths stay.
9. **S-221.** Home → افزایش موجودی opens S-222. One control cannot open two screen IDs, and the two
   pages are the same form, so the S-221 demo path stays.
10. **S-091.** The owner's page of six سوابق entries is the replica's S-110. Its labels now match,
    and each entry opens S-091.
11. **S-275 and S-250.** The owner found one entry, عوارض آزادراهی خودرو و موتور, for both. It opens
    S-250. S-275 keeps its own recovered menu entry, عوارض طرح ترافیک, which the owner did not see.
12. **D-001 and D-002.** The support menu and the چکاد menu have no recovered screen node, so each
    has a `D-` ID labelled `Designed for mock` in `navigation-graph.json`. Their entries are
    owner-traced.

## Second trace round (owner, 5 Nov 2026)

| Screen | Real-app taps from Home | Owner checked | Replica |
|---|---|---|---|
| S-125 |  | yes | Replica route unchanged |
| S-118, S-122 | Home > کارت > مسدود سازی کارت > card picker "یک کارت را جهت مسدود کردن انتخاب کنید" | yes | Lands on S-121. Demo paths kept, note 8 |
| S-076 | Home > پشتیبانی > چت بات | yes | Real route: `menu-support > support-menu-chatbot` |
| S-078 | Home > پشتیبانی > سوالات متداول | yes | Real route: `menu-support > support-menu-faq` |
| S-079 | Home > پشتیبانی > بازخورد (ایده) | yes | Real route: `menu-support > support-menu-suggestion` |
| S-080 | Home > پشتیبانی > صندوق (پیام ها) | yes | Real route: `menu-support > support-menu-inbox` |
| S-082 | Home > پشتیبانی > پیام (تیکت) | yes | Real route: `menu-support > support-menu-ticket` |
| S-066 | Home > پروفایل > مدیریت مقصد های آشنا | yes | Real route: `control-PROFILE > profile-destinations` |
| S-197, S-198 |  | not found in the real app | Demo paths kept |
| S-174, S-175 | Home > کارت به کارت > form with شماره مقصد and مبلغ | yes | Real link: `menu-card-transfer`, then `card-transfer-typed` for S-175 |
| S-176 | Home > کارت به کارت | yes | Replica route from the form |
| S-089 |  | not found in the real app | Demo path kept, note 2 |
| S-210 | No card bill payment exists; bill payment is Home > قبض > شناسه قبض and شناسه پرداخت | not found in the real app | Demo path kept |
| S-208 |  | not found in the real app | Demo path kept |
| S-403 | Home > بیمه | yes | Real link: `menu-insurance` |
| S-405 |  | not found in the real app | Demo path kept |
| S-211, S-213, S-094 | Home > شارژ (same screen) | yes | Real link: `menu-topup`, then `topup-pay-card` for S-213. S-094 is a receipt, note 7 |
| S-214 |  | not found in the real app | Demo path kept |
| S-201 | Home > تبدیل بانکی | yes | Real link: `menu-conversion` |
| S-202 | … > تبدیل حساب به شبا | yes | Real route: `conversion-mode-ACCOUNT_TO_IBAN` |
| S-203 | … > تبدیل کارت به شبا | yes | Real route: `conversion-mode-CARD_TO_IBAN` |
| S-204 | … > تبدیل شبا به حساب | yes | Real route: `conversion-mode-IBAN_TO_ACCOUNT` |
| S-324 | Home > تسهیلات | yes | Real link: `menu-loan` |
| S-307 | Home > تسهیلات > تسهیلات باشگاه مشتریان | yes | Real route: `menu-loan > loan-groups` |
| S-338 | Home > تسهیلات > تسهیلات در جریان و پرداخت | yes | Real route: `menu-loan > loan-current` |
| S-221 | Home > افزایش موجودی | yes | Demo path kept, note 9 |
| S-230, S-157, S-100 |  | not found in the real app | Demo paths kept. S-100 keeps its wallet menu route too |
| S-130 | Home > پروفایل > تنظیمات > وضعیت نمایش کارت مبدا | yes | Replica route, label updated |
| S-192 | Home > انتقال وجه > انتقال وجه داخلی | yes | Replica route, label updated |
| S-191 | … > انتقال وجه بین بانکی | yes | Replica route, label updated |
| S-187 | … > انتقال وجه گروهی | yes | Replica route |
| S-180 | … > انتقال وجه خودکار | yes | Replica route |
| S-106 | Home > کیف پول > برداشت از کیف | yes | Replica route |
| S-116 | … > کیف به کیف | yes | Replica route, label updated |
| S-104 | … > واریز به کیف | yes | Replica route, label updated |
| S-115 | … > گردش کیف | yes | Replica route, label updated |
| S-110 | … > سوابق | yes | Replica route, label updated |
| S-109 | … > قبض | yes | Replica route, label updated |
| S-102 | … > شارژ | yes | Replica route, label updated |
| S-091 | Home > کیف پول > سوابق > a page with سوابق کیف به کیف، سوابق برداشت از کیف، سوابق خرید با کیف، سوابق واریز به کیف، سوابق خرید شارژ، سوابق پرداخت قبض | yes | That page is S-110, note 10 |
| S-142 | Home > چک > دسته چک های من | yes | Replica route, label updated |
| S-143 | … > صدور دسته چک دیجیتال | yes | Replica route, label updated |
| S-145 | … > پیگیری صدور دسته چک | yes | Replica route, label updated |
| S-149 | … > پیگیری چک‌های من | yes | Replica route |
| S-150 | … > پیگیری چک‌های واگذار شده | yes | Replica route |
| S-148 | … > گزارش چک‌های واگذاری | yes | Replica route, label updated |
| S-151 | Home > چک > چکاد > واگذاری چک دیجیتال | yes | Real route through new menu D-002 |
| S-254 | Home > خدمات خودرو > سوابق | yes | Replica route, label updated |
| S-241 | … > عوارض سالیانه خودرو و موتور | yes | Replica route, label updated |
| S-275, S-250 | … > عوارض آزادراهی خودرو و موتور | yes | S-250 route, label updated, note 11 |

## Every edge (243)

| From | To | Control | Type | Graph | Real-app taps | Owner checked |
|---|---|---|---|---|---|---|
| S-026 Home: main | S-111 Wallet: wallet main | `menu-wallet` | Replica route | None |  | yes |
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
| S-026 Home: main | S-016 Statement: statement list | `control-ACCOUNT > account-statement` | Owner-traced | None | Home > صورتحساب | yes |
| S-026 Home: main | S-063 Profile: account management | `control-ACCOUNT > account-management` | Replica route | None |  | yes |
| S-026 Home: main | S-174 Transfer: anonymous card transfer | `menu-card-transfer` | Owner-traced | None | Home > کارت به کارت | yes |
| S-026 Home: main | S-193 Transfer: menu transfer | `menu-transfer` | Replica route | Recovered | Home → انتقال وجه | yes |
| S-026 Home: main | S-211 Mobile top-up: charge by account | `menu-topup` | Owner-traced | Recovered | Home > شارژ | yes |
| S-026 Home: main | S-209 Bills: bill payment from account | `menu-bill` | Owner-traced | Recovered | Home →قبض | yes |
| S-026 Home: main | S-016 Statement: statement list | `menu-statement` | Owner-traced | None | Home > صورتحساب | yes |
| S-026 Home: main | D-001 Support: menu | `menu-support` | Replica route | None |  |  |
| S-026 Home: main | S-283 Donations: charity picker | `menu-charity` | Replica route | Recovered | Home →خیریه | yes |
| S-026 Home: main | S-324 Loans: loan menu | `menu-loan` | Owner-traced | None | Home > تسهیلات | yes |
| S-026 Home: main | S-201 IBAN conversion: convert iban login | `menu-conversion` | Owner-traced | None | Home > تبدیل بانکی | yes |
| S-026 Home: main | S-403 Insurance: insurance menu | `menu-insurance` | Owner-traced | None | Home > بیمه | yes |
| S-026 Home: main | S-091 Transactions: transaction list | `menu-history` | Replica route | None |  |  |
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
| S-026 Home: main | S-066 Profile: familiar destinations | `control-PROFILE > profile-destinations` | Owner-traced | None | Home > پروفایل > مدیریت مقصد های آشنا | yes |
| S-026 Home: main | S-025 Settings: app settings menu | `control-PROFILE > profile-settings` | Replica route | None |  | yes |
| S-026 Home: main | S-069 Profile: invitation | `control-PROFILE > profile-invite` | Replica route | None |  | yes |
| S-026 Home: main | S-074 What's new: changelog list | `control-PROFILE > profile-versions` | Replica route | None |  | yes |
| S-026 Home: main | S-086 Statement: request statement | `demo-menu > demo-path-statement` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-120 Cards: card balance without source picker | `demo-menu > demo-path-S-120` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-118 Cards: block card without source picker | `demo-menu > demo-path-S-118` | Demo path, not a real-app route | None | Home > کارت > مسدود سازی کارت > card picker (matches S-121, see note 8) | yes |
| S-026 Home: main | S-122 Cards: card block no login 2 | `demo-menu > demo-path-S-122` | Demo path, not a real-app route | None | Home > کارت > مسدود سازی کارت > card picker (matches S-121, see note 8) | yes |
| S-026 Home: main | S-197 Saved destinations: familiar destination management | `demo-menu > demo-path-S-197` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-198 Saved destinations: familiar destination picker | `demo-menu > demo-path-S-198` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-093 Receipts: card transfer receipt | `demo-menu > demo-path-S-093` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-098 Receipts: internal transfer receipt | `demo-menu > demo-path-S-098` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-097 Receipts: iban transfer receipt | `demo-menu > demo-path-S-097` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-089 Transactions: group transfer history | `demo-menu > demo-path-S-089` | Demo path, not a real-app route | Recovered |  | not found in the real app |
| S-026 Home: main | S-096 Receipts: group transfer receipt | `demo-menu > demo-path-S-096` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-210 Bills: bill payment by card | `demo-menu > demo-path-S-210` | Demo path, not a real-app route | None | No card bill payment exists; bill payment is Home > قبض | not found in the real app |
| S-026 Home: main | S-208 Bills: bill payment by typed card (no source picker) | `demo-menu > demo-path-S-208` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-092 Receipts: bill receipt | `demo-menu > demo-path-S-092` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-405 Insurance: insurance payment receipt | `demo-menu > demo-path-S-405` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-133 Cards: credit card detail receipt | `demo-menu > demo-path-S-133` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-214 Mobile top-up: charge by typed card (no source picker) | `demo-menu > demo-path-S-214` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-094 Receipts: charge receipt | `demo-menu > demo-path-S-094` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-095 Receipts: charity receipt | `demo-menu > demo-path-S-095` | Demo path, not a real-app route | None | Result of the user-only confirm, not a navigation target | yes |
| S-026 Home: main | S-113 Wallet: wallet receipt | `demo-menu > demo-path-S-113` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-261 Road toll: receipt | `demo-menu > demo-path-S-261` | Demo path, not a real-app route | None |  |  |
| S-026 Home: main | S-221 Balance top-up: increase account balance amount in flow | `demo-menu > demo-path-S-221` | Demo path, not a real-app route | None | Home > افزایش موجودی (same page as S-222, see note 9) | yes |
| S-026 Home: main | S-230 Balance top-up: increase wallet balance amount in flow | `demo-menu > demo-path-S-230` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-157 Digital cheque: activate with hamun | `demo-menu > demo-path-digital-active` | Demo path, not a real-app route | None |  | not found in the real app |
| S-026 Home: main | S-100 Wallet: buy with wallet | `demo-menu > demo-path-wallet-buy-qr` | Demo path, not a real-app route | None |  | not found in the real app |
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
| S-025 Settings: app settings menu | S-130 Cards: card status visibility | `settings-cards` | Owner-traced | None | Home > پروفایل > تنظیمات > وضعیت نمایش کارت مبدا | yes |
| S-025 Settings: app settings menu | S-071 Settings: notification settings | `settings-notifications` | Replica route | None | Home →پروفایل →تنظیمات →تنظیمات اعلان | yes |
| S-193 Transfer: menu transfer | S-192 Transfer: internal transfer | `transfer-internal` | Owner-traced | None | Home > انتقال وجه > انتقال وجه داخلی | yes |
| S-193 Transfer: menu transfer | S-191 Transfer: interbank transfer | `transfer-interbank` | Owner-traced | None | Home > انتقال وجه > انتقال وجه بین بانکی | yes |
| S-193 Transfer: menu transfer | S-187 Transfer: group transfer first | `transfer-group` | Owner-traced | None | Home > انتقال وجه > انتقال وجه گروهی | yes |
| S-193 Transfer: menu transfer | S-180 Transfer: continuous transfer menu | `transfer-continuous` | Owner-traced | None | Home > انتقال وجه > انتقال وجه خودکار | yes |
| S-180 Transfer: continuous transfer menu | S-181 Transfer: continuous transfer request details | `recurring-new` | Replica route | None |  |  |
| S-180 Transfer: continuous transfer menu | S-178 Transfer: continuous history | `recurring-history` | Owner-traced | None | Home > انتقال وجه > انتقال وجه خودکار > لیست درخواست ها | yes |
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
| S-174 Transfer: anonymous card transfer | S-175 Transfer: card to card without source | `card-transfer-typed` | Owner-traced | None | Home > کارت به کارت | yes |
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
| S-211 Mobile top-up: charge by account | S-213 Mobile top-up: charge by card | `topup-pay-card` | Owner-traced | None | Home > شارژ | yes |
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
| S-324 Loans: loan menu | S-338 Loans: loan summary | `loan-current` | Owner-traced | None | Home > تسهیلات > تسهیلات در جریان و پرداخت | yes |
| S-324 Loans: loan menu | S-307 Loans: loan group | `loan-groups` | Owner-traced | None | Home > تسهیلات > تسهیلات باشگاه مشتریان | yes |
| S-338 Loans: loan summary | S-306 Loans: loan detail | `loan-*` | Replica route | Recovered |  |  |
| S-306 Loans: loan detail | S-331 Loans: loan payment confirmation | `loan-review > loan-source-* > loan-source-confirm` | Replica route | Recovered |  |  |
| S-307 Loans: loan group | S-309 Loans: loan group detail | `loan-group-*` | Replica route | None |  |  |
| S-201 IBAN conversion: convert iban login | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-202 IBAN conversion: convert iban no login | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-203 IBAN conversion: convert iban no login pay | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-204 IBAN conversion: convert iban no login pay without source picker | S-011 IBAN conversion: ownership result | `conversion-calculate` | Replica route | Recovered |  |  |
| S-201 IBAN conversion: convert iban login | S-202 IBAN conversion: convert iban no login | `conversion-mode-ACCOUNT_TO_IBAN` | Owner-traced | None | Home > تبدیل بانکی > تبدیل حساب به شبا | yes |
| S-201 IBAN conversion: convert iban login | S-203 IBAN conversion: convert iban no login pay | `conversion-mode-CARD_TO_IBAN` | Owner-traced | None | Home > تبدیل بانکی > تبدیل کارت به شبا | yes |
| S-201 IBAN conversion: convert iban login | S-204 IBAN conversion: convert iban no login pay without source picker | `conversion-mode-IBAN_TO_ACCOUNT` | Owner-traced | None | Home > تبدیل بانکی > تبدیل شبا به حساب | yes |
| S-111 Wallet: wallet main | S-106 Wallet: de charge wallet | `wallet-action-AUTHENTICATED_WALLET_DECHARGE` | Owner-traced | None | Home > کیف پول > برداشت از کیف | yes |
| S-111 Wallet: wallet main | S-116 Wallet: wallet to wallet | `wallet-action-AUTHENTICATED_WALLET_W2W` | Owner-traced | None | Home > کیف پول > کیف به کیف | yes |
| S-111 Wallet: wallet main | S-104 Wallet: charge wallet | `wallet-action-AUTHENTICATED_WALLET_CHARGE > wallet-charge-AUTHENTICATED_WALLET_CHARGE_ACCOUNT` | Owner-traced | None | Home > کیف پول > واریز به کیف | yes |
| S-111 Wallet: wallet main | S-231 Balance top-up: increase wallet balance amount standalone | `wallet-action-AUTHENTICATED_WALLET_CHARGE > wallet-charge-AUTHENTICATED_WALLET_CHARGE_IPG` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-115 Wallet: wallet statement list | `wallet-menu-AUTHENTICATED_WALLET_STATEMENT` | Owner-traced | None | Home > کیف پول > گردش کیف | yes |
| S-111 Wallet: wallet main | S-110 Wallet: wallet history menu | `wallet-menu-AUTHENTICATED_WALLET_TRANSACTION_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-111 Wallet: wallet main | S-112 Wallet: wallet qr | `wallet-menu-AUTHENTICATED_WALLET_RECEIVE_BARCODE` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-100 Wallet: buy with wallet | `wallet-menu-AUTHENTICATED_WALLET_BUY_WITH_WALLET` | Replica route | None |  |  |
| S-111 Wallet: wallet main | S-109 Wallet: pay bill by wallet | `wallet-menu-AUTHENTICATED_WALLET_PAY_BILL` | Owner-traced | None | Home > کیف پول > قبض | yes |
| S-111 Wallet: wallet main | S-102 Wallet: charge by wallet | `wallet-menu-AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE` | Owner-traced | None | Home > کیف پول > شارژ | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_CHARGE_TRANSACTION_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_DE_CHARGE_TRANSACTION_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_W2W_TRANSACTION_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_BUY_TRANSACTION_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_BUY_MOBILE_CHARGE_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
| S-110 Wallet: wallet history menu | S-091 Transactions: transaction list | `wallet-menu-AUTHENTICATED_WALLET_PAY_BILL_HISTORY` | Owner-traced | None | Home > کیف پول > سوابق | yes |
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
| S-147 Cheques: cheque menu | S-142 Cheques: cheque book | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_CHEQUEBOOK` | Owner-traced | None | Home > چک > دسته چک های من | yes |
| S-147 Cheques: cheque menu | S-143 Cheques: cheque book issue | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_PAPER_CHECKBOOK` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | S-143 Cheques: cheque book issue | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_DIGITAL_CHECKBOOK` | Owner-traced | None | Home > چک > صدور دسته چک دیجیتال | yes |
| S-147 Cheques: cheque menu | S-145 Cheques: cheque issuance inquiry | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRACE_ISSUANCE_CHECKBOOK` | Owner-traced | None | Home > چک > پیگیری صدور دسته چک | yes |
| S-147 Cheques: cheque menu | S-149 Cheques: track my cheques | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_MY_CHEQUE_TRACE` | Owner-traced | None | Home > چک > پیگیری چک‌های من | yes |
| S-147 Cheques: cheque menu | S-150 Cheques: track transferred cheques | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_FOLLOWUP` | Owner-traced | None | Home > چک > پیگیری چک‌های واگذار شده | yes |
| S-147 Cheques: cheque menu | S-148 Cheques: transferred cheques report filter | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_REPORT` | Owner-traced | None | Home > چک > گزارش چک‌های واگذاری | yes |
| S-147 Cheques: cheque menu | S-148 Cheques: transferred cheques report filter | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_KARAFARIN_TRANSFERRED_CHEQUES_REPORT` | Replica route | None |  |  |
| S-147 Cheques: cheque menu | D-002 Cheques: digital cheque menu | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD` | Replica route | None |  |  |
| D-002 Cheques: digital cheque menu | S-157 Digital cheque: activate with hamun | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD_ACTIVATION` | Replica route | None |  |  |
| D-002 Cheques: digital cheque menu | S-151 Cheques: transfer digital cheque | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFER_DIGITAL_CHEQUE` | Owner-traced | None | Home > چک > چکاد > واگذاری چک دیجیتال | yes |
| D-002 Cheques: digital cheque menu | S-159 Digital cheque: digital cheque cartable list | `cheque-menu-AUTHENTICATED_HOME_CHEQUE_CHAKAD_CARTABLE_DIGITAL_CHEQUE` | Replica route | None |  |  |
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
| S-253 Road toll: menu list | S-254 Road toll: menu list | `vehicle-menu-AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY` | Owner-traced | None | Home > خدمات خودرو > سوابق | yes |
| S-256 Road toll: plaque list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-246 Road toll: contact list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-248 Road toll: driving license list | S-252 Road toll: inquiry history | `vehicle-history-*` | Replica route | Recovered |  |  |
| S-253 Road toll: menu list | S-241 Road toll: annual toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_ANNUAL_TOLL > vehicle-new` | Owner-traced | None | Home > خدمات خودرو > عوارض سالیانه خودرو و موتور | yes |
| S-241 Road toll: annual toll inquiry | S-242 Road toll: annual toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-242 Road toll: annual toll inquiry result | S-240 Road toll: annual toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-275 Road toll: traffic toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_TRAFFIC_TOLL > vehicle-new` | Owner-traced | None | Home > خدمات خودرو > عوارض آزادراهی خودرو و موتور | yes |
| S-275 Road toll: traffic toll inquiry | S-276 Road toll: traffic toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-276 Road toll: traffic toll inquiry result | S-274 Road toll: traffic toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-250 Road toll: free way toll inquiry | `vehicle-menu-AUTHENTICATED_HOME_FREEWAY_TOLL > vehicle-new` | Owner-traced | None | Home > خدمات خودرو > عوارض آزادراهی خودرو و موتور | yes |
| S-250 Road toll: free way toll inquiry | S-251 Road toll: free way toll inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| S-251 Road toll: free way toll inquiry result | S-249 Road toll: free way toll confirmation | `toll-pay` | User-only step | Recovered |  |  |
| S-253 Road toll: menu list | S-257 Road toll: pollution area inquiry | `vehicle-menu-AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY > vehicle-new` | Replica route | None |  |  |
| S-257 Road toll: pollution area inquiry | S-258 Road toll: pollution area inquiry result | `toll-inquiry > toll-fee-* > otp-ok` | User-only step | Recovered |  |  |
| D-001 Support: menu | S-076 Support: chatbot ask question | `support-menu-chatbot` | Owner-traced | None | Home > پشتیبانی > چت بات | yes |
| D-001 Support: menu | S-078 Support: faq no login | `support-menu-faq` | Owner-traced | None | Home > پشتیبانی > سوالات متداول | yes |
| D-001 Support: menu | S-079 Support: suggestion | `support-menu-suggestion` | Owner-traced | None | Home > پشتیبانی > بازخورد (ایده) | yes |
| D-001 Support: menu | S-080 Support: ticket list | `support-menu-inbox` | Owner-traced | None | Home > پشتیبانی > صندوق (پیام ها) | yes |
| D-001 Support: menu | S-082 Support: view ticket | `support-menu-ticket` | Owner-traced | None | Home > پشتیبانی > پیام (تیکت) | yes |
| S-078 Support: faq no login | S-081 Support: view or send message | `faq-support > support-mobile > support-identity-continue > support-otp > support-otp-continue` | User-only step | Recovered |  |  |
| S-080 Support: ticket list | S-082 Support: view ticket | `ticket-*` | Replica route | Recovered |  |  |
