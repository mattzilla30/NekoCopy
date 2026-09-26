package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.NetworkCapabilities

data class NetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isUnmetered: Boolean,
) {
    val isOnline = isConnected && isValidated
}

fun Context.activeNetworkState(): NetworkState {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return NetworkState(
        isConnected =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        isValidated =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
        isUnmetered =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: false,
    )
}
