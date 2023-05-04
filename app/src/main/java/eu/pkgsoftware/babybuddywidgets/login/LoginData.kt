package eu.pkgsoftware.babybuddywidgets.login

import org.json.JSONException
import org.json.JSONObject

class InvalidQRCodeException() : Exception("Invalid QR Code") {}

class LoginData(val url: String, val token: String) {
    companion object {
        fun fromQrcodeJSON(qrcode: String): LoginData {
            if (!qrcode.startsWith("BABYBUDDY-LOGIN:")) {
                throw InvalidQRCodeException();
            }
            val jsonPayload = qrcode.split(":", limit = 2)[1]
            try {
                val json = JSONObject(jsonPayload)
                val url = json.getString("url")
                val token = json.getString("api_key")
                return LoginData(url, token)
            } catch (e: JSONException) {
                throw InvalidQRCodeException();
            }
        }
    }
}