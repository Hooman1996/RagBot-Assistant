package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class LoanGroup(
    val key: String,
    val title: String,
    val details: List<String>,
    val active: Boolean = true,
    val accountOpenElapsed: Boolean = true,
    val needsCredit: Boolean = false,
)

internal data class LoanGroupOption(
    val key: String,
    val title: String,
    val amountRial: Long,
    val active: Boolean = true,
    val availableMonths: Int = 12,
    val maximumMonths: Int = 12,
    val subtitle: String? = null,
    val details: List<String> = emptyList(),
) {
    val warningMonths: Int?
        get() = availableMonths.takeIf { active && it < maximumMonths }
}

/** LG-FIXTURE: synthetic catalogue and eligibility examples; Designed for mock. */
internal object LoanGroupFixture {
    val groups =
        listOf(
            LoanGroup(
                "standard",
                "گروه تسهیلات نمونه",
                listOf("نمایش شرایط ساختگی", "بررسی گزینه‌های بازپرداخت"),
            ),
            LoanGroup(
                "inactive",
                "گروه غیرفعال نمونه",
                listOf("این گروه در دسترس نیست."),
                active = false,
            ),
            LoanGroup(
                "recent",
                "حساب تازه نمونه",
                listOf("بررسی مدت افتتاح حساب"),
                accountOpenElapsed = false,
            ),
            LoanGroup(
                "credit",
                "گروه نیازمند استعلام نمونه",
                listOf("نیازمند بررسی رتبه اعتباری"),
                needsCredit = true,
            ),
        )
    val options =
        listOf(
            LoanGroupOption(
                "normal",
                "گزینه عادی نمونه",
                100_000_000L,
                subtitle = "بازپرداخت نمونه",
                details = listOf("شرایط صرفاً برای نمایش است."),
            ),
            LoanGroupOption(
                "limited",
                "گزینه با دوره محدود نمونه",
                100_000_000L,
                availableMonths = 6,
                details = listOf("دوره در دسترس بر اساس امتیاز نمونه محدود است."),
            ),
            LoanGroupOption("inactive", "گزینه غیرفعال نمونه", 200_000_000L, active = false),
        )

    fun unavailableReason(group: LoanGroup): String? =
        when {
            !group.active -> "این گروه در دسترس نیست."
            !group.accountOpenElapsed -> "مدت لازم از زمان افتتاح حساب نگذشته است."
            group.needsCredit -> "استعلام رتبه اعتباری و انتخاب طرح در این پیش‌نمایش در دسترس نیست."
            else -> null
        }
}

/** LG-GROUP: Recovered catalogue/eligibility presentation; later application steps unavailable. */
@Composable
internal fun LoanGroupContent(
    page: String,
    groupKey: String,
    onGroup: (String) -> Unit,
    onOpen: (String) -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Text("گروه‌ها و شرایط نمایش داده‌شده ساختگی است.", fontSize = 11.sp, color = Color.Gray)
    if (page == "S-307") {
        LoanGroupFixture.groups.forEach { group ->
            Surface(
                Modifier.fillMaxWidth().testTag("loan-group-${group.key}").clickable(
                    enabled = group.active,
                    role = Role.Button,
                ) {
                    val reason = LoanGroupFixture.unavailableReason(group)
                    if (reason != null) onUnavailable(reason)
                    else {
                        onGroup(group.key)
                        onOpen("S-309")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(if (group.active) Color(0xFFE7F3E9) else Color(0xFFEEEEEE))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("▤", color = if (group.active) ServiceGreen else Color.Gray)
                        Text(
                            group.title,
                            Modifier.weight(1f).padding(horizontal = 8.dp),
                            fontSize = 14.sp,
                            color = if (group.active) Color.Black else Color.Gray,
                        )
                        Text("‹", color = if (group.active) ServiceGreen else Color.Gray)
                    }
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        group.details.forEach {
                            Text("• $it", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    } else {
        val group = LoanGroupFixture.groups.find { it.key == groupKey }
        if (group == null || LoanGroupFixture.unavailableReason(group) != null) {
            Text("گروه قابل نمایش نیست.", Modifier.testTag("loan-group-missing"))
            return
        }
        Text(
            group.title,
            Modifier.testTag("loan-group-detail-title"),
            style = MaterialTheme.typography.titleMedium,
        )
        LoanGroupFixture.options
            .groupBy { it.amountRial }
            .forEach { (amountRial, options) ->
                Text("مبلغ تسهیلات: ${persianNumber(amountRial)} ریال", fontSize = 13.sp)
                options.forEach { option ->
                    Surface(
                        Modifier.fillMaxWidth().testTag("loan-option-${option.key}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (option.active) Color.White else Color(0xFFEEEEEE),
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                option.title,
                                color = if (option.active) Color.Black else Color.Gray,
                            )
                            option.subtitle?.let { Text(it, fontSize = 13.sp) }
                            option.details.forEach { Text(it, fontSize = 12.sp) }
                            Text(
                                "حداکثر دوره: ${persianNumber(option.maximumMonths.toLong())} ماه",
                                fontSize = 12.sp,
                            )
                            option.warningMonths?.let {
                                Text(
                                    "دوره بازپرداخت بر اساس امتیاز: ${persianNumber(it.toLong())} ماه",
                                    Modifier.testTag("loan-option-warning-${option.key}"),
                                    color = Color(0xFF925C00),
                                    fontSize = 12.sp,
                                )
                            }
                            TextButton(
                                {
                                    onUnavailable(
                                        "محاسبه و درخواست تسهیلات در این پیش‌نمایش در دسترس نیست."
                                    )
                                },
                                Modifier.testTag("loan-option-open-${option.key}"),
                                enabled = option.active,
                            ) {
                                Text(
                                    if (option.active) "محاسبه و درخواست · در دسترس نیست"
                                    else "غیرفعال"
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
    }
}
