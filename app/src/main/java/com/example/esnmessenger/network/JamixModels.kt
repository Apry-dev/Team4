package com.example.esnmessenger.network

import com.google.gson.annotations.SerializedName

data class JamixKitchen(
    @SerializedName("kitchenName") val kitchenName: String = "",
    @SerializedName("menuTypes") val menuTypes: List<JamixMenuType> = emptyList()
)

data class JamixMenuType(
    @SerializedName("menuTypeName") val menuTypeName: String = "",
    @SerializedName("menus") val menus: List<JamixMenu> = emptyList()
)

data class JamixMenu(
    @SerializedName("days") val days: List<JamixDay> = emptyList()
)

data class JamixDay(
    @SerializedName("date") val date: String = "",
    @SerializedName("mealoptions") val mealOptions: List<JamixMealOption> = emptyList()
)

data class JamixMealOption(
    @SerializedName("name") val name: String = "",
    @SerializedName("menuItems") val menuItems: List<JamixMenuItem> = emptyList()
)

data class JamixMenuItem(
    @SerializedName("name") val name: String = "",
    @SerializedName("diets") val diets: String = "",
    @SerializedName("price") val price: String = ""
)
