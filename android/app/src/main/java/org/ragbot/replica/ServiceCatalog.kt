package org.ragbot.replica

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

    val flows = sayad

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
