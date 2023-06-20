package eu.pkgsoftware.babybuddywidgets

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

open class CredStoreEncryptionEngine {
    private val ENCRYPTION_STRING =
        "gK,8kwXJZRmL6/yz&Dp;tr5&Muk,A;h,VGeb\$qN-Gid3xLW&a/Xi0YOomVpQVAiFn:hP$8dbIX;L*v*cie&Tnkf+obFEN;a+DTmrILQO6CkY.oOV25dBjpXbep%qAu1bnbeS3A-zn%m"

    fun generateNewSalt(): String {
        val rnd = ByteArray(32)
        SecureRandom().nextBytes(rnd)
        return Base64.encodeToString(rnd, Base64.DEFAULT)
    }

    protected var SALT: String = generateNewSalt()

    protected fun encryptMessage(message: String?): String? {
        try {
            return if (message == null) {
                null
            } else {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val rawKey: ByteArray =
                    (SALT + ":::::" + ENCRYPTION_STRING).toByteArray(
                        StandardCharsets.ISO_8859_1
                    )
                val md5Key = MessageDigest.getInstance("MD5").digest(rawKey)
                val ivGen = MessageDigest.getInstance("MD5").digest(
                    (String(
                        md5Key,
                        StandardCharsets.ISO_8859_1
                    ) + ":::::" + SALT).toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
                val key: SecretKey = SecretKeySpec(md5Key, "AES")
                cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ivGen))
                val encoded = cipher.doFinal(message.toByteArray(StandardCharsets.ISO_8859_1))
                Base64.encodeToString(encoded, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    protected fun decryptMessage(encrypted: String?): String? {
        try {
            return if (encrypted == null) {
                null
            } else {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val rawKey: ByteArray =
                    (SALT + ":::::" + ENCRYPTION_STRING).toByteArray(
                        StandardCharsets.ISO_8859_1
                    )
                val md5Key = MessageDigest.getInstance("MD5").digest(rawKey)
                val ivGen = MessageDigest.getInstance("MD5").digest(
                    (String(
                        md5Key,
                        StandardCharsets.ISO_8859_1
                    ) + ":::::" + SALT).toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
                val key: SecretKey = SecretKeySpec(md5Key, "AES")
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ivGen))
                val decoded = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
                String(decoded, StandardCharsets.ISO_8859_1)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }
}
