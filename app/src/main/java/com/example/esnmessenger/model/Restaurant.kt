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

val FALLBACK_MENUS = listOf(
    MealOption(
        name = "Lunch",
        items = listOf(
            MenuItem("Chicken soup with vegetables", "G, L", "2.95 €"),
            MenuItem("Grilled salmon with mashed potato", "G, L", "10.50 €"),
            MenuItem("Pasta with tomato sauce", "VEG", "8.90 €"),
            MenuItem("Caesar salad", "VEG, G", "7.50 €")
        )
    ),
    MealOption(
        name = "Vegetarian",
        items = listOf(
            MenuItem("Lentil curry with rice", "VEG, G, L", "8.90 €"),
            MenuItem("Falafel wrap with hummus", "VEG", "7.50 €")
        )
    ),
    MealOption(
        name = "Dessert",
        items = listOf(
            MenuItem("Seasonal fruit salad", "VEG, G, L", "2.50 €"),
            MenuItem("Yogurt with granola", "VEG", "1.90 €")
        )
    )
)
