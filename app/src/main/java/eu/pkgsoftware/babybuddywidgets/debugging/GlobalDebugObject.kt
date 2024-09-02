package eu.pkgsoftware.babybuddywidgets.debugging

import android.util.Log

class GlobalDebugObject {
    companion object {
        @JvmStatic
        val ENABLED = true
        @JvmStatic
        val DO_PRINT = true
        @JvmStatic
        val LOG_FILE_MESSAGE_LIMIT = 10000

        val LOCK = Object()

        private val log = mutableListOf<String>()

        @JvmStatic
        fun log(msg: String) {
            if (!ENABLED) return
            synchronized(LOCK) {
                while (log.size > LOG_FILE_MESSAGE_LIMIT) {
                    log.removeAt(0)
                }
                if (DO_PRINT) {
                    Log.println(Log.DEBUG, "GlobalDebugObject.log", msg)
                }
                log.add(msg)
            }
        }

        @JvmStatic
        fun getLog(): List<String> {
            if (!ENABLED) return listOf()
            val result = synchronized(LOCK) {
                log.toList()
            }
            return result
        }
    }
}
