package org.ragbot.replica

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** VH-RECOVERY: vehicle menu, history menu, saved plate/contact/licence lists and history. */
@Composable
internal fun VehiclePage(
    page: String,
    scenario: String,
    menuKey: String,
    recordId: String,
    deleted: Set<String>,
    onMenu: (String) -> Unit,
    onRecord: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    val service = VehicleFixture.menu.find { it.first == menuKey }?.second ?: ""
    val title =
        when (page) {
            VehicleDestinations.MENU -> "خدمات خودرو"
            VehicleDestinations.HISTORY_MENU -> "سوابق پرداخت"
            VehicleDestinations.INQUIRY_HISTORY -> "سوابق استعلام"
            else -> service
        }
    PageFrame(page, title, onBack) {
        if (scenario != "populated") {
            PreviewDataState(scenario, onRetry)
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(VehicleFixture.NOTICE, style = MaterialTheme.typography.bodySmall)
                when (page) {
                    VehicleDestinations.MENU ->
                        VehicleFixture.menu.forEach { (key, label) ->
                            VehicleMenuRow("vehicle-menu-$key", label) {
                                onMenu(key)
                                onOpen(VehicleRules.target(key))
                            }
                        }
                    VehicleDestinations.HISTORY_MENU ->
                        VehicleFixture.historyMenu.forEach { (key, label) ->
                            VehicleMenuRow("vehicle-history-$key", label) { onUnavailable(label) }
                        }
                    VehicleDestinations.INQUIRY_HISTORY ->
                        VehicleFixture.records
                            .find { it.id == recordId }
                            ?.let {
                                VehicleRecordSummary(it)
                                // Designed for mock: one earlier inquiry for this record.
                                OutlinedCard(Modifier.fillMaxWidth().testTag("vehicle-inquiry")) {
                                    Column(Modifier.padding(12.dp)) {
                                        PaymentRow("خدمت", service)
                                        PaymentRow("تاریخ استعلام", "۱۴۰۵/۰۸/۰۱")
                                        PaymentRow("کد رهگیری", "DEMO-ITOLL-0001")
                                    }
                                }
                                Button(
                                    { onUnavailable("نتیجه استعلام $service") },
                                    Modifier.fillMaxWidth().testTag("vehicle-inquiry-result"),
                                ) {
                                    Text("مشاهده نتیجه")
                                }
                            }
                    else -> {
                        val kind =
                            when (page) {
                                VehicleDestinations.PLATES -> "plate"
                                VehicleDestinations.CONTACTS -> "contact"
                                else -> "licence"
                            }
                        VehicleList(
                            kind,
                            menuKey,
                            service,
                            deleted,
                            onRecord,
                            onDelete,
                            onOpen,
                            onUnavailable,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleMenuRow(tag: String, label: String, onClick: () -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth().testTag(tag).clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(label, Modifier.padding(16.dp))
    }
}

@Composable
private fun VehicleList(
    kind: String,
    menuKey: String,
    service: String,
    deleted: Set<String>,
    onRecord: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    Text(
        if (kind == "contact") "مخاطب خدمت را انتخاب کنید."
        else "یکی از اطلاعات ثبت‌شده خود را انتخاب کنید."
    )
    val rows = VehicleRules.records(kind, menuKey, deleted)
    if (rows.isEmpty()) Text("موردی برای نمایش وجود ندارد.", Modifier.testTag("vehicle-empty"))
    rows.forEach { record ->
        OutlinedCard(
            Modifier.fillMaxWidth().testTag("vehicle-${record.id}").clickable(role = Role.Button) {
                // Selecting continues to the inquiry form, which is not built (FR-17/18).
                onUnavailable("استعلام $service")
            }
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VehicleRecordSummary(record)
                Row {
                    TextButton(
                        {
                            onRecord(record.id)
                            onOpen(VehicleDestinations.INQUIRY_HISTORY)
                        },
                        Modifier.testTag("vehicle-history-${record.id}"),
                    ) {
                        Text("سوابق استعلام")
                    }
                    TextButton(
                        { confirm = record.id },
                        Modifier.testTag("vehicle-delete-${record.id}"),
                    ) {
                        Text("حذف")
                    }
                }
            }
        }
    }
    OutlinedButton(
        { onUnavailable("استعلام $service") },
        Modifier.fillMaxWidth().testTag("vehicle-new"),
    ) {
        Text(
            when (kind) {
                "plate" -> "پلاک جدید"
                "contact" -> "مخاطب جدید"
                else -> "گواهینامه جدید"
            }
        )
    }
    confirm?.let { id ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            text = { Text("آیا مایل به حذف این مورد هستید؟") },
            confirmButton = {
                TextButton(
                    {
                        onDelete(id)
                        confirm = null
                    },
                    Modifier.testTag("vehicle-delete-confirm"),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun VehicleRecordSummary(record: VehicleRecord) {
    when (record.kind) {
        "plate" -> VehiclePlate(record)
        "licence" -> PaymentRow("شماره گواهینامه", record.licence)
        else -> PaymentRow("کد ملی", record.ownerCode)
    }
    PaymentRow("تلفن همراه", record.ownerMobile)
    Text(VehicleRules.ownership(record), fontSize = 13.sp, color = ServiceGreen)
}

/** Iranian plate fragments laid out left to right inside the RTL page. */
@Composable
private fun VehiclePlate(record: VehicleRecord) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.testTag("vehicle-plate-${record.id}")
                .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (record.automobile) {
                val (first, letter, second, region) = record.plate
                Text(first)
                Text(letter)
                Text(second)
                Text("ایران $region")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(record.plate[0])
                    Text(record.plate[1])
                }
            }
        }
    }
}
