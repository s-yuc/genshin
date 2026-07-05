package com.example.genshin

data class GachaResult(
    val name: String,
    val rarity: Int, // 3, 4, 5
    val type: String, // "Character" or "Weapon"
    val date: String
)
