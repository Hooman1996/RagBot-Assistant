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

    val flows = sayad + vehicle

    /** Result pages: each shows the receipt of the service that opened it. */
    val results
        get() = flows.mapNotNull { it.result }.toSet() - TollDestinations.RECEIPT

    val pages = flows.flatMap { flow -> flow.steps.map { it.page } }.toSet()
    val implemented
        get() = pages + results

    fun flow(key: String) = flows.first { it.key == key }

    /** The one flow that begins on [page], if only one does. */
    fun starting(page: String) = flows.filter { it.steps.first().page == page }.singleOrNull()
}
