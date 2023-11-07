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

    @GET("feeding")
    fun getFeedingEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<FeedingEntry>>

    @GET("tummy-times")
    fun getTummyTimeEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<TummyTimeEntry>>

    @GET("pumping")
    fun getPumpingEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<PumpingEntry>>

    @GET("changes")
    fun getChangeEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<ChangeEntry>>

    @GET("notes")
    fun getNoteEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<NoteEntry>>

    @GET("temperature")
    fun getTemperatureEnties(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<TemperatureEntry>>

    @GET("weight")
    fun getWeightEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<WeightEntry>>

    @GET("height")
    fun getHeightEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<HeightEntry>>

    @GET("head-circumference")
    fun getHeadCircumferenceEntries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Call<PaginatedEntries<HeadCircumferenceEntry>>
}
