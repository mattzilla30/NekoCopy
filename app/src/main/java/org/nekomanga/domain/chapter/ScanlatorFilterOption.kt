package org.nekomanga.domain.chapter

enum class ScanlatorFilterOption(val value: Int) {
    ALL(0),
    ANY(1);

    companion object {
        fun fromInt(value: Int): ScanlatorFilterOption {
            return when (value) {
                0 -> ALL
                else -> ANY
            }
        }
    }
}
