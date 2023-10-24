package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {
    @GET("profile")
    fun getProfile(): Call<Profile>
}