package org.ragbot.replica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat

/** Recovered drag/draft/apply/cancel behavior, with local-only preview persistence. HC-EDIT. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeMenuEditor(
    order: List<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onMenu: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(order.toList()) }
    val positions = remember { mutableMapOf<String, Rect>() }
    var dragged by remember { mutableStateOf<String?>(null) }
    var hovered by remember { mutableStateOf<String?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var delta by remember { mutableStateOf(Offset.Zero) }
    val items = HomeFixture.services.associateBy { it.key }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.ui.graphics.Color(0xFFFBFBFB),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = false
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("layout-editor")) {
            Text("ویرایش چیدمان", fontFamily = replicaFont("replica_medium"), fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Text("با جابه‌جایی، خدمات پرکاربرد خانه را شخصی‌سازی نمایید.", fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            draft.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        key(key) {
                            val item = checkNotNull(items[key])
                            Column(
                                Modifier.weight(1f)
                                    .height(88.dp)
                                    .testTag("edit-$key")
                                    .onGloballyPositioned { positions[key] = it.boundsInRoot() }
                                    .zIndex(if (dragged == key) 1f else 0f)
                                    .graphicsLayer {
                                        translationX = if (dragged == key) delta.x else 0f
                                        translationY = if (dragged == key) delta.y else 0f
                                    }
                                    .clickable(role = Role.Button) { onMenu(item.title) }
                                    .background(
                                        if (hovered == key)
                                            androidx.compose.ui.graphics.Color(0xFFE3F4E9)
                                        else androidx.compose.ui.graphics.Color.Transparent,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .pointerInput(key) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                dragged = key
                                                hovered = key
                                                delta = Offset.Zero
                                                origin =
                                                    checkNotNull(positions[key]).topLeft + offset
                                            },
                                            onDragCancel = {
                                                dragged = null
                                                hovered = null
                                            },
                                            onDragEnd = {
                                                val from = draft.indexOf(key)
                                                val to = draft.indexOf(hovered)
                                                if (from >= 0 && to >= 0 && from != to) {
                                                    draft =
                                                        ArrayList(
                                                            draft.toMutableList().apply {
                                                                add(to, removeAt(from))
                                                            }
                                                        )
                                                }
                                                dragged = null
                                                hovered = null
                                            },
                                        ) { change, amount ->
                                            change.consume()
                                            delta += amount
                                            hovered =
                                                positions.entries
                                                    .firstOrNull {
                                                        it.key != key &&
                                                            it.value.contains(origin + delta)
                                                    }
                                                    ?.key ?: key
                                        }
                                    }
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = item.title
                                        stateDescription =
                                            "جایگاه ${draft.indexOf(key) + 1}؛ برای جابه‌جایی بکشید"
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction("انتقال به قبل") {
                                                    val index = draft.indexOf(key)
                                                    if (index > 0) {
                                                        draft =
                                                            ArrayList(
                                                                draft.toMutableList().apply {
                                                                    add(index - 1, removeAt(index))
                                                                }
                                                            )
                                                        true
                                                    } else false
                                                },
                                                CustomAccessibilityAction("انتقال به بعد") {
                                                    val index = draft.indexOf(key)
                                                    if (index < draft.lastIndex) {
                                                        draft =
                                                            ArrayList(
                                                                draft.toMutableList().apply {
                                                                    add(index + 1, removeAt(index))
                                                                }
                                                            )
                                                        true
                                                    } else false
                                                },
                                            )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                ReplicaIcon(
                                    item.artwork,
                                    "◇",
                                    Modifier.size(28.dp),
                                    androidx.compose.ui.graphics.Color.Unspecified,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.title, fontSize = 11.sp, maxLines = 1)
                                ReplicaIcon("replica_move", "↔", Modifier.size(14.dp), ServiceGreen)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onApply(draft) },
                    enabled = draft != order,
                    modifier = Modifier.weight(1f).testTag("apply-order"),
                ) {
                    Text("اعمال تغییرات")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("انصراف")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
