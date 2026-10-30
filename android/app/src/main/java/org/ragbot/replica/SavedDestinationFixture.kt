package org.ragbot.replica

import java.io.Serializable

object SavedDestinationRoutes {
    const val LEGACY = "S-066"
    const val MANAGEMENT = "S-197"
    const val PICKER = "S-198"
    val implemented = setOf(LEGACY, MANAGEMENT, PICKER)
}

data class SavedDestination(
    val id: String,
    val type: String,
    val name: String,
    val destination: String,
    val description: String = "داده ساختگی برای پیش‌نمایش",
) : Serializable

/**
 * SD-FIXTURE: invented records, recovered field lengths and local duplicate checks; no ownership
 * inquiry.
 */
object SavedDestinationFixture {
    val types =
        linkedMapOf("ACCOUNT" to "حساب", "IBAN" to "شبا", "CARD" to "کارت", "WALLET" to "کیف")
    val records =
        types.keys.flatMap { type ->
            (1..6).map { index ->
                val number =
                    when (type) {
                        "ACCOUNT" -> "000000000000$index"
                        "IBAN" -> "IR00000000000000000000000$index"
                        "CARD" -> "000000000000000$index"
                        else -> "111111111111111$index"
                    }
                SavedDestination(
                    "${type.lowercase()}-$index",
                    type,
                    "${types[type]} آزمایشی $index",
                    number,
                    if (index == 6)
                        "توضیح ساختگی بلند برای بررسی خوانایی فارسی و شناسه DEMO-6. ".repeat(6)
                    else "داده ساختگی برای پیش‌نمایش",
                )
            }
        } +
            (1..6).map { index ->
                SavedDestination("loan-$index", "LOAN", "وام آزمایشی $index", "DEMO-LOAN-$index")
            }

    fun visible(records: List<SavedDestination>, type: String, query: String) =
        records.filter {
            it.type == type &&
                (query.isBlank() ||
                    it.name.contains(query.trim(), ignoreCase = true) ||
                    it.destination.contains(query.trim(), ignoreCase = true))
        }

    fun nextId(records: List<SavedDestination>): String {
        var next = 1
        while (records.any { it.id == "local-$next" }) next++
        return "local-$next"
    }

    fun validation(
        records: List<SavedDestination>,
        id: String?,
        type: String,
        name: String,
        destination: String,
    ): String? =
        when {
            name.isBlank() || destination.isBlank() -> "نام و شماره مقصد را وارد کنید."
            type == "ACCOUNT" &&
                (destination.length != 13 || destination.any { it !in '0'..'9' }) ->
                "شماره حساب باید ۱۳ رقم باشد."
            type == "WALLET" && destination.length != 16 -> "شناسه کیف باید ۱۶ نویسه باشد."
            records.any { it.id != id && it.destination == destination.trim() } ->
                "این مقصد آشنا هم اکنون وجود دارد."
            type !in types -> "افزودن این نوع مقصد در پیش‌نمایش در دسترس نیست."
            else -> null
        }
}
