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
            _rawGachaResults.value = listOf(
                GachaResult(90, "キャラ(★5)", "雷電将軍", "2024-01-01", 5),
                GachaResult(10, "武器(★4)", "流浪楽章", "2024-01-01", 4),
                GachaResult(1, "武器", "冷刃", "2024-01-01", 3),
                GachaResult(45, "キャラ(★4)", "ベネット", "2024-01-01", 4),
                GachaResult(78, "武器(★5)", "草薙の稲光", "2024-01-01", 5)
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

            if (parts.size < 2 || parts[1].isBlank()) continue

            try {
                val pullCount = parts[0].toIntOrNull() ?: continue
                
                val col2 = parts[1] // ユーザー指定の優先判定箇所
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

                // レアリティ判定の超強化ロジック
                // 2つめの項目(col2)または名称(name)に含まれる情報をくまなく探す
                val checkText = "$col2 $name"
                
                val rarity = when {
                    // ★5 判定: 「5」という数字、または主要な星5キーワード
                    checkText.contains(Regex("[5５]")) || 
                    name.contains(Regex("雷電|ナヒーダ|鍾離|フリーナ|ヌヴィレット|神里|万葉|胡桃|夜蘭|エウルア|放浪者|魈|ニィロウ|セノ|ディシア|白朮|リネ|シロネン|ムアラニ|キィニチ|マーヴィカ|クロリンデ|ナヴィア|リオセスリ|千織|召使|アルレッキーノ|エミリエ|ティナリ|刻晴|モナ|ジン|ディルック|七七|ネフェル")) -> 5
                    
                    // ★4 判定: 「4」という数字、または主要な星4キーワード
                    checkText.contains(Regex("[4４]")) || 
                    name.contains(Regex("ベネット|行秋|香菱|久岐忍|シュヴルーズ|ファルザン|カチーナ|嘉明|セトス|キャンディス|ヨォーヨ|シャルロット|リネット|フレミネ|綺良々|ミカ|レイラ|ドリー|コレイ|久岐忍|雲菫|ゴロー|トーマ|早柚|サラ|ロサリア|煙緋|ディオナ|辛炎|スクロース|重雲|北斗|フィッシュル|凝光|バーバラ|アンバー|リサ|ガイア|キョーコ")) -> 4
                    
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
