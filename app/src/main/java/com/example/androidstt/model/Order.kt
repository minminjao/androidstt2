package com.example.androidstt.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val food: Food,
    var option: String? = null,
    var count: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var sideMenu: Food? = null,

    @Expose(serialize = false, deserialize = false)
    var sideMenuOption: String? = null,

    @Expose(serialize = false, deserialize = false)
    var beverage: Food? = null,

    @Expose(serialize = false, deserialize = false)
    var beverageOption: String? = null,

    @Expose(serialize = false, deserialize = false)
    @PropertyName("isTakeOut")
    var isTakeOut: Boolean = false,
) : Parcelable {
    val price: Long
        get() = food.options[option]!! +
                (sideMenu?.options?.values?.first() ?: 0) +
                (beverage?.options?.values?.first() ?: 0)
}
