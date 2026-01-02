package eu.pkgsoftware.babybuddywidgets.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    private val listeners = mutableListOf<NetworkChangeListener>()
    private var currentNetwork: Network? = null
    private var isMonitoring = false

    interface NetworkChangeListener {
        fun onNetworkChanged(newNetwork: Network?)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            val previousNetwork = currentNetwork
            currentNetwork = network

            // Notify listeners
            synchronized(listeners) {
                // If we switched from one network to another, also notify of the change
                if (previousNetwork != null && previousNetwork != network) {
                    Log.d(TAG, "Network switched from $previousNetwork to $network")
                    listeners.forEach { it.onNetworkChanged(network) }
                }
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")

            if (currentNetwork == network) {
                currentNetwork = null
                synchronized(listeners) {
                    listeners.forEach { it.onNetworkChanged(null) }
                }
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.d(TAG, "Network capabilities changed: $network")

            // Check if this is a different type of network (WiFi vs Cellular)
            val previousNetwork = currentNetwork
            if (previousNetwork != null && previousNetwork != network) {
                currentNetwork = network
                synchronized(listeners) {
                    listeners.forEach { it.onNetworkChanged(network) }
                }
            }
        }
    }

    /**
     * Start monitoring network changes
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring network changes")
            return
        }

        Log.d(TAG, "Starting network monitoring")

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isMonitoring = true

            // Get current active network
            currentNetwork = connectivityManager.activeNetwork
            Log.d(TAG, "Current active network: $currentNetwork")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    fun addListener(listener: NetworkChangeListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
                Log.d(TAG, "Added network listener, total: ${listeners.size}")
            }
        }
    }

    fun removeListener(listener: NetworkChangeListener) {
        synchronized(listeners) {
            listeners.remove(listener)
            Log.d(TAG, "Removed network listener, total: ${listeners.size}")
        }
    }

    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getCurrentNetwork(): Network? = currentNetwork

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}

