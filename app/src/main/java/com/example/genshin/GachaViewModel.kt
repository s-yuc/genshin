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
            // デモ用サンプルデータ
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
            // タブまたはカンマで分割し、前後の空白や引用符(")を削除
            val parts = (if (line.contains("\t")) {
                line.split("\t")
            } else {
                line.split(",")
            }).map { it.trim().removeSurrounding("\"") }

            if (parts.size >= 4) {
                try {
                    val pullCount = parts[0].toInt()
                    val type = parts[1]
                    val name = parts[2]
                    val date = parts[3]
                    
                    // レアリティ判定ロジック
                    // ガチャ結果の名前に基づいてレアリティを推測（本来はマスタデータが必要）
                    val rarity = when {
                        // 星5の例
                        name.contains("雷電") || name.contains("ナヒーダ") || name.contains("草薙") || 
                        name.contains("神里") || name.contains("鍾離") || name.contains("エウルア") -> 5
                        // 星4の例
                        name.contains("ベネット") || name.contains("流浪") || name.contains("行秋") || 
                        name.contains("香菱") || name.contains("フィッシュル") -> 4
                        // その他は星3
                        else -> 3
                    }

                    results.add(GachaResult(pullCount, type, name, date, rarity))
                } catch (e: Exception) {
                    // 数値変換失敗（ヘッダー行など）は無視して次へ
                }
            }
        }

        if (results.isNotEmpty()) {
            // 新しい順（日付降順）に並び替えてセット
            _gachaResults.value = results.reversed()
        }
    }
}
