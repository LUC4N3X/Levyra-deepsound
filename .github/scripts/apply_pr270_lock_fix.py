from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/support/RemoteAnnouncementRepository.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "private class RemoteAnnouncementStore(context: Context) {\n    private val preferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)\n",
    "private class RemoteAnnouncementStore(context: Context) {\n    private val preferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)\n    private val stateLock = Any()\n",
    "state lock",
)
replace_once(
    "fun recordAppLaunch(nowMs: Long): Int = synchronized(this)",
    "fun recordAppLaunch(nowMs: Long): Int = synchronized(stateLock)",
    "launch lock",
)
replace_once(
    """    fun completedIds(): Set<String> =
        preferences.getStringSet(KEY_COMPLETED_IDS, emptySet()).orEmpty().toSet()
""",
    """    fun completedIds(): Set<String> = synchronized(stateLock) {
        preferences.getStringSet(KEY_COMPLETED_IDS, emptySet()).orEmpty().toSet()
    }
""",
    "completed IDs lock",
)
replace_once(
    """    fun snoozedUntil(id: String): Long {
        if (!RemoteAnnouncementRules.isValidId(id)) return Long.MAX_VALUE
        return preferences.getLong(snoozeKey(id), 0L)
    }
""",
    """    fun snoozedUntil(id: String): Long {
        if (!RemoteAnnouncementRules.isValidId(id)) return Long.MAX_VALUE
        return synchronized(stateLock) {
            preferences.getLong(snoozeKey(id), 0L)
        }
    }
""",
    "snooze read lock",
)
replace_once(
    """    fun snooze(id: String, untilMs: Long) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        preferences.edit().putLong(snoozeKey(id), untilMs.coerceAtLeast(0L)).apply()
    }
""",
    """    fun snooze(id: String, untilMs: Long) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        synchronized(stateLock) {
            preferences.edit().putLong(snoozeKey(id), untilMs.coerceAtLeast(0L)).apply()
        }
    }
""",
    "snooze write lock",
)
replace_once(
    """    fun markCompleted(id: String) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        synchronized(this) {
            val updated = completedIds().toMutableSet()
            if (updated.size >= MAX_COMPLETED_IDS && id !in updated) {
                updated.firstOrNull()?.let(updated::remove)
            }
            updated += id
            preferences.edit()
                .putStringSet(KEY_COMPLETED_IDS, updated)
                .remove(snoozeKey(id))
                .apply()
        }
    }
""",
    """    fun markCompleted(id: String) {
        if (!RemoteAnnouncementRules.isValidId(id)) return
        synchronized(stateLock) {
            val updated = preferences.getStringSet(KEY_COMPLETED_IDS, emptySet())
                .orEmpty()
                .toMutableSet()
            if (updated.size >= MAX_COMPLETED_IDS && id !in updated) {
                updated.firstOrNull()?.let(updated::remove)
            }
            updated += id
            preferences.edit()
                .putStringSet(KEY_COMPLETED_IDS, updated)
                .remove(snoozeKey(id))
                .apply()
        }
    }
""",
    "completion lock",
)

path.write_text(text, encoding="utf-8")
