package org.ragbot.replica

import org.junit.Assert.*
import org.junit.Test

class HomeFixtureTest {
    @Test
    fun configurationContainsReferenceGroupsInReadingOrder() {
        assertEquals(
            listOf("کیف پول", "باشگاه مشتریان", "پشتیبانی", "سرمایه‌گذاری", "عملکرد مالی"),
            HomeFixture.top.map { it.title },
        )
        assertEquals(
            listOf("کارت‌به‌کارت", "انتقال وجه", "شارژ", "قبض", "صورتحساب"),
            HomeFixture.quick.map { it.title },
        )
        assertEquals(
            listOf(
                listOf("چک", "تسهیلات", "پایانه فروش", "مساعده"),
                listOf("سفته الکترونیک", "خدمات خودرو", "تبدیل بانکی", "استعلامات"),
                listOf("خیریه", "بیمه", "کارتابل", "سوابق"),
                listOf("حساب وکالتی", "ضمانت‌نامه", "افتتاح حساب", "شعب"),
            ),
            HomeFixture.services.chunked(4).map { row -> row.map { it.title } },
        )
        val keys = (HomeFixture.top + HomeFixture.quick + HomeFixture.services).map { it.key }
        assertEquals(26, keys.toSet().size)
    }

    @Test
    fun moneyRemainsExactAndExplicitlyUsesRials() {
        assertEquals("IRR", HomeFixture.currencyUnit)
        assertEquals("۹٬۰۰۷٬۱۹۹٬۲۵۴٬۷۴۰٬۹۹۳", persianNumber(9_007_199_254_740_993L))
        assertEquals("۰", persianNumber(0L))
        HomeFixture.transactions.forEach {
            assertEquals("IRR", it.unit)
            assertTrue(it.amount > 0L)
        }
    }
}
