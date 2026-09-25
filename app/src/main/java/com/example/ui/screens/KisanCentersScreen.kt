package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.KisanCenterItem
import kotlinx.coroutines.launch

@Composable
fun KisanCentersScreen(
    geminiService: GeminiService,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedCity by remember { mutableStateOf("Multan") }
    var isLoading by remember { mutableStateOf(false) }

    val defaultCenters = remember {
        listOf(
            KisanCenterItem(
                name = "Punjab Agriculture Extension Directorate",
                type = "Government Extension",
                city = "Multan",
                address = "Old Shujabad Road, Agricultural Complex, Multan",
                phone = "061-9200234",
                services = "Free soil testing, certified pesticide subsidy coupons, spray advisories",
                rating = 4.6
            ),
            KisanCenterItem(
                name = "Fauji Fertilizer Company (FFC) Sona Center",
                type = "Agri Inputs & Advisory",
                city = "Multan",
                address = "Vehari Road, Industrial Area, Multan",
                phone = "061-6512300",
                services = "Subsidized Sona Urea/DAP, soil nutrition profiling, micronutrient kits",
                rating = 4.8
            ),
            KisanCenterItem(
                name = "Punjab Seed Corporation (PSC) Sales Hub",
                type = "Certified Seeds",
                city = "Multan",
                address = "Khanewal Road near Grain Market, Multan",
                phone = "061-9239011",
                services = "BTI certified cotton seed, approved wheat seed varieties (Fakhr-e-Bhakkar, Dilkash)",
                rating = 4.4
            ),
            KisanCenterItem(
                name = "Central Cotton Research Institute (CCRI)",
                type = "Research & Pathology",
                city = "Multan",
                address = "Old Shujabad Road, CCRI Complex, Multan",
                phone = "061-9201183",
                services = "Leaf curl virus screening, whitefly biological control cards (Chrysoperla)",
                rating = 4.7
            ),
            KisanCenterItem(
                name = "Zarai Taraqiati Bank (ZTBL) Zonal Office",
                type = "Agri Finance",
                city = "Multan",
                address = "Katchery Chowk, Multan Cantt",
                phone = "061-9200155",
                services = "Kisan credit card, tubewell solarization loans, tractor leasing",
                rating = 4.2
            )
        )
    }

    var centersList by remember { mutableStateOf(defaultCenters) }

    fun fetchCenters() {
        isLoading = true
        scope.launch {
            val result = geminiService.findKisanCentersWithMaps(selectedCity)
            isLoading = false
            result.onSuccess { items ->
                if (items.isNotEmpty()) {
                    centersList = items
                }
            }
        }
    }

    LaunchedEffect(selectedCity) {
        fetchCenters()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("kisan_centers_screen")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nearby Kisan Centers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Grounded with Google Maps (gemini-3.5-flash)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { fetchCenters() },
                enabled = !isLoading,
                modifier = Modifier.testTag("refresh_centers_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Centers", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // City Selector Chips
        val cities = listOf("Multan", "Faisalabad", "Sahiwal", "Bahawalpur", "Sargodha", "Hyderabad", "Sukkur", "Peshawar")
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
                    modifier = Modifier.testTag("center_city_${city.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grounding notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verified agricultural extension, seed suppliers, and dealer locations in $selectedCity, Pakistan.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Centers List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(centersList) { center ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kisan_center_card"),
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
                                    text = center.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = center.type,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Rating badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF8E1)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${center.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF795548))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Address
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(center.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (center.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(center.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Services: ${center.services}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
