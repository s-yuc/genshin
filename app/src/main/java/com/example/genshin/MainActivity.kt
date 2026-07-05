package com.example.genshin

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genshin.ui.theme.GenshinTheme
import java.io.InputStream

class MainActivity : ComponentActivity() {
    private val viewModel: GachaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenshinTheme {
                var showImportDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                // ファイル選択用のランチャー
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult

                    val content = readTextFromUri(uri)
                    when (content) {
                        "ERROR_EXCEL_BINARY" -> {
                            Toast.makeText(context, "Excel(.xlsx)は直接読み込めません。Googleドライブ上で「Googleスプレッドシートとして保存」したファイルを選択するか、CSVで保存し直してください。", Toast.LENGTH_LONG).show()
                        }
                        null, "" -> {
                            Toast.makeText(context, "取得失敗。左上のメニューから『Googleドライブ』を直接選び、ファイルを選択してみてください。", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            viewModel.importGachaResults(content)
                            Toast.makeText(context, "インポート完了しました", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("原神ガチャ履歴記録", fontWeight = FontWeight.ExtraBold) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "インポート")
                        }
                    }
                ) { innerPadding ->
                    GachaListScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showImportDialog) {
                        ImportDialog(
                            onDismiss = { showImportDialog = false },
                            onImport = { data ->
                                viewModel.importGachaResults(data)
                                showImportDialog = false
                            },
                            onPickFile = {
                                // 仮想ファイルを含め、あらゆる形式を選択対象にする
                                filePickerLauncher.launch(arrayOf(
                                    "text/*",
                                    "application/vnd.google-apps.spreadsheet",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "application/octet-stream"
                                ))
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * URIからテキストを読み込む。
     * Googleスプレッドシートのような仮想ドキュメントをCSVとして引き出す処理を強化。
     */
    private fun readTextFromUri(uri: Uri): String? {
        val resolver = contentResolver
        
        // 1. まず、Googleドライブ側の変換機能(CSVエクスポート)を試みる
        try {
            val types = resolver.getStreamTypes(uri, "text/csv")
            if (!types.isNullOrEmpty()) {
                resolver.openTypedAssetFileDescriptor(uri, "text/csv", null)?.use { afd ->
                    val text = afd.createInputStream().bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) return text
                }
            }
        } catch (e: Exception) {
            Log.e("Genshin", "CSV export attempt failed: ${e.message}")
        }

        // 2. CSVがダメなら、プレーンテキストとしてのエクスポートを試みる
        try {
            val types = resolver.getStreamTypes(uri, "text/plain")
            if (!types.isNullOrEmpty()) {
                resolver.openTypedAssetFileDescriptor(uri, "text/plain", null)?.use { afd ->
                    val text = afd.createInputStream().bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) return text
                }
            }
        } catch (e: Exception) {
            Log.e("Genshin", "Text export attempt failed: ${e.message}")
        }

        // 3. 通常のファイルストリームとしての読み込み
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().use { it.readText() }
                // XLSXなどのZIPバイナリ(先頭がPK)を検知
                if (text.startsWith("PK")) "ERROR_EXCEL_BINARY" else text
            }
        } catch (e: Exception) {
            Log.e("Genshin", "Standard stream attempt failed: ${e.message}")
            null
        }
    }
}

@Composable
fun GachaListScreen(viewModel: GachaViewModel, modifier: Modifier = Modifier) {
    val results by viewModel.gachaResults.collectAsState()

    if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "履歴がありません。「+」ボタンからデータをインポートしてください。",
                color = Color.Gray,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(results) { result ->
                GachaItemCard(result)
            }
        }
    }
}

@Composable
fun GachaItemCard(result: GachaResult) {
    val (gradientColors, starColor) = when (result.rarity) {
        5 -> listOf(Color(0xFFF3D17E), Color(0xFFC07E33)) to Color(0xFFFFEB3B)
        4 -> listOf(Color(0xFFA279C3), Color(0xFF6E4A8E)) to Color(0xFFE040FB)
        else -> listOf(Color(0xFF7B8E9B), Color(0xFF515969)) to Color(0xFFB0BEC5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = result.type,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${result.pullCount}連目",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "★".repeat(result.rarity),
                        color = starColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.date,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onPickFile: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("データの取り込み") },
        text = {
            Column {
                Text(
                    text = "Googleドライブ等のスプレッドシートを選択するか、内容を直接貼り付けてください。",
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("スプレッドシートを選択 (Googleドライブ)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("または直接貼り付け:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("例:\n75\tキャラ\t雷電将軍\t2024-01-01") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(text) }) {
                Text("インポート")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
