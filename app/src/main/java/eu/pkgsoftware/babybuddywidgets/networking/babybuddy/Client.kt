package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.ServerAccessProviderInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ApiInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChildKey
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.IncorrectApiConfiguration
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.PaginatedEntries
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Profile
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classAPIPath
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classUIPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.ByteArrayOutputStream
import java.net.MalformedURLException
import java.net.URL
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

fun genRequestId(): String {
    return Random.nextInt(0xFFFFFF + 1).toString(16).padStart(6, '0')
}

class AuthInterceptor(private val authToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithAuth = originalRequest.newBuilder()
            .header("Authorization", authToken)
            .build()
        return chain.proceed(requestWithAuth)
    }
}

class DebugNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val buf = Buffer();
        request.body()?.writeTo(buf) ?: buf.writeUtf8("null");
        val bodyStream = ByteArrayOutputStream(10000)
        buf.copyTo(bodyStream)

        GlobalDebugObject.log(
            "Raw request: ${request.url()} - Request body: ${bodyStream} - Response: ${response.message()}"
        )
        return response
    }
}

class InvalidBody() : Exception("Invalid body")

data class PaginatedResult<T> (
    val entries: List<T>,
    val offset: Int,
    val totalCount: Int,
)

class Client(val credStore: ServerAccessProviderInterface) {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor("Token " + credStore.appToken))
        .addInterceptor(DebugNetworkInterceptor())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(credStore.serverUrl.trimEnd('/') + "/api/")
        .addConverterFactory(JacksonConverterFactory.create())
        .client(httpClient)
        .build()

    private val api = retrofit.create(ApiInterface::class.java)

    private fun <T> executeCall(call: Call<T>, ignoredBody: T? = null): T {
        val r = call.execute()
        return if (r.isSuccessful) {
            if (ignoredBody != null) {
                return ignoredBody
            }
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
        val prefix = credStore.serverUrl.trimEnd('/')
        val trimmedPath = path.trimStart('/')
        return URL("$prefix/$trimmedPath")
    }

    @Throws(MalformedURLException::class)
    fun entryUserPath(entry: TimeEntry): URL {
        val uiPath = classUIPath(entry::class)
        return pathToUrl("$uiPath/${entry.id}/")
    }

    suspend fun getProfile(): Profile {
        val REQID = genRequestId()
        return withContext(Dispatchers.IO) {
            GlobalDebugObject.log("${REQID} V2Client::getProfile")
            try {
                val call = api.getProfile()
                return@withContext executeCall(call)
            }
            catch (e: Exception) {
                GlobalDebugObject.log("${REQID} V2Client::getProfile !exception! ${e.message}")
                throw e
            }
        }
    }

    suspend fun <T : Any> getEntries(
        itemClass: KClass<T>,
        offset: Int = 0,
        limit: Int = 100,
        childId: Int? = null,
        extraArgs: Map<String, String> = mapOf(),
    ): PaginatedResult<T> {
        val REQID = genRequestId()

        return withContext(Dispatchers.IO) {
            try {
                GlobalDebugObject.log("${REQID} V2Client::getEntries ${itemClass.simpleName} childId=${childId} offset=${offset} limit=${limit}")

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
                    api, offset, limit, lExtraArgs
                ) as Call<PaginatedEntries<T>>
                val callResult = executeCall(call)

                GlobalDebugObject.log("${REQID} V2Client::getEntries ${itemClass.simpleName} retrieved ${callResult.count} results")
                PaginatedResult(callResult.entries, offset, callResult.count)
            }
            catch (e: Exception) {
                GlobalDebugObject.log("${REQID} V2Client::getEntries ${itemClass.simpleName} !exception! ${e.message}")
                throw e
            }
        }
    }

    suspend fun <T : TimeEntry> deleteEntry(item: T) {
        val REQID = genRequestId()
        val klass = item.javaClass.kotlin

        return withContext(Dispatchers.IO) {
            try {

                GlobalDebugObject.log("${REQID} V2Client::deleteEntry ${klass.simpleName} id=${item.id}")
                val apiPath = classAPIPath(klass)

                val call = api.genericDeleteEntry(apiPath, item.id)
                executeCall(call, ignoredBody = Unit)

                GlobalDebugObject.log("${REQID} V2Client::deleteEntry ${klass.simpleName} deleted")
            }
            catch (e: Exception) {
                GlobalDebugObject.log("${REQID} V2Client::deleteEntry ${klass.simpleName} !exception! ${e.message}")
                throw e
            }
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

    suspend fun <T : TimeEntry> createEntry(itemClass: KClass<T>, item: T): T {
        val REQID = genRequestId()
        val klass = item.javaClass.kotlin

        val desiredArgumentType = JsonNode::class.createType()
        val desiredReturnType = Call::class.createType(
            arguments = listOf(
                KTypeProjection(
                    KVariance.INVARIANT,
                    itemClass.createType()
                )
            )
        )
        var selected: KFunction<*>? = null
        for (func in ApiInterface::class.functions) {
            if (desiredArgumentType == func.findParameterByName("data")?.type) {
                if (desiredReturnType == func.returnType) {
                    selected = func
                }
            }
        }

        if (selected == null) {
            throw IncorrectApiConfiguration(
                "${REQID} V2Client::createEntry setter for ${klass.qualifiedName} is missing"
            )
        }

        val mapper = jacksonObjectMapper()
        val node = mapper.valueToTree<ObjectNode>(item)
        node.remove("id")

        GlobalDebugObject.log("${REQID} V2Client::createEntry ${klass.simpleName}")
        return withContext(Dispatchers.IO) {
            try {
                val call: Call<T> = selected.javaMethod!!.invoke(api, node) as Call<T>
                return@withContext executeCall(call)
            }
            catch (e: Exception) {
                GlobalDebugObject.log("${REQID} V2Client::createEntry ${klass.simpleName} !exception! ${e.message}")
                throw e
            }
        }
    }
}