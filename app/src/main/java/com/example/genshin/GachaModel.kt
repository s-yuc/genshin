package com.example.genshin

data class GachaResult(
    val pullCount: Int,  // 入力された元の数値
    val type: String,    // 武器/キャラ
    val name: String,    // 名称
    val date: String,    // 日付
    val rarity: Int = 3, // レアリティ
    val totalCount: Int = 0, // 累計のカウント
    val pityCount: Int = 0   // ☆5リセット後のカウント
)
