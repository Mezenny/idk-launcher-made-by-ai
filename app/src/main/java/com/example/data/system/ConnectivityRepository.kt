package com.example.data.system

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import com.example.model.NetworkStatusUi
import com.example.model.NetworkTransport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityRepository(private val context: Context) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun observeNetworkStatus(): Flow<NetworkStatusUi> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(NetworkStatusUi())
            awaitClose { }
            return@callbackFlow
        }

        fun emitCurrent() {
            trySend(readCurrentStatus(cm))
        }

        emitCurrent()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitCurrent()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(parseCapabilities(networkCapabilities))
            }

            override fun onLost(network: Network) {
                emitCurrent()
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    private fun readCurrentStatus(cm: ConnectivityManager): NetworkStatusUi {
        val network = cm.activeNetwork ?: return NetworkStatusUi()
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkStatusUi()
        return parseCapabilities(caps)
    }

    private fun parseCapabilities(caps: NetworkCapabilities): NetworkStatusUi {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
            hasInternet -> NetworkTransport.WIFI
            else -> NetworkTransport.NONE
        }

        return NetworkStatusUi(
            isConnected = hasInternet,
            isValidated = isValidated,
            transport = transport,
            isMetered = isMetered
        )
    }

    companion object {
        fun buildInternetPanelIntent(): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
        }

        fun buildWifiPanelIntent(): Intent {
            // Full Wi-Fi settings allows picking any home SSID and entering its password
            return Intent(Settings.ACTION_WIFI_SETTINGS)
        }

        fun buildMobileDataIntent(): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
        }

        fun openInternetPanel(context: Context): Boolean {
            val candidates = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                }
                add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                add(Intent(Settings.ACTION_WIFI_SETTINGS))
                add(Intent(Settings.ACTION_SETTINGS))
            }
            return launchFirstValidIntent(context, candidates)
        }

        fun openWifiControls(context: Context): Boolean {
            // Open full Android Wi-Fi network picker first so users can tap their home Wi-Fi and enter password
            val candidates = buildList {
                add(Intent(Settings.ACTION_WIFI_SETTINGS))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Intent(Settings.Panel.ACTION_WIFI))
                }
                add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
            return launchFirstValidIntent(context, candidates)
        }

        fun openMobileDataControls(context: Context): Boolean {
            val candidates = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(Intent(Settings.ACTION_DATA_USAGE_SETTINGS))
                }
                add(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
                add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                add(Intent(Settings.ACTION_SETTINGS))
            }
            return launchFirstValidIntent(context, candidates)
        }

        fun suggestHomeWifiNetwork(context: Context, ssid: String, password: String): String {
            val cleanSsid = ssid.trim()
            if (cleanSsid.isEmpty()) return "Enter your Wi-Fi network name (SSID)"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    val builder = WifiNetworkSuggestion.Builder()
                        .setSsid(cleanSsid)
                        .setIsAppInteractionRequired(false)
                    if (password.isNotEmpty()) {
                        builder.setWpa2Passphrase(password)
                    }
                    val status = runCatching {
                        wifiManager.addNetworkSuggestions(listOf(builder.build()))
                    }.getOrDefault(WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL)

                    if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                        openWifiControls(context)
                        return "Saved \"$cleanSsid\" suggestion — confirm or tap it in Wi-Fi Settings"
                    }
                }
            }
            openWifiControls(context)
            return "Select \"$cleanSsid\" in Android Wi-Fi Settings to connect"
        }

        private fun launchFirstValidIntent(context: Context, intents: List<Intent>): Boolean {
            for (intent in intents) {
                val ok = runCatching {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (ok) return true
            }
            return false
        }
    }
}
