package eu.pkgsoftware.babybuddywidgets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


import eu.pkgsoftware.babybuddywidgets.test.R;
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class TestTokenEncryptionV1 {
    @Test
    fun testDecryptionOfV1Settings() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val inputStream = inst.context.resources.openRawResource(R.raw.settings_v1);

        val adapter = object : CredStore.SettingsFileOpener {
            override fun openReadStream(): InputStream {
                return inputStream
            }

            override fun openWriteStream(): OutputStream {
                throw IOException("Not implemented")
            }

        }

        val credStore = CredStore(adapter, "0.0");
        Assert.assertEquals(credStore.appToken, "97aa51ef1e60ea733cc7ede37c1e65376e9f0b45")
    }
}