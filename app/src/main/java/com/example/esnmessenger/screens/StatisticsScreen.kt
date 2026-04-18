package com.example.esnmessenger.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esnmessenger.ui.theme.*
import com.example.esnmessenger.viewmodel.MessagingStats
import com.example.esnmessenger.viewmodel.StatisticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen() {
    val vm: StatisticsViewModel = viewModel()
    val stats by vm.stats.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "My Stats",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Messaging activity insights",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ESNCyan)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading your stats…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error banner
                if (error != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    error!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Empty state
                if (stats.totalSent == 0 && stats.totalReceived == 0 && error == null) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📊", fontSize = 64.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No stats yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Start chatting to see your messaging statistics here!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // 1. Overview 2×2 grid
                    item { OverviewGrid(stats) }

                    // 2. Last 7 days bar chart
                    item {
                        SectionCard(title = "Last 7 Days", icon = Icons.Default.DateRange) {
                            Spacer(Modifier.height(12.dp))
                            WeeklyBarChart(stats.last7DaysActivity)
                        }
                    }

                    // 3. Activity patterns
                    item {
                        SectionCard(title = "Activity Patterns", icon = Icons.Default.Schedule) {
                            Spacer(Modifier.height(4.dp))
                            if (stats.mostActiveDayOfWeek.isNotEmpty()) {
                                StatRow(Icons.Default.CalendarToday, "Most active day", stats.mostActiveDayOfWeek)
                            }
                            if (stats.mostActiveHour >= 0) {
                                StatRow(Icons.Default.AccessTime, "Busiest hour", formatHour(stats.mostActiveHour))
                            }
                            StatRow(
                                Icons.Default.TrendingUp, "Avg messages/day",
                                if (stats.avgMessagesPerDay < 0.1f) "< 1"
                                else "%.1f".format(stats.avgMessagesPerDay)
                            )
                            StatRow(Icons.Default.CheckCircle, "Days active", "${stats.totalDaysActive}")
                            stats.avgResponseTimeMinutes?.let { mins ->
                                StatRow(Icons.Default.Reply, "Avg response time", formatResponseTime(mins))
                            }
                        }
                    }

                    // 4. Streaks
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StreakCard(
                                title = "Current Streak",
                                value = stats.currentStreak,
                                unit = if (stats.currentStreak == 1) "day" else "days",
                                color = Color(0xFFF97316),
                                emoji = "🔥",
                                modifier = Modifier.weight(1f)
                            )
                            StreakCard(
                                title = "Longest Streak",
                                value = stats.longestStreak,
                                unit = if (stats.longestStreak == 1) "day" else "days",
                                color = Color(0xFF8B5CF6),
                                emoji = "🏆",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 5. Top conversations
                    if (stats.mostChattedWith.isNotEmpty()) {
                        item {
                            SectionCard(title = "Top Conversations", icon = Icons.Default.Forum) {
                                Spacer(Modifier.height(4.dp))
                                stats.mostChattedWith.forEachIndexed { i, (name, count) ->
                                    TopConvoRow(rank = i + 1, name = name, count = count)
                                    if (i < stats.mostChattedWith.lastIndex) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Message insights
                    item {
                        SectionCard(title = "Message Insights", icon = Icons.Default.Create) {
                            Spacer(Modifier.height(4.dp))
                            StatRow(Icons.Default.Subject, "Avg words/message", "%.1f".format(stats.avgWordCount))
                            StatRow(Icons.Default.Star, "Total words sent", formatCompact(stats.totalWordsSent))
                            stats.firstMessageDate?.let { date ->
                                StatRow(
                                    Icons.Default.DateRange, "First message",
                                    SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(date)
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun OverviewGrid(stats: MessagingStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStatCard(
                label = "Sent",
                value = formatCompact(stats.totalSent),
                icon = Icons.Default.Send,
                tint = ESNCyan,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                label = "Received",
                value = formatCompact(stats.totalReceived),
                icon = Icons.Default.ChatBubble,
                tint = ESNMagenta,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStatCard(
                label = "Conversations",
                value = stats.totalConversations.toString(),
                icon = Icons.Default.Group,
                tint = ESNCyan,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                label = "Words Sent",
                value = formatCompact(stats.totalWordsSent),
                icon = Icons.Default.Create,
                tint = ESNMagentaDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BigStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ESNCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun WeeklyBarChart(data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return
    val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val slotWidth = size.width / data.size
        val barWidth = slotWidth * 0.48f
        val maxBarH = size.height * 0.88f

        data.forEachIndexed { i, (_, count) ->
            val fraction = count.toFloat() / maxVal
            val barH = (fraction * maxBarH).coerceAtLeast(if (count > 0) 4f else 0f)
            val x = slotWidth * i + (slotWidth - barWidth) / 2f
            val y = size.height - barH
            drawRoundRect(
                color = if (count > 0) ESNCyan else ESNCyan.copy(alpha = 0.12f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(6f)
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    Row(Modifier.fillMaxWidth()) {
        data.forEach { (label, count) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (count > 0) {
                    Text(
                        count.toString(),
                        fontSize = 9.sp,
                        color = ESNCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (label == "Today") ESNCyan else TextSecondary,
                    fontWeight = if (label == "Today") FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StreakCard(
    title: String,
    value: Int,
    unit: String,
    color: Color,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopConvoRow(rank: Int, name: String, count: Int) {
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank." }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(medal, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            "$count msg${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}

private fun formatResponseTime(minutes: Double): String = when {
    minutes < 1.0 -> "< 1 min"
    minutes < 60.0 -> "${minutes.toInt()} min"
    else -> "${(minutes / 60).toInt()}h ${(minutes % 60).toInt()}m"
}

private fun formatCompact(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000 -> "${n / 1_000}k"
    else -> n.toString()
}
