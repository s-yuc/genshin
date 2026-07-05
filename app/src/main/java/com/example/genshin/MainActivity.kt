package com.example.genshin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genshin.ui.theme.GenshinTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GachaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenshinTheme {
                var showImportDialog by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("原神ガチャ履歴", fontWeight = FontWeight.ExtraBold) },
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
                            }
                        )
                    }
                }
            }
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
    // レアリティに応じた背景グラデーションと星の色
    val (gradientColors, starColor) = when (result.rarity) {
        5 -> listOf(Color(0xFFF3D17E), Color(0xFFC07E33)) to Color(0xFFFFEB3B) // 金色
        4 -> listOf(Color(0xFFA279C3), Color(0xFF6E4A8E)) to Color(0xFFE040FB) // 紫色
        else -> listOf(Color(0xFF7B8E9B), Color(0xFF515969)) to Color(0xFFB0BEC5) // 青/灰
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
                Column {
                    Text(
                        text = result.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("データの取り込み") },
        text = {
            Column {
                Text(
                    text = "ガチャ結果を貼り付けてください（空のままインポートするとサンプルデータが表示されます）",
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("ここにデータをペースト...") }
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
