package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esnmessenger.model.DailyMenu
import com.example.esnmessenger.model.MealOption
import com.example.esnmessenger.ui.theme.*
import com.example.esnmessenger.viewmodel.RestaurantsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RestaurantsScreen(viewModel: RestaurantsViewModel = viewModel()) {
    val menus by viewModel.menus.collectAsState()
    val isWeekend by viewModel.isWeekend.collectAsState()
    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                    text = todayLabel,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!isWeekend) {
                IconButton(
                    onClick = { viewModel.fetchAllMenus() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        if (isWeekend) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🍽️", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No service today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Campus restaurants are closed on weekends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menus) { dailyMenu ->
                    RestaurantCard(dailyMenu)
                }
            }
        }
    }
}

@Composable
private fun RestaurantCard(dailyMenu: DailyMenu) {
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
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ESNCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
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
                dailyMenu.mealOptions.isEmpty() -> {
                    Text(
                        text = "No menu available today",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> {
                    dailyMenu.mealOptions.forEachIndexed { index, option ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        MealOptionSection(option)
                    }
                }
            }
        }
    }
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
                    color = TextPrimary
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
