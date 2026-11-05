package org.ragbot.replica

private typealias Rows = (String) -> List<Pair<String, String>>

/** Every service built on [ServiceFlow]. Their fields and steps are Designed for mock. */
internal object ServiceCatalog {
    private fun rows(vararg items: Pair<String, String?>) =
        items.mapNotNull { (label, value) -> value?.let { label to it } }

    private fun amount(values: Map<String, String>, key: String = "amount") =
        persianNumber(latinDigits(values[key].orEmpty()).toLongOrNull() ?: 0L)

    private val sayadId = FlowField("sayad", "شناسه صیاد", check = digits(16))
    private val receiver =
        FlowStep(
            "S-171",
            "گیرنده چک",
            listOf(
                FlowField("receiver", "نام گیرنده"),
                FlowField("code", "کد ملی گیرنده", check = digits(10)),
            ),
        )

    private fun chequeChoice(label: String, cheques: () -> List<SayadCheque>) =
        choice("sayad", label) {
            cheques().map {
                it.sayadId to "${it.party} · ${persianNumber(it.amount)} ریال · ${it.dueDate}"
            }
        }

    private fun chequeRows(id: String?) =
        MockBank.sayadCheques
            .find { it.sayadId == id }
            ?.let {
                rows(
                    "شناسه صیاد" to it.sayadId,
                    "مبلغ (ریال)" to persianNumber(it.amount),
                    "تاریخ سررسید" to it.dueDate,
                    "طرف چک" to it.party,
                    "وضعیت" to it.status,
                )
            }
            .orEmpty()

    private val sayad =
        listOf(
            ServiceFlow(
                "sayad-register",
                listOf(
                    FlowStep(
                        "S-165",
                        "صدور چک",
                        listOf(
                            sayadId,
                            money("amount", "مبلغ (ریال)"),
                            FlowField("due", "تاریخ سررسید"),
                        ),
                    ),
                    receiver,
                    FlowStep(
                        "S-170",
                        "پیش‌نمایش چک",
                        show = {
                            rows(
                                "شناسه صیاد" to it["sayad"],
                                "مبلغ (ریال)" to amount(it),
                                "تاریخ سررسید" to it["due"],
                                "گیرنده" to it["receiver"],
                            )
                        },
                    ),
                ),
                "S-168",
            ) {
                MockBank.sayadRegister(
                    it.getValue("sayad"),
                    latinDigits(it.getValue("amount")).toLong(),
                    it.getValue("due"),
                    it.getValue("receiver"),
                )
            },
            ServiceFlow(
                "sayad-answer",
                listOf(
                    FlowStep(
                        "S-167",
                        "چک‌های دریافتی",
                        listOf(chequeChoice("چک دریافتی") { MockBank.sayadPending }),
                    ),
                    FlowStep(
                        "S-163",
                        "اطلاعات چک",
                        listOf(
                            choice("answer", "پاسخ شما") {
                                listOf("accept" to "تایید چک", "reject" to "رد چک")
                            }
                        ),
                        { chequeRows(it["sayad"]) },
                    ),
                    FlowStep(
                        "S-170",
                        "پیش‌نمایش چک",
                        show = {
                            chequeRows(it["sayad"]) +
                                ("پاسخ شما" to
                                    if (it["answer"] == "accept") "تایید چک" else "رد چک")
                        },
                    ),
                ),
                "S-168",
            ) {
                MockBank.sayadAnswer(it.getValue("sayad"), it["answer"] == "accept")
            },
            ServiceFlow(
                "sayad-transfer",
                listOf(
                    FlowStep(
                        "S-172",
                        "انتقال چک",
                        listOf(chequeChoice("چک قابل انتقال") { MockBank.sayadHeld }),
                    ),
                    receiver,
                    FlowStep(
                        "S-170",
                        "پیش‌نمایش چک",
                        show = { chequeRows(it["sayad"]) + rows("گیرنده جدید" to it["receiver"]) },
                    ),
                ),
                "S-168",
            ) {
                MockBank.sayadTransfer(it.getValue("sayad"), it.getValue("receiver"))
            },
            ServiceFlow(
                "sayad-inquiry",
                listOf(
                    FlowStep(
                        "S-164",
                        "نوع استعلام",
                        listOf(
                            choice("kind", "نوع استعلام") {
                                listOf("holder" to "استعلام گیرنده چک", "issuer" to "استعلام صادرکننده")
                            }
                        ),
                    ),
                    FlowStep("S-162", "شناسه صیاد", listOf(sayadId)),
                ),
                "S-166",
                otp = false,
            ) {
                MockBank.sayadInquiry(it.getValue("sayad"))
            },
            ServiceFlow(
                "sayad-history",
                listOf(
                    FlowStep(
                        "S-169",
                        "سوابق چک صیادی",
                        show = {
                            MockBank.sayadCheques.map { cheque ->
                                "${cheque.sayadId} · ${cheque.status}" to
                                    "${cheque.party} · ${persianNumber(cheque.amount)} ریال"
                            }
                        },
                    )
                ),
            ),
        )

    private fun vehicleChoice(kind: String) =
        choice("record", if (kind == "plate") "پلاک" else "انتخاب") {
            VehicleFixture.records
                .filter { it.kind == kind }
                .map {
                    it.id to
                        when (kind) {
                            "plate" -> it.plate.joinToString(" ")
                            "licence" -> it.licence
                            else -> "${it.ownerCode} ${VehicleRules.ownership(it)}"
                        }
                }
        }

    private fun violations(values: Map<String, String>) =
        MockData.current.violations[values["record"]].orEmpty().filter {
            !MockBank.settled("violation:${it.id}")
        }

    private fun tax(values: Map<String, String>) =
        values["record"].orEmpty().let {
            if (MockBank.settled("tax:$it")) 0L else MockData.current.transferTax[it] ?: 0L
        }

    private fun total(label: String, amount: Long) = listOf(label to "${persianNumber(amount)} ریال")

    private fun inquiry(key: String, page: String, title: String, kind: String, data: Rows) =
        ServiceFlow(
            key,
            listOf(
                FlowStep(page, title, listOf(vehicleChoice(kind)), { data(it["record"].orEmpty()) })
            ),
        )

    private val vehicle =
        listOf(
            ServiceFlow(
                "violations",
                listOf(
                    FlowStep("S-244", "خلافی خودرو", listOf(vehicleChoice("plate"))),
                    FlowStep(
                        "S-245",
                        "نتیجه خلافی",
                        show = { values ->
                            violations(values).map {
                                "${it.title} · ${it.date}" to "${persianNumber(it.amount)} ریال"
                            } + total("مجموع", violations(values).sumOf { it.amount })
                        },
                    ),
                    FlowStep(
                        "S-243",
                        "پرداخت خلافی",
                        listOf(accountChoice()),
                        { total("مبلغ قابل پرداخت", violations(it).sumOf { v -> v.amount }) },
                    ),
                ),
                TollDestinations.RECEIPT,
            ) { values ->
                val items = violations(values)
                if (items.isEmpty()) MockBank.nothingToPay()
                else
                    MockBank.pay(
                        "violation",
                        "پرداخت خلافی خودرو",
                        values.getValue("source"),
                        items.sumOf { it.amount },
                        values.getValue("record"),
                        settles = items.map { "violation:${it.id}" },
                    )
            },
            ServiceFlow(
                "transfer-tax",
                listOf(
                    FlowStep("S-278", "مالیات نقل و انتقال", listOf(vehicleChoice("plate"))),
                    FlowStep(
                        "S-279",
                        "نتیجه مالیات نقل و انتقال",
                        show = { total("مالیات", tax(it)) },
                    ),
                    FlowStep(
                        "S-277",
                        "پرداخت مالیات",
                        listOf(accountChoice()),
                        { total("مبلغ قابل پرداخت", tax(it)) },
                    ),
                ),
                TollDestinations.RECEIPT,
            ) { values ->
                if (tax(values) == 0L) MockBank.nothingToPay()
                else
                    MockBank.pay(
                        "transfer-tax",
                        "پرداخت مالیات نقل و انتقال",
                        values.getValue("source"),
                        tax(values),
                        values.getValue("record"),
                        settles = listOf("tax:${values.getValue("record")}"),
                    )
            },
            ServiceFlow(
                "documents",
                listOf(
                    FlowStep("S-280", "مدارک خودرو", listOf(vehicleChoice("plate"))),
                    FlowStep(
                        "S-281",
                        "نتیجه مدارک خودرو",
                        show = { MockData.current.documents[it["record"]].orEmpty() },
                    ),
                ),
            ),
            ServiceFlow(
                "active-plates",
                listOf(
                    FlowStep("S-238", "پلاک‌های فعال", listOf(vehicleChoice("contact"))),
                    FlowStep(
                        "S-239",
                        "نتیجه پلاک‌های فعال",
                        show = { MockData.current.activePlates[it["record"]].orEmpty() },
                    ),
                ),
            ),
            inquiry("licence", "S-247", "وضعیت گواهینامه", "contact") {
                MockData.current.licences[it].orEmpty()
            },
            inquiry("negative-points", "S-255", "نمره منفی گواهینامه", "licence") {
                MockData.current.negativePoints[it].orEmpty()
            },
        )

    /** The form a vehicle menu entry opens, once a row is picked. */
    val vehicleMenu =
        mapOf(
            "AUTHENTICATED_HOME_RAHVAR_TOLL" to "violations",
            "AUTHENTICATED_HOME_TRANSFER_TOLL" to "transfer-tax",
            "AUTHENTICATED_HOME_VEHICLE_DOC_INQUIRY" to "documents",
            "AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY" to "active-plates",
            "AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY" to "licence",
            "AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY" to "negative-points",
        )

    private val fee
        get() = MockData.current.inquiries.fee

    private fun feePage() =
        FlowStep("S-442", "کارمزد استعلام", listOf(accountChoice()), { total("کارمزد", fee) })

    private fun paidInquiry(
        key: String,
        title: String,
        result: String,
        detail: String?,
        rows: () -> List<Pair<String, String>>,
    ) =
        ServiceFlow(key, listOf(feePage()), result, detail = detail) {
            MockBank.pay(
                key,
                title,
                it.getValue("source"),
                fee,
                MockData.current.customer.name,
                rows = rows(),
            )
        }

    private val inquiries =
        listOf(
            paidInquiry("credit-report", "گزارش اعتباری", "S-445", "S-441") {
                MockData.current.inquiries.report
            },
            ServiceFlow(
                "credit-report-detail",
                listOf(
                    FlowStep(
                        "S-441",
                        "جزئیات گزارش اعتباری",
                        show = { MockData.current.inquiries.reportDetail },
                    )
                ),
            ),
            paidInquiry("bounced-cheques", "استعلام چک برگشتی", "S-444", "S-440") {
                MockData.current.inquiries.bounced
            },
            ServiceFlow(
                "bounced-cheques-detail",
                listOf(
                    FlowStep(
                        "S-440",
                        "جزئیات چک برگشتی",
                        show = { MockData.current.inquiries.bouncedDetail },
                    )
                ),
            ),
            paidInquiry("credit-rate", "رتبه اعتباری", "S-453", null) {
                MockData.current.inquiries.rate
            },
        )

    /** Menu pages by screen: a title and the services they offer. */
    val menus =
        mapOf(
            "S-443" to
                ("استعلامات" to
                    listOf(
                        Triple("credit-report", "گزارش اعتباری", "S-442"),
                        Triple("bounced-cheques", "استعلام چک برگشتی", "S-442"),
                        Triple("credit-rate", "رتبه اعتباری", "S-442"),
                    ))
        )

    private fun product(values: Map<String, String>) =
        MockData.current.loanProducts.find { it.key == values["product"] }

    private fun plan(values: Map<String, String>) =
        product(values)?.let {
            LoanRequestRules.installment(
                latinDigits(values["amount"].orEmpty()).toLongOrNull() ?: 0L,
                latinDigits(values["months"].orEmpty()).toLongOrNull() ?: 0L,
                it.rate,
            )
        }

    private val loanRequest =
        ServiceFlow(
            "loan-request",
            listOf(
                FlowStep(
                    "S-287",
                    "درخواست تسهیلات",
                    listOf(
                        choice("product", "نوع تسهیلات") {
                            MockData.current.loanProducts.map { it.key to it.title }
                        }
                    ),
                ),
                FlowStep(
                    "S-308",
                    "محاسبه اقساط",
                    listOf(
                        money("amount", "مبلغ درخواستی (ریال)"),
                        FlowField("months", "تعداد اقساط", check = ::amountCheck),
                    ),
                    { values ->
                        rows(
                            "سقف مبلغ (ریال)" to product(values)?.maxAmount?.let(::persianNumber),
                            "سقف اقساط" to product(values)?.maxMonths?.toLong()?.let(::persianNumber),
                            "نرخ سود (درصد)" to product(values)?.rate?.let(::persianNumber),
                            "قسط ماهانه (ریال)" to plan(values)?.let(::persianNumber),
                        )
                    },
                ),
                FlowStep(
                    "S-286",
                    "اطلاعات متقاضی",
                    listOf(FlowField("job", "شغل")),
                    {
                        MockData.current.customer.let { customer ->
                            rows("نام" to customer.name, "کد ملی" to customer.nationalCode)
                        }
                    },
                ),
                FlowStep(
                    "S-335",
                    "ضامن",
                    listOf(
                        FlowField("guarantor", "نام ضامن"),
                        FlowField("guarantorCode", "کد ملی ضامن", check = digits(10)),
                    ),
                ),
                FlowStep(
                    "S-285",
                    "تعهدنامه",
                    listOf(choice("agree", "تعهدنامه") { listOf("yes" to "شرایط را می‌پذیرم") }),
                ),
                FlowStep(
                    "S-334",
                    "سفته الکترونیک",
                    listOf(
                        choice("note", "مرحله نمایشی؛ سفته‌ای صادر نمی‌شود") {
                            listOf("issue" to "صدور سفته نمایشی")
                        }
                    ),
                ),
                FlowStep(
                    "S-378",
                    "امضای دیجیتال",
                    listOf(
                        choice("sign", "مرحله نمایشی؛ امضایی ثبت نمی‌شود") {
                            listOf("sign" to "امضای نمایشی")
                        }
                    ),
                ),
                FlowStep(
                    "S-336",
                    "تایید درخواست",
                    show = { values ->
                        rows(
                            "نوع تسهیلات" to product(values)?.title,
                            "مبلغ (ریال)" to amount(values),
                            "تعداد اقساط" to values["months"],
                            "قسط ماهانه (ریال)" to plan(values)?.let(::persianNumber),
                            "ضامن" to values["guarantor"],
                        )
                    },
                ),
            ),
            "S-337",
        ) { values ->
            MockBank.loanRequest(
                values.getValue("product"),
                latinDigits(values.getValue("amount")).toLong(),
                latinDigits(values.getValue("months")).toLong(),
            )
        }

    private val accountOpening =
        ServiceFlow(
            "account-opening",
            listOf(
                FlowStep(
                    "S-062",
                    "افتتاح حساب",
                    listOf(
                        choice("kind", "نوع حساب") {
                            MockData.current.openable.map { it.first to it.second }
                        }
                    ),
                ),
                FlowStep(
                    "S-054",
                    "اطلاعات تماس",
                    listOf(
                        FlowField("mobile", "تلفن همراه", check = digits(11)),
                        FlowField("email", "ایمیل"),
                    ),
                ),
                FlowStep("S-053", "اطلاعات تکمیلی", listOf(FlowField("job", "شغل"))),
                FlowStep(
                    "S-061",
                    "مدارک هویتی",
                    listOf(
                        choice("document", "مرحله نمایشی؛ فایلی بارگذاری نمی‌شود") {
                            listOf("card" to "کارت ملی نمایشی")
                        }
                    ),
                ),
                FlowStep(
                    "S-052",
                    "انتخاب شعبه",
                    listOf(
                        choice("branch", "شعبه") {
                            MockData.current.branches.map { it.first to it.second }
                        }
                    ),
                ),
                FlowStep(
                    "S-058",
                    "قوانین و مقررات",
                    listOf(choice("agree", "قوانین") { listOf("yes" to "قوانین را می‌پذیرم") }),
                ),
                FlowStep(
                    "S-057",
                    "تایید درخواست",
                    show = { values ->
                        rows(
                            "نوع حساب" to
                                MockData.current.openable.find { it.first == values["kind"] }?.second,
                            "شعبه" to
                                MockData.current.branches.find { it.first == values["branch"] }?.second,
                            "تلفن همراه" to values["mobile"],
                        )
                    },
                ),
            ),
            "S-059",
        ) { values ->
            MockBank.request(
                "account-opening",
                "افتتاح حساب",
                MockData.current.openable.first { it.first == values["kind"] }.second,
                listOf("وضعیت" to "درخواست ثبت شد"),
            )
        }

    val flows = sayad + vehicle + inquiries + loanRequest + accountOpening

    /** Result pages: each shows the receipt of the service that opened it. */
    val results
        get() = flows.mapNotNull { it.result }.toSet() - TollDestinations.RECEIPT

    val pages = flows.flatMap { flow -> flow.steps.map { it.page } }.toSet()
    val implemented
        get() = pages + results + menus.keys

    fun flow(key: String) = flows.first { it.key == key }

    /** The one flow that begins on [page], if only one does. */
    fun starting(page: String) = flows.filter { it.steps.first().page == page }.singleOrNull()
}
