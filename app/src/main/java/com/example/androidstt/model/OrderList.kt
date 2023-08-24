package com.example.androidstt.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderList(
    var elements: ArrayList<Order>,
    var takeOut: Boolean,
) : Parcelable {
    fun copy(): OrderList {
        return OrderList(
            ArrayList(elements.map {
                Order(
                    it.food,
                    it.option,
                    it.count,
                    it.sideMenu,
                    it.sideMenuOption,
                    it.beverage,
                    it.beverageOption,
                    it.isTakeOut
                )
            }),
            takeOut,
        )
    }
}