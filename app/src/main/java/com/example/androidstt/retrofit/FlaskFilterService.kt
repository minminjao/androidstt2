package com.example.androidstt.retrofit

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FlaskFilterService {
    @Multipart
    @POST("model")
    fun sendRawFile(@Part raw : MultipartBody.Part): Call<Void>
}