package org.ragbot.replica

import android.content.Context
import java.io.File

/** A mock file that cannot be used; [message] says why, and [file] names it on the error page. */
internal class MockDataError(message: String, val file: String = "") : Exception(message)

/** The demo user. The password and OTP are synthetic and exist only in customer.json. */
internal data class Customer(
    val name: String,
    val customerNumber: String,
    val lastLogin: String,
    val inviteCode: String,
    val nationalCode: String,
    val mobile: String,
    val username: String,
    val password: String,
    val otp: String,
    val perTransferLimit: Long,
    val dailyLimit: Long,
    val addresses: List<PreviewAddress>,
)

/** A saved person from contacts.json; each identifier becomes one saved destination. */
internal data class Contact(
    val id: String,
    val name: String,
    val card: String,
    val account: String,
    val iban: String,
    val wallet: String,
)

/** The one record the identifier conversion knows. */
internal data class ConversionSample(
    val owner: String,
    val account: String,
    val iban: String,
    val cards: List<String>,
    val fee: Long,
)

/** A Sayad cheque; [party] is the other side, and [received] means the customer holds it. */
internal data class SayadCheque(
    val sayadId: String,
    val amount: Long,
    val dueDate: String,
    val party: String,
    val received: Boolean,
    val status: String,
) {
    companion object {
        const val PENDING = "در انتظار تایید"
        const val ACCEPTED = "تایید شده"
        const val REJECTED = "رد شده"
        const val REGISTERED = "ثبت شده"
        const val TRANSFERRED = "منتقل شده"
    }
}

/** One traffic violation of a saved plate. */
internal data class Violation(
    val id: String,
    val title: String,
    val date: String,
    val amount: Long,
)

/** Credit inquiries of the demo user; each list is label and value rows. */
internal data class Inquiries(
    val fee: Long,
    val report: List<Pair<String, String>>,
    val reportDetail: List<Pair<String, String>>,
    val bounced: List<Pair<String, String>>,
    val bouncedDetail: List<Pair<String, String>>,
    val rate: List<Pair<String, String>>,
)

/** Forces [outcome] for [operation], or for every operation when it is "*". */
internal data class ForcedOutcome(val operation: String, val outcome: String)

internal data class StatementSeed(
    val key: String,
    val amount: Long,
    val balanceOffset: Long,
    val daysAgo: Int,
    val description: String,
)

internal data class WalletRowSeed(
    val amount: Long,
    val credit: Boolean,
    val balanceOffset: Long,
    val type: WalletTranType,
    val typeDescription: String,
    val daysAgo: Int,
    val time: String,
    val traceId: String,
    val transactionId: String,
    val participant: String,
    val participantName: String,
    val description: String,
)

/** Every mock file, parsed. Field names match the JSON keys. */
internal class MockFiles(read: (String) -> Json) {
    private val customerFile = read("customer")
    val customer =
        customerFile.let {
            Customer(
                it.str("name"),
                it.str("customerNumber"),
                it.str("lastLogin"),
                it.str("inviteCode"),
                it.str("nationalCode"),
                it.str("mobile"),
                it.str("username"),
                it.str("password"),
                it.str("otp"),
                it.long("perTransferLimit"),
                it.long("dailyLimit"),
                it.list("addresses") { a ->
                    PreviewAddress(a.str("key"), a.str("title"), a.str("address"), a.str("postalCode"))
                },
            )
        }
    private fun pairs(rows: Json) =
        rows.list { row -> row.list { it.string() }.let { (label, value) -> label to value } }

    val inquiries =
        customerFile["inquiries"].let {
            Inquiries(
                it.long("fee"),
                pairs(it["report"]),
                pairs(it["reportDetail"]),
                pairs(it["bounced"]),
                pairs(it["bouncedDetail"]),
                pairs(it["rate"]),
            )
        }
    private val accountFile = read("accounts")
    val accounts =
        accountFile.list("accounts") {
            PreviewAccount(
                it.str("key"),
                it.str("number"),
                it.str("owner"),
                it.str("type"),
                it.str("iban"),
                it.longOrNull("balance"),
                it.str("unit"),
                it.str("ownership"),
            )
        }
    private fun titled(key: String) =
        accountFile.list(key) { it.str("key") to it.str("title") }

    val openable = titled("openable")
    val branches = titled("branches")
    val conversion =
        accountFile["conversion"].let {
            ConversionSample(
                it.str("owner"),
                it.str("account"),
                it.str("iban"),
                it.strings("cards"),
                it.long("fee"),
            )
        }
    private val cardFile = read("cards")
    private fun card(it: Json) =
        PreviewCard(
            it.str("key"),
            CardKind.valueOf(it.str("kind")),
            it.str("number"),
            it.str("owner"),
            it.longOrNull("cardBalance"),
            it.longOrNull("accountBalance"),
            it.str("primaryAccount"),
            it.strings("otherAccounts"),
            it.str("iban"),
            it.str("status"),
            it.bool("isDefault"),
            it.bool("altLogo"),
            it.bool("locked"),
            it.str("unit"),
        )
    val cards = cardFile.list("cards", ::card)
    val candidates = cardFile.list("candidates", ::card)
    val credit =
        cardFile["credit"].map {
            CreditSummary(
                it.str("state"),
                it.long("creditLimit"),
                it.long("remainingCredit"),
                it.long("usedAmount"),
                it.long("overdueDebt"),
                it.long("totalPayable"),
                it.str("unit"),
            )
        }
    val fileLoans =
        cardFile["fileLoans"].map { loans ->
            loans.list {
                FileLoan(
                    it.str("fileNumber"),
                    it.str("dueDate"),
                    it.int("instalments"),
                    it.long("remaining"),
                    it.str("unit"),
                )
            }
        }
    private val contactFile = read("contacts")
    val people =
        contactFile.list("people") {
            Contact(
                it.str("id"),
                it.str("name"),
                it.str("card"),
                it.str("account"),
                it.str("iban"),
                it.str("wallet"),
            )
        }
    val destinations =
        people.flatMap {
            listOf(
                SavedDestination("${it.id}-account", "ACCOUNT", it.name, it.account),
                SavedDestination("${it.id}-iban", "IBAN", it.name, it.iban),
                SavedDestination("${it.id}-card", "CARD", it.name, it.card),
                SavedDestination("${it.id}-wallet", "WALLET", it.name, it.wallet),
            )
        } +
            contactFile.list("destinations") {
                SavedDestination(
                    it.str("id"),
                    it.str("type"),
                    it.str("name"),
                    it.str("destination"),
                    it.str("description"),
                )
            }
    val bills =
        read("bills").list("bills") {
            BillInquiry(
                it.str("billId"),
                it.str("payId"),
                it.str("company"),
                it.long("amount"),
                it.bool("paid"),
            )
        }
    val operators =
        read("operators").list("operators") {
            OperatorConfig(
                it.str("operator"),
                it.str("title"),
                it.long("min"),
                it.long("max"),
                it.long("multiplier"),
                it["defaults"].list { n -> n.long() },
                it.strings("prefixes"),
            )
        }
    val charities =
        read("charities").list("charities") {
            Charity(it.long("id"), it.str("name"), it.str("desc"), it.str("accountNo"))
        }
    private val loanFile = read("loans")
    val loans =
        loanFile.list("loans") {
            LoanRecord(
                it.str("id"),
                it.str("title"),
                it.str("fileNumber"),
                it.long("principalIrr"),
                it.long("paidIrr"),
                it.long("totalDebtIrr"),
                it.long("discountIrr"),
                it.str("guaranteeAccount"),
                it.list("installments") { n ->
                    LoanInstallment(
                        n.int("number"),
                        n.str("dueDate"),
                        LoanInstallmentStatus.valueOf(n.str("status")),
                        n.long("amountIrr"),
                        n.long("penaltyIrr"),
                        n.long("paidIrr"),
                        n.long("remainingIrr"),
                    )
                },
                it.strOrNull("corporateTitle"),
                it.str("loanType"),
            )
        }
    val loanProducts =
        loanFile.list("products") {
            LoanProduct(
                it.str("key"),
                it.str("title"),
                it.long("maxAmount"),
                it.int("maxMonths"),
                it.long("rate"),
            )
        }
    val loanGroups =
        loanFile.list("groups") {
            LoanGroup(
                it.str("key"),
                it.str("title"),
                it.strings("details"),
                it.bool("active"),
                it.bool("accountOpenElapsed"),
                it.bool("needsCredit"),
            )
        }
    val loanOptions =
        loanFile.list("options") {
            LoanGroupOption(
                it.str("key"),
                it.str("title"),
                it.long("amountRial"),
                it.bool("active"),
                it.int("availableMonths"),
                it.int("maximumMonths"),
                it.strOrNull("subtitle"),
                it.strings("details"),
            )
        }
    private val chequeFile = read("cheques")
    val chequeFee = chequeFile.long("fee")
    val digitalChequeFee = chequeFile.long("digitalFee")
    val books =
        chequeFile.list("books") {
            ChequeBook(
                it.str("reference"),
                it.str("accountKey"),
                it.int("pageCount"),
                it.str("startNo"),
                it.str("endNo"),
                it.str("issueDate"),
                it.str("docType"),
                it.list("summary") { pair -> pair.list { v -> v }.let { (a, b) -> a.string() to b.int() } },
            )
        }
    val papers =
        chequeFile["papers"].map { book ->
            book.list {
                ChequePaper(
                    it.str("no"),
                    it.longOrNull("amount"),
                    it.str("sayadId"),
                    it.str("branch"),
                    it.str("date"),
                    it.str("statusDesc"),
                    it.str("lastStatusDate"),
                )
            }
        }
    val chequeReport =
        chequeFile.list("report") {
            TransferredCheque(
                it.str("no"),
                it.long("amount"),
                it.str("date"),
                it.str("type"),
                it.str("subtype"),
            )
        }
    val issuance =
        chequeFile.list("issuance") {
            IssuanceRecord(
                it.str("trace"),
                it.str("accountKey"),
                ChequeMedia.valueOf(it.str("media")),
                it.str("pageCount"),
                it.str("date"),
                it.str("status"),
                it.strOrNull("rejectReason"),
            )
        }
    val tasks =
        chequeFile.list("tasks") {
            WorkflowTask(
                it.str("id"),
                it.str("tab"),
                it.str("processKey"),
                it.str("processName"),
                it.str("issuer"),
                it.str("date"),
                it.str("statusDesc"),
                it.long("amount"),
            )
        }
    val digitalCheques =
        chequeFile.list("digital") {
            DigitalCheque(
                it.str("sayadId"),
                it.long("amount"),
                it.str("dueDate"),
                it.str("bank"),
                it.str("branch"),
                it.str("status"),
                it.str("guarantee"),
                it.str("block"),
                it.str("lock"),
                it.str("holderStatus"),
                it.str("reason"),
                it.str("description"),
                it.strings("holders"),
                it.str("digitalIban"),
            )
        }
    val sayad =
        chequeFile.list("sayad") {
            SayadCheque(
                it.str("sayadId"),
                it.long("amount"),
                it.str("dueDate"),
                it.str("party"),
                it.bool("received"),
                it.str("status"),
            )
        }
    private val vehicleFile = read("vehicles")
    val vehicles =
        vehicleFile.list("records") {
            VehicleRecord(
                it.str("id"),
                it.str("kind"),
                it.bool("mine"),
                it.str("ownerCode"),
                it.str("ownerMobile"),
                it.strings("plate"),
                it.bool("automobile"),
                it.str("licence"),
            )
        }
    val inquiryFee = vehicleFile.long("inquiryFee")
    val commission = vehicleFile.long("commission")
    val duesPlate = vehicleFile.str("duesPlate")
    val tolls =
        vehicleFile["tolls"].map { items ->
            items.list {
                TollItem(
                    it.str("id"),
                    it.str("title"),
                    it.long("price"),
                    it.long("penalty"),
                    it.long("frazzle"),
                )
            }
        }
    private fun rows(key: String) = vehicleFile[key].map(::pairs)

    val violations =
        vehicleFile["violations"].map { items ->
            items.list {
                Violation(it.str("id"), it.str("title"), it.str("date"), it.long("amount"))
            }
        }
    val transferTax = vehicleFile["transferTax"].map { it.long() }
    val documents = rows("documents")
    val activePlates = rows("activePlates")
    val licences = rows("licences")
    val negativePoints = rows("negativePoints")
    val quotaAllowed = vehicleFile.int("quotaAllowed")
    val quotaRemaining = vehicleFile.int("quotaRemaining")
    val consumed =
        vehicleFile.list("consumed") { row ->
            row.list { it.int() }.let { (a, b, c) -> Triple(a, b, c) }
        }
    private val walletFile = read("wallets")
    val wallets =
        walletFile.list("wallets") {
            PreviewWallet(
                it.str("number"),
                it.str("title"),
                it.str("owner"),
                it.str("holderType"),
                it.str("status"),
                it.str("statusDescription"),
                it.bool("individual"),
                it.long("balance"),
            )
        }
    val walletDestinations = walletFile["destinations"].map { it.string() }
    val walletStatement =
        walletFile.list("statement") {
            WalletRowSeed(
                it.long("amount"),
                it.bool("credit"),
                it.long("balanceOffset"),
                WalletTranType.valueOf(it.str("type")),
                it.str("typeDescription"),
                it.int("daysAgo"),
                it.str("time"),
                it.str("traceId"),
                it.str("transactionId"),
                it.str("participant"),
                it.str("participantName"),
                it.str("description"),
            )
        }
    private val transactionFile = read("transactions")
    val transactions =
        transactionFile.list("recent") {
            PreviewTransaction(
                it.str("key"),
                it.str("title"),
                it.long("amount"),
                it.str("unit"),
                it.str("time"),
                it.str("source"),
                it.str("destination"),
            )
        }
    val statement =
        transactionFile.list("statement") {
            StatementSeed(
                it.str("key"),
                it.long("amount"),
                it.long("balanceOffset"),
                it.int("daysAgo"),
                it.str("description"),
            )
        }
    val groupFiles =
        transactionFile.list("groupFiles") { file ->
            GroupFile(
                file.str("key"),
                file.str("name"),
                file.list("sheet") { row ->
                    row.list { if (it.isNull) null else it.string() }
                },
                file["rejected"].list { it.long() },
            )
        }
    val groupHistory =
        transactionFile.list("groupHistory") {
            GroupHistory(
                it.str("traceId"),
                it.str("date"),
                it.str("status"),
                it.str("source"),
                it.str("sourceName"),
                it.str("withdrawId"),
                it.str("sourceDescription"),
                it.str("note"),
                it.list("rows") { row ->
                    GroupRow(
                        row.long("id"),
                        row.str("iban"),
                        row.str("amount"),
                        row.str("reason"),
                        row.str("paymentId"),
                        row.str("description"),
                    )
                },
            )
        }
    val standingOrders =
        transactionFile.list("standingOrders") {
            ContinuousRecord(
                it.str("traceId"),
                it.str("name"),
                it.str("type"),
                it.str("source"),
                it.str("destination"),
                it.long("amount"),
                it.str("created"),
                it.str("start"),
                it.str("end"),
                it.str("frequency"),
                it.bool("cancelled"),
                it.list("executions") { e -> ScheduleExecution(e.str("date"), e.str("status")) },
            )
        }
    val repetitive =
        transactionFile.list("repetitive") {
            RepetitiveRecord(
                it.str("id"),
                it.str("type"),
                it.str("title"),
                it.str("source"),
                it.str("destination"),
                it.str("destinationName"),
                it.str("bank"),
                it.long("amount"),
                it.str("created"),
            )
        }
    val categories = transactionFile["categories"].map { it.string() }
    val spending =
        transactionFile.list("spending") {
            Spending(it.str("category"), it.str("title"), it.long("amount"))
        }
    val budgets = transactionFile["budgets"].map { it.long() }
    private val insuranceFile = read("insurance")
    val insuredName = insuranceFile.str("insuredName")
    val insuredNationalCode = insuranceFile.str("insuredNationalCode")
    val policies =
        insuranceFile.list("policies") {
            InsurancePolicy(
                it.str("id"),
                it.str("number"),
                it.str("begin"),
                it.str("end"),
                it.str("state"),
                it.int("addendum"),
                it.list("installments") { n ->
                    Installment(
                        n.int("number"),
                        n.int("year"),
                        n.str("dueDate"),
                        n.long("amount"),
                        n.long("remained"),
                        n.str("paymentId"),
                        n.str("status"),
                    )
                },
            )
        }
    private val scenarioFile = read("scenarios")
    val forced =
        scenarioFile.list("forced") { ForcedOutcome(it.str("operation"), it.str("outcome")) }
    val messages = scenarioFile["messages"].map { it.string() }
}

/** The loaded mock files. An override folder on the device replaces a bundled file by name. */
internal object MockData {
    const val FOLDER = "mock"
    private var source: (String) -> String = { name ->
        // JVM tests read the same files from their classpath.
        checkNotNull(javaClass.classLoader?.getResource("$name.json")) { name }.readText()
    }
    private var files: MockFiles? = null

    val loaded
        get() = files != null

    val current: MockFiles
        get() = files ?: parse().also { files = it }

    /** Loads the files for this launch; a bad file becomes the returned error, not a crash. */
    fun load(context: Context): MockDataError? {
        val override = context.getExternalFilesDir(FOLDER)
        source = { name ->
            val file = File(override, "$name.json")
            if (file.exists()) file.readText()
            else context.assets.open("$FOLDER/$name.json").bufferedReader().use { it.readText() }
        }
        return try {
            files = parse()
            null
        } catch (error: MockDataError) {
            error
        }
    }

    private fun parse(): MockFiles {
        var name = ""
        try {
            return MockFiles { file ->
                name = "$file.json"
                Json.parse(name, source(file))
            }
        } catch (error: MockDataError) {
            throw MockDataError(error.message.orEmpty(), name)
        } catch (error: Exception) {
            // A wrong enum value, or a file that cannot be read.
            throw MockDataError(error.message.orEmpty(), name)
        }
    }
}
