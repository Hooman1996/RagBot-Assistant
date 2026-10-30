package org.ragbot.replica

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ShortcutPage(
    scenario: String,
    pinnedIds: List<String>,
    onPinsChanged: (List<String>) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val records =
        if (scenario == "empty") emptyList() else ShortcutFixture.visible(query, pinnedIds)
    PageFrame(ShortcutDestinations.SCREEN, "جستجوی خدمات", onBack) {
        Column(Modifier.fillMaxSize().testTag("shortcut-page")) {
            Text(
                "پیش‌نمایش محلی • خدمات ساختگی؛ سنجاق‌کردن فقط در این نسخه است.",
                Modifier.fillMaxWidth().padding(12.dp),
                color = ServiceGreen,
                fontSize = 12.sp,
            )
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("shortcut-search"),
                label = { Text("جستجوی عنوان خدمت") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    if (query.isBlank()) "علاقه‌مندی‌ها" else "نتایج جستجو",
                    Modifier.padding(horizontal = 12.dp).testTag("shortcut-mode"),
                    color = Color.Gray,
                    fontSize = 14.sp,
                )
                HorizontalDivider(Modifier.weight(1f))
            }
            if (scenario == "loading" || scenario == "error") {
                PreviewDataState(scenario, onRetry)
            } else if (records.isEmpty()) {
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("shortcut-empty"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ReplicaIcon(
                        if (query.isBlank()) "replica_shortcut_empty"
                        else "replica_shortcut_no_result",
                        "☆",
                        Modifier.size(196.dp).semantics {
                            contentDescription =
                                if (query.isBlank()) "خدمت سنجاق‌شده‌ای وجود ندارد"
                                else "نتیجه‌ای یافت نشد"
                        },
                        Color.Unspecified,
                    )
                    if (!BuildConfig.PRIVATE_RESOURCES)
                        Text(
                            if (query.isBlank()) "خدمت سنجاق‌شده‌ای وجود ندارد"
                            else "نتیجه‌ای یافت نشد"
                        )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("shortcut-list"),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        val pinned = record.id in pinnedIds
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("shortcut-item-${record.id}")
                                .clickable {
                                    keyboard?.hide()
                                    onUnavailable(
                                        "مقصد این خدمت ساختگی بدون نگاشت منوی سرویس در دسترس نیست."
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(record.title, Modifier.weight(1f).padding(12.dp), fontSize = 14.sp)
                            IconButton(
                                { onPinsChanged(ShortcutFixture.toggle(pinnedIds, record.id)) },
                                Modifier.testTag("shortcut-pin-${record.id}").semantics {
                                    contentDescription =
                                        (if (pinned) "برداشتن سنجاق " else "سنجاق‌کردن ") +
                                            record.title
                                    stateDescription = if (pinned) "سنجاق‌شده" else "سنجاق‌نشده"
                                },
                            ) {
                                ReplicaIcon(
                                    if (pinned && query.isNotBlank()) "replica_shortcut_unpin"
                                    else "replica_shortcut_pin",
                                    if (pinned) "−" else "+",
                                    Modifier.size(24.dp),
                                    if (pinned && query.isNotBlank()) Color.Gray else ServiceGreen,
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE5E5E5))
                    }
                }
            }
        }
    }
}
