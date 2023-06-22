package eu.pkgsoftware.babybuddywidgets.login

import org.junit.Assert
import org.junit.Test

class LoginDataTest {
    val classLoader = this.javaClass.classLoader!!

    @Test
    fun testLoginData_v1_1() {
        val resourceUrl = classLoader.getResource("raw/login_data_v1.json")
        val data = LoginData.fromQrcodeJSON(resourceUrl.readText())

        Assert.assertEquals(data.url, "http://example.com/babybuddy/")
        Assert.assertEquals(data.token, "abcdefabcdefabcdefacbdefabcdefbaadf00d55")
        Assert.assertEquals(data.cookies.size, 0)
    }

    @Test
    fun testLoginData_v2_1() {
        val resourceUrl = classLoader.getResource("raw/login_data_v2-1.json")
        val data = LoginData.fromQrcodeJSON(resourceUrl.readText())

        Assert.assertEquals(data.url, "http://example.com/api/hassio_ingress/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA--Xs/")
        Assert.assertEquals(data.token, "abcdefabcdefabcdefacbdefabcdefbaadf00d55")
        Assert.assertEquals(data.cookies.size, 1)
        Assert.assertEquals(data.cookies["ingress_session"], "abcdefabcdefabcdefacbdefabcdefbaadf00dabcdefabcdefabcdefacbdefabcdefbaadf00dabcdefabcdefabcdefacbdefabcdefbaadf00dabcdefabcdefab")
    }

    @Test
    fun testLoginData_v2_2() {
        val resourceUrl = classLoader.getResource("raw/login_data_v2-2.json")
        val data = LoginData.fromQrcodeJSON(resourceUrl.readText())

        Assert.assertEquals(data.url, "http://example.com/api/hassio_ingress/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA--Xs/")
        Assert.assertEquals(data.token, "abcdefabcdefabcdefacbdefabcdefbaadf00d55")
        Assert.assertEquals(data.cookies.size, 0)
    }
}