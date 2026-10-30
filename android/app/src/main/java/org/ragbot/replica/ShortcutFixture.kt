package org.ragbot.replica

object ShortcutDestinations {
    const val SCREEN = "S-084"
    val implemented = setOf(SCREEN)
}

data class PreviewShortcut(val id: String, val title: String)

/** Synthetic candidate set and order; these keys do not identify server menus. */
object ShortcutFixture {
    val services =
        listOf(
            PreviewShortcut("preview-accounts", "حساب‌ها — نمونه"),
            PreviewShortcut("preview-cards", "کارت‌ها — نمونه"),
            PreviewShortcut("preview-statement", "صورت‌حساب — نمونه"),
            PreviewShortcut("preview-support", "پشتیبانی — نمونه"),
            PreviewShortcut("preview-notifications", "اعلان‌ها — نمونه"),
        )
    val initialPinnedIds = listOf("preview-accounts", "preview-statement")

    fun visible(query: String, pinnedIds: List<String>): List<PreviewShortcut> =
        if (query.isBlank()) pinnedIds.mapNotNull { id -> services.find { it.id == id } }
        else services.filter { it.title.contains(query.trim()) }

    fun toggle(pinnedIds: List<String>, id: String): List<String> {
        if (services.none { it.id == id }) return pinnedIds
        return if (id in pinnedIds) pinnedIds - id else pinnedIds + id
    }
}
