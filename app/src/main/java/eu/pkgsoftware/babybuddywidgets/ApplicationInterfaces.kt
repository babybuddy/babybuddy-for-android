package eu.pkgsoftware.babybuddywidgets

interface DialogCallback {
    fun call(b: Boolean)
}

interface ConnectingDialogInterface {
    fun interruptLoading(): Boolean
    fun showConnecting(currentTimeout: Long, error: Exception?)
    fun hideConnecting()
}

interface DisconnectInterface {
    fun setDisconnected(reason: String, disconnected: Boolean)
    fun reportError(message: String, error: Exception?)
}
