package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("profile")
    fun getProfile(): Call<Profile>

    @GET("sleep")
    @ActivityNameAnnotation(ACTIVITIES.SLEEP)
    fun getSleepEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<SleepEntry>>
}
