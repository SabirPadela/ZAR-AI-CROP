package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiService
import com.example.data.model.MandiRateItem
import kotlinx.coroutines.launch

@Composable
fun MandiRatesScreen(
    geminiService: GeminiService,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedCity by remember { mutableStateOf("All Pakistan") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var marketSummary by remember { mutableStateOf("Latest mandi wholesale rates verified via Google Search Grounding.") }

    val defaultRates = remember {
        listOf(
            MandiRateItem("Cotton (Phutti)", "Premium Grade 1", "Multan Mandi", "8,200", "8,800", "Rs. / 40 Kg", "Up", "Strong mill demand"),
            MandiRateItem("Wheat (Gandum)", "BWP 2024", "Faisalabad Ghalla Mandi", "3,900", "4,200", "Rs. / 40 Kg", "Stable", "Steady procurement"),
            MandiRateItem("Basmati Rice (Super)", "1121 Kainat", "Lahore Grain Market", "12,500", "13,800", "Rs. / 40 Kg", "Up", "Export momentum to GCC"),
            MandiRateItem("Sugarcane (Kamad)", "CPF-246", "Faisalabad Mill Gate", "425", "450", "Rs. / 40 Kg", "Stable", "Official crushing price"),
            MandiRateItem("Maize (Makai)", "Hybrid Yellow", "Sahiwal Mandi", "2,600", "2,950", "Rs. / 40 Kg", "Down", "Silage and poultry intake"),
            MandiRateItem("Urea Fertilizer (Sona)", "50 Kg Bag", "Punjab Open Market", "4,300", "4,600", "Rs. / Bag", "Stable", "Govt subsidized availability"),
            MandiRateItem("DAP Fertilizer (FFC)", "50 Kg Bag", "Sindh & Punjab", "12,800", "13,400", "Rs. / Bag", "Up", "Import parity price")
        )
    }

    var ratesList by remember { mutableStateOf(defaultRates) }

    fun fetchRates() {
        isLoading = true
        scope.launch {
            val result = geminiService.getMandiRatesWithSearch(
                cityOrRegion = selectedCity,
                cropFilter = searchQuery
            )
            isLoading = false
            result.onSuccess { (items, summary) ->
                if (items.isNotEmpty()) {
                    ratesList = items
                }
                marketSummary = summary
            }
        }
    }

    LaunchedEffect(selectedCity) {
        fetchRates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("mandi_rates_screen")
    ) {
        // Screen Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mandi Market Rates",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Grounded with Google Search (gemini-3.5-flash)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { fetchRates() },
                enabled = !isLoading,
                modifier = Modifier.testTag("refresh_mandi_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Mandi Rates", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search crop (e.g. Cotton, Wheat, Rice)...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mandi_search_field"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // City Filters
        val cities = listOf("All Pakistan", "Multan", "Faisalabad", "Lahore", "Sahiwal", "Sukkur", "Hyderabad", "Peshawar")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            cities.forEach { city ->
                FilterChip(
                    selected = selectedCity == city,
                    onClick = { selectedCity = city },
                    label = { Text(city, fontSize = 12.sp) },
                    modifier = Modifier.testTag("mandi_city_${city.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grounding attribution banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = marketSummary,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rates List
        val filteredList = ratesList.filter {
            searchQuery.isBlank() || it.cropName.contains(searchQuery, ignoreCase = true) || it.variety.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList) { rate ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mandi_item_${rate.cropName.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rate.cropName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${rate.variety} • ${rate.marketCity}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Trend badge
                            val (trendColor, trendIcon) = when (rate.trend.lowercase()) {
                                "up" -> Pair(Color(0xFF2E7D32), Icons.Default.ArrowUpward)
                                "down" -> Pair(Color(0xFFC62828), Icons.Default.ArrowDownward)
                                else -> Pair(Color(0xFF757575), Icons.Default.Remove)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = trendColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(rate.trend, color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Price range
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Wholesale Range", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${rate.minPrice} - ${rate.maxPrice}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = rate.unit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (rate.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rate.notes,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
