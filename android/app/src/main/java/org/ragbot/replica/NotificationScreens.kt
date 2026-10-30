package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationPage(
    scenario: String,
    readIds: List<String>,
    deletedIds: List<String>,
    onRead: (List<String>) -> Unit,
    onDelete: (List<String>) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
    hasNotice: Boolean,
) {
    var category by rememberSaveable { mutableStateOf("all") }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectMode by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var deleteMode by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable(category) { mutableIntStateOf(1) }
    val listState = rememberLazyListState()
    val records = NotificationFixture.visible(deletedIds, category)
    val loaded = records.take(page * 3)
    val lastVisible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }
    LaunchedEffect(lastVisible, loaded.size) {
        if (lastVisible >= loaded.lastIndex && loaded.size < records.size) page++
    }
    fun markRead(ids: List<String>) {
        onRead((readIds + ids).distinct())
    }
    fun askDelete(ids: List<String>, mode: String) {
        pendingDelete = ids
        deleteMode = mode
    }
    BackHandler(!hasNotice && selectMode && detailId == null && pendingDelete.isEmpty()) {
        selectMode = false
        selected = emptyList()
    }
    // Own transient sheets when Back reaches the Activity dispatcher as well.
    BackHandler(!hasNotice && (detailId != null || pendingDelete.isNotEmpty())) {
        if (pendingDelete.isNotEmpty()) pendingDelete = emptyList() else detailId = null
    }
    PageFrame("S-075", "اعلان‌ها", onBack) {
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else {
            Text(
                "داده ساختگی؛ خواندن و حذف فقط در پیش‌نمایش محلی است.",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Gray,
                fontSize = 11.sp,
            )
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(NotificationFixture.categories.entries.toList()) { (key, title) ->
                    FilterChip(
                        category == key,
                        {
                            category = key
                            selected = emptyList()
                            detailId = null
                        },
                        { Text(title) },
                        Modifier.testTag("notification-filter-$key"),
                    )
                }
            }
            Text(
                "${NotificationFixture.unreadCount(readIds, deletedIds)} اعلان دیده نشده",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("notification-unread-count"),
                fontSize = 12.sp,
            )
            if (records.isEmpty()) Box(Modifier.weight(1f)) { PreviewDataState("empty", onRetry) }
            else
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("notification-list"),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(loaded, key = { it.id }) { record ->
                        val unread = !record.read && record.id !in readIds
                        Surface(
                            Modifier.fillMaxWidth()
                                .testTag("notification-item-${record.id}")
                                .clickable {
                                    if (selectMode)
                                        selected =
                                            if (record.id in selected) selected - record.id
                                            else selected + record.id
                                    else {
                                        detailId = record.id
                                        if (unread) markRead(listOf(record.id))
                                    }
                                },
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectMode)
                                        Checkbox(
                                            record.id in selected,
                                            { checked ->
                                                selected =
                                                    if (checked) selected + record.id
                                                    else selected - record.id
                                            },
                                            Modifier.testTag("notification-select-${record.id}")
                                                .semantics {
                                                    contentDescription = "انتخاب ${record.title}"
                                                },
                                        )
                                    if (unread)
                                        Box(
                                            Modifier.padding(end = 8.dp)
                                                .size(8.dp)
                                                .background(Color(0xFFFFA942), CircleShape)
                                                .testTag("notification-unread-${record.id}")
                                                .semantics { contentDescription = "خوانده نشده" }
                                        )
                                    Text(record.title, Modifier.weight(1f), fontSize = 14.sp)
                                    if (!selectMode)
                                        IconButton(
                                            { askDelete(listOf(record.id), "single") },
                                            Modifier.size(48.dp)
                                                .testTag("notification-delete-${record.id}")
                                                .semantics {
                                                    contentDescription = "حذف ${record.title}"
                                                },
                                        ) {
                                            ReplicaIcon(
                                                "replica_card_delete",
                                                "×",
                                                Modifier.size(20.dp),
                                                Color.Gray,
                                            )
                                        }
                                }
                                Text(
                                    record.message,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${record.date}  ${record.time}",
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    style = TextStyle(textDirection = TextDirection.Content),
                                )
                            }
                        }
                    }
                }
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp)) {
                if (selectMode)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            records.isNotEmpty() && records.all { it.id in selected },
                            { checked ->
                                selected = if (checked) records.map { it.id } else emptyList()
                            },
                            Modifier.testTag("notification-select-all").semantics {
                                contentDescription = "انتخاب همه اعلان‌ها"
                            },
                        )
                        Text("انتخاب همه", fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            { askDelete(selected, "group") },
                            Modifier.testTag("notification-delete-selected"),
                            enabled = selected.isNotEmpty(),
                        ) {
                            Text("حذف انتخاب شده‌ها", fontSize = 12.sp)
                        }
                    }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        {
                            selectMode = !selectMode
                            selected = emptyList()
                        },
                        Modifier.testTag("notification-select"),
                        enabled = records.isNotEmpty() || selectMode,
                    ) {
                        Text(if (selectMode) "انصراف" else "انتخاب", fontSize = 12.sp)
                    }
                    TextButton(
                        { markRead(records.map { it.id }) },
                        Modifier.testTag("notification-read-all"),
                        enabled = records.any { !it.read && it.id !in readIds },
                    ) {
                        Text("خواندن همه", fontSize = 12.sp)
                    }
                    TextButton(
                        {
                            askDelete(
                                records.map { it.id },
                                if (category == "all") "all" else "category",
                            )
                        },
                        Modifier.testTag("notification-delete-all"),
                        enabled = records.isNotEmpty(),
                    ) {
                        Text("حذف همه", fontSize = 12.sp)
                    }
                }
            }
        }
    }
    val detail = NotificationFixture.records.find { it.id == detailId && it.id !in deletedIds }
    if (detail != null) {
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { detailId = null },
            modifier = Modifier.testTag("notification-detail"),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("جزئیات اعلان", fontSize = 18.sp)
                Text(
                    checkNotNull(NotificationFixture.categories[detail.category]),
                    color = ServiceGreen,
                    fontSize = 12.sp,
                )
                Text(detail.title, fontSize = 16.sp)
                Text("${detail.date}  ${detail.time}", color = Color.Gray, fontSize = 12.sp)
                Text(
                    detail.message,
                    fontSize = 14.sp,
                    style = TextStyle(textDirection = TextDirection.Content),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        {
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText(
                                            "اعلان آزمایشی",
                                            detail.message,
                                        )
                                    )
                                )
                            }
                        },
                        Modifier.testTag("notification-copy"),
                    ) {
                        Text("کپی")
                    }
                    TextButton(
                        { onUnavailable("اشتراک‌گذاری اعلان") },
                        Modifier.testTag("notification-share"),
                    ) {
                        Text("اشتراک‌گذاری")
                    }
                    TextButton(
                        { detailId = null },
                        Modifier.testTag("notification-detail-dismiss"),
                    ) {
                        Text("بستن")
                    }
                }
            }
        }
    }
    if (pendingDelete.isNotEmpty())
        ModalBottomSheet(
            onDismissRequest = { pendingDelete = emptyList() },
            modifier = Modifier.testTag("notification-delete-sheet"),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("حذف اعلان", fontSize = 18.sp)
                Text(
                    when (deleteMode) {
                        "single" -> "آیا از حذف اعلان اطمینان دارید؟"
                        "group" -> "آیا از حذف اعلان‌های انتخاب شده اطمینان دارید؟"
                        "category" ->
                            "آیا از حذف همه اعلان‌های ${NotificationFixture.categories[category]} اطمینان دارید؟"
                        else -> "آیا از حذف همه اعلان‌ها اطمینان دارید؟"
                    }
                )
                Text("فقط داده پیش‌نمایش محلی حذف می‌شود.", color = Color.Gray, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        {
                            onDelete((deletedIds + pendingDelete).distinct())
                            selected = selected - pendingDelete.toSet()
                            pendingDelete = emptyList()
                        },
                        Modifier.weight(1f).testTag("notification-delete-confirm"),
                    ) {
                        Text("حذف")
                    }
                    OutlinedButton(
                        { pendingDelete = emptyList() },
                        Modifier.weight(1f).testTag("notification-delete-cancel"),
                    ) {
                        Text("انصراف")
                    }
                }
            }
        }
}
