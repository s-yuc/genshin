package com.example.genshin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GachaViewModel : ViewModel() {
    private val _gachaResults = MutableStateFlow<List<GachaResult>>(emptyList())
    val gachaResults: StateFlow<List<GachaResult>> = _gachaResults.asStateFlow()

    fun importGachaResults(input: String) {
        if (input.isBlank()) {
            // サンプルデータ
            _gachaResults.value = listOf(
                GachaResult(90, "キャラ", "雷電将軍", "2024-01-01", 5),
                GachaResult(10, "武器", "流浪楽章", "2024-01-01", 4),
                GachaResult(1, "武器", "冷刃", "2024-01-01", 3),
                GachaResult(45, "キャラ", "ベネット", "2024-01-01", 4),
                GachaResult(78, "武器", "草薙の稲光", "2024-01-01", 5)
            )
            return
        }

        val results = mutableListOf<GachaResult>()
        val lines = input.trim().split("\n")

        for (line in lines) {
            // カンマまたはタブで分割
            val parts = if (line.contains("\t")) {
                line.split("\t")
            } else {
                line.split(",")
            }

            if (parts.size >= 4) {
                try {
                    val pullCount = parts[0].trim().toInt()
                    val type = parts[1].trim()
                    val name = parts[2].trim()
                    val date = parts[3].trim()
                    
                    // レアリティ判定の簡易ロジック（本来はマスターデータが必要）
                    // ここではデモ用に特定の名前や条件で判定
                    val rarity = when {
                        name.contains("雷電") || name.contains("草薙") -> 5
                        name.contains("ベネット") || name.contains("流浪") -> 4
                        else -> 3
                    }

                    results.add(GachaResult(pullCount, type, name, date, rarity))
                } catch (e: Exception) {
                    // パース失敗した行はスキップ
                }
            }
        }

        if (results.isNotEmpty()) {
            _gachaResults.value = results
        }
    }
}
