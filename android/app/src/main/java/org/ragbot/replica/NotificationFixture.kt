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
    val records =
        (1..6).map { index ->
            PreviewNotification(
                id = "notice-$index",
                category = if (index % 2 == 0) "reminder" else "info",
                title = "اعلان آزمایشی $index",
                message =
                    "این متن ساختگی برای بررسی نمایش اعلان‌ها است. " +
                        "شناسه نمونه DEMO-$index فقط به داده محلی مربوط است و پیام بانک نیست. " +
                        if (index == 6)
                            "برای بررسی متن بلند، این بخش چند بار ادامه دارد. ".repeat(18)
                        else "می‌توانید جزئیات را ببینید، اعلان را بخوانید یا حذف کنید.",
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
