package com.example.androidstt.retrofit

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class FlaskFilterServiceImpl {

    fun sendRawFile(file: File) {
        val api = Connection.getInstance().create(FlaskFilterService::class.java)

        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val body = MultipartBody.Part.createFormData("raw", file.name, requestFile)

        api.sendRawFile(body)
    }
}