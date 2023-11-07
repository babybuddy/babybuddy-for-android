package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.EVENTS
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("profile")
    fun getProfile(): Call<Profile>

    @GET("children")
    fun getChildEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<Child>>

    @GET("sleep")
    fun getSleepEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<SleepEntry>>

    @GET("changes")
    fun getChangeEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<ChangeEntry>>
}
