package com.example.androidstt.model

data class BotResponse(
    val success: Boolean,
    val fulfillmentText: String,
    val foods: List<Order>?
) {
}