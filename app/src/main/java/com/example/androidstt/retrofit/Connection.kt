package com.example.androidstt.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Connection {

    companion object {

        // 2023.09.12. botbinoo
        // TODO: 여기 url 을 반드시 바꿔주세요.
        private const val BASE_URL = "http://0.0.0.0:5000/"
        private var INSTANCE: Retrofit? = null

        fun getInstance(): Retrofit {
            if(INSTANCE == null) {
                INSTANCE = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return INSTANCE!!
        }
    }
}