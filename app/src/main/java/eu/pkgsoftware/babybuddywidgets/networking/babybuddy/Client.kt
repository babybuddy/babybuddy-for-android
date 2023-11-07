package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ApiInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChildKey
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.IncorrectApiConfiguration
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.PaginatedEntries
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Profile
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.UIPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.MalformedURLException
import java.net.URL
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

class AuthInterceptor(private val authToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithAuth = originalRequest.newBuilder()
            .header("Authorization", authToken)
            .build()
        return chain.proceed(requestWithAuth)
    }
}

class InvalidBody() : Exception("Invalid body")

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

    private fun <T> executeCall(call: Call<T>): T {
        val r = call.execute()
        return if (r.isSuccessful) {
            val p = r.body() ?: throw InvalidBody()
            p
        } else {
            throw RequestCodeFailure(
                r.code(),
                "Failed",
                r.errorBody()?.charStream()?.readText() ?: "[no message]",
            )
        }
    }

    fun pathToUrl(path: String): URL {
        val prefix = credStore.serverUrl.replace("/*$".toRegex(), "")
        val trimmedPath = path.trimStart('/')
        return URL("$prefix/$trimmedPath")
    }

    inline fun <reified T : Any> getKClass(): KClass<T> {
        return T::class
    }

    @Throws(MalformedURLException::class)
    fun entryUserPath(entry: TimeEntry): URL {
        val uiPath = entry.javaClass.getAnnotationsByType(UIPath::class.java)
        if (uiPath.isEmpty()) {
            throw IncorrectApiConfiguration("UIPath is missing for ${entry.javaClass.name}")
        }
        return pathToUrl(uiPath[0].path + "/")
    }

    suspend fun getProfile(): Profile {
        return withContext(Dispatchers.IO) {
            val call = api.getProfile()
            executeCall(call)
        }
    }

    suspend fun <T : Any> getEntries(
        itemClass: KClass<T>,
        offset: Int = 0,
        limit: Int = 100,
        childId: Int? = null,
        extraArgs: Map<String, String> = mapOf(),
    ): PaginatedResult<T> {
        return withContext(Dispatchers.IO) {
            val selected: KFunction<*> = selectPaginatedGetterFunction(itemClass)
                ?: throw IncorrectApiConfiguration("getter for ${itemClass.qualifiedName} is missing")

            val lExtraArgs = extraArgs.toMutableMap()
            if (childId != null) {
                val childKeyAnn = selected.findAnnotation<ChildKey>()
                    ?: throw IncorrectApiConfiguration(
                        "ChildKey annotation for ${selected.name} is missing"
                    )
                lExtraArgs[childKeyAnn.name] = childId.toString()
            }

            val call: Call<PaginatedEntries<T>> = selected.javaMethod!!.invoke(
                api, offset, limit, extraArgs
            ) as Call<PaginatedEntries<T>>
            val callResult = executeCall(call)
            PaginatedResult(callResult.entries, offset, callResult.count)
        }
    }

    private fun <T : Any> selectPaginatedGetterFunction(itemClass: KClass<T>): KFunction<*>? {
        val desiredReturnType = Call::class.createType(
            arguments = listOf(
                KTypeProjection(
                    KVariance.INVARIANT,
                    PaginatedEntries::class.createType(
                        arguments = listOf(
                            KTypeProjection(
                                KVariance.INVARIANT,
                                itemClass.createType()
                            )
                        )
                    )
                )
            )
        )

        var selected: KFunction<*>? = null
        for (func in ApiInterface::class.functions) {
            if (func.returnType == desiredReturnType) {
                selected = func
            }
        }
        return selected
    }
}