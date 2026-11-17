# D-031 — Real-looking mock data, and the held-out set follows it

**Source:** Owner, 17 Nov 2026 (issue #173): "Make the mock datas, cards or any examples like real
life not just numbers with lots of zeros… just add something that looks like real iranian banks,
you can use any prefix needed… we should have some real looking iranian names."

## Decision

1. **Numbers look real and stay synthetic.** Cards carry Iranian bank prefixes with a valid Luhn
   checksum: the user's own cards are Mellat (610433), رضا's card is Melli (603799), and other
   banks' cards appear as destinations. IBANs carry real bank codes (the user's own: Mellat, 012)
   and pass mod 97. National codes pass their checksum, phones use the operators' prefixes (0912,
   0919 Hamrah-e Aval; 0935, 0936 Irancell; 0921, 0922 Rightel), and Sayad IDs, bill IDs, loan file
   numbers, trace numbers and ticket IDs look like the real ones. Every number is generated. A test
   input that was deliberately invalid stays invalid in the same way.
2. **Names look real and are invented.** People are common Iranian full names (the user is
   «امیرحسین کاظمی», the contact is «رضا محمدی»), and accounts, branches, bills, charities, loans,
   transfer reasons and notes read like a real bank's («سپرده کوتاه‌مدت», «سپرده پس‌انداز»,
   «پرداخت اجاره»). The names «علی» and «سارا» stay unknown, and «حساب جاری» stays a name no account
   has, as the held-out cases expect.
3. **The app's own demo notices stay.** The banner and the notices that say a step is simulated
   («نسخه نمایشی…», «مرحله نمایشی…») are kept, so the replica is never taken for the real bank.
4. **The held-out set follows the data.** 73 of the 145 cases change only in the numbers and names
   they type or expect («از حساب نمایشی» → «از سپرده کوتاه‌مدت», «کارت 0018» → «کارت 3566»). No
   case changes in meaning: the same feature, the same records, the same questions and the same
   end. The set is frozen on 17 Nov 2026 at SHA-256 `a052b637c75cb5054a3b7dea5133f26395c8772d2499f2b9951768239e967910`. This replaces the hash in
   [D-030](0030-live-test-fixes.md). Everything else in D-025 to D-030 holds.

## Why

The demo should look like the bank's app on a real customer's phone, not like test data.

## The fresh run

Held-out run twice: 0 / 145 critical wrong actions, 101 / 101 unambiguous, 108 / 108 prepared and
27 / 27 safe stops, the same on both runs. Tuning: 0 / 55, 41 / 41, 45 / 45, 7 / 7. A first run found
one held-out turn written with Arabic letters («کيف نمايشي تجاري») that the renaming had missed; it
now names «کيف پول فروشگاه» like the others.
