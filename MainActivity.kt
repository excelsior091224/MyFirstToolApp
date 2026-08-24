package com.example.myfirsttoolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.myfirsttoolapp.ui.theme.MyFirstToolAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFirstToolAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ここで画面の部品（コンポーザブル関数）を呼び出す
                    CounterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// カウンター画面のUIを定義する関数
@Composable
fun CounterScreen(modifier: Modifier = Modifier) {
    // 状態を保持する変数（値が変化すると画面が自動で再描画される）
    var count by remember { mutableStateOf(0) }

    // 縦方向（Column）に要素を並べる
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 現在の数字を表示するテキスト
        Text(
            text = "カウント: $count",
            fontSize = 30.sp
        )
        
        // ボタン
        Button(
            onClick = { count++ } // ボタンが押されたら count を1増やす
        ) {
            Text(text = "増やす")
        }
    }
}
