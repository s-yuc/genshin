package com.example.genshin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GachaViewModel : ViewModel() {
    private val _gachaResults = MutableStateFlow<List<GachaResult>>(emptyList())
    val gachaResults: StateFlow<List<GachaResult>> = _gachaResults.asStateFlow()

    fun importGachaResults(jsonString: String) {
        // For simplicity, let's simulate parsing or just add some sample data if input is empty
        // In a real app, we'd use kotlinx.serialization or Gson
        if (jsonString.isBlank()) {
            _gachaResults.value = listOf(
                GachaResult("Raiden Shogun", 5, "Character", "2023-01-01"),
                GachaResult("The Widsith", 4, "Weapon", "2023-01-01"),
                GachaResult("Cool Steel", 3, "Weapon", "2023-01-01"),
                GachaResult("Bennett", 4, "Character", "2023-01-01"),
                GachaResult("Engulfing Lightning", 5, "Weapon", "2023-01-01")
            )
        } else {
            // Placeholder for real parsing logic
        }
    }
}
