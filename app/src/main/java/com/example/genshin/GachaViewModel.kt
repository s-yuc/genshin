package com.example.genshin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

enum class SortOrder {
    ASCENDING, DESCENDING
}

class GachaViewModel : ViewModel() {
    private val _rawGachaResults = MutableStateFlow<List<GachaResult>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // 読み込んだデータとソート順を合成して、常に最新のリストを表示用に公開する
    val gachaResults: StateFlow<List<GachaResult>> = combine(_rawGachaResults, _sortOrder) { results, order ->
        when (order) {
            SortOrder.ASCENDING -> results.sortedBy { it.pullCount }
            SortOrder.DESCENDING -> results.sortedByDescending { it.pullCount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.DESCENDING) {
            SortOrder.ASCENDING
        } else {
            SortOrder.DESCENDING
        }
    }

    fun importGachaResults(input: String) {
        if (input.isBlank()) {
            // デモ用サンプルデータ
            _rawGachaResults.value = listOf(
                GachaResult(90, "キャラ", "雷電将軍", "2024-01-01", 5),
                GachaResult(10, "武器", "流浪楽章", "2024-01-01", 4),
                GachaResult(1, "武器", "冷刃", "2024-01-01", 3),
                GachaResult(45, "キャラ", "ベネット", "2024-01-01", 4),
                GachaResult(78, "武器", "草薙の稲光", "2024-01-01", 5)
            )
            return
        }

        val results = mutableListOf<GachaResult>()
        val lines = input.split(Regex("\\R"))

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val parts = trimmedLine.split(Regex("[,\t]")).map { 
                it.trim().removeSurrounding("\"").trim() 
            }

            // キャラ名（2つめの項目）がブランクのものは取り込まない
            if (parts.size < 2 || parts[1].isBlank()) continue

            try {
                val pullCount = parts[0].toIntOrNull() ?: continue
                
                val col2 = parts[1]
                val col3 = if (parts.size >= 3) parts[2] else ""
                val col4 = if (parts.size >= 4) parts[3] else ""

                var type: String; var name: String; var date: String

                if (col2 == "キャラ" || col2 == "武器" || col2 == "キャラクター") {
                    if (col3.isBlank()) continue
                    type = col2; name = col3; date = col4
                } else {
                    name = col2
                    type = if (col3.isNotBlank() && !col3.contains(Regex("\\d{4}"))) col3 else "不明"
                    date = if (col4.isNotBlank()) col4 else if (col3.contains(Regex("\\d{4}"))) col3 else ""
                }

                if (name.isBlank()) continue

                val rarity = when {
                    name.contains(Regex("雷電|ナヒーダ|鍾離|フリーナ|ヌヴィレット|神里|万葉|胡桃|夜蘭|エウルア|シロネン|ムアラニ|キィニチ|マーヴィカ|クロリンデ|草薙|飛雷|萃光|若水|赤砂")) -> 5
                    name.contains(Regex("ベネット|行秋|香菱|久岐忍|シュヴルーズ|ファルザン|カチーナ|嘉明|セトス|流浪|祭礼|西風")) -> 4
                    else -> 3
                }
                results.add(GachaResult(pullCount, type, name, date, rarity))
            } catch (e: Exception) {}
        }
        if (results.isNotEmpty()) {
            _rawGachaResults.value = results
        }
    }
}
