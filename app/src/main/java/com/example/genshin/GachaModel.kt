package com.example.genshin

data class GachaResult(
    val pullCount: Int, // 何連目か
    val type: String,   // 武器/キャラ
    val name: String,   // 名称
    val date: String,   // 日付
    val rarity: Int = 3 // 表示用に名前などから判定する（デモ用）
)
