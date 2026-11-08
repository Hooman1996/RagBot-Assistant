package org.ragbot.replica

/*
 * The short description and the Persian question of every form field the assistant may fill.
 * Read by ProfileForms when it builds the profile. Service fields take theirs from ServiceCatalog.
 * It holds words only. A secret field has no entry, because nobody may ask for it here.
 */

/** A field's short description, and the question that asks the user for it. */
internal data class Words(val description: String, val ask: String)

internal object ProfileWords {
    private fun w(description: String, ask: String) = Words(description, ask)

    private val money =
        mapOf(
            "card-transfer-amount" to w("مبلغ انتقال", "چند ریال منتقل شود؟"),
            "account-transfer-amount" to w("مبلغ انتقال", "چند ریال منتقل شود؟"),
            "group-total" to w("جمع مبلغ گروهی", "جمع کل انتقال گروهی چند ریال است؟"),
            "group-row-*-amount" to w("مبلغ این ردیف", "مبلغ این ردیف چند ریال است؟"),
            "budget-limit" to w("سقف بودجه", "سقف ماهانه این دسته چند ریال باشد؟"),
            "credit-amount" to w("مبلغ بازپرداخت", "چند ریال بازپرداخت شود؟"),
            "topup-amount" to w("مبلغ شارژ", "مبلغ شارژ چند ریال باشد؟"),
            "charity-amount" to w("مبلغ کمک", "چند ریال کمک می‌کنید؟"),
            "wallet-amount" to w("مبلغ", "مبلغ چند ریال است؟"),
            "wallet-op-amount" to w("مبلغ", "مبلغ چند ریال است؟"),
            "funding-amount" to w("مبلغ افزایش موجودی", "موجودی چند ریال افزایش یابد؟"),
        )

    private val sources =
        mapOf(
            "card-transfer-source" to w("کارت مبدا", "از کدام کارت انتقال انجام شود؟"),
            "card-transfer-destination" to w("کارت مقصد", "به چه کسی یا کدام کارت منتقل شود؟"),
            "account-transfer-source" to w("حساب مبدا", "از کدام حساب برداشت شود؟"),
            "account-transfer-destination" to w("حساب مقصد", "شماره حساب یا شبای مقصد چیست؟"),
            "group-source" to w("حساب مبدا", "از کدام حساب برداشت شود؟"),
            "bill-source" to w("مبدا پرداخت", "قبض از کدام حساب پرداخت شود؟"),
            "topup-source" to w("مبدا پرداخت", "شارژ از کدام حساب خریداری شود؟"),
            "charity-source" to w("حساب مبدا", "کمک از کدام حساب برداشت شود؟"),
            "insurance-source" to w("حساب مبدا", "قسط بیمه از کدام حساب پرداخت شود؟"),
            "credit-source" to w("حساب مبدا", "بازپرداخت از کدام حساب انجام شود؟"),
            "loan-source" to w("حساب مبدا", "قسط از کدام حساب پرداخت شود؟"),
            "conversion-source" to w("کارت مبدا", "کارمزد از کدام کارت برداشت شود؟"),
            "export-account" to w("حساب", "صورتحساب کدام حساب را می‌خواهید؟"),
            "cheque-account" to w("حساب جاری", "کدام حساب؟"),
            "wallet-picker" to w("کیف پول", "کدام کیف پول؟"),
            "wallet-op-source" to w("مبدا", "از کدام مبدا برداشت شود؟"),
            "wallet-op-destination" to w("مقصد", "مقصد کدام است؟"),
            "funding-target" to w("مقصد افزایش موجودی", "موجودی کدام مقصد افزایش یابد؟"),
            "digital-sayad" to w("شناسه صیاد", "کدام چک صیاد؟"),
        )

    private val identifiers =
        mapOf(
            "bill-id" to w("شناسه قبض", "شناسه قبض چیست؟"),
            "bill-pay-id" to w("شناسه پرداخت", "شناسه پرداخت قبض چیست؟"),
            "topup-phone" to w("شماره موبایل", "شارژ برای کدام شماره موبایل باشد؟"),
            "wallet-op-mobile" to w("شماره موبایل", "شارژ برای کدام شماره موبایل باشد؟"),
            "wallet-op-bill" to w("شناسه قبض", "شناسه قبض چیست؟"),
            "wallet-op-pay" to w("شناسه پرداخت", "شناسه پرداخت قبض چیست؟"),
            "conversion-identifier" to w("شناسه", "شماره کارت، حساب یا شبا چیست؟"),
            "cheque-no" to w("شماره چک", "شماره چک چیست؟"),
            "secure-number" to w("شماره کارت", "شماره کارت چیست؟"),
            "noLogin-number" to w("شماره کارت", "شماره کارت چیست؟"),
            "saved-number" to w("شماره مقصد", "شماره مقصد چیست؟"),
            "saved-name" to w("نام مقصد", "نام این مقصد چیست؟"),
            "account-transfer-payment-id" to w("شناسه واریز", "شناسه واریز چیست؟"),
            "account-transfer-withdraw-id" to w("شناسه برداشت", "شناسه برداشت چیست؟"),
            "charity-payment-id" to w("شناسه واریز", "شناسه واریز چیست؟"),
            "loan-payment-id" to w("شناسه واریز", "شناسه واریز چیست؟"),
            "group-withdraw-id" to w("شناسه برداشت", "شناسه برداشت چیست؟"),
            "group-row-*-iban" to w("شبای مقصد", "شبای مقصد این ردیف چیست؟"),
            "group-row-*-payment-id" to w("شناسه واریز", "شناسه واریز این ردیف چیست؟"),
            "toll-code" to w("کد ملی مالک", "کد ملی مالک خودرو چیست؟"),
            "toll-phone" to w("موبایل مالک", "شماره موبایل مالک خودرو چیست؟"),
            "toll-vin" to w("شماره شاسی", "شماره شاسی خودرو چیست؟"),
            "toll-plate-0" to w("پلاک، بخش اول", "دو رقم اول پلاک چیست؟"),
            "toll-plate-1" to w("پلاک، بخش دوم", "بخش دوم پلاک چیست؟"),
            "toll-plate-2" to w("پلاک، بخش سوم", "سه رقم میانی پلاک چیست؟"),
            "toll-plate-3" to w("پلاک، کد شهر", "دو رقم آخر پلاک چیست؟"),
            "login-username" to w("نام کاربری", "نام کاربری چیست؟"),
        )

    private val dates =
        mapOf(
            "ledger-start" to w("از تاریخ", "از چه تاریخی؟"),
            "ledger-end" to w("تا تاریخ", "تا چه تاریخی؟"),
            "statement-start" to w("از تاریخ", "از چه تاریخی؟"),
            "statement-end" to w("تا تاریخ", "تا چه تاریخی؟"),
            "wallet-start" to w("از تاریخ", "از چه تاریخی؟"),
            "wallet-end" to w("تا تاریخ", "تا چه تاریخی؟"),
            "cheque-from" to w("از تاریخ", "از چه تاریخی؟"),
            "cheque-to" to w("تا تاریخ", "تا چه تاریخی؟"),
            "schedule-start" to w("تاریخ شروع", "انتقال از چه تاریخی شروع شود؟"),
            "schedule-end" to w("تاریخ پایان", "انتقال تا چه تاریخی ادامه یابد؟"),
            "field-issueDate" to w("تاریخ صدور", "تاریخ صدور شناسنامه چیست؟"),
        )

    private val notes =
        mapOf(
            "card-transfer-note" to w("توضیحات", "توضیحی برای انتقال دارید؟"),
            "account-transfer-note" to w("توضیحات", "توضیحی برای انتقال دارید؟"),
            "account-transfer-source-description" to w("شرح مبدا", "شرح برداشت چیست؟"),
            "account-transfer-destination-description" to w("شرح مقصد", "شرح واریز چیست؟"),
            "group-source-description" to w("شرح مبدا", "شرح برداشت چیست؟"),
            "group-row-*-description" to w("شرح مقصد", "شرح این ردیف چیست؟"),
            "bill-note" to w("توضیحات", "توضیحی برای پرداخت دارید؟"),
            "charity-description" to w("توضیحات", "توضیحی برای کمک دارید؟"),
            "saved-description" to w("توضیحات", "توضیحی برای این مقصد دارید؟"),
            "schedule-title" to w("عنوان انتقال", "عنوان این انتقال خودکار چیست؟"),
            "chatbot-draft" to w("پیام", "پیام شما چیست؟"),
            "support-draft" to w("پیام", "پیام شما چیست؟"),
            "suggestion-description" to w("پیشنهاد", "پیشنهاد شما چیست؟"),
            "field-title" to w("عنوان آدرس", "عنوان این آدرس چیست؟"),
            "field-postal" to w("کد پستی", "کد پستی چیست؟"),
            "field-address" to w("آدرس", "آدرس کامل چیست؟"),
            "field-firstNameEn" to w("نام انگلیسی", "نام شما به انگلیسی چیست؟"),
            "field-lastNameEn" to w("نام خانوادگی انگلیسی", "نام خانوادگی شما به انگلیسی چیست؟"),
            "field-fathersName" to w("نام پدر", "نام پدر شما چیست؟"),
            "field-taxCode" to w("کد مالیاتی", "کد مالیاتی چیست؟"),
        )

    private val choices =
        mapOf(
            "account-transfer-reason" to w("علت انتقال", "علت انتقال چیست؟"),
            "group-row-*-reason" to w("علت انتقال", "علت انتقال این ردیف چیست؟"),
            "schedule-frequency" to w("دوره تکرار", "انتقال هر چند وقت تکرار شود؟"),
            "schedule-day" to w("روز انتقال", "در چه روزی انجام شود؟"),
            "topup-operator" to w("اپراتور", "کدام اپراتور؟"),
            "wallet-op-operator" to w("اپراتور", "کدام اپراتور؟"),
            "conversion-mode" to w("نوع تبدیل", "چه نوع تبدیلی می‌خواهید؟"),
            "conversion-bank" to w("بانک", "کدام بانک؟"),
            "export-format" to w("قالب فایل", "فایل با چه قالبی باشد؟"),
            "installment" to w("قسط", "کدام قسط؟"),
            "loan-installment" to w("تعداد قسط", "چند قسط پرداخت شود؟"),
            "source" to w("حساب کارت", "کارت به کدام حساب وصل شود؟"),
            "delivery" to w("نحوه تحویل", "کارت چگونه تحویل داده شود؟"),
            "address" to w("آدرس تحویل", "به کدام آدرس ارسال شود؟"),
            "color" to w("طرح کارت", "کدام طرح کارت را می‌خواهید؟"),
            "wallet" to w("نوع ارسال", "کارت کیف پول چگونه ارسال شود؟"),
            "wallet-address" to w("آدرس ارسال", "به کدام آدرس ارسال شود؟"),
            "cheque-page" to w("تعداد برگ", "دسته چک چند برگی باشد؟"),
            "cheque-status" to w("وضعیت چک", "چک‌ها با چه وضعیتی نمایش داده شوند؟"),
            "digital-bounce" to w("چک برگشتی", "چک برگشتی هم بررسی شود؟"),
            "toll-letter" to w("حرف پلاک", "حرف پلاک چیست؟"),
            "toll-type" to w("نوع وسیله", "وسیله نقلیه خودرو است یا موتور؟"),
            "toll-owner" to w("مالکیت", "خودرو به نام خودتان است؟"),
            "pick-province" to w("استان", "استان محل صدور کدام است؟"),
            "pick-city" to w("شهر", "شهر محل صدور کدام است؟"),
            "pick-addressProvince" to w("استان", "استان کدام است؟"),
            "pick-addressCity" to w("شهر", "شهر کدام است؟"),
        )

    private val all = money + sources + identifiers + dates + notes + choices

    /** The words of field [ref]. A field with none is a mistake in this file. */
    fun of(ref: String): Words = checkNotNull(all[ref]) { "No words for $ref" }
}
