package org.ragbot.replica

data class PreviewNotification(
    val id: String,
    val category: String,
    val title: String,
    val message: String,
    val date: String,
    val time: String,
    val read: Boolean = false,
)

/** Synthetic local inbox; categories and read totals are preview data, not server rules. */
object NotificationFixture {
    val categories = linkedMapOf("all" to "همه", "info" to "اطلاع‌رسانی", "reminder" to "یادآوری")
    private val titles =
        listOf(
            "واریز حقوق",
            "یادآوری قسط تسهیلات",
            "تمدید کارت",
            "سررسید چک",
            "به‌روزرسانی برنامه",
            "تغییر شرایط سپرده‌ها",
        )
    private val messages =
        listOf(
            "مبلغ ۴۵٬۰۰۰٬۰۰۰ ریال بابت حقوق آبان به سپرده کوتاه‌مدت شما واریز شد.",
            "قسط تسهیلات خرید خودرو تا ۱۴۰۵/۰۹/۰۱ سررسید می‌شود؛ موجودی حساب را بررسی کنید.",
            "کارت شما با پایان ۳۵۶۶ تا یک ماه دیگر منقضی می‌شود؛ برای تمدید به شعبه مراجعه کنید.",
            "چک صیادی شما به مبلغ ۲٬۵۰۰٬۰۰۰ ریال فردا سررسید می‌شود.",
            "نسخه جدید همراه‌بانک با امکانات تازه منتشر شد.",
            "نرخ سود سپرده‌های کوتاه‌مدت از ابتدای ماه آینده تغییر می‌کند.",
        )
    private const val terms =
        "سود سپرده‌های کوتاه‌مدت به‌صورت روزشمار محاسبه و در پایان هر ماه به حساب واریز می‌شود. "
    val records =
        (1..6).map { index ->
            PreviewNotification(
                id = "notice-$index",
                category = if (index % 2 == 0) "reminder" else "info",
                title = titles[index - 1],
                message =
                    messages[index - 1] +
                        " شماره پیگیری 1405-$index. " +
                        if (index == 6) terms.repeat(6) else "",
                date = "۱۴۰۵/۰۸/۰${"۶۵۴۳۲۱"[index - 1]}",
                time = "۱۰:۱۵",
                read = index == 3,
            )
        }

    fun visible(deletedIds: List<String>, category: String = "all") =
        records.filter { it.id !in deletedIds && (category == "all" || it.category == category) }

    fun unreadCount(readIds: List<String>, deletedIds: List<String>) =
        visible(deletedIds).count { !it.read && it.id !in readIds }
}
