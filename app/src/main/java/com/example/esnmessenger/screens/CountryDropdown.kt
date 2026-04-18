package com.example.esnmessenger.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.esnmessenger.model.COUNTRY_FLAGS
import com.example.esnmessenger.ui.theme.ESNCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var inputText by remember(value) { mutableStateOf(value) }

    val filtered = remember(inputText) {
        val keys = COUNTRY_FLAGS.keys.sorted()
        if (inputText.isBlank()) keys
        else keys.filter { it.contains(inputText, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                expanded = true
            },
            label = { Text("Country") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filtered.isNotEmpty()) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = {
                expanded = false
                // Revert to last valid selection if user dismissed without choosing
                inputText = value
            }
        ) {
            filtered.take(60).forEach { country ->
                val flag = COUNTRY_FLAGS[country] ?: ""
                DropdownMenuItem(
                    text = { Text("$flag  $country") },
                    onClick = {
                        onValueChange(country)
                        inputText = country
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
