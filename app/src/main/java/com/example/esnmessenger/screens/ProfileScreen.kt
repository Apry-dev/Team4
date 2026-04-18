package com.example.esnmessenger.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esnmessenger.model.COUNTRY_FLAGS
import com.example.esnmessenger.model.INTEREST_EMOJIS
import com.example.esnmessenger.model.INTERESTS
import com.example.esnmessenger.model.LANGUAGES
import com.example.esnmessenger.model.STUDENT_TYPES
import com.example.esnmessenger.model.YEARS
import com.example.esnmessenger.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private suspend fun uriToBase64(context: android.content.Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = BitmapFactory.decodeStream(stream)
            stream.close()
            val maxSize = 300
            val ratio = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height)
            val scaled = if (ratio < 1f)
                Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
            else original
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 65, output)
            Base64.encodeToString(output.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ProfilePhoto", "Encoding failed", e)
            null
        }
    }

private fun base64ToBitmap(base64: String): Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}

@Composable
fun ProfileScreen(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var profile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uid == null) { isLoading = false; return@LaunchedEffect }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                profile = doc.data
                photoBase64 = doc.getString("photoBase64")
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || uid == null) return@rememberLauncherForActivityResult
        isUploadingPhoto = true
        scope.launch {
            val base64 = uriToBase64(context, uri)
            if (base64 == null) { isUploadingPhoto = false; return@launch }
            withContext(Dispatchers.Main) {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("photoBase64", base64)
                    .addOnSuccessListener { photoBase64 = base64; isUploadingPhoto = false }
                    .addOnFailureListener { isUploadingPhoto = false }
            }
        }
    }

    val photoBitmap = remember(photoBase64) { photoBase64?.let { base64ToBitmap(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val name = profile?.get("name") as? String ?: ""
        val studentType = profile?.get("studentType") as? String ?: ""
        val country = profile?.get("country") as? String ?: ""
        val flag = COUNTRY_FLAGS.entries.find { it.key.equals(country, ignoreCase = true) }?.value

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable(enabled = !isUploadingPhoto) { photoPicker.launch("image/*") }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable(enabled = !isUploadingPhoto) { photoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase().ifEmpty { "?" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(ESNMagenta, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (flag != null) "$flag ${name.ifEmpty { "—" }}" else name.ifEmpty { "—" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (studentType.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                        Text(
                            text = studentType,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Edit / Close button
            if (!isLoading && profile != null) {
                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Cancel" else "Edit profile",
                        tint = Color.White
                    )
                }
            }
        }

        when {
            isLoading -> ProfileSkeleton()
            profile == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Could not load profile.", color = TextSecondary)
            }
            isEditing -> EditProfileContent(
                profile = profile!!,
                uid = uid!!,
                onSaved = { updatedProfile ->
                    profile = updatedProfile
                    isEditing = false
                }
            )
            else -> ViewProfileContent(profile!!, onLogout)
        }
    }
}

@Composable
private fun EditProfileContent(
    profile: Map<String, Any>,
    uid: String,
    onSaved: (Map<String, Any>) -> Unit
) {
    var name by remember { mutableStateOf(profile["name"] as? String ?: "") }
    var country by remember { mutableStateOf(profile["country"] as? String ?: "") }
    var university by remember { mutableStateOf(profile["university"] as? String ?: "") }
    var studentType by remember { mutableStateOf(profile["studentType"] as? String ?: "") }
    var major by remember { mutableStateOf(profile["major"] as? String ?: "") }
    var year by remember { mutableStateOf(profile["year"] as? String ?: "") }
    @Suppress("UNCHECKED_CAST")
    var selectedInterests by remember { mutableStateOf((profile["interests"] as? List<String> ?: emptyList()).toSet()) }
    @Suppress("UNCHECKED_CAST")
    var selectedLanguages by remember { mutableStateOf((profile["languages"] as? List<String> ?: emptyList()).toSet()) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ESNCyan, focusedLabelColor = ESNCyan)
        )

        // Country
        CountryDropdown(
            value = country,
            onValueChange = { country = it },
            modifier = Modifier.fillMaxWidth()
        )

        // University
        OutlinedTextField(
            value = university,
            onValueChange = { university = it },
            label = { Text("University") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ESNCyan, focusedLabelColor = ESNCyan)
        )

        // Student type
        Column {
            Text("Student Type", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                STUDENT_TYPES.forEach { type ->
                    FilterChip(
                        selected = studentType == type,
                        onClick = { studentType = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNCyan,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Major
        OutlinedTextField(
            value = major,
            onValueChange = { major = it },
            label = { Text("Major / Field of Study") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ESNCyan, focusedLabelColor = ESNCyan)
        )

        // Year
        Column {
            Text("Year of Study", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                YEARS.forEach { y ->
                    FilterChip(
                        selected = year == y,
                        onClick = { year = y },
                        label = { Text(y) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNCyan,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Interests
        Column {
            Text("Interests", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                INTERESTS.forEach { interest ->
                    val selected = interest in selectedInterests
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedInterests = if (selected) selectedInterests - interest
                            else selectedInterests + interest
                        },
                        label = { Text("${INTEREST_EMOJIS[interest] ?: ""} $interest") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNMagenta,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Languages
        Column {
            Text("Languages", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { lang ->
                    val selected = lang in selectedLanguages
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedLanguages = if (selected) selectedLanguages - lang
                            else selectedLanguages + lang
                        },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNCyan,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                if (name.isBlank() || country.isBlank() || university.isBlank() || major.isBlank() || studentType.isBlank() || year.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                if (selectedInterests.isEmpty()) {
                    errorMessage = "Select at least one interest"
                    return@Button
                }
                isSaving = true
                errorMessage = ""
                val updates = mapOf(
                    "name" to name,
                    "country" to country,
                    "university" to university,
                    "studentType" to studentType,
                    "major" to major,
                    "year" to year,
                    "interests" to selectedInterests.toList(),
                    "languages" to selectedLanguages.toList()
                )
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        isSaving = false
                        onSaved(profile + updates)
                    }
                    .addOnFailureListener {
                        isSaving = false
                        errorMessage = "Failed to save. Try again."
                    }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
        ) {
            if (isSaving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Text("Save changes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ViewProfileContent(profile: Map<String, Any>, onLogout: () -> Unit) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    val country = profile["country"] as? String ?: ""
    val university = profile["university"] as? String ?: ""
    val major = profile["major"] as? String ?: ""
    val year = profile["year"] as? String ?: ""
    val email = profile["email"] as? String ?: ""
    @Suppress("UNCHECKED_CAST")
    val interests = profile["interests"] as? List<String> ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val languages = profile["languages"] as? List<String> ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileCard(title = "About") {
            ProfileRow(label = "Email", value = email)
            ProfileRow(label = "Country", value = country)
        }

        ProfileCard(title = "Academic Info") {
            ProfileRow(label = "University", value = university)
            ProfileRow(label = "Major", value = major)
            ProfileRow(label = "Year", value = year)
        }

        if (languages.isNotEmpty()) {
            ProfileCard(title = "Languages") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    languages.forEach { lang ->
                        Surface(shape = RoundedCornerShape(20.dp), color = ESNCyan.copy(alpha = 0.12f)) {
                            Text(
                                text = lang,
                                color = ESNCyanDark,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (interests.isNotEmpty()) {
            ProfileCard(title = "Interests") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    interests.forEach { interest ->
                        val emoji = INTEREST_EMOJIS[interest] ?: ""
                        Surface(shape = RoundedCornerShape(20.dp), color = ESNMagenta.copy(alpha = 0.12f)) {
                            Text(
                                text = "$emoji $interest",
                                color = ESNMagenta,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = { showSignOutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign out", color = MaterialTheme.colorScheme.error)
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    TextButton(onClick = { showSignOutDialog = false; onLogout() }) {
                        Text("Sign out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(14.dp))
                    repeat(2) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
                        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ESNCyanDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.width(90.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
