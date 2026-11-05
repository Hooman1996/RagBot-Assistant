package org.ragbot.replica

/**
 * How an edge may be used. [REAL] and [DEMO] are exported to the executor. [USER_ONLY] needs the
 * user's OTP, card secret or payment step. [STEP] is a move the exported edges already cover.
 */
internal enum class EdgeKind {
    REAL,
    DEMO,
    USER_ONLY,
    STEP,
}

/** [action] is the test tags a user taps, in order, joined by " > "; * stands for a row key. */
internal data class Edge(
    val from: String,
    val to: String,
    val action: String,
    val kind: EdgeKind = EdgeKind.REAL,
)

/** An entry with no Home route. Its tag is demo-[key] before Home and demo-path-[key] on Home. */
internal data class DemoEntry(val key: String, val to: String, val title: String)

private fun entries(vararg items: Pair<String, String>) =
    items.map { (id, title) -> DemoEntry(id, id, title) }

/** Home tiles the owner traced in the real app, by tile key. */
internal val tracedHomeTiles =
    mapOf(
        "card-transfer" to CardTransferDestinations.ANONYMOUS,
        "transfer" to AccountTransferDestinations.MENU,
        "topup" to TopUpDestinations.ACCOUNT,
        "bill" to BillPaymentDestinations.ACCOUNT,
        "statement" to AccountDestinations.STATEMENT_LIST,
        "support" to SupportDestinations.MENU,
        "charity" to CharityDestinations.PICKER,
        "loan" to LoanDestinations.MENU,
        "conversion" to ConversionDestinations.LOGIN,
        "insurance" to InsuranceDestinations.MENU,
        "history" to AccountDestinations.HISTORY,
    )

/** Home tiles of Designed-for-mock services, by tile key. */
internal val designedHomeTiles = mapOf("inquiry" to "S-443")

/** Screens the owner traced below Home; they have real routes, so no Home demo path. */
private val ownerTraced =
    setOf(
        CardTransferDestinations.WITHOUT_SOURCE,
        TopUpDestinations.CARD,
        LoanDestinations.SUMMARY,
        LoanDestinations.GROUP,
        ConversionDestinations.NO_LOGIN,
        ConversionDestinations.PAID,
        ConversionDestinations.PAID_TYPED,
        SupportDestinations.CHATBOT,
        SupportDestinations.FAQ_NO_LOGIN,
        SupportDestinations.SUGGESTION,
        SupportDestinations.TICKETS,
        SupportDestinations.TICKET,
        SavedDestinationRoutes.LEGACY,
    )

internal object DemoPaths {
    val history =
        listOf(
            DemoEntry("history", AccountDestinations.HISTORY, "سوابق قدیمی"),
            DemoEntry("statement", AccountDestinations.STATEMENT, "درخواست صورتحساب"),
        )
    val review =
        entries(
            CardDestinations.ANONYMOUS_BALANCE to "موجودی بدون ورود",
            CardSecurityDestinations.LEGACY_BLOCK to "مسدودی بدون ورود",
            CardSecurityDestinations.ANONYMOUS_BLOCK to "مسدودی بدون ورود ۲",
            SupportDestinations.CHATBOT to "پرسش از چت‌بات",
            SupportDestinations.FAQ_NO_LOGIN to "پرسش‌های متداول بدون ورود",
            SupportDestinations.SUGGESTION to "پیشنهاد",
            SupportDestinations.TICKETS to "لیست تیکت‌ها",
            SupportDestinations.TICKET to "پیام جدید بدون تیکت",
        )
    val saved =
        entries(
            SavedDestinationRoutes.LEGACY to "مقصدهای ذخیره‌شده قدیمی",
            SavedDestinationRoutes.MANAGEMENT to "مدیریت مقصد تسهیلات",
        )
    val picker =
        DemoEntry(
            SavedDestinationRoutes.PICKER,
            SavedDestinationRoutes.PICKER,
            "انتخاب مقصد تسهیلات — بازبینی مستقل",
        )
    val journeys =
        entries(
            CardTransferDestinations.ANONYMOUS to "کارت به کارت بدون ورود با انتخاب مبدا",
            CardTransferDestinations.WITHOUT_SOURCE to "کارت به کارت بدون انتخابگر مبدا",
            AccountTransferDestinations.MENU to "منوی انتقال — نمونهٔ مستقل",
            GroupTransferDestinations.HISTORY to "سوابق تراکنش گروهی — نمونه",
            BillPaymentDestinations.ACCOUNT to "پرداخت قبض با حساب",
            BillPaymentDestinations.CARD to "پرداخت قبض با کارت",
            BillPaymentDestinations.WITHOUT_SOURCE to "پرداخت قبض بدون ورود",
            InsuranceDestinations.MENU to "منوی بیمه — نمونهٔ مستقل",
            TopUpDestinations.ACCOUNT to "خرید شارژ با حساب",
            TopUpDestinations.CARD to "خرید شارژ با کارت",
            TopUpDestinations.WITHOUT_SOURCE to "خرید شارژ بدون ورود",
            CharityDestinations.PICKER to "کمک به خیریه",
            WalletOverviewDestinations.MAIN to "کیف پول",
            ConversionDestinations.LOGIN to "تبدیل شناسه با ورود — نمونه",
            ConversionDestinations.NO_LOGIN to "تبدیل شناسه بدون ورود — نمونه",
            ConversionDestinations.PAID to "استعلام کارمزدی با انتخابگر — نمونهٔ مستقل",
            ConversionDestinations.PAID_TYPED to "استعلام کارمزدی با کارت تایپی — نمونهٔ مستقل",
            LoanDestinations.MENU to "منوی تسهیلات — نمونهٔ مستقل",
            LoanDestinations.SUMMARY to "تسهیلات جاری و پرداخت اقساط",
            LoanDestinations.GROUP to "گروه‌های تسهیلات — فقط مشاهده",
            // The in-flow callers (wage advance) are deferred.
            FundingFlow.ACCOUNT_IN_FLOW.amount to "افزایش موجودی حساب درون جریان — نمونهٔ مستقل",
            FundingFlow.WALLET_IN_FLOW.amount to "افزایش موجودی کیف درون جریان — نمونهٔ مستقل",
        )
    // The active state needs the external app, so it has its own entry.
    val digitalActive =
        DemoEntry(
            "digital-active",
            DigitalChequeDestinations.ACTIVATION,
            "امضای چکاد فعال — نمونهٔ مستقل",
        )
    // Stands in for a scanned payment: a fixed synthetic destination, no camera.
    val walletBuy =
        DemoEntry(
            "wallet-buy-qr",
            WalletOperation.BUY.form,
            "خرید با کیف با بارکد نمونه — نمونهٔ مستقل",
        )

    private val all = history + review + saved + picker + journeys + digitalActive + walletBuy

    /** Every entry the Home demo menu offers: the ones with no real Home route. */
    val onHome =
        all.filter {
            it.to != AccountDestinations.HISTORY &&
                it.to != WalletOverviewDestinations.MAIN &&
                it.to !in tracedHomeTiles.values &&
                it.to !in ownerTraced
        }

    /** Review entries before Home whose screens have a real route deeper than Home. */
    val reviewOnly = all.filter { it.to in ownerTraced }
}

/** The replica's transitions. [ReplicaApp.open] refuses any move not declared here. */
internal object ReplicaGraph {
    private const val HOME = HomeDestinations.HOME

    private fun all(froms: Iterable<String>, to: String, action: String, kind: EdgeKind) =
        froms.map { Edge(it, to, action, kind) }

    private fun many(from: String, tos: List<String>, action: String) =
        tos.map { Edge(from, it, action) }

    private val home =
        listOf(
            Edge(HOME, HomeDestinations.MENU, "home-scroll", EdgeKind.STEP),
            Edge(HOME, WalletOverviewDestinations.MAIN, "menu-wallet"),
            Edge(HOME, SupportDestinations.FAQ, "header-SUPPORT"),
            Edge(HOME, ChequeDestinations.MENU, "menu-cheque"),
            Edge(HOME, ChequeDestinations.CARDBOARD, "menu-cartable"),
            Edge(HOME, VehicleDestinations.MENU, "menu-vehicle"),
            Edge(HOME, "S-075", "header-NOTIFICATIONS"),
            Edge(HOME, ShortcutDestinations.SCREEN, "header-SEARCH"),
            Edge(HOME, RecurringTransferDestinations.REPETITIVE, "header-SHORTCUTS"),
            Edge(HOME, FundingFlow.ACCOUNT_STANDALONE.amount, "home-increase"),
            Edge(HOME, AccountDestinations.MANAGEMENT, "expand-account > home-account-management"),
            Edge(HOME, AccountDestinations.LIST, "control-ACCOUNT > account-list"),
            Edge(HOME, AccountDestinations.STATEMENT_LIST, "control-ACCOUNT > account-statement"),
            Edge(HOME, AccountDestinations.MANAGEMENT, "control-ACCOUNT > account-management"),
            Edge(HOME, AccountDestinations.TRANSACTION, "transaction-*"),
        ) +
            (tracedHomeTiles + designedHomeTiles).map { (key, to) ->
                Edge(HOME, to, "menu-$key")
            } +
            CardFixture.actions.flatMap { (kind, actions) ->
                actions.mapNotNull { action ->
                    action.destination?.let {
                        Edge(
                            HOME,
                            it,
                            "control-CARD > card-kind-${kind.name} > card-action-${action.key}",
                        )
                    }
                }
            } +
            ProfileFixture.menu.mapNotNull { action ->
                action.destination?.let {
                    Edge(HOME, it, "control-PROFILE > profile-${action.key}")
                }
            } +
            DemoPaths.onHome.map {
                Edge(HOME, it.to, "demo-menu > demo-path-${it.key}", EdgeKind.DEMO)
            } +
            DemoPaths.reviewOnly.map { Edge(HOME, it.to, "demo-${it.key}", EdgeKind.STEP) }

    private val accountsAndCards =
        listOf(
            Edge(
                AccountDestinations.STATEMENT_LIST,
                AccountDestinations.STATEMENT_DETAIL,
                "statement-*",
            ),
            Edge(AccountDestinations.MANAGEMENT, AccountDestinations.FETCH, "fetch-accounts"),
            Edge(AccountDestinations.HISTORY, AccountDestinations.TRANSACTION, "history-*"),
            Edge(CardDestinations.MANAGEMENT, CardDestinations.FETCH, "fetch-cards"),
            Edge(
                CardDestinations.CREDIT,
                CreditRepaymentDestinations.CONFIRM,
                "credit-pay > credit-amount-pay > credit-source-*",
            ),
            Edge(ProfileDestinations.ADDRESSES, ProfileDestinations.ADD_ADDRESS, "address-add"),
            Edge(CardRequestDestinations.ADDRESS, ProfileDestinations.ADD_ADDRESS, "add-address"),
            Edge(CardRequestDestinations.ADDRESS, CardRequestDestinations.COLOR, "address-ok"),
            Edge(CardRequestDestinations.COLOR, CardRequestDestinations.CONFIRM, "color-ok"),
            Edge(WalletDestinations.SEND_TYPE, WalletDestinations.ADDRESS, "wallet-type-ok"),
            Edge(WalletDestinations.ADDRESS, ProfileDestinations.ADD_ADDRESS, "wallet-add-address"),
            Edge(WalletDestinations.ADDRESS, WalletDestinations.CONFIRM, "wallet-address-ok"),
        ) +
            listOf(CardRequestDestinations.ISSUE, CardRequestDestinations.MODIFY).flatMap {
                many(
                    it,
                    listOf(CardRequestDestinations.ADDRESS, CardRequestDestinations.COLOR),
                    "first-ok",
                )
            } +
            ProfileFixture.settings.mapNotNull { action ->
                action.destination?.let {
                    Edge(ProfileDestinations.SETTINGS_MENU, it, "settings-${action.key}")
                }
            }

    private val transfers =
        listOf(
            Edge(
                AccountTransferDestinations.MENU,
                AccountTransferDestinations.INTERNAL,
                "transfer-internal",
            ),
            Edge(
                AccountTransferDestinations.MENU,
                AccountTransferDestinations.INTERBANK,
                "transfer-interbank",
            ),
            Edge(
                AccountTransferDestinations.MENU,
                GroupTransferDestinations.FIRST,
                "transfer-group",
            ),
            Edge(
                AccountTransferDestinations.MENU,
                RecurringTransferDestinations.MENU,
                "transfer-continuous",
            ),
            Edge(
                RecurringTransferDestinations.MENU,
                RecurringTransferDestinations.REQUEST,
                "recurring-new",
            ),
            Edge(
                RecurringTransferDestinations.MENU,
                RecurringTransferDestinations.HISTORY,
                "recurring-history",
            ),
            Edge(
                RecurringTransferDestinations.HISTORY,
                RecurringTransferDestinations.REQUEST,
                "continuous-new",
            ),
            Edge(
                RecurringTransferDestinations.HISTORY,
                RecurringTransferDestinations.DETAIL,
                "continuous-*",
            ),
            Edge(
                RecurringTransferDestinations.REQUEST,
                RecurringTransferDestinations.EXECUTION,
                "schedule-continue > schedule-type-*",
            ),
            Edge(
                GroupTransferDestinations.FILE_PICK,
                GroupTransferDestinations.FILE_LIST,
                "group-file-apply",
            ),
            Edge(
                GroupTransferDestinations.ERRORS,
                GroupTransferDestinations.WAITING,
                "group-remove-rejected",
            ),
            Edge(
                GroupTransferDestinations.HISTORY,
                GroupTransferDestinations.DESTINATIONS,
                "group-history-*-destinations",
            ),
            Edge(
                GroupTransferDestinations.DESTINATIONS,
                GroupTransferDestinations.DESTINATION,
                "group-destination-*",
            ),
        ) +
            many(
                RecurringTransferDestinations.EXECUTION,
                listOf(AccountTransferDestinations.INTERNAL, AccountTransferDestinations.INTERBANK),
                "schedule-execution-continue",
            ) +
            many(
                RecurringTransferDestinations.REPETITIVE,
                listOf(
                    CardTransferDestinations.CONFIRM,
                    AccountTransferDestinations.CONFIRM,
                    TopUpDestinations.CONFIRM,
                ),
                "repetitive-*-repeat",
            ) +
            all(
                listOf(CardTransferDestinations.ANONYMOUS, CardTransferDestinations.WITHOUT_SOURCE),
                CardTransferDestinations.CONFIRM,
                "card-transfer-review",
                EdgeKind.REAL,
            ) +
            Edge(
                CardTransferDestinations.ANONYMOUS,
                CardTransferDestinations.WITHOUT_SOURCE,
                "card-transfer-typed",
            ) +
            all(
                listOf(AccountTransferDestinations.INTERNAL, AccountTransferDestinations.INTERBANK),
                AccountTransferDestinations.CONFIRM,
                "account-transfer-continue",
                EdgeKind.REAL,
            ) +
            many(
                GroupTransferDestinations.FIRST,
                listOf(GroupTransferDestinations.MANUAL, GroupTransferDestinations.FILE_PICK),
                "group-continue",
            ) +
            all(
                listOf(GroupTransferDestinations.MANUAL, GroupTransferDestinations.FILE_LIST),
                GroupTransferDestinations.WAITING,
                "group-accept",
                EdgeKind.REAL,
            ) +
            many(
                GroupTransferDestinations.WAITING,
                listOf(GroupTransferDestinations.VALIDATION, GroupTransferDestinations.VERIFY),
                "group-refresh",
            ) +
            many(
                GroupTransferDestinations.VALIDATION,
                listOf(GroupTransferDestinations.ERRORS, GroupTransferDestinations.WAITING),
                "group-validation-continue",
            )

    private val payments =
        all(
            BillPaymentDestinations.forms,
            BillPaymentDestinations.CONFIRM,
            "bill-continue",
            EdgeKind.REAL,
        ) +
            listOf(BillPaymentDestinations.CARD_INFO, BillPaymentDestinations.CARD_INFO_NO_LOGIN)
                .map {
                    Edge(BillPaymentDestinations.CONFIRM, it, "bill-accept", EdgeKind.USER_ONLY)
                } +
            Edge(TopUpDestinations.ACCOUNT, TopUpDestinations.CARD, "topup-pay-card") +
            all(
                TopUpDestinations.forms,
                TopUpDestinations.CONFIRM,
                "topup-continue",
                EdgeKind.REAL,
            ) +
            listOf(TopUpDestinations.CARD_INFO, TopUpDestinations.CARD_INFO_NO_LOGIN).map {
                Edge(TopUpDestinations.CONFIRM, it, "topup-accept", EdgeKind.USER_ONLY)
            } +
            listOf(
                Edge(InsuranceDestinations.MENU, InsuranceDestinations.LIST, "insurance-list"),
                Edge(
                    InsuranceDestinations.LIST,
                    InsuranceDestinations.INSTALLMENTS,
                    "insurance-policy-*",
                ),
                Edge(
                    InsuranceDestinations.INSTALLMENTS,
                    InsuranceDestinations.CONFIRM,
                    "insurance-pay > insurance-source-*",
                ),
                Edge(CharityDestinations.PICKER, CharityDestinations.TRANSFER, "charity-*"),
                Edge(CharityDestinations.TRANSFER, CharityDestinations.CONFIRM, "charity-continue"),
                Edge(LoanDestinations.MENU, LoanDestinations.SUMMARY, "loan-current"),
                Edge(LoanDestinations.MENU, LoanDestinations.GROUP, "loan-groups"),
                Edge(LoanDestinations.MENU, "S-287", "loan-request"),
                Edge(LoanDestinations.SUMMARY, LoanDestinations.DETAIL, "loan-*"),
                Edge(
                    LoanDestinations.DETAIL,
                    LoanDestinations.CONFIRM,
                    "loan-review > loan-source-* > loan-source-confirm",
                ),
                Edge(LoanDestinations.GROUP, LoanDestinations.GROUP_DETAIL, "loan-group-*"),
            ) +
            all(
                ConversionDestinations.forms + ConversionDestinations.paid,
                ConversionDestinations.RESULT,
                "conversion-calculate",
                EdgeKind.REAL,
            ) +
            ConversionDestinations.modePages.map { (mode, page) ->
                Edge(ConversionDestinations.LOGIN, page, "conversion-mode-${mode.name}")
            }

    private val wallet =
        WalletFixture.actions.mapNotNull { item ->
            item.destination?.let {
                Edge(WalletOverviewDestinations.MAIN, it, "wallet-action-${item.key}")
            }
        } +
            WalletOperationFixture.chargeTypes.mapNotNull { item ->
                item.destination?.let {
                    Edge(
                        WalletOverviewDestinations.MAIN,
                        it,
                        "wallet-action-AUTHENTICATED_WALLET_CHARGE > wallet-charge-${item.key}",
                    )
                }
            } +
            WalletFixture.menu.mapNotNull { item ->
                item.destination?.let {
                    Edge(WalletOverviewDestinations.MAIN, it, "wallet-menu-${item.key}")
                }
            } +
            WalletFixture.history.mapNotNull { item ->
                item.destination?.let {
                    Edge(WalletOverviewDestinations.HISTORY_MENU, it, "wallet-menu-${item.key}")
                }
            } +
            Edge(
                WalletOverviewDestinations.STATEMENT,
                WalletOverviewDestinations.STATEMENT_DETAIL,
                "wallet-row-*",
            ) +
            WalletOperation.entries.map {
                // Without a scan, S-100 stops at a notice: only the demo path carries a payment.
                val kind = if (it == WalletOperation.BUY) EdgeKind.DEMO else EdgeKind.REAL
                Edge(it.form, it.confirm, "wallet-op-continue", kind)
            } +
            FundingFlow.entries.flatMap {
                listOf(
                    Edge(it.amount, it.gateway, "funding-confirm", EdgeKind.USER_ONLY),
                    Edge(it.gateway, it.waiting, "funding-outcome-*", EdgeKind.USER_ONLY),
                    Edge(it.waiting, it.receipt, "funding-fetch", EdgeKind.USER_ONLY),
                )
            }

    private val cheques =
        ChequeFixture.menu.mapNotNull { item ->
            item.destination?.let { Edge(ChequeDestinations.MENU, it, "cheque-menu-${item.key}") }
        } +
            ChequeFixture.chakadMenu.mapNotNull { item ->
                item.destination?.let {
                    Edge(ChequeDestinations.CHAKAD, it, "cheque-menu-${item.key}")
                }
            } +
            many(
                ChequeDestinations.CARDBOARD,
                listOf(ChequeDestinations.PAY_DETAIL, ChequeDestinations.REQUEST_DETAIL),
                "cardboard-*",
            ) +
            listOf(
                Edge(ChequeDestinations.BOOKS, ChequeDestinations.CHEQUES, "cheque-book-*"),
                Edge(
                    ChequeDestinations.ISSUE,
                    ChequeDestinations.ISSUE_CONFIRM,
                    "cheque-issue-continue",
                ),
                Edge(
                    ChequeDestinations.REPORT_FILTER,
                    ChequeDestinations.REPORT,
                    "cheque-report-apply",
                ),
                Edge(
                    DigitalChequeDestinations.CARTABLE,
                    DigitalChequeDestinations.DETAIL,
                    "digital-*",
                ),
                Edge(
                    DigitalChequeDestinations.DETAIL,
                    DigitalChequeDestinations.TRANSFER,
                    "digital-transfer",
                ),
                Edge(
                    DigitalChequeDestinations.TRANSFER,
                    DigitalChequeDestinations.CONFIRM,
                    "digital-continue",
                ),
            )

    private val vehicles =
        VehicleFixture.menu.map { (key, _) ->
            Edge(VehicleDestinations.MENU, VehicleRules.target(key), "vehicle-menu-$key")
        } +
            listOf(
                    VehicleDestinations.PLATES,
                    VehicleDestinations.CONTACTS,
                    VehicleDestinations.LICENCES,
                )
                .map { Edge(it, VehicleDestinations.INQUIRY_HISTORY, "vehicle-history-*") } +
            Edge(VehicleDestinations.HISTORY_MENU, AccountDestinations.HISTORY, "vehicle-paid-*") +
            ServiceCatalog.vehicleMenu.flatMap { (key, flow) ->
                val first = ServiceCatalog.flow(flow).steps.first().page
                listOf(
                    Edge(VehicleDestinations.MENU, first, "vehicle-menu-$key > vehicle-new"),
                    Edge(VehicleRules.target(key), first, "vehicle-*", EdgeKind.STEP),
                )
            } +
            TollService.entries.flatMap { service ->
                // The plate list's onward move depends on the menu item that opened it.
                listOfNotNull(
                    Edge(
                        VehicleDestinations.MENU,
                        service.inquiry,
                        "vehicle-menu-${service.key} > vehicle-new",
                    ),
                    Edge(VehicleDestinations.PLATES, service.inquiry, "vehicle-*", EdgeKind.STEP),
                    Edge(
                        service.inquiry,
                        service.result,
                        "toll-inquiry > toll-fee-* > otp-ok",
                        EdgeKind.USER_ONLY,
                    ),
                    service.confirm?.let {
                        Edge(service.result, it, "toll-pay", EdgeKind.USER_ONLY)
                    },
                )
            }

    private val support =
        SupportFixture.menu.map { (key, _, to) ->
            Edge(SupportDestinations.MENU, to, "support-menu-$key")
        } +
            listOf(
                Edge(
                    SupportDestinations.FAQ_NO_LOGIN,
                    SupportDestinations.MESSAGE,
                    "faq-support > support-mobile > support-identity-continue > support-otp > support-otp-continue",
                    EdgeKind.USER_ONLY,
                ),
                Edge(SupportDestinations.TICKETS, SupportDestinations.TICKET, "ticket-*"),
            )

    /** A receipt follows only the user's confirm, so none of these reach the executor. */
    private val receipts =
        listOf(
                CardTransferDestinations.CONFIRM to
                    (CardTransferDestinations.RECEIPT to "card-transfer-submit"),
                AccountTransferDestinations.CONFIRM to
                    (AccountTransferDestinations.INTERNAL_RECEIPT to "account-transfer-submit"),
                AccountTransferDestinations.CONFIRM to
                    (AccountTransferDestinations.IBAN_RECEIPT to "account-transfer-submit"),
                GroupTransferDestinations.VERIFY to
                    (GroupTransferDestinations.RECEIPT to "group-verify-accept"),
                BillPaymentDestinations.CONFIRM to
                    (BillPaymentDestinations.RECEIPT to "bill-accept"),
                BillPaymentDestinations.CARD_INFO to
                    (BillPaymentDestinations.RECEIPT to "payment-submit"),
                BillPaymentDestinations.CARD_INFO_NO_LOGIN to
                    (BillPaymentDestinations.RECEIPT to "payment-submit"),
                TopUpDestinations.CONFIRM to (TopUpDestinations.RECEIPT to "topup-accept"),
                TopUpDestinations.CARD_INFO to (TopUpDestinations.RECEIPT to "payment-submit"),
                TopUpDestinations.CARD_INFO_NO_LOGIN to
                    (TopUpDestinations.RECEIPT to "payment-submit"),
                CharityDestinations.CONFIRM to (CharityDestinations.RECEIPT to "payment-submit"),
                InsuranceDestinations.CONFIRM to
                    (InsuranceDestinations.RECEIPT to "payment-submit"),
                CreditRepaymentDestinations.CONFIRM to
                    (CreditRepaymentDestinations.RECEIPT to "payment-submit"),
            )
            .map { (from, target) ->
                Edge(from, target.first, target.second, EdgeKind.USER_ONLY)
            } +
            WalletOperation.entries.map {
                Edge(
                    it.confirm,
                    WalletOverviewDestinations.RECEIPT,
                    "wallet-op-submit",
                    EdgeKind.USER_ONLY,
                )
            } +
            TollService.entries.mapNotNull { service ->
                service.confirm?.let {
                    Edge(it, TollDestinations.RECEIPT, "payment-submit", EdgeKind.USER_ONLY)
                }
            }

    private val services =
        ServiceCatalog.flows
            .flatMap { flow ->
                flow.steps.zipWithNext { from, to -> Edge(from.page, to.page, "svc-continue") } +
                    listOfNotNull(
                        flow.result?.let {
                            Edge(flow.steps.last().page, it, "svc-confirm", EdgeKind.USER_ONLY)
                        },
                        flow.detail?.let { Edge(flow.result!!, it, "svc-detail") },
                    )
            }
            .distinct() +
            ServiceCatalog.menus.flatMap { (page, menu) ->
                menu.second.map { (key, _, to) -> Edge(page, to, "svc-menu-$key") }
            }

    val edges =
        home +
            accountsAndCards +
            transfers +
            payments +
            wallet +
            cheques +
            vehicles +
            support +
            receipts +
            services

    private val moves = edges.map { it.from to it.to }.toSet()

    fun declared(from: String, to: String) = (from to to) in moves
}
