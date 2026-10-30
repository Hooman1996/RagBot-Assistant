package org.ragbot.replica

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ProfileDestinations {
    const val ADDITIONAL = "S-064"
    const val EDIT = "S-065"
    const val INVITATION = "S-069"
    const val SETTINGS_MENU = "S-025"
    const val BIOMETRIC = "S-070"
    const val NOTIFICATIONS = "S-071"
    const val SERVICES = "S-072"
    const val STOREFRONT = "S-073"
    const val CHANGELOG = "S-074"
    const val ADD_ADDRESS = "S-463"
    const val ADDRESSES = "S-464"
    val implemented =
        setOf(
            ADDITIONAL,
            EDIT,
            INVITATION,
            SETTINGS_MENU,
            BIOMETRIC,
            NOTIFICATIONS,
            SERVICES,
            STOREFRONT,
            CHANGELOG,
            ADD_ADDRESS,
            ADDRESSES,
        )
}

/** PF-FIXTURE: invented profile values, menus, facilities, notifications and versions. */
object ProfileFixture {
    const val CUSTOMER_NUMBER = "۰۰۰۰۰۱"
    const val LAST_LOGIN = "۱۴۰۵/۰۸/۰۷ - ۰۹:۳۰"
    const val INVITE_CODE = "PREVIEW01"
    val menu =
        listOf(
            CardAction("edit", "ویرایش پروفایل", ProfileDestinations.EDIT),
            CardAction("additional", "اطلاعات تکمیلی", ProfileDestinations.ADDITIONAL),
            CardAction("accounts", "مدیریت حساب", AccountDestinations.MANAGEMENT),
            CardAction("cards", "مدیریت کارت‌ها", CardDestinations.MANAGEMENT),
            CardAction("addresses", "مدیریت آدرس‌ها", ProfileDestinations.ADDRESSES),
            CardAction("settings", "تنظیمات برنامه", ProfileDestinations.SETTINGS_MENU),
            CardAction("invite", "دعوت از دوستان", ProfileDestinations.INVITATION),
            CardAction("versions", "تاریخچه نسخه‌ها", ProfileDestinations.CHANGELOG),
            CardAction("limits", "مدیریت سقف تراکنش‌ها", null),
            CardAction("signature", "امضای دیجیتال", null),
        )
    val settings =
        listOf(
            CardAction("biometric", "ورود با سنسور زیستی", ProfileDestinations.BIOMETRIC),
            CardAction("services", "خدمات مالی", ProfileDestinations.SERVICES),
            CardAction(
                "storefront",
                "خدمات مالی در ورود با سنسور زیستی",
                ProfileDestinations.STOREFRONT,
            ),
            CardAction("cards", "وضعیت نمایش کارت", CardDestinations.VISIBILITY),
        )
    val facilities = listOf("انتقال وجه", "پرداخت قبض", "خرید شارژ", "کارت به کارت")
    val notifications = listOf("پیامک تراکنش‌ها", "اعلان پیشنهادها", "اعلان سررسیدها")
    val versions =
        listOf(
            "۵.۱.۰" to "بهبود صفحه کارت‌ها و رفع اشکالات",
            "۵.۰.۰" to "طراحی جدید خانه و حساب‌ها",
            "۴.۹.۰" to "افزودن مدیریت آدرس‌ها",
        )
    val provinces = mapOf("تهران" to listOf("تهران", "ری"), "اصفهان" to listOf("اصفهان", "کاشان"))
    val choices =
        mapOf(
            "marital" to listOf("مجرد", "متاهل"),
            "education" to listOf("دیپلم", "کارشناسی", "کارشناسی ارشد"),
            "job" to listOf("کارمند", "آزاد", "بازنشسته"),
            "salary" to
                listOf(
                    "کمتر از ۱۰۰ میلیون ریال",
                    "۱۰۰ تا ۵۰۰ میلیون ریال",
                    "بیش از ۵۰۰ میلیون ریال",
                ),
        )
}

/** PF-RULES: recovered field rules; failures name the field that must be corrected. */
object ProfileRules {
    private val latinName = Regex("[A-Za-z][A-Za-z ]*")

    fun englishName(value: String) = latinName.matches(value.trim())

    /** 8–20 English characters with upper, lower, digit and one of !@$^&* (recovered hint). */
    fun password(value: String) =
        value.length in 8..20 &&
            value.any { it in 'A'..'Z' } &&
            value.any { it in 'a'..'z' } &&
            value.any { it in '0'..'9' } &&
            value.any { it in "!@$^&*" } &&
            value.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "!@$^&*" }

    /** 6–20 English letters, digits or dots, not starting or ending with a dot (recovered hint). */
    fun username(value: String) =
        Regex("[A-Za-z0-9.]{6,20}").matches(value) && !value.startsWith('.') && !value.endsWith('.')

    fun mobile(value: String) = Regex("09[0-9]{9}").matches(latinDigits(value))

    fun email(value: String) = Regex("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}").matches(value)

    fun postalCode(value: String) = Regex("[0-9]{10}").matches(latinDigits(value))
}

/** Hosted Profile tab content (PF-TAB): header, configured menu, last login and version. */
@Composable
internal fun ProfileTab(onAction: (CardAction) -> Unit, onUnavailable: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFFFBFBFB), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp)
            .testTag("profile-tab")
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFFE3F4E9), CircleShape), Alignment.Center) {
                ReplicaIcon("replica_avatar_person", "○", Modifier.size(40.dp), Color(0xFFA5A5A5))
            }
            Text(HomeFixture.accountOwner, Modifier.padding(top = 8.dp), fontSize = 16.sp)
            Text(
                "شماره مشتری: ${ProfileFixture.CUSTOMER_NUMBER}",
                fontSize = 12.sp,
                color = Color.Gray,
            )
            Text(
                "${AccountFixture.accounts.take(2).joinToString("، ") { it.number }} و...",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
        ProfileFixture.menu.forEach { action ->
            AccountAction(action.title, "replica_account_list", "profile-${action.key}") {
                if (action.destination != null) onAction(action) else onUnavailable(action.title)
            }
        }
        Text(
            "آخرین ورود: ${ProfileFixture.LAST_LOGIN}",
            Modifier.align(Alignment.CenterHorizontally),
            fontSize = 14.sp,
            color = Color.Gray,
        )
        Text(
            "نسخه نمایشی ${BuildConfig.SOURCE_REVISION}",
            Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 30.dp),
            fontSize = 14.sp,
            color = Color.Gray,
        )
    }
}

@Composable
internal fun ProfilePage(
    page: String,
    scenario: String,
    profile: Map<String, String>,
    addresses: List<PreviewAddress>,
    toggles: List<String>,
    onProfile: (Map<String, String>) -> Unit,
    onAddresses: (List<PreviewAddress>) -> Unit,
    onToggles: (List<String>) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val title =
        when (page) {
            ProfileDestinations.ADDITIONAL -> "اطلاعات تکمیلی"
            ProfileDestinations.EDIT -> "ویرایش پروفایل"
            ProfileDestinations.INVITATION -> "دعوت از دوستان"
            ProfileDestinations.SETTINGS_MENU -> "تنظیمات برنامه"
            ProfileDestinations.BIOMETRIC -> "ورود با سنسور زیستی"
            ProfileDestinations.NOTIFICATIONS -> "تنظیمات اعلان‌ها"
            ProfileDestinations.SERVICES -> "خدمات مالی"
            ProfileDestinations.STOREFRONT -> "خدمات مالی در ورود با سنسور زیستی"
            ProfileDestinations.CHANGELOG -> "تاریخچه نسخه‌ها"
            ProfileDestinations.ADD_ADDRESS -> "افزودن آدرس"
            else -> "مدیریت آدرس‌ها"
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    ProfileDestinations.ADDITIONAL -> AdditionalInfo(profile, onProfile, onBack)
                    ProfileDestinations.EDIT -> EditUser(onBack, onUnavailable)
                    ProfileDestinations.INVITATION -> Invitation(onUnavailable)
                    ProfileDestinations.SETTINGS_MENU ->
                        ProfileFixture.settings.forEach { action ->
                            AccountAction(
                                action.title,
                                "replica_account_list",
                                "settings-${action.key}",
                            ) {
                                onOpen(checkNotNull(action.destination))
                            }
                        }
                    ProfileDestinations.BIOMETRIC -> {
                        Text("لطفا اثر انگشت خود را بر روی سنسور گوشی قرار دهید.", fontSize = 14.sp)
                        Button(
                            { onUnavailable("فعال‌سازی سنسور زیستی") },
                            Modifier.testTag("biometric"),
                        ) {
                            Text("فعال‌سازی سنسور زیستی")
                        }
                    }
                    ProfileDestinations.NOTIFICATIONS -> Notifications(toggles, onToggles)
                    ProfileDestinations.SERVICES,
                    ProfileDestinations.STOREFRONT -> Facilities(page, toggles, onToggles)
                    ProfileDestinations.CHANGELOG ->
                        ProfileFixture.versions.forEach { (version, note) ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .testTag("version-$version")
                            ) {
                                Text("نسخه $version", fontSize = 14.sp)
                                Text(note, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    ProfileDestinations.ADD_ADDRESS ->
                        AddAddress({ onAddresses(addresses + it) }, onBack)
                    else -> AddressList(addresses, onAddresses, onOpen)
                }
            }
    }
}

@Composable
private fun LocalNote() {
    Text("تغییرات فقط در این نسخه نمایشی نگه داشته می‌شود.", fontSize = 11.sp, color = Color.Gray)
}

@Composable
private fun Field(
    key: String,
    label: String,
    value: String,
    errors: Map<String, String>,
    secret: Boolean = false,
    number: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().testTag("field-$key"),
        label = { Text(label) },
        singleLine = true,
        isError = key in errors,
        supportingText = errors[key]?.let { { Text(it) } },
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    when {
                        secret -> KeyboardType.Password
                        number -> KeyboardType.Number
                        else -> KeyboardType.Text
                    }
            ),
        visualTransformation =
            if (secret) PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun Picker(
    key: String,
    label: String,
    options: List<String>,
    value: String?,
    error: String?,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton({ open = true }, Modifier.fillMaxWidth().testTag("pick-$key")) {
        Text(value ?: label, color = if (error != null) Color(0xFFCB3434) else Color.Unspecified)
    }
    if (error != null) Text(error, fontSize = 12.sp, color = Color(0xFFCB3434))
    if (open)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column {
                    options.forEach { option ->
                        TextButton(
                            {
                                onPick(option)
                                open = false
                            },
                            Modifier.fillMaxWidth().testTag("option-$option"),
                        ) {
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ open = false }) { Text("بستن") } },
        )
}

/** S-064: recovered fields; saving keeps the values in local profile state. */
@Composable
private fun AdditionalInfo(
    profile: Map<String, String>,
    onSave: (Map<String, String>) -> Unit,
    onDone: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(profile) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var saved by rememberSaveable { mutableStateOf(false) }
    fun set(key: String, value: String) {
        draft = draft + (key to value)
    }
    LocalNote()
    Field("firstNameEn", "نام به انگلیسی", draft["firstNameEn"].orEmpty(), errors) {
        set("firstNameEn", it)
    }
    Field("lastNameEn", "نام خانوادگی به انگلیسی", draft["lastNameEn"].orEmpty(), errors) {
        set("lastNameEn", it)
    }
    Field("fathersName", "نام پدر به انگلیسی", draft["fathersName"].orEmpty(), errors) {
        set("fathersName", it)
    }
    Field("issueDate", "تاریخ صدور شناسنامه", draft["issueDate"].orEmpty(), errors) {
        set("issueDate", it)
    }
    Picker(
        "province",
        "استان محل تولد",
        ProfileFixture.provinces.keys.toList(),
        draft["province"],
        errors["province"],
    ) {
        draft = draft + ("province" to it) - "city"
    }
    Picker(
        "city",
        "شهر محل تولد",
        ProfileFixture.provinces[draft["province"]].orEmpty(),
        draft["city"],
        errors["city"],
    ) {
        set("city", it)
    }
    val titles =
        mapOf(
            "marital" to "وضعیت تاهل",
            "education" to "تحصیلات",
            "job" to "نوع شغل",
            "salary" to "میزان درآمد",
        )
    ProfileFixture.choices.forEach { (key, options) ->
        Picker(key, titles.getValue(key), options, draft[key], errors[key]) { set(key, it) }
    }
    Field("taxCode", "کد‌مالیاتی (اختیاری)", draft["taxCode"].orEmpty(), errors, number = true) {
        set("taxCode", it)
    }
    Buttons(
        true,
        "additional-ok",
        {
            errors = buildMap {
                listOf("firstNameEn", "lastNameEn", "fathersName").forEach {
                    if (!ProfileRules.englishName(draft[it].orEmpty()))
                        put(it, "لطفا با حروف انگلیسی وارد کنید.")
                }
                if (persianDateMillis(latinDigits(draft["issueDate"].orEmpty())) == null)
                    put("issueDate", "تاریخ را به صورت ۱۴۰۰/۰۱/۰۱ وارد کنید.")
                if (draft["province"] == null)
                    put("province", "لطفا استان مورد نظر را انتخاب کنید.")
                if (draft["city"] == null) put("city", "لطفا شهر مورد نظر را انتخاب کنید.")
                ProfileFixture.choices.keys.forEach {
                    if (draft[it] == null) put(it, "لطفا یک گزینه را انتخاب کنید")
                }
            }
            if (errors.isEmpty()) {
                onSave(draft)
                saved = true
            }
        },
        onDone,
    )
    if (saved)
        AlertDialog(
            onDismissRequest = {},
            text = { Text("عملیات با موفقیت انجام شد. (فقط در این نسخه نمایشی)") },
            confirmButton = {
                TextButton(onDone, Modifier.testTag("additional-done")) { Text("بستن") }
            },
        )
}

/** S-065: expandable credential sections; confirmed changes end in the labelled FR-04 result. */
@Composable
private fun EditUser(onDone: () -> Unit, onUnavailable: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var stage by rememberSaveable { mutableStateOf("form") }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var third by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton({ onUnavailable("تغییر تصویر پروفایل") }, Modifier.testTag("profile-photo")) {
            Text("تصویر پروفایل")
        }
        Text(HomeFixture.accountOwner, fontSize = 16.sp)
        Text("شماره مشتری ${ProfileFixture.CUSTOMER_NUMBER}", fontSize = 12.sp, color = Color.Gray)
    }
    val sections =
        listOf(
            "password" to "تغییر رمز ورود",
            "username" to "تغییر نام‌ کاربری",
            "mobile" to "تغییر تلفن همراه",
            "email" to "تغییر ایمیل",
        )
    sections.forEach { (key, title) ->
        AccountAction(title, "replica_account_list", "edit-$key") {
            open = if (open == key) null else key
            first = ""
            second = ""
            third = ""
            errors = emptyMap()
        }
        if (open == key) {
            when (key) {
                "password" -> {
                    Field("current", "رمز فعلی", first, errors, secret = true) { first = it }
                    Field("new", "رمز جدید", second, errors, secret = true) { second = it }
                    Text(
                        "طول رمز بین 8 تا 20 کاراکتر انگلیسی و ترکیبی از حروف بزرگ و کوچک، اعداد و کاراکتر خاص (!@\$^&*) باشد.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                    Field("retype", "تکرار رمز جدید", third, errors, secret = true) { third = it }
                }
                "username" -> {
                    Field("username", "نام کاربری جدید", first, errors) { first = it }
                    Text(
                        "طول نام کاربری بین 6 تا 20 کاراکتر انگلیسی بوده و می‌تواند شامل حروف، اعداد و نقطه باشد. نام کاربری نمی‌تواند با نقطه آغاز یا پایان یابد.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
                "mobile" ->
                    Field("mobile", "شماره تلفن همراه جدید", first, errors, number = true) {
                        first = it
                    }
                else -> Field("email", "email@example.com", first, errors) { first = it }
            }
            Button(
                {
                    errors = credentialErrors(key, first, second, third)
                    if (errors.isEmpty()) stage = "otp"
                },
                Modifier.fillMaxWidth().testTag("edit-ok"),
            ) {
                Text("تایید")
            }
        }
    }
    when (stage) {
        "otp" -> OtpStep({ stage = "done" }, { stage = "form" })
        "done" -> MockResult("عملیات با موفقیت انجام شد.", onDone)
    }
}

internal fun credentialErrors(
    key: String,
    first: String,
    second: String,
    third: String,
): Map<String, String> = buildMap {
    val required = "تکمیل این فیلد اجباری است."
    when (key) {
        "password" -> {
            if (first.isEmpty()) put("current", required)
            if (!ProfileRules.password(second))
                put("new", "رمز جدید با شرایط گفته شده مطابقت ندارد.")
            if (third != second || third.isEmpty())
                put("retype", "تکرار رمز با رمز جدید یکسان نیست.")
        }
        "username" -> if (!ProfileRules.username(first)) put("username", "نام کاربری معتبر نیست.")
        "mobile" -> if (!ProfileRules.mobile(first)) put("mobile", "شماره تلفن همراه معتبر نیست.")
        else -> if (!ProfileRules.email(first)) put("email", "ایمیل معتبر نیست.")
    }
}

/** S-069: guide, copyable code; sharing and history (S-013) are outside FR-05. */
@Composable
private fun Invitation(onUnavailable: (String) -> Unit) {
    val context: Context = LocalContext.current
    var copied by rememberSaveable { mutableStateOf(false) }
    Text("کدت رو بفرست و امتیاز جمع کن!", fontSize = 16.sp)
    Text("با کد زیر، دوستانت رو به برنامه دعوت کن و پاداش بگیر.", fontSize = 14.sp)
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ProfileFixture.INVITE_CODE, Modifier.weight(1f).testTag("invite-code"))
        TextButton(
            {
                copyPreview(context, ProfileFixture.INVITE_CODE)
                copied = true
            },
            Modifier.testTag("invite-copy"),
        ) {
            Text("کپی")
        }
    }
    TextButton({ onUnavailable("سوابق دعوت‌های شما") }, Modifier.testTag("invite-history")) {
        Text("سوابق دعوت‌های شما")
    }
    Button({ onUnavailable("ارسال دعوت نامه") }, Modifier.fillMaxWidth()) {
        Text("ارسال دعوت نامه")
    }
    if (copied)
        AlertDialog(
            onDismissRequest = { copied = false },
            text = { Text("با موفقیت کپی شد.") },
            confirmButton = { TextButton({ copied = false }) { Text("بستن") } },
        )
}

/** S-071: local notification switches; turning one off asks first, as recovered. */
@Composable
private fun Notifications(toggles: List<String>, onToggles: (List<String>) -> Unit) {
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    LocalNote()
    ProfileFixture.notifications.forEach { item ->
        val key = "notify:$item"
        val on = key !in toggles
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("دریافت $item", Modifier.weight(1f), fontSize = 14.sp)
            Switch(
                on,
                { if (on) confirm = key else onToggles(toggles - key) },
                Modifier.testTag("notify-$item"),
            )
        }
    }
    confirm?.let { key ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            text = { Text("آیا مایل به غیرفعال کردن این اعلان هستید؟") },
            confirmButton = {
                TextButton(
                    {
                        onToggles(toggles + key)
                        confirm = null
                    },
                    Modifier.testTag("notify-off"),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
        )
    }
}

/** S-072/S-073: each change asks for the login password; it is never checked or stored. */
@Composable
private fun Facilities(page: String, toggles: List<String>, onToggles: (List<String>) -> Unit) {
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    Text(
        if (page == ProfileDestinations.SERVICES) "فعال/غیرفعال‌سازی خدمات مالی"
        else "فعال/غیرفعال‌سازی خدمات مالی در ورود با سنسور زیستی",
        fontSize = 14.sp,
    )
    LocalNote()
    ProfileFixture.facilities.forEach { item ->
        val key = "$page:$item"
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item, Modifier.weight(1f), fontSize = 14.sp)
            Switch(key !in toggles, { pending = key }, Modifier.testTag("facility-$item"))
        }
    }
    pending?.let { key ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("رمز ورود") },
            text = {
                Column {
                    OutlinedTextField(
                        password,
                        { password = it },
                        Modifier.testTag("facility-password"),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(
                        "رمز در این نسخه نمایشی بررسی نمی‌شود.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        onToggles(if (key in toggles) toggles - key else toggles + key)
                        pending = null
                        password = ""
                    },
                    Modifier.testTag("facility-ok"),
                    enabled = password.isNotEmpty(),
                ) {
                    Text("تایید")
                }
            },
            dismissButton = { TextButton({ pending = null }) { Text("انصراف") } },
        )
    }
}

/** S-464: saved addresses with local delete confirmation; add opens S-463. */
@Composable
private fun AddressList(
    addresses: List<PreviewAddress>,
    onAddresses: (List<PreviewAddress>) -> Unit,
    onOpen: (String) -> Unit,
) {
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    LocalNote()
    if (addresses.isEmpty()) Text("موردی برای نمایش وجود ندارد.")
    addresses.forEach { item ->
        Row(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("saved-${item.key}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 14.sp)
                Text(item.address, fontSize = 12.sp, color = Color.Gray)
                Text("کد پستی: ${item.postalCode}", fontSize = 12.sp, color = Color.Gray)
            }
            TextButton({ deleting = item.key }, Modifier.testTag("delete-${item.key}")) {
                Text("حذف")
            }
        }
    }
    Button(
        { onOpen(ProfileDestinations.ADD_ADDRESS) },
        Modifier.fillMaxWidth().testTag("address-add"),
    ) {
        Text("افزودن آدرس")
    }
    deleting?.let { key ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف آدرس") },
            text = { Text("آیا مایل به حذف آدرس مورد نظر هستید؟") },
            confirmButton = {
                TextButton(
                    {
                        onAddresses(addresses.filter { it.key != key })
                        deleting = null
                    },
                    Modifier.testTag("delete-ok"),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("انصراف") } },
        )
    }
}

/** S-463: title, province, city, 10-digit postal code and address; saved locally. */
@Composable
private fun AddAddress(onAdd: (PreviewAddress) -> Unit, onDone: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf<String?>(null) }
    var city by rememberSaveable { mutableStateOf<String?>(null) }
    var postal by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    Text("اطلاعات آدرس خود را با دقت وارد کنید", fontSize = 14.sp)
    LocalNote()
    Field("title", "عنوان آدرس", title, errors) { title = it }
    Picker(
        "addressProvince",
        "انتخاب استان",
        ProfileFixture.provinces.keys.toList(),
        province,
        errors["province"],
    ) {
        province = it
        city = null
    }
    Picker(
        "addressCity",
        "انتخاب شهر",
        ProfileFixture.provinces[province].orEmpty(),
        city,
        errors["city"],
    ) {
        city = it
    }
    Field("postal", "کد پستی", postal, errors, number = true) { postal = it.take(10) }
    Field("address", "آدرس", address, errors) { address = it }
    Buttons(
        true,
        "address-save",
        {
            errors = buildMap {
                if (title.isBlank()) put("title", "عنوان آدرس را وارد کنید")
                if (province == null) put("province", "لطفا استان مورد نظر را انتخاب کنید.")
                if (city == null) put("city", "لطفا شهر مورد نظر را انتخاب کنید.")
                if (!ProfileRules.postalCode(postal)) put("postal", "کد پستی 10 رقمی را وارد کنید")
                if (address.isBlank()) put("address", "تکمیل این فیلد اجباری است.")
            }
            if (errors.isEmpty()) {
                onAdd(
                    PreviewAddress(
                        "address-local-${latinDigits(postal)}",
                        title.trim(),
                        "$province، $city، ${address.trim()}",
                        postal,
                    )
                )
                onDone()
            }
        },
        onDone,
        "تایید و ثبت",
    )
}
