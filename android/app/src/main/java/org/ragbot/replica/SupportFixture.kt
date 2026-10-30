package org.ragbot.replica

object SupportDestinations {
    const val CHATBOT = "S-076"
    const val FAQ = "S-077"
    const val FAQ_NO_LOGIN = "S-078"
    const val SUGGESTION = "S-079"
    const val TICKETS = "S-080"
    const val MESSAGE = "S-081"
    const val TICKET = "S-082"
    val implemented = setOf(CHATBOT, FAQ, FAQ_NO_LOGIN, SUGGESTION, TICKETS, MESSAGE, TICKET)
}

data class PreviewFaq(val question: String, val answer: String)

data class PreviewSupportTicket(
    val key: String,
    val caption: String,
    val question: String,
    val status: String,
    val modified: String,
)

data class PreviewSupportMessage(
    val ticketKey: String,
    val text: String,
    val fromSupport: Boolean,
    val time: String,
)

/** NS-FIXTURE: every question, answer, ticket, conversation and identifier is invented. */
object SupportFixture {
    const val firstTicket = "DEMO-1001"
    val faqs =
        listOf(
            PreviewFaq(
                "این صفحه نمونه چیست؟",
                "این پاسخ صرفاً محتوای ساختگی برای بررسی ظاهر صفحه است و راهنمای بانکی نیست.",
            ),
            PreviewFaq(
                "آیا پیام نمونه ارسال می‌شود؟",
                "خیر. متن پیش‌نویس فقط در همین پیش‌نمایش محلی باقی می‌ماند.",
            ),
            PreviewFaq(
                "شماره DEMO چه معنایی دارد؟",
                "این شناسه ساختگی است و به درخواست واقعی ارتباطی ندارد.",
            ),
            PreviewFaq(
                "متن طولانی چگونه نمایش داده می‌شود؟",
                "این متن ساختگی برای بررسی خوانایی فارسی و پیمایش صفحه قرار داده شده است. هیچ سیاست، خدمت یا پاسخ رسمی بانک را بیان نمی‌کند.",
            ),
        )
    val tickets =
        listOf(
            PreviewSupportTicket(
                firstTicket,
                "بررسی نمونه چیدمان",
                "این یک پیام ساختگی برای بررسی نمایش گفتگو است.",
                "ASKED",
                "۱۴۰۵/۰۸/۰۸ • ۱۰:۳۰",
            ),
            PreviewSupportTicket(
                "DEMO-1002",
                "نمونه پاسخ و ادامه گفتگو",
                "این درخواست ساختگی فقط برای بررسی متن فارسی است.",
                "ANSWERED",
                "۱۴۰۵/۰۸/۰۷ • ۰۹:۲۳",
            ),
            PreviewSupportTicket(
                "DEMO-1003",
                "نمونه پیام پایان یافته",
                "این پیام ساختگی پایان‌یافته است.",
                "CLOSED",
                "۱۴۰۵/۰۸/۰۶ • ۱۶:۴۰",
            ),
        )
    val messages =
        tickets.flatMap { ticket ->
            listOf(
                PreviewSupportMessage(
                    ticket.key,
                    ticket.question,
                    false,
                    if (ticket.key == "DEMO-1002") "۱۴۰۵/۰۸/۰۷ • ۰۹:۱۰" else ticket.modified,
                )
            ) +
                if (ticket.status == "ASKED") emptyList()
                else
                    listOf(
                        PreviewSupportMessage(
                            ticket.key,
                            "پاسخ ساختگی برای بررسی نمایش گفتگو؛ این متن پاسخ پشتیبانی بانک نیست.",
                            true,
                            if (ticket.key == "DEMO-1002") "۱۴۰۵/۰۸/۰۷ • ۰۹:۱۵" else ticket.modified,
                        )
                    )
        } +
            List(8) { index ->
                PreviewSupportMessage(
                    "DEMO-1002",
                    "پیام ساختگی ${index + 1} درباره چیدمان نمایشی و شناسه DEMO-1002؛ این متن طولانی فقط برای بررسی خوانایی فارسی، ترتیب گفتگو و پیمایش است. هیچ پاسخ یا سیاست بانکی را بیان نمی‌کند. "
                        .repeat(2),
                    index % 2 == 0,
                    "۱۴۰۵/۰۸/۰۷ • ۰۹:${persianNumber((16 + index).toLong())}",
                )
            }
}

/** NS-RULES: recovered input enablement checks use raw UTF-16 length. */
object SupportRules {
    fun message(value: String) = value.length in 5..250

    fun question(value: String) = value.length in 4..250

    fun suggestion(value: String) = value.isNotEmpty()
}
