package org.ragbot.replica

/*
 * What each row-dependent edge needs chosen before the move.
 * Read by the profile export, and by the bridge to apply navigate's arguments to a step's tags.
 * A row tag holds one *, and the chosen key replaces it. Keys come from the assistant, so the
 * tag they make still has to pass Policy.
 */

/** One choice needed before an edge can be taken: a record from [lookup], or a menu entry. */
internal data class Selection(
    val ref: String,
    val type: String,
    val description: String,
    val ask: String,
    val lookup: String? = null,
)

internal object ProfileSelections {
    private fun record(ref: String, description: String, ask: String, lookup: String) =
        Selection(ref, "ref", description, ask, lookup)

    private fun entry(ref: String, description: String, ask: String) =
        Selection(ref, "choice", description, ask)

    /** By the edge's row tag. */
    private val byTag =
        mapOf(
            "budget-*" to entry("budget", "دسته بودجه", "بودجه کدام دسته؟"),
            "transaction-*" to record("transaction", "تراکنش", "کدام تراکنش؟", "transactions"),
            "history-*" to entry("history-row", "ردیف سوابق", "کدام ردیف از سوابق؟"),
            "statement-*" to record("statement-row", "ردیف صورتحساب", "کدام ردیف؟", "statements"),
            "credit-source-*" to record("credit-source", "حساب مبدا", "بازپرداخت از کدام حساب انجام شود؟", "accounts"),
            "continuous-*" to record("standing-order", "انتقال خودکار", "کدام انتقال خودکار؟", "standing_orders"),
            "schedule-type-*" to entry("schedule-type", "نوع انتقال", "انتقال داخلی باشد یا بین‌بانکی؟"),
            "group-history-*-destinations" to record("group-history", "انتقال گروهی", "کدام انتقال گروهی؟", "group_history"),
            "group-destination-*" to entry("group-destination", "مقصد گروهی", "کدام مقصد؟"),
            "repetitive-*-repeat" to record("repetitive", "تراکنش تکراری", "کدام تراکنش تکرار شود؟", "repetitive"),
            "insurance-policy-*" to record("insurance-policy", "بیمه‌نامه", "قسط کدام بیمه‌نامه؟", "insurance"),
            "insurance-source-*" to record("insurance-source", "حساب مبدا", "قسط بیمه از کدام حساب پرداخت شود؟", "accounts"),
            "charity-*" to record("charity", "خیریه", "به کدام خیریه کمک می‌کنید؟", "charities"),
            "loan-*" to record("loan", "تسهیلات", "قسط کدام تسهیلات را پرداخت می‌کنید؟", "loans"),
            "loan-source-*" to record("loan-source", "حساب مبدا", "قسط از کدام حساب پرداخت شود؟", "accounts"),
            "loan-group-*" to record("loan-group", "گروه تسهیلات", "کدام گروه تسهیلات؟", "loan_groups"),
            "wallet-row-*" to record("wallet-row", "ردیف صورتحساب کیف", "کدام ردیف؟", "wallet_statement"),
            "cheque-menu-*" to entry("cheque-kind", "نوع خدمت چک", "کاغذی یا دیجیتال؟"),
            "cardboard-*" to record("cardboard-task", "کار کارتابل", "کدام مورد کارتابل؟", "cardboard_tasks"),
            "cheque-book-*" to record("cheque-book", "دسته چک", "کدام دسته چک؟", "cheque_books"),
            "digital-*" to record("digital-cheque", "چک دیجیتال", "کدام چک دیجیتال؟", "digital_cheques"),
            "vehicle-menu-*" to entry("vehicle-service", "خدمت خودرو", "کدام خدمت خودرو؟"),
            "vehicle-history-*" to record("vehicle", "سابقه وسیله", "کدام وسیله؟", "vehicles"),
            "vehicle-paid-*" to entry("vehicle-paid", "سابقه پرداخت خودرو", "کدام پرداخت؟"),
            "ticket-*" to entry("ticket", "تیکت", "کدام تیکت؟"),
            "svc-menu-*" to entry("inquiry-kind", "نوع استعلام", "کدام استعلام را می‌خواهید؟"),
            "card-action-*" to entry("card-action", "خدمت کارت", "کدام خدمت کارت؟"),
        )

    /** The one row tag of [action], or null when the move needs no choice. */
    fun rowTag(action: String) = action.split(" > ").firstOrNull { "*" in it }

    /** The choice [action] needs, or null. */
    fun of(action: String): Selection? = rowTag(action)?.let { byTag.getValue(it) }
}
