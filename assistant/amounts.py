"""Reads the amounts of money a user wrote, in code, so the model's amount can be checked.

Called by slots.checked. It reads Persian and Latin digits, number words up to the billions
(«دویست», «یک میلیون و پونصد هزار»), and the units تومان/تومن/ت and ریال. Only a number with a
scale word or a unit counts as an amount. It returns whole rials and never guesses.
"""

import re

DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")
WORDS = {
    "یک": 1, "يك": 1, "یه": 1, "دو": 2, "سه": 3, "چهار": 4, "پنج": 5, "شش": 6, "شیش": 6, "هفت": 7,
    "هشت": 8, "نه": 9, "ده": 10, "یازده": 11, "دوازده": 12, "سیزده": 13, "چهارده": 14, "پانزده": 15,
    "پونزده": 15, "شانزده": 16, "شونزده": 16, "هفده": 17, "هجده": 18, "هیجده": 18, "نوزده": 19,
    "بیست": 20, "سی": 30, "چهل": 40, "پنجاه": 50, "شصت": 60, "هفتاد": 70, "هشتاد": 80, "نود": 90,
    "صد": 100, "یکصد": 100, "دویست": 200, "سیصد": 300, "چهارصد": 400, "پانصد": 500, "پونصد": 500,
    "ششصد": 600, "شیشصد": 600, "هفتصد": 700, "هشتصد": 800, "نهصد": 900,
}  # fmt: skip
SCALES = {"هزار": 1000, "میلیون": 1_000_000, "ملیون": 1_000_000, "میلیارد": 1_000_000_000}
TOMAN = {"تومان", "تومن", "ت", "تومانی", "تومنی", "تومانه", "تومنه", "تمن", "تمنی", "تمنه", "تومنو", "تومانو"}
RIAL = {"ریال", "ريال", "ریالی", "ریاله"}


def tokens(text: str) -> list[str]:
    """The words, with digits made Latin, thousands separators dropped and numbers split from words."""
    text = text.translate(DIGITS).replace("ي", "ی").replace("ك", "ک").replace("‌", " ")
    text = re.sub(r"(?<=\d)[,٬](?=\d{3})", "", text)
    text = re.sub(r"(\d)(?=[^\d\s])|(?<=[^\d\s])(?=\d)", r"\1 ", text)
    return re.findall(r"\d+|[^\W\d]+", text)


def amounts(text: str) -> list[int]:
    """Every amount in the text, in rials, in order. No unit means rials, as the extract prompt says."""
    found, total, current, scaled, joined = [], 0, 0, False, True
    for word in tokens(text) + [""]:
        number = int(word) if word.isdigit() else WORDS.get(word)
        if number is not None and current == 0 and joined:
            current, joined = number, False
        elif number is not None and word in WORDS and joined:
            current, joined = current + number, False
        elif word in SCALES and (current or not total):
            total, current, scaled, joined = total + (current or 1) * SCALES[word], 0, True, True
        elif word == "و" and (current or total):
            joined = True
        else:
            unit = 10 if word in TOMAN else 1
            if (scaled or word in TOMAN | RIAL) and total + current:
                found.append((total + current) * unit)
            total, current, scaled, joined = 0, 0, False, True
            if number is not None:  # a number that cannot join the last one starts a new amount
                current, joined = number, False
    return found
