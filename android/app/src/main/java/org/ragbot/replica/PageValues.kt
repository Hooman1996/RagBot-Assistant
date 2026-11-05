package org.ragbot.replica

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import kotlin.reflect.KProperty

/** Non-secret page fields, held by [ReplicaApp] so normal handlers can set them. */
internal class PageValues(
    val page: () -> String,
    val read: () -> Map<String, Any?>,
    val write: (Map<String, Any?>) -> Unit,
)

internal val LocalPageValues = compositionLocalOf<PageValues> { error("No PageValues") }

internal class PageValue<T>(
    private val values: PageValues,
    private val key: String,
    private val initial: T,
) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(owner: Any?, property: KProperty<*>): T =
        values.read().let { if (key in it) it[key] as T else initial }

    operator fun setValue(owner: Any?, property: KProperty<*>, value: T) {
        values.write(values.read() + (key to value))
    }
}

/** Like rememberSaveable, for a field the bridge fills. Never use it for a secret. */
@Composable
internal fun <T> pageValue(field: String, initial: T): PageValue<T> {
    val values = LocalPageValues.current
    val key = "${values.page()}/$field"
    val first = remember(key) { initial }
    return remember(key) { PageValue(values, key, first) }
}
