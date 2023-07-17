package eu.pkgsoftware.babybuddywidgets.login

import org.json.JSONException
import org.json.JSONObject

class InvalidQRCodeException() : Exception("Invalid QR Code") {}

class LoginData(val url: String, val token: String, val cookies: Map<String, String>) {
    companion object {
        fun fromQrcodeJSON(qrcode: String): LoginData {
            fun objectToStringMap(o: JSONObject?): Map<String, String> {
                if (o == null) {
                    return mapOf()
                }

                val result = mutableMapOf<String, String>()
                for (key in o.keys()) {
                    val value = o.get(key)
                    if (value is String) {
                        result[key] = value
                    }
                }
                return result
            }


            if (!qrcode.startsWith("BABYBUDDY-LOGIN:")) {
                throw InvalidQRCodeException();
            }
            val jsonPayload = qrcode.split(":", limit = 2)[1]
            try {
                val json = JSONObject(jsonPayload)
                val url = json.getString("url")
                val token = json.getString("api_key")
                val cookies = objectToStringMap(json.optJSONObject("session_cookies"))
                return LoginData(url, token, cookies)
            } catch (e: JSONException) {
                throw InvalidQRCodeException();
            }
        }

    }
}
