package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.*
import com.example.esnmessenger.network.JamixService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RestaurantsViewModel : ViewModel() {

    private val _menus = MutableStateFlow(OAMK_RESTAURANTS.map { DailyMenu(it) })
    val menus: StateFlow<List<DailyMenu>> = _menus

    /** Mon–Fri of the relevant week as (dateString "YYYYMMDD", display label). */
    val weekDays: List<Pair<String, String>> = buildWeekDays()

    private val _selectedDate = MutableStateFlow(defaultDate())
    val selectedDate: StateFlow<String> = _selectedDate

    init {
        fetchMenusForDate(_selectedDate.value)
    }

    fun selectDate(date: String) {
        if (date == _selectedDate.value) return
        _selectedDate.value = date
        fetchMenusForDate(date)
    }

    fun fetchAllMenus() {
        fetchMenusForDate(_selectedDate.value)
    }

    private fun fetchMenusForDate(date: String) {
        _menus.value = OAMK_RESTAURANTS.map { DailyMenu(it, isLoading = true) }
        OAMK_RESTAURANTS.forEach { restaurant ->
            viewModelScope.launch {
                try {
                    val response = JamixService.instance.getMenu(
                        customerId = 93077,
                        kitchenId = restaurant.kitchenId,
                        date = date,
                        date2 = date
                    )
                    val mealOptions = response
                        .flatMap { it.menuTypes }
                        .map { menuType ->
                            MealOption(
                                name = menuType.menuTypeName,
                                items = menuType.menus
                                    .flatMap { it.days }
                                    .flatMap { it.mealOptions }
                                    .flatMap { opt ->
                                        opt.menuItems.map { item ->
                                            MenuItem(item.name, item.diets, item.price)
                                        }
                                    }
                            )
                        }
                        .filter { it.items.isNotEmpty() }
                    _menus.value = _menus.value.map { menu ->
                        if (menu.restaurant.id == restaurant.id)
                            menu.copy(mealOptions = mealOptions, isLoading = false)
                        else menu
                    }
                } catch (e: Exception) {
                    _menus.value = _menus.value.map { menu ->
                        if (menu.restaurant.id == restaurant.id)
                            menu.copy(isLoading = false, error = "Could not load menu")
                        else menu
                    }
                }
            }
        }
    }

    /** Returns Mon–Fri of the week containing the default date. */
    private fun buildWeekDays(): List<Pair<String, String>> {
        val todayStr = formatDate(Calendar.getInstance())
        val anchor = defaultCalendar()

        // Find Monday of the week that contains anchor
        val monday = anchor.clone() as Calendar
        val dow = monday.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dow) {
            Calendar.MONDAY   -> 0
            Calendar.TUESDAY  -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY   -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY   -> 6
            else              -> 0
        }
        monday.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        val dayNumFmt = SimpleDateFormat("d", Locale.getDefault())

        return (0..4).map { i ->
            val dayCal = monday.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val dateStr = formatDate(dayCal)
            val label = if (dateStr == todayStr) "Today" else "${dayNames[i]} ${dayNumFmt.format(dayCal.time)}"
            dateStr to label
        }
    }

    /**
     * Returns today's calendar, shifted to Friday if today is Saturday
     * or to next Monday if today is Sunday, so the day selector always
     * lands on a meaningful weekday.
     */
    private fun defaultCalendar(): Calendar {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
            Calendar.SUNDAY   -> (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            else              -> cal.clone() as Calendar
        }
    }

    private fun defaultDate(): String = formatDate(defaultCalendar())

    private fun formatDate(cal: Calendar): String {
        val year  = cal.get(Calendar.YEAR)
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val day   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "$year$month$day"
    }
}
