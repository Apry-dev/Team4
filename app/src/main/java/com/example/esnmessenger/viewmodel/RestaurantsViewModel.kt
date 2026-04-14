package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.*
import com.example.esnmessenger.network.JamixService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class RestaurantsViewModel : ViewModel() {

    private val _menus = MutableStateFlow(OAMK_RESTAURANTS.map { DailyMenu(it) })
    val menus: StateFlow<List<DailyMenu>> = _menus

    private val _isWeekend = MutableStateFlow(false)
    val isWeekend: StateFlow<Boolean> = _isWeekend

    init {
        fetchAllMenus()
    }

    fun fetchAllMenus() {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            _isWeekend.value = true
            _menus.value = OAMK_RESTAURANTS.map { DailyMenu(it, isLoading = false) }
            return
        }
        _isWeekend.value = false
        val today = todayString()
        _menus.value = OAMK_RESTAURANTS.map { DailyMenu(it, isLoading = true) }
        OAMK_RESTAURANTS.forEach { restaurant ->
            viewModelScope.launch {
                try {
                    val response = JamixService.instance.getMenu(
                        customerId = 93077,
                        kitchenId = restaurant.kitchenId,
                        date = today,
                        date2 = today
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

    private fun todayString(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "$year$month$day"
    }
}
