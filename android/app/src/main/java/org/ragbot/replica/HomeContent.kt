package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val HomeGreen = Color(0xFF51D674)
internal val ServiceGreen = Color(0xFF308A48)

@Composable
internal fun HomeContent(
    services: List<HomeMenuItem>,
    onMenu: (HomeMenuItem) -> Unit,
    onEdit: () -> Unit,
    onTransaction: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag(HomeDestinations.MENU)) {
        MenuRow(HomeFixture.top, "top", onMenu)
        Spacer(Modifier.height(16.dp))
        MenuRow(HomeFixture.quick, "quick", onMenu)
        Spacer(Modifier.height(8.dp))
        PerformanceBanner { onMenu(HomeFixture.top.last()) }
        Spacer(Modifier.height(8.dp))
        services.chunked(4).forEach { row ->
            MenuRow(row, "service", onMenu)
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Column(
                Modifier.fillMaxWidth(.25f)
                    .heightIn(min = 80.dp)
                    .clickable(role = Role.Button, onClick = onEdit)
                    .testTag("edit-layout"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    ReplicaIcon("replica_edit_layout", "⇄", Modifier.size(36.dp), Color.Unspecified)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "ویرایش چیدمان",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = replicaFont("replica_medium"),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            replicaRecentHeading(),
            fontFamily = replicaFont("replica_medium"),
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Start).testTag("recent-heading"),
        )
        Spacer(Modifier.height(16.dp))
        HomeFixture.transactions.forEach { transaction ->
            TransactionRow(transaction, onTransaction)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuRow(items: List<HomeMenuItem>, group: String, onMenu: (HomeMenuItem) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("group-$group"), verticalAlignment = Alignment.Top) {
        items.forEach { item ->
            Column(
                Modifier.weight(1f)
                    .heightIn(min = if (group == "quick") 82.dp else 80.dp)
                    .clickable(role = Role.Button) { onMenu(item) }
                    .testTag("menu-${item.key}")
                    .semantics(mergeDescendants = true) { contentDescription = item.title },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (group) {
                    "quick" ->
                        Box(
                            Modifier.size(62.dp).background(Color(0xFFE3F4E9), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            ReplicaIcon(item.artwork, "◇", Modifier.size(36.dp), Color.Unspecified)
                        }
                    "top" -> ReplicaIcon(item.artwork, "◇", Modifier.size(60.dp), Color.Unspecified)
                    else ->
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            ReplicaIcon(item.artwork, "◇", Modifier.size(36.dp), Color.Unspecified)
                        }
                }
                if (group != "top") Spacer(Modifier.height(8.dp))
                Text(
                    item.title,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = replicaFont("replica_medium"),
                    maxLines = 1,
                )
                if (group == "top") Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PerformanceBanner(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .height(100.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFFE85A19), Color(0xFF800118))),
                RoundedCornerShape(12.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("performance-banner")
            .semantics(mergeDescendants = true) {
                stateDescription = "۹ نشانگر؛ فقط بنر مرجع در پیش‌نمایش موجود است"
            }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "عملکرد مالی",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = replicaFont("replica_bold"),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "با نمایش گرافیکی و تحلیل جریان مالی به\nتفکیک نوع تراکنش.",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                )
            }
            ReplicaIcon(
                "replica_banner_performance",
                "▥",
                Modifier.width(96.dp).height(72.dp),
                Color.Unspecified,
            )
        }
        Row(
            Modifier.align(Alignment.CenterHorizontally).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(9) { index ->
                Box(
                    Modifier.size(6.dp)
                        .background(if (index == 5) Color.White else Color(0xFFC5C5C5), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: PreviewTransaction, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("transaction-${transaction.key}")
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFEDEDED), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ReplicaIcon("replica_transaction_success", "✓", Modifier.size(24.dp), HomeGreen)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    "موفق",
                    color = HomeGreen,
                    fontSize = 13.sp,
                    fontFamily = replicaFont("replica_medium"),
                )
                Text(" - ${transaction.title}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text("از ${transaction.source}", color = Color.Gray, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(transaction.time, color = Color.Gray, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${persianNumber(transaction.amount)} ریال",
                fontFamily = replicaFont("replica_medium"),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("به ${transaction.destination}", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
