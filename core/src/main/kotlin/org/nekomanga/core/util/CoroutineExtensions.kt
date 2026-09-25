package org.nekomanga.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.launchDelayed(timeMillis: Long = 150L, block: () -> Unit) {
    this.launch {
        delay(timeMillis)
        block()
    }
}
