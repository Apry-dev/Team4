package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esnmessenger.model.DIET_FILTERS
import com.example.esnmessenger.model.DailyMenu
import com.example.esnmessenger.model.MealOption
import com.example.esnmessenger.model.MenuItem
import com.example.esnmessenger.ui.theme.*
import com.example.esnmessenger.viewmodel.RestaurantsViewModel

@Composable
fun RestaurantsScreen(viewModel: RestaurantsViewModel = viewModel()) {
    val menus by viewModel.menus.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val weekDays = viewModel.weekDays

    // null = no active filter
    var activeDiet by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text(
                    text = "Campus Restaurants",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weekDays.find { it.first == selectedDate }?.second ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(
                onClick = { viewModel.fetchAllMenus() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        // Day selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { (dateStr, label) ->
                FilterChip(
                    selected = selectedDate == dateStr,
                    onClick = { viewModel.selectDate(dateStr) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNCyan,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Dietary filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Diet:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            // "All" chip to clear filter
            FilterChip(
                selected = activeDiet == null,
                onClick = { activeDiet = null },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ESNCyanDark,
                    selectedLabelColor = Color.White
                )
            )
            DIET_FILTERS.forEach { (code, label) ->
                FilterChip(
                    selected = activeDiet == code,
                    onClick = { activeDiet = if (activeDiet == code) null else code },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNMagenta,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        HorizontalDivider(color = OutlineColor.copy(alpha = 0.4f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(menus) { dailyMenu ->
                RestaurantCard(dailyMenu, activeDiet)
            }
        }
    }
}

@Composable
private fun RestaurantCard(dailyMenu: DailyMenu, activeDiet: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Restaurant name + campus badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ESNCyanLight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍽", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dailyMenu.restaurant.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ESNCyan.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = dailyMenu.restaurant.campus,
                            color = ESNCyanDark,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            when {
                dailyMenu.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(3) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(11.dp))
                        }
                    }
                }
                dailyMenu.error != null -> {
                    Text(
                        text = dailyMenu.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> {
                    val filtered = dailyMenu.mealOptions.applyDietFilter(activeDiet)
                    if (filtered.isEmpty()) {
                        Text(
                            text = if (activeDiet != null) "No items match the selected filter"
                                   else "No menu available today",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filtered.forEachIndexed { index, option ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            MealOptionSection(option)
                        }
                    }
                }
            }
        }
    }
}

/** Filters meal options by diet code; removes empty sections. */
private fun List<MealOption>.applyDietFilter(code: String?): List<MealOption> {
    if (code == null) return this
    return mapNotNull { option ->
        val matchingItems = option.items.filter { it.matchesDiet(code) }
        if (matchingItems.isEmpty()) null else option.copy(items = matchingItems)
    }
}

/** Checks whether a MenuItem carries the given Jamix diet code. */
private fun MenuItem.matchesDiet(code: String): Boolean {
    if (diets.isBlank()) return false
    val tokens = diets.split(",", " ", "/").map { it.trim().uppercase() }
    return code.uppercase() in tokens
}

@Composable
private fun MealOptionSection(option: MealOption) {
    if (option.name.isNotBlank()) {
        Text(
            text = option.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ESNCyanDark,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
    option.items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.diets.isNotBlank()) {
                    Text(
                        text = item.diets,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            if (item.price.isNotBlank()) {
                Text(
                    text = "€${item.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ESNCyanDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
