package com.example.esnmessenger.model

data class Restaurant(
    val id: Int,
    val name: String,
    val campus: String,
    val kitchenId: Int
)

data class DailyMenu(
    val restaurant: Restaurant,
    val mealOptions: List<MealOption> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MealOption(
    val name: String,
    val items: List<MenuItem>
)

data class MenuItem(
    val name: String,
    val diets: String,
    val price: String
)

val OAMK_RESTAURANTS = listOf(
    Restaurant(1, "Ravintola Mara", "Linnanmaa", 49),
    Restaurant(2, "Ravintola Alwari", "Kontinkangas", 73),
    Restaurant(3, "Ravintola Foobar", "Linnanmaa", 69)
)

// Jamix diet codes → readable label (based on actual API response from OAMK)
val DIET_FILTERS = listOf(
    "VEG" to "Vegetarian",
    "G"   to "Gluten-free",
    "L"   to "Lactose-free",
    "M"   to "Milk-free"
)
