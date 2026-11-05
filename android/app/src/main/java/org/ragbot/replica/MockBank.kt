package org.ragbot.replica

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What a finished service shows. [amount] is exact, in [unit]. */
internal data class Receipt(
    val trace: String,
    val title: String,
    val amount: Long,
    val source: String,
    val destination: String,
    val time: String,
    val rows: List<Pair<String, String>> = emptyList(),
    val unit: String = "IRR",
)

internal sealed interface BankResult {
    data class Done(val receipt: Receipt) : BankResult

    data class Failed(val message: String) : BankResult
}

/**
 * The one local bank every service screen calls. It checks a request against the mock files and
 * returns a receipt or a Persian failure. Changes live in memory and reset when the app restarts.
 */
internal object MockBank {
    private data class Session(
        val balances: Map<String, Long>,
        val wallets: Map<String, Long>,
        val added: List<PreviewTransaction> = emptyList(),
        val settled: Set<String> = emptySet(),
        val sayad: Map<String, SayadCheque> = emptyMap(),
        val spent: Long = 0,
        val trace: Int = 0,
    )

    private fun fresh() =
        Session(
            MockData.current.accounts.mapNotNull { a -> a.balance?.let { a.key to it } }.toMap(),
            MockData.current.wallets.associate { it.number to it.balance },
        )

    private var session by mutableStateOf<Session?>(null)
    private val current
        get() = session ?: fresh().also { session = it }

    /** JVM tests have no Persian calendar, so they replace this. */
    var clock: () -> String = {
        "${todayPersian()} - ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}"
    }

    var receipt by mutableStateOf<Receipt?>(null)
        private set

    fun reset() {
        session = null
        receipt = null
    }

    fun balance(accountKey: String) = current.balances[accountKey]

    fun walletBalance(number: String) = current.wallets.getValue(number)

    val transactions
        get() = current.added + MockData.current.transactions

    /** What this session spent from the account [number], newest first. */
    fun spentFrom(number: String) = current.added.filter { number in it.source && it.amount > 0 }

    /** True once a bill, installment or toll item has been paid in this session. */
    fun settled(key: String) = key in current.settled

    /** A card with its account's live balance. */
    fun liveCard(card: PreviewCard): PreviewCard {
        val account = MockData.current.accounts.find { it.number == card.primaryAccount }
        val live = account?.let { balance(it.key) } ?: return card
        return card.copy(
            cardBalance = if (card.cardBalance == card.accountBalance) live else card.cardBalance,
            accountBalance = live,
        )
    }

    private fun failure(outcome: String) =
        MockData.current.messages.let {
            BankResult.Failed(it[outcome] ?: it.getValue("failed"))
        }

    private fun forced(operation: String) =
        MockData.current.forced
            .firstOrNull { it.operation == operation || it.operation == "*" }
            ?.outcome

    /** The account a source stands for: an account key or number, or a card's own account. */
    private fun account(source: String): PreviewAccount? {
        val digits = latinDigits(source)
        val card =
            MockData.current.cards.find { it.key == source || latinDigits(it.number) == digits }
        val number = latinDigits(card?.primaryAccount ?: source)
        return MockData.current.accounts.find {
            it.key == source || latinDigits(it.number) == number
        }
    }

    fun validCard(number: String): Boolean {
        val digits = latinDigits(number)
        if (digits.length != 16 || digits.any { it !in '0'..'9' }) return false
        val sum =
            digits.reversed().mapIndexed { i, ch ->
                val value = ch.digitToInt() * (i % 2 + 1)
                if (value > 9) value - 9 else value
            }
        return sum.sum() % 10 == 0
    }

    private fun known(type: String, value: String) =
        MockData.current.destinations.any {
            it.type == type && latinDigits(it.destination) == value
        }

    fun knownCard(number: String) =
        latinDigits(number).let { n ->
            MockData.current.cards.any { latinDigits(it.number) == n } || known("CARD", n)
        }

    fun knownAccount(number: String) =
        latinDigits(number).let { n ->
            MockData.current.accounts.any { latinDigits(it.number) == n } || known("ACCOUNT", n)
        }

    fun knownIban(iban: String) =
        ("IR" + latinDigits(iban).trim().uppercase().removePrefix("IR")).let { n ->
            MockData.current.accounts.any { it.iban == n } || known("IBAN", n)
        }

    /** The name saved for a destination, for review pages. */
    fun recipient(destination: String) =
        latinDigits(destination).let { n ->
            MockData.current.destinations.find { latinDigits(it.destination) == n }?.name
        }

    private fun record(
        state: Session,
        title: String,
        amount: Long,
        source: String,
        destination: String,
        rows: List<Pair<String, String>>,
    ): BankResult {
        val time = clock()
        val trace = "MOCK-%06d".format(state.trace + 1)
        val done = Receipt(trace, title, amount, source, destination, time, rows)
        val entry = PreviewTransaction(trace, title, amount, "IRR", time, source, destination)
        session = state.copy(added = listOf(entry) + state.added, trace = state.trace + 1)
        receipt = done
        return BankResult.Done(done)
    }

    fun invalidSecret() = failure("invalid-secret")

    fun incomplete() = failure("incomplete")

    /** The Sayad cheques known now: the mock file with this session's changes on top. */
    val sayadCheques
        get() = (MockData.current.sayad.associateBy { it.sayadId } + current.sayad).values.toList()

    val sayadPending
        get() = sayadCheques.filter { it.received && it.status == SayadCheque.PENDING }

    val sayadHeld
        get() = sayadCheques.filter { it.received && it.status == SayadCheque.ACCEPTED }

    private fun sayadChange(cheque: SayadCheque, title: String, party: String): BankResult {
        val result =
            request("sayad", title, party, listOf("شناسه صیاد" to cheque.sayadId))
        if (result is BankResult.Done)
            session = current.copy(sayad = current.sayad + (cheque.sayadId to cheque))
        return result
    }

    fun sayadRegister(id: String, amount: Long, due: String, receiver: String): BankResult {
        val sayad = latinDigits(id)
        if (sayadCheques.any { it.sayadId == sayad }) return failure("sayad-duplicate")
        val cheque = SayadCheque(sayad, amount, due, receiver, false, SayadCheque.REGISTERED)
        return sayadChange(cheque, "ثبت چک صیادی", receiver)
    }

    fun sayadAnswer(id: String, accept: Boolean): BankResult {
        val cheque = sayadPending.find { it.sayadId == id } ?: return failure("sayad-not-found")
        val status = if (accept) SayadCheque.ACCEPTED else SayadCheque.REJECTED
        return sayadChange(
            cheque.copy(status = status),
            if (accept) "تایید چک دریافتی" else "رد چک دریافتی",
            cheque.party,
        )
    }

    fun sayadTransfer(id: String, receiver: String): BankResult {
        val cheque = sayadHeld.find { it.sayadId == id } ?: return failure("sayad-not-found")
        return sayadChange(
            cheque.copy(status = SayadCheque.TRANSFERRED),
            "انتقال چک",
            receiver,
        )
    }

    fun sayadInquiry(id: String): BankResult {
        val sayad = latinDigits(id)
        val cheque =
            sayadCheques.find { it.sayadId == sayad } ?: return failure("sayad-not-found")
        return request(
            "sayad-inquiry",
            "استعلام چک",
            cheque.party,
            listOf(
                "شناسه صیاد" to cheque.sayadId,
                "مبلغ (ریال)" to persianNumber(cheque.amount),
                "تاریخ سررسید" to cheque.dueDate,
                "وضعیت" to cheque.status,
            ),
        )
    }

    /** A balance inquiry on one of the customer's cards; it changes nothing. */
    fun cardBalance(number: String): BankResult {
        val card =
            CardFixture.cards.find { latinDigits(it.number) == latinDigits(number) }
                ?: return failure("recipient-not-found")
        return request(
            "card-balance",
            "موجودی کارت",
            card.number,
            listOf("موجودی (ریال)" to persianNumber(card.cardBalance ?: 0L)),
        )
    }

    /** A paid service: moves [amount] out of [source], or fails with the reason. */
    fun pay(
        operation: String,
        title: String,
        source: String,
        amount: Long,
        destination: String,
        otp: String? = null,
        rows: List<Pair<String, String>> = emptyList(),
        settles: List<String> = emptyList(),
    ): BankResult {
        val forced = forced(operation)
        if (forced != null && forced != "success") return failure(forced)
        val state = current
        val from = account(source) ?: return failure("source-not-found")
        val balance = state.balances[from.key] ?: return failure("source-not-found")
        if (forced == null) {
            val customer = MockData.current.customer
            if (otp != null && otp != customer.otp) return failure("wrong-otp")
            if (amount > customer.perTransferLimit) return failure("over-limit")
            if (state.spent + amount > customer.dailyLimit) return failure("over-daily-limit")
            if (amount > balance) return failure("insufficient-balance")
        }
        val next =
            state.copy(
                balances = state.balances + (from.key to (balance - amount).coerceAtLeast(0)),
                settled = state.settled + settles,
                spent = state.spent + amount,
            )
        return record(next, title, amount, "${from.type} ${from.number}", destination, rows)
    }

    /** A service that moves no money but still has an outcome and a history entry. */
    fun request(
        operation: String,
        title: String,
        destination: String,
        rows: List<Pair<String, String>> = emptyList(),
    ): BankResult {
        val forced = forced(operation)
        if (forced != null && forced != "success") return failure(forced)
        return record(current, title, 0, MockData.current.customer.name, destination, rows)
    }

    /** Money coming in, such as a confirmed top-up. */
    fun credit(operation: String, title: String, target: String, amount: Long): BankResult {
        val forced = forced(operation)
        if (forced != null && forced != "success") return failure(forced)
        val state = current
        val account = account(target)
        val next =
            if (account != null) {
                val balance = state.balances[account.key] ?: return failure("source-not-found")
                state.copy(balances = state.balances + (account.key to balance + amount))
            } else {
                val balance = state.wallets[target] ?: return failure("source-not-found")
                state.copy(wallets = state.wallets + (target to balance + amount))
            }
        return record(next, title, amount, "درگاه پرداخت", target, emptyList())
    }

    /** A wallet operation; a wallet is its own source, and [toWallet] may receive the money. */
    fun walletPay(
        operation: String,
        title: String,
        wallet: String,
        amount: Long,
        destination: String,
        toWallet: String? = null,
        toAccount: String? = null,
        settles: List<String> = emptyList(),
    ): BankResult {
        val forced = forced(operation)
        if (forced != null && forced != "success") return failure(forced)
        val state = current
        val balance = state.wallets[wallet] ?: return failure("source-not-found")
        if (forced == null && amount > balance) return failure("insufficient-balance")
        var wallets = state.wallets + (wallet to (balance - amount).coerceAtLeast(0))
        if (toWallet in wallets) wallets = wallets + (toWallet!! to wallets.getValue(toWallet) + amount)
        val balances =
            toAccount?.let { key ->
                state.balances + (key to (state.balances[key] ?: 0L) + amount)
            } ?: state.balances
        val next =
            state.copy(wallets = wallets, balances = balances, settled = state.settled + settles)
        return record(next, title, amount, wallet, destination, emptyList())
    }

    /** Account to wallet: the money leaves the account and arrives in the wallet. */
    fun walletCharge(accountKey: String, wallet: String, amount: Long): BankResult {
        val result =
            pay("wallet-charge", "واریز به کیف", accountKey, amount, wallet)
        if (result is BankResult.Done) {
            val state = current
            val balance = state.wallets.getValue(wallet)
            session = state.copy(wallets = state.wallets + (wallet to balance + amount))
        }
        return result
    }

    fun cardTransfer(draft: CardTransferDraft, cvv2: String, expiry: String, password: String) =
        when {
            cvv2.length !in 3..4 || expiry.length != 4 -> failure("invalid-secret")
            !validCard(draft.destination) -> failure("invalid-card")
            !knownCard(draft.destination) -> failure("recipient-not-found")
            else ->
                pay(
                    "card-transfer",
                    "کارت به کارت",
                    draft.source,
                    latinDigits(draft.amount).toLong(),
                    recipient(draft.destination) ?: draft.destination,
                    password,
                    listOf("کارت مقصد" to latinDigits(draft.destination)),
                )
        }

    fun accountTransfer(draft: AccountTransferDraft): BankResult {
        val internal = draft.mode == AccountTransferDestinations.INTERNAL
        val known =
            if (internal) knownAccount(draft.destination) else knownIban(draft.destination)
        if (!known) return failure("recipient-not-found")
        return pay(
            "account-transfer",
            if (internal) "انتقال وجه داخلی" else "انتقال وجه بین بانکی",
            draft.sourceId,
            latinDigits(draft.amount).toLong(),
            recipient(draft.destination) ?: draft.destination,
            rows = listOf("مقصد" to draft.destination, "بابت" to draft.reason),
        )
    }

    fun bill(source: String, billId: String, payId: String, otp: String? = null): BankResult {
        val id = latinDigits(billId)
        val bill =
            MockData.current.bills.find { it.billId == id && it.payId == latinDigits(payId) }
                ?: return failure("bill-not-found")
        if (bill.paid || settled("bill:$id")) return failure("bill-paid")
        return pay(
            "bill",
            "پرداخت قبض",
            source,
            bill.amount,
            bill.company,
            otp,
            listOf("شناسه قبض" to id, "شناسه پرداخت" to bill.payId),
            listOf("bill:$id"),
        )
    }

    fun topUp(draft: TopUpDraft, otp: String? = null) =
        pay(
            "charge",
            "خرید شارژ",
            draft.source,
            latinDigits(draft.amount).toLong(),
            latinDigits(draft.phone),
            otp,
            listOf("اپراتور" to (TopUpRules.config(draft.operator)?.title ?: "")),
        )
}
