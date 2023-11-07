package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ActivityNameAnnotation
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ApiInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.PaginatedEntries
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Profile
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.lang.reflect.Method
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

data class PaginatedResult<T> (
    val entries: List<T>,
    val offset: Int,
    val totalCount: Int,
)

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

    inline fun <reified T : Any> getKClass(): KClass<T> {
        return T::class
    }

    suspend fun <T : TimeEntry> getEntries(itemClass: KClass<T>, offset: Int = 0, limit: Int = 100): PaginatedResult<T> {
        return withContext(Dispatchers.IO) {
            val refAnnotation = itemClass.findAnnotation<ActivityNameAnnotation>()
            if (refAnnotation == null) {
                throw RuntimeException("annotation missing");
            }

            var selectedMethod: Method? = null
            for (method in ApiInterface::class.java.methods) {
                for (an in method.getAnnotationsByType(ActivityNameAnnotation::class.java)) {
                    if (an.name == refAnnotation.name) {
                        selectedMethod = method
                    }
                }
            }

            if (selectedMethod == null) {
                throw RuntimeException("getter is missing");
            }


            if (selectedMethod.returnType != getKClass<Call<PaginatedEntries<T>>>().java) {
                throw RuntimeException("method has incorrect return type");
            }
            val call: Call<PaginatedEntries<T>> = selectedMethod.invoke(api, offset, limit) as Call<PaginatedEntries<T>>
            val r = call.execute()


            if (r.isSuccessful) {
                val p = r.body()
                if (p == null) {
                    throw InvalidBody()
                }
                PaginatedResult(p.entries, offset, p.count)
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