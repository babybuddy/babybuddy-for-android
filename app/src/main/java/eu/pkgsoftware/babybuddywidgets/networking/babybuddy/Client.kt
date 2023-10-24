package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ApiInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.Date
import kotlin.Exception

class AuthInterceptor(private val authToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithAuth = originalRequest.newBuilder()
            .header("Authorization", authToken)
            .build()
        return chain.proceed(requestWithAuth)
    }
}

class InvalidBody() : Exception("Invalid body") {}

class Client(val credStore: CredStore) {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor("Token " + credStore.appToken))
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(credStore.serverUrl.replace("/*$", "") + "/api/")
        .addConverterFactory(JacksonConverterFactory.create())
        .client(httpClient)
        .build()

    private val api = retrofit.create(ApiInterface::class.java)

    suspend fun getProfile(): Profile {
        return withContext(Dispatchers.IO) {
            val call = api.getProfile()
            val r = call.execute()

            if (r.isSuccessful) {
                val p = r.body()
                if (p == null) {
                    throw InvalidBody()
                }
                p
            } else {
                throw RequestCodeFailure(
                    r.code(),
                    "Failed",
                    r.errorBody()?.charStream()?.readText() ?: "[no message]",
                )
            }
        }
    }
}