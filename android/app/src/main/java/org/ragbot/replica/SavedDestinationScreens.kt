package org.ragbot.replica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavedDestinationScreen(
    screenId: String,
    scenario: String,
    records: List<SavedDestination>,
    onRecordsChanged: (List<SavedDestination>) -> Unit,
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onUnavailable: (String) -> Unit,
    hasNotice: Boolean = false,
) {
    val legacy = screenId == SavedDestinationRoutes.LEGACY
    val picker = screenId == SavedDestinationRoutes.PICKER
    var type by rememberSaveable(screenId) { mutableStateOf(if (legacy) "ACCOUNT" else "LOAN") }
    var query by rememberSaveable(screenId, type) { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var orderMode by rememberSaveable { mutableStateOf(false) }
    var draftOrder by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val listState = rememberLazyListState()
    val visible = SavedDestinationFixture.visible(records, type, query)
    val displayed =
        if (orderMode) draftOrder.mapNotNull { id -> visible.find { it.id == id } } else visible
    val latestOrder by rememberUpdatedState(draftOrder)
    fun closeOrBack() {
        when {
            deleteId != null -> deleteId = null
            editing -> editing = false
            orderMode -> orderMode = false
            else -> onBack()
        }
    }
    BackHandler(!hasNotice && (deleteId != null || editing || orderMode)) { closeOrBack() }
    fun edit(record: SavedDestination?) {
        editingId = record?.id
        name = record?.name.orEmpty()
        number = record?.destination.orEmpty()
        description = record?.description.orEmpty()
        error = null
        editing = true
    }
    fun move(id: String, delta: Int): Boolean {
        val from = latestOrder.indexOf(id)
        val target = from + delta
        if (from < 0 || target !in latestOrder.indices) return false
        draftOrder =
            latestOrder.toMutableList().also { java.util.Collections.swap(it, from, target) }
        return true
    }
    PageFrame(screenId, if (picker) "انتخاب مقصد آشنا" else "مقصدهای آشنا", { closeOrBack() }) {
        Text(
            "داده ساختگی؛ تغییرات فقط در پیش‌نمایش محلی نگهداری می‌شود.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 11.sp,
            color = Color.Gray,
        )
        if (legacy) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                items(SavedDestinationFixture.types.entries.toList()) { (key, title) ->
                    FilterChip(
                        type == key,
                        {
                            type = key
                            orderMode = false
                        },
                        { Text(title) },
                        Modifier.padding(horizontal = 4.dp).testTag("saved-type-$key"),
                        enabled = !orderMode,
                    )
                }
            }
        } else Text("وام", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (scenario != "populated") PreviewDataState(scenario, onRetry)
        else {
            if (!orderMode && (records.count { it.type == type } > 5 || query.isNotEmpty())) {
                OutlinedTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("saved-search"),
                    label = { Text("جستجوی نام یا شماره") },
                    singleLine = true,
                )
            }
            if (legacy && !orderMode)
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    TextButton({ edit(null) }, Modifier.testTag("saved-add")) {
                        Text("${SavedDestinationFixture.types[type]} جدید +")
                    }
                    Spacer(Modifier.weight(1f))
                    if (records.count { it.type == type } > 1)
                        TextButton(
                            {
                                query = ""
                                draftOrder = records.filter { it.type == type }.map { it.id }
                                orderMode = true
                            },
                            Modifier.testTag("saved-edit-order"),
                        ) {
                            Text("ویرایش ترتیب")
                        }
                }
            if (orderMode)
                Text(
                    "با نگه‌داشتن و جابه‌جایی آیتم‌های زیر می‌توانید پرکاربردها را مرتب کنید.",
                    Modifier.padding(16.dp),
                    fontSize = 12.sp,
                )
            if (displayed.isEmpty()) Box(Modifier.weight(1f)) { PreviewDataState("empty", onRetry) }
            else
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().testTag("saved-list"),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayed, key = { it.id }) { record ->
                        var dragY by remember(record.id) { mutableFloatStateOf(0f) }
                        var revealed by rememberSaveable(record.id) { mutableStateOf(false) }
                        val dragModifier =
                            if (orderMode)
                                Modifier.pointerInput(record.id, orderMode) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { dragY = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragY += amount.y
                                                val row =
                                                    listState.layoutInfo.visibleItemsInfo.find {
                                                        it.key == record.id
                                                    }
                                                val threshold = row?.size?.toFloat() ?: 100f
                                                if (kotlin.math.abs(dragY) > threshold / 2f) {
                                                    val delta = if (dragY > 0f) 1 else -1
                                                    if (move(record.id, delta)) dragY = 0f
                                                }
                                            },
                                        )
                                    }
                                    .semantics {
                                        contentDescription = "جابه‌جایی ${record.name}"
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction("انتقال به بالا") {
                                                    move(record.id, -1)
                                                },
                                                CustomAccessibilityAction("انتقال به پایین") {
                                                    move(record.id, 1)
                                                },
                                            )
                                    }
                            else
                                Modifier.pointerInput(record.id) {
                                        var distance = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { distance = 0f },
                                            onHorizontalDrag = { change, amount ->
                                                change.consume()
                                                distance += amount
                                                if (kotlin.math.abs(distance) > 40.dp.toPx())
                                                    revealed = true
                                            },
                                        )
                                    }
                                    .semantics {
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction("نمایش گزینه‌ها") {
                                                    revealed = true
                                                    true
                                                }
                                            )
                                    }
                        Surface(
                            Modifier.fillMaxWidth()
                                .then(dragModifier)
                                .testTag("saved-item-${record.id}")
                                .then(
                                    if (picker && !orderMode)
                                        Modifier.clickable { onSelected(record.id) }
                                    else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(record.name, fontSize = 15.sp)
                                Text(
                                    record.destination,
                                    style =
                                        TextStyle(
                                            textDirection = TextDirection.Ltr,
                                            fontSize = 14.sp,
                                        ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                )
                                Text(record.description, fontSize = 12.sp, color = Color.Gray)
                                if (!orderMode && revealed)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        if (!picker)
                                            TextButton(
                                                {
                                                    if (legacy) edit(record)
                                                    else
                                                        onUnavailable(
                                                            "ویرایش این وام در پیش‌نمایش در دسترس نیست."
                                                        )
                                                },
                                                Modifier.testTag("saved-edit-${record.id}"),
                                            ) {
                                                Text("ویرایش")
                                            }
                                        TextButton(
                                            { deleteId = record.id },
                                            Modifier.testTag("saved-delete-${record.id}"),
                                        ) {
                                            Text("حذف", color = Color(0xFFB3261E))
                                        }
                                    }
                            }
                        }
                    }
                }
            if (orderMode)
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        {
                            val ordered =
                                draftOrder
                                    .mapNotNull { id -> records.find { it.id == id } }
                                    .iterator()
                            onRecordsChanged(
                                records.map { if (it.type == type) ordered.next() else it }
                            )
                            orderMode = false
                        },
                        Modifier.weight(1f).testTag("saved-order-apply"),
                    ) {
                        Text("اعمال تغییرات محلی")
                    }
                    OutlinedButton({ orderMode = false }, Modifier.testTag("saved-order-cancel")) {
                        Text("انصراف")
                    }
                }
        }
    }
    if (deleteId != null)
        ModalBottomSheet(onDismissRequest = { deleteId = null }) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    if (legacy) "آیا مایل به حذف مقصد مورد نظر هستید؟"
                    else "آیا مایل به حذف شماره تسهیلات مورد نظر هستید؟"
                )
                Text(
                    records.find { it.id == deleteId }?.name.orEmpty(),
                    Modifier.padding(vertical = 12.dp),
                )
                Button(
                    {
                        onRecordsChanged(records.filterNot { it.id == deleteId })
                        deleteId = null
                    },
                    Modifier.fillMaxWidth().testTag("saved-delete-confirm"),
                ) {
                    Text("حذف فقط از پیش‌نمایش محلی")
                }
                TextButton(
                    { deleteId = null },
                    Modifier.fillMaxWidth().testTag("saved-delete-cancel"),
                ) {
                    Text("انصراف")
                }
            }
        }
    if (editing)
        ModalBottomSheet(
            onDismissRequest = { editing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    if (editingId == null) "${SavedDestinationFixture.types[type]} جدید"
                    else "ویرایش مقصد آشنا"
                )
                Text(
                    "ثبت فقط محلی است؛ استعلام بانک و اعتبارسنجی سرویس در دسترس نیست.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
                OutlinedTextField(
                    name,
                    {
                        name = it
                        error = null
                    },
                    Modifier.fillMaxWidth().testTag("saved-name"),
                    label = { Text("نام مقصد") },
                    singleLine = true,
                )
                OutlinedTextField(
                    number,
                    {
                        number = it
                        error = null
                    },
                    Modifier.fillMaxWidth().testTag("saved-number"),
                    label = { Text("شماره ${SavedDestinationFixture.types[type]}") },
                    textStyle = TextStyle(textDirection = TextDirection.Ltr),
                    singleLine = true,
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    Modifier.fillMaxWidth().testTag("saved-description"),
                    label = { Text("توضیحات") },
                )
                if (error != null)
                    Text(
                        error.orEmpty(),
                        Modifier.testTag("saved-validation"),
                        color = MaterialTheme.colorScheme.error,
                    )
                Button(
                    {
                        error =
                            SavedDestinationFixture.validation(
                                records,
                                editingId,
                                type,
                                name,
                                number,
                            )
                        if (error == null) {
                            val updated =
                                SavedDestination(
                                    editingId ?: SavedDestinationFixture.nextId(records),
                                    type,
                                    name.trim(),
                                    number.trim(),
                                    description.trim(),
                                )
                            onRecordsChanged(
                                if (editingId == null) records + updated
                                else records.map { if (it.id == editingId) updated else it }
                            )
                            editing = false
                        }
                    },
                    Modifier.fillMaxWidth().padding(top = 12.dp).testTag("saved-save"),
                ) {
                    Text("ثبت فقط در پیش‌نمایش محلی")
                }
                TextButton(
                    { editing = false },
                    Modifier.fillMaxWidth().testTag("saved-editor-cancel"),
                ) {
                    Text("انصراف")
                }
            }
        }
}
