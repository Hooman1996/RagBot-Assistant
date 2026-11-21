# D-037: A bare number correcting an amount is in that amount's scale and unit

**Source:** Owner, 21 Nov 2026 (issue #202): "approve #202 with option B and implement it".

## Decision

1. **The rule.** When the user's newest message corrects an amount with a bare number of up to three
   digits, with no scale or unit word («نه ۱۵۰ بشه»), the number takes the scale and unit of the
   amount it replaces. That amount is the last one the user wrote with digits and a scale or unit
   word. So «۱۵۰» after «۱۰۰ هزار تومن» is 150 thousand toman, which is 1,500,000 rials.
2. **The model must have read that number.** The model's amount counts only when it is the bare
   number itself (150, which is how the workflow's reader reads the message on its own) or the
   number already in that scale (150,000 toman). The amount used is always the number times the
   replaced amount's scale, never the model's own scale.
3. **Anything else is asked, and the old amount is never used.** A different reading, no reading,
   two different bare numbers, or no earlier amount with a scale or unit leaves the amount missing,
   so the agent asks «چند ریال…؟».
4. **The corrected amount stays the user's.** In later turns, that bare number counts as an amount
   the user wrote, in the same scale. So a later correction of something else («از کارت 4544
   باشه») keeps 150 thousand toman and does not bring the first amount back. This was found live.
5. **Unchanged:**
   - A newest message that names a full amount («نه ۳۰۰ هزار تومن») replaces the old one, as today.
   - A message with no bare number keeps the amount held. A card's last four digits are four
     digits, so they are not a bare amount.
   - A bare number answering the amount question, with no earlier amount, is read in rials, as
     today.
   - D-030: an amount with no unit is in tomans.
6. **The shared check changes, so both agents get it.** Both are measured on the frozen sets.

## Why

The money check used to fall back to "the one amount the user wrote" when the model's amount was not
the user's. In a correction, that brought back the very amount the user was changing: the review
was prepared again with the old amount, a critical wrong action in both agents (tuning CT-006).
People correct by the number alone, in the scale they just used, so reading it in that scale is what
they mean. Requiring that the model read the same number keeps money to the user's own words.

## The run

See [P6-11 in the conversation report](../reports/conversation-baseline.md#p6-11-202-a-bare-number-correcting-an-amount).
