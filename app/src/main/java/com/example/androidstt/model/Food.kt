package com.example.androidstt.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Food(
    var documentId: String = "",    // 고유 ID

    @PropertyName("type")
    @SerializedName("type")
    private val _type: Int = 0,     // 타입 (햄버거: 0, 사이드 메뉴: 1, 음료: 2, 디저트: 3)

    val name: String = "",          // 이름
    val options: HashMap<String, Long> = hashMapOf(),    // 옵션 (단품, 세트 / M, L / 6조각, 10조각 등)
    val image: String = "",         // 이미지
) : Parcelable {
    enum class Type {
        HAMBURGER, SIDE_MENU, BEVERAGE, DESSERT
    }

    val type: Type
        get() {
            return Type.values()[_type]
        }
}