"""Reading the amounts a user wrote: digits or words, scale words, toman or rial, always whole rials."""

import pytest

from assistant.amounts import amounts, scale_of


@pytest.mark.parametrize(
    ("text", "rials"),
    [
        ("۲۵۰ هزار تومان به رضا کارت به کارت کن", [2500000]),
        ("یه مساعده یک میلیون و پونصد هزار تومنی", [15000000]),
        ("مبلغ ۲ میلیون و ۵۰۰ هزار ریال", [2500000]),
        ("میخوام یه دو میلیون ریال اضافه کنم", [2000000]),
        ("صد و بیست و پنج هزار تومان", [1250000]),
        ("2,500,000 ریال", [2500000]),
        ("۵۰تومن", [500]),
        ("دویست هزار تومن به علی", [2000000]),
        ("۲۰۰ هزار تومن، نه، ۳۰۰ هزار تومن", [2000000, 3000000]),
        ("۵۰ هزار کارت به کارت کن", [500000]),
        ("یک میلیون و دویست هزار", [12000000]),
    ],
)
def test_an_amount_is_read_in_whole_rials(text: str, rials: list[int]) -> None:
    assert amounts(text) == rials
    assert all(type(value) is int for value in amounts(text))


@pytest.mark.parametrize(
    "text",
    [
        "۱۲ قسط، کد ملی 5840573851",
        "برای 09128978800",
        "از 1405/09/01 تا 1405/12/01",
        "۱۰۰ امتیاز",
        "نه، ممنون",
    ],
)
def test_a_number_with_no_unit_or_scale_word_is_not_an_amount(text: str) -> None:
    assert amounts(text) == []


@pytest.mark.parametrize(
    ("text", "scale"),
    [
        ("۱۰۰ هزار تومن از کارت 3566 به رضا", 10_000),
        ("۲ میلیون ریال بفرست", 1_000_000),
        ("۵۰۰ تومن", 10),
        ("۵۰ هزار کارت به کارت کن", 10_000),  # no unit: tomans (D-030)
        ("از کارت 4544 به رضا", None),
        ("یک میلیون تومن", None),  # a number word, not digits: no shorthand is read from it
    ],
)
def test_the_scale_of_a_written_amount(text: str, scale: int | None) -> None:
    assert scale_of(text) == scale
