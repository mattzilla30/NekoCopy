package eu.kanade.tachiyomi.source.online.utils

/** The MangaDex reading status stored with each manga from older versions. */
enum class FollowStatus(val int: Int) {
    UNFOLLOWED(0),
    READING(1),
    COMPLETED(2),
    ON_HOLD(3),
    PLAN_TO_READ(4),
    DROPPED(5),
    RE_READING(6);

    companion object {
        fun fromInt(value: Int): FollowStatus =
            entries.firstOrNull { it.int == value } ?: UNFOLLOWED
    }
}
