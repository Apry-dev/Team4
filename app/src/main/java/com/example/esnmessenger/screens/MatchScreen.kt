package com.example.esnmessenger.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.esnmessenger.model.COUNTRY_FLAGS
import com.example.esnmessenger.model.INTEREST_EMOJIS
import com.example.esnmessenger.model.LANGUAGES
import com.example.esnmessenger.model.STUDENT_TYPES
import com.example.esnmessenger.ui.theme.ESNCyan
import com.example.esnmessenger.ui.theme.ESNCyanDark
import com.example.esnmessenger.ui.theme.ESNCyanLight
import com.example.esnmessenger.ui.theme.ShimmerBox
import com.example.esnmessenger.viewmodel.MatchProfile
import com.example.esnmessenger.viewmodel.MatchViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MatchScreen(onOpenChat: (String) -> Unit) {
    val viewModel: MatchViewModel = viewModel()
    val candidates by viewModel.candidates.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val matchedWith by viewModel.matchedWith.collectAsState()
    val myInterests by viewModel.myInterests.collectAsState()
    val filterLanguage by viewModel.filterLanguage.collectAsState()
    val filterStudentType by viewModel.filterStudentType.collectAsState()
    val pendingMatchNotification by viewModel.pendingMatchNotification.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    val activeFilters = listOfNotNull(filterLanguage, filterStudentType).size

    pendingMatchNotification?.let { fromName ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingNotification() },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎉", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("New Match!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                }
            },
            text = {
                Text(
                    "$fromName connected with you while you were away!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearPendingNotification() },
                    colors = ButtonDefaults.buttonColors(containerColor = ESNCyan),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Check my chats") }
            }
        )
    }

    matchedWith?.let { profile ->
        MatchDialog(
            profile = profile,
            onMessage = { onOpenChat(profile.uid); viewModel.clearMatch() },
            onDismiss = { viewModel.clearMatch() }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            currentLanguage = filterLanguage,
            currentStudentType = filterStudentType,
            onApply = { lang, type ->
                viewModel.setFilterLanguage(lang)
                viewModel.setFilterStudentType(type)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                "Meet People",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                }
                if (activeFilters > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = Color.White,
                        contentColor = ESNCyan
                    ) {
                        Text(activeFilters.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> MatchCardSkeleton()
                candidates.isEmpty() -> EmptyMatchState()
                else -> CardStack(
                    candidates = candidates,
                    myInterests = myInterests,
                    onLike = { viewModel.like(it) },
                    onPass = { viewModel.pass(it) }
                )
            }
        }
    }
}

@Composable
private fun CardStack(
    candidates: List<MatchProfile>,
    myInterests: Set<String>,
    onLike: (MatchProfile) -> Unit,
    onPass: (MatchProfile) -> Unit
) {
    val current = candidates[0]
    val next = candidates.getOrNull(1)

    val offsetX = remember(current.uid) { Animatable(0f) }
    val offsetY = remember(current.uid) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val threshold = screenWidthPx * 0.35f

    val swipeProgress = (offsetX.value / (screenWidthPx * 0.5f)).coerceIn(-1f, 1f)
    val absProgress = abs(swipeProgress)
    val backScale = 0.93f + 0.07f * absProgress
    val backOffsetYDp = (20f - 20f * absProgress).dp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (next != null) {
            ProfileCard(
                profile = next,
                myInterests = myInterests,
                swipeProgress = 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                        translationY = with(density) { backOffsetYDp.toPx() }
                    },
                onLike = {},
                onPass = {}
            )
        }

        key(current.uid) {
            var hapticFired by remember { mutableStateOf(false) }

            ProfileCard(
                profile = current,
                myInterests = myInterests,
                swipeProgress = swipeProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        rotationZ = (offsetX.value / screenWidthPx) * 12f
                    }
                    .pointerInput(current.uid) {
                        detectDragGestures(
                            onDragStart = { hapticFired = false },
                            onDragEnd = {
                                scope.launch {
                                    when {
                                        offsetX.value > threshold -> {
                                            offsetX.animateTo(screenWidthPx * 2f, tween(280))
                                            onLike(current)
                                        }
                                        offsetX.value < -threshold -> {
                                            offsetX.animateTo(-screenWidthPx * 2f, tween(280))
                                            onPass(current)
                                        }
                                        else -> {
                                            launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                                            launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                                        }
                                    }
                                }
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + drag.x)
                                    offsetY.snapTo(offsetY.value + drag.y)
                                }
                                if (!hapticFired && abs(offsetX.value) > threshold) {
                                    hapticFired = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    },
                onLike = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        offsetX.animateTo(screenWidthPx * 2f, tween(280))
                        onLike(current)
                    }
                },
                onPass = {
                    scope.launch {
                        offsetX.animateTo(-screenWidthPx * 2f, tween(280))
                        onPass(current)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(
    profile: MatchProfile,
    myInterests: Set<String>,
    swipeProgress: Float,
    modifier: Modifier = Modifier,
    onLike: () -> Unit,
    onPass: () -> Unit
) {
    val bitmap = remember(profile.photoBase64) {
        profile.photoBase64?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val flag = COUNTRY_FLAGS[profile.country] ?: ""
    val commonCount = profile.interests.count { it in myInterests }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = profile.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            profile.name.take(1).uppercase(),
                            fontSize = 80.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        profile.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (flag.isNotEmpty()) Text(flag, fontSize = 22.sp)
                }

                if (swipeProgress > 0.1f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(20.dp)
                            .graphicsLayer { rotationZ = -20f },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF22C55E).copy(alpha = (swipeProgress * 1.5f).coerceIn(0f, 0.9f)),
                        border = BorderStroke(2.5.dp, Color(0xFF22C55E))
                    ) {
                        Text(
                            "CONNECT",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (swipeProgress < -0.1f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .graphicsLayer { rotationZ = 20f },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = (abs(swipeProgress) * 1.5f).coerceIn(0f, 0.9f)),
                        border = BorderStroke(2.5.dp, Color(0xFFEF4444))
                    ) {
                        Text(
                            "PASS",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        profile.university,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.year.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = ESNCyanLight) {
                            Text(
                                profile.year,
                                style = MaterialTheme.typography.labelSmall,
                                color = ESNCyanDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (profile.major.isNotEmpty()) {
                    Text(
                        profile.major,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (profile.interests.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (commonCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ESNCyan.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ESNCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    "★ $commonCount in common",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ESNCyanDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        profile.interests.take(4).forEach { interest ->
                            val emoji = INTEREST_EMOJIS[interest] ?: ""
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (interest in myInterests)
                                    ESNCyan.copy(alpha = 0.08f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "$emoji $interest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (interest in myInterests) ESNCyanDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (profile.languages.isNotEmpty()) {
                    Text(
                        "🗣️ " + profile.languages.take(4).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPass,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Pass", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pass", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onLike,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Connect", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Connect", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(18.dp), shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp), shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        ShimmerBox(modifier = Modifier.width(80.dp).height(26.dp), shape = RoundedCornerShape(20.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp), shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp))
                    ShimmerBox(modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(
    currentLanguage: String?,
    currentStudentType: String?,
    onApply: (String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var selectedType by remember { mutableStateOf(currentStudentType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter people", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Student Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    STUDENT_TYPES.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ESNCyan,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Language", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LANGUAGES.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = if (selectedLanguage == lang) null else lang },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ESNCyan,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedLanguage, selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = { onApply(null, null) }) { Text("Clear filters") }
        }
    )
}

@Composable
private fun EmptyMatchState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("empty ghost.json"))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(220.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "You've seen everyone!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Check back later for new students joining ESN",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatchDialog(
    profile: MatchProfile,
    onMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("It's a Match!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Text(
                "You and ${profile.name} both want to connect!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onMessage,
                colors = ButtonDefaults.buttonColors(containerColor = ESNCyan),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send a message") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep browsing") }
        }
    )
}
