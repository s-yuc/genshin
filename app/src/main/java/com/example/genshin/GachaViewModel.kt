package com.example.genshin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

enum class SortOrder {
    ASCENDING, DESCENDING
}

enum class GachaCountMode {
    TOTAL, PITY
}

class GachaViewModel : ViewModel() {
    private val _rawGachaResults = MutableStateFlow<List<GachaResult>>(emptyList())
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    private val _countMode = MutableStateFlow(GachaCountMode.PITY)
    
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    val countMode: StateFlow<GachaCountMode> = _countMode.asStateFlow()

    val gachaResults: StateFlow<List<GachaResult>> = combine(_rawGachaResults, _sortOrder) { results, order ->
        when (order) {
            SortOrder.ASCENDING -> results.sortedBy { it.totalCount }
            SortOrder.DESCENDING -> results.sortedByDescending { it.totalCount }
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

    fun toggleCountMode() {
        _countMode.value = if (_countMode.value == GachaCountMode.PITY) {
            GachaCountMode.TOTAL
        } else {
            GachaCountMode.PITY
        }
    }

    fun importGachaResults(input: String) {
        if (input.isBlank()) {
            _rawGachaResults.value = calculatePityCounts(listOf(
                GachaResult(90, "キャラクター", "雷電将軍 (★5)", "2024-01-01", 5, 90),
                GachaResult(100, "武器", "流浪楽章 (★4)", "2024-01-01", 4, 100),
                GachaResult(178, "武器", "草薙の稲光 (★5)", "2024-01-01", 5, 178)
            ))
            return
        }

        val tempResults = mutableListOf<GachaResult>()
        val lines = input.split(Regex("\\R"))

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val parts = trimmedLine.split(Regex("[,\t]")).map { 
                it.trim().removeSurrounding("\"").trim() 
            }

            if (parts.size < 2 || parts[1].isBlank()) continue

            try {
                val pullCount = parts[0].toIntOrNull() ?: continue
                val col2 = parts[1]
                val col3 = if (parts.size >= 3) parts[2] else ""
                val col4 = if (parts.size >= 4) parts[3] else ""

                var type: String; var name: String; var date: String
                if (col2.contains(Regex("キャラ|武器|キャラクター"))) {
                    if (col3.isBlank()) continue
                    type = col2; name = col3; date = col4
                } else {
                    name = col2; type = col3; date = col4
                }
                if (name.isBlank()) continue

                val checkText = "$col2 $name"
                val rarity = when {
                    checkText.contains(Regex("[★☆]5|[★☆]５|\\(5\\)|\\(５\\)|\\[5\\]|\\[５\\]")) ||
                    checkText.contains(Regex("(?<!\\d)[5５](?!連)(?!\\d)")) ||
                    name.contains(Regex("雷電|ナヒーダ|鍾離|フリーナ|ヌヴィレット|神里|万葉|胡桃|夜蘭|エウルア|シロネン|ムアラニ|キィニチ|マーヴィカ|クロリンデ|ナヴィア|リオセスリ|千織|召使|アルレッキーノ|エミリエ|ティナリ|刻晴|モナ|ジン|ディルック|七七|ネフェル|魈|甘雨|白朮|セノ|リネ|放浪者")) -> 5
                    checkText.contains(Regex("[★☆]4|[★☆]４|\\(4\\)|\\(４\\)|\\[4\\]|\\[４\\]")) ||
                    checkText.contains(Regex("(?<!\\d)[4４](?!連)(?!\\d)")) ||
                    name.contains(Regex("ベネット|行秋|香菱|久岐忍|シュヴルーズ|ファルザン|カチーナ|嘉明|セトス|キャンディス|ヨォーヨ|シャルロット|リネット|フレミネ|綺良々|ミカ|レイラ|ドリー|コレイ|久岐忍|雲菫|ゴロー|トーマ|早柚|サラ|ロサリア|煙緋|ディオナ|辛炎|スクロース|重雲|北斗|フィッシュル|凝光|バーバラ|アンバー|リサ|ガイア|キョーコ|ノエル")) -> 4
                    else -> 3
                }

                tempResults.add(GachaResult(pullCount, type, name, date, rarity, totalCount = pullCount))
            } catch (e: Exception) {}
        }

        if (tempResults.isNotEmpty()) {
            _rawGachaResults.value = calculatePityCounts(tempResults)
        }
    }

    private fun calculatePityCounts(results: List<GachaResult>): List<GachaResult> {
        val sorted = results.sortedBy { it.totalCount }
        var last5StarTotal = 0
        return sorted.map { res ->
            val pity = res.totalCount - last5StarTotal
            if (res.rarity == 5) {
                val updatedRes = res.copy(pityCount = pity)
                last5StarTotal = res.totalCount
                updatedRes
            } else {
                res.copy(pityCount = pity)
            }
        }
    }
}
