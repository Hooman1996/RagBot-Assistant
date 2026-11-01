package org.ragbot.replica

internal object VehicleDestinations {
    const val MENU = "S-253"
    const val HISTORY_MENU = "S-254"
    const val PLATES = "S-256"
    const val CONTACTS = "S-246"
    const val LICENCES = "S-248"
    const val INQUIRY_HISTORY = "S-252"
    // The host S-262, the back-handler wrappers S-259/S-260 and the nested graphs S-263–S-273
    // are not pages.
    val implemented = setOf(MENU, HISTORY_MENU, PLATES, CONTACTS, LICENCES, INQUIRY_HISTORY)
}

/** VH-RECOVERY: a saved plate, contact or licence; [mine] is the CUSTOMER ownership. */
internal data class VehicleRecord(
    val id: String,
    val kind: String,
    val mine: Boolean,
    val ownerCode: String,
    val ownerMobile: String,
    // Automobile: two digits, letter, three digits, region. Motorcycle: three and five digits.
    // The original's single plate-number encoding is Unknown, so the parts are kept apart.
    val plate: List<String> = emptyList(),
    val automobile: Boolean = true,
    val licence: String = "",
)

internal object VehicleRules {
    /**
     * Recovered: each service key opens the plate, contact or licence list, or the history menu.
     */
    fun target(key: String): String =
        when (key) {
            "AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY",
            "AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY" -> VehicleDestinations.CONTACTS
            "AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY" -> VehicleDestinations.LICENCES
            "AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY" -> VehicleDestinations.HISTORY_MENU
            else -> VehicleDestinations.PLATES
        }

    /** Recovered: the pollution-area inquiry lists automobile plates only. */
    fun records(kind: String, menuKey: String, deleted: Set<String>): List<VehicleRecord> =
        VehicleFixture.records.filter {
            it.kind == kind &&
                it.id !in deleted &&
                (menuKey != "AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY" || it.automobile)
        }

    fun ownership(record: VehicleRecord) = if (record.mine) "(خودم)" else "(دیگران)"
}

internal object VehicleFixture {
    const val NOTICE = "پیش‌نمایش محلی با دادهٔ ساختگی؛ هیچ استعلام یا پرداختی ارسال نمی‌شود."

    // Recovered keys of the vehicle menu; titles are server-supplied, so synthetic here.
    val menu =
        listOf(
            "AUTHENTICATED_HOME_ANNUAL_TOLL" to "عوارض سالیانه",
            "AUTHENTICATED_HOME_TRAFFIC_TOLL" to "عوارض طرح ترافیک",
            "AUTHENTICATED_HOME_FREEWAY_TOLL" to "عوارض آزادراه",
            "AUTHENTICATED_HOME_TRANSFER_TOLL" to "مالیات نقل و انتقال",
            "AUTHENTICATED_HOME_RAHVAR_TOLL" to "خلافی خودرو",
            "AUTHENTICATED_HOME_VEHICLE_DOC_INQUIRY" to "مدارک خودرو",
            "AUTHENTICATED_HOME_POLLUTION_AREA_INQUIRY" to "طرح آلودگی هوا",
            "AUTHENTICATED_HOME_ACTIVE_PLATES_INQUIRY" to "پلاک‌های فعال",
            "AUTHENTICATED_HOME_DRIVING_LICENSE_INQUIRY" to "وضعیت گواهینامه",
            "AUTHENTICATED_HOME_NEGATIVE_POINT_INQUIRY" to "نمره منفی گواهینامه",
            "AUTHENTICATED_HOME_PAYMENT_TOLL_HISTORY" to "سوابق پرداخت",
        )

    // Recovered history keys; their targets are unproved, so they show notices.
    val historyMenu =
        listOf(
            "AUTHENTICATED_HOME_RAHVAR_TOLL_HISTORY" to "سوابق خلافی",
            "AUTHENTICATED_HOME_ANNUAL_TOLL_HISTORY" to "سوابق عوارض سالیانه",
            "AUTHENTICATED_HOME_TRAFFIC_TOLL_HISTORY" to "سوابق طرح ترافیک",
            "AUTHENTICATED_HOME_TRANSFER_TOLL_HISTORY" to "سوابق مالیات نقل و انتقال",
            "AUTHENTICATED_HOME_FREEWAY_TOLL_HISTORY" to "سوابق آزادراه",
        )

    val records =
        listOf(
            VehicleRecord(
                "plate-1",
                "plate",
                true,
                "0000000001",
                "09000000001",
                listOf("12", "ب", "345", "67"),
            ),
            VehicleRecord(
                "plate-2",
                "plate",
                false,
                "0000000002",
                "09000000002",
                listOf("123", "45678"),
                automobile = false,
            ),
            VehicleRecord("contact-1", "contact", true, "0000000001", "09000000001"),
            VehicleRecord("contact-2", "contact", false, "0000000003", "09000000003"),
            VehicleRecord(
                "licence-1",
                "licence",
                true,
                "0000000001",
                "09000000001",
                licence = "9000000001",
            ),
        )
}
