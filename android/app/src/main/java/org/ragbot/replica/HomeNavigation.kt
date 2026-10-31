package org.ragbot.replica

/** Only implemented graph content is registered. Tab states have no separate graph IDs. */
object HomeDestinations {
    const val HOME = "S-026"
    const val MENU = "F-066"
    val implemented =
        setOf(HOME, MENU) +
            AccountDestinations.implemented +
            CardDestinations.implemented +
            CardRequestDestinations.implemented +
            CardSecurityDestinations.implemented +
            WalletDestinations.implemented +
            ProfileDestinations.implemented +
            setOf("S-075") +
            SupportDestinations.implemented +
            SavedDestinationRoutes.implemented +
            ShortcutDestinations.implemented +
            CardTransferDestinations.implemented +
            AccountTransferDestinations.implemented +
            GroupTransferDestinations.implemented +
            RecurringTransferDestinations.implemented
}

enum class HomeControl(val title: String) {
    HOME("خانه"),
    ACCOUNT("حساب"),
    CARD("کارت"),
    PROFILE("پروفایل"),
    QR("بارکد"),
    SEARCH("جستجو"),
    SHORTCUTS("نشان‌شده‌ها"),
    LOGOUT("خروج"),
    NOTIFICATIONS("اعلان‌ها"),
    SUPPORT("پیام پشتیبانی"),
    BALANCE("مشاهده موجودی"),
    INCREASE("افزایش موجودی"),
    ACCOUNT_MANAGEMENT("مدیریت حساب"),
    SHARE("اشتراک‌گذاری"),
}

/** Ordinary UI handler. Unsupported controls never change the current destination. */
class HomeNavigation {
    val destination = HomeDestinations.HOME

    fun navigate(destinationId: String): Boolean = destinationId in HomeDestinations.implemented

    fun unavailable(control: HomeControl): String? =
        when (control) {
            HomeControl.HOME -> null
            else -> "${control.title} در این پیش‌نمایش آماده نیست. در خانه می‌مانید."
        }
}
