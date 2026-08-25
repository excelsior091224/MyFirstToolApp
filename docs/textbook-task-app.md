# Kotlin + Android タスクアプリ教科書

## はじめに

この教材は、今のプロジェクトを使って、Kotlin学習からAndroid実装までを段階的に進めるための実践ノートです。

特徴:

- 何をするか
- なぜそれをするか
- どのファイルを触るか
- 完了条件は何か

を毎節で明確にしています。

---

## 第1章 学習の地図

### 1-1. 目標

最初の到達点は、次の4つです。

1. タスクを追加できる
2. 一覧表示できる
3. 完了/未完了を切り替えできる
4. 再起動後もデータが残る

### 1-2. 技術の役割

- Kotlin: アプリのロジックを書く言語
- Room: 端末内データベース
- ViewModel: 画面ロジックの置き場
- RecyclerView: 一覧表示

### 1-3. 進め方

1. 依存関係を入れる
2. データ層を作る
3. ViewModelを作る
4. 画面につなぐ
5. 動作確認する

---

## 第2章 先に知っておくこと

### 2-1. ディレクトリの意味

- app/src/main/java/... : Kotlinコード
- app/src/main/res/layout : XMLレイアウト
- app/build.gradle.kts : アプリモジュールの設定
- gradle/libs.versions.toml : 依存ライブラリの一覧

### 2-2. 実装で増える主なフォルダ

- data/local
- data/repository
- ui/tasklist

---

## 第3章 Day 1 依存関係とRoom導入

### 3-1. この節のゴール

Roomが使える状態にして、TaskテーブルとDAOを作る。

### 3-2. 編集するファイル

- gradle/libs.versions.toml
- app/build.gradle.kts

### 3-3. 追加するライブラリ

- Room runtime
- Room ktx
- Room compiler (KSP)
- lifecycle-viewmodel-ktx
- lifecycle-runtime-ktx
- recyclerview

### 3-4. なぜ必要か

- Room runtime: DB本体
- Room ktx: suspendやFlowを使いやすくする
- Room compiler: アノテーション処理で実装コード生成
- lifecycle-viewmodel-ktx: ViewModel + coroutine
- recyclerview: リスト画面

### 3-5. 新規作成ファイル

- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskEntity.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskDao.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/AppDatabase.kt

### 3-6. 写経用コード

TaskEntity.kt

```kotlin
package com.example.myfirsttoolapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val dueDateEpochMillis: Long? = null,
    val priority: String = "MEDIUM",
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
```

TaskDao.kt

```kotlin
package com.example.myfirsttoolapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
```

AppDatabase.kt

```kotlin
package com.example.myfirsttoolapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_app.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

### 3-7. 完了チェック

- ビルドが通る
- Room関連のimportエラーがない

### 3-8. よくあるつまずき

- KSPプラグインを入れ忘れてコンパイルが通らない
- Room compilerをkspではなくimplementationに入れてしまう

---

## 第4章 Day 2 RepositoryとViewModel

### 4-1. この節のゴール

データ取得・追加・更新・削除をViewModelから呼べるようにする。

### 4-2. 新規作成ファイル

- app/src/main/java/com/example/myfirsttoolapp/data/repository/TaskRepository.kt
- app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskListUiState.kt
- app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskListViewModel.kt

### 4-3. 写経用コード

TaskRepository.kt

```kotlin
package com.example.myfirsttoolapp.data.repository

import com.example.myfirsttoolapp.data.local.TaskDao
import com.example.myfirsttoolapp.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()

    suspend fun addTask(title: String) {
        if (title.isBlank()) return
        taskDao.insert(TaskEntity(title = title.trim()))
    }

    suspend fun toggleCompleted(task: TaskEntity) {
        taskDao.update(
            task.copy(
                isCompleted = !task.isCompleted,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }
}
```

TaskListUiState.kt

```kotlin
package com.example.myfirsttoolapp.ui.tasklist

import com.example.myfirsttoolapp.data.local.TaskEntity

data class TaskListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val inputTitle: String = ""
)
```

TaskListViewModel.kt

```kotlin
package com.example.myfirsttoolapp.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myfirsttoolapp.data.local.TaskEntity
import com.example.myfirsttoolapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllTasks().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputTitle = value) }
    }

    fun onAddClicked() {
        val title = uiState.value.inputTitle
        viewModelScope.launch {
            repository.addTask(title)
            _uiState.update { it.copy(inputTitle = "") }
        }
    }

    fun onTaskClicked(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleCompleted(task)
        }
    }

    fun onTaskLongClicked(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    class Factory(
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskListViewModel(repository) as T
        }
    }
}
```

### 4-4. 完了チェック

- ViewModelのimportエラーがない
- 追加、完了切替、削除の関数が揃っている

---

## 第5章 Day 3 画面接続

### 5-1. この節のゴール

XML画面をタスク一覧に置き換え、ViewModelと接続する。

### 5-2. 新規/更新ファイル

- 新規: app/src/main/res/layout/item_task.xml
- 新規: app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskAdapter.kt
- 更新: app/src/main/res/layout/activity_main.xml
- 更新: app/src/main/java/com/example/myfirsttoolapp/MainActivity.kt

### 5-3. 役割

- item_task.xml: 一行分の見た目
- TaskAdapter: データを各行に表示
- activity_main.xml: 入力欄 + 追加ボタン + 一覧
- MainActivity: ボタンクリックや一覧更新を橋渡し

### 5-4. 写経用コード

item_task.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/taskTitleText"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingHorizontal="16dp"
    android:paddingVertical="12dp"
    android:textSize="16sp" />
```

TaskAdapter.kt

```kotlin
package com.example.myfirsttoolapp.ui.tasklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirsttoolapp.R
import com.example.myfirsttoolapp.data.local.TaskEntity

class TaskAdapter(
    private val onClick: (TaskEntity) -> Unit,
    private val onLongClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.taskTitleText)

        fun bind(
            item: TaskEntity,
            onClick: (TaskEntity) -> Unit,
            onLongClick: (TaskEntity) -> Unit
        ) {
            titleText.text = if (item.isCompleted) "✓ ${item.title}" else item.title
            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}
```

activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <EditText
        android:id="@+id/inputTitleEdit"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:layout_marginTop="16dp"
        android:layout_marginEnd="8dp"
        android:hint="タスク名"
        app:layout_constraintEnd_toStartOf="@id/addTaskButton"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/addTaskButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="16dp"
        android:text="追加"
        app:layout_constraintBaseline_toBaselineOf="@id/inputTitleEdit"
        app:layout_constraintEnd_toEndOf="parent" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/taskRecyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="12dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/inputTitleEdit" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

MainActivity.kt

```kotlin
package com.example.myfirsttoolapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirsttoolapp.data.local.AppDatabase
import com.example.myfirsttoolapp.data.repository.TaskRepository
import com.example.myfirsttoolapp.ui.tasklist.TaskAdapter
import com.example.myfirsttoolapp.ui.tasklist.TaskListViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TaskListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val repository = TaskRepository(AppDatabase.getInstance(this).taskDao())
        viewModel = ViewModelProvider(this, TaskListViewModel.Factory(repository))[TaskListViewModel::class.java]

        val inputTitleEdit = findViewById<EditText>(R.id.inputTitleEdit)
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)
        val taskRecyclerView = findViewById<RecyclerView>(R.id.taskRecyclerView)

        val adapter = TaskAdapter(
            onClick = { task -> viewModel.onTaskClicked(task) },
            onLongClick = { task -> viewModel.onTaskLongClicked(task) }
        )
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = adapter

        addTaskButton.setOnClickListener {
            viewModel.onInputChanged(inputTitleEdit.text.toString())
            viewModel.onAddClicked()
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (inputTitleEdit.text.toString() != state.inputTitle) {
                    inputTitleEdit.setText(state.inputTitle)
                }
                adapter.submitList(state.tasks)
            }
        }
    }
}
```

### 5-5. 手順の意味（なぜこの順で書くか）

1. レイアウトを先に作る
   理由: 画面部品のIDが決まるので、Activity側でfindViewByIdを書きやすい。

2. Adapterを作る
   理由: 一覧表示の責務を分離し、Activityを肥大化させないため。

3. MainActivityで接続する
   理由: UIイベントとViewModelを最後に配線すると、依存関係が整理される。

### 5-6. 完了チェック

- タスク追加できる
- タップで完了/未完了切替
- 長押しで削除
- 再起動後も残る

---

## 第6章 実装の進め方（作業手順書）

### 6-1. 1日の作業テンプレート

1. 目標を1つに絞る
   例: 今日はTaskEntityとTaskDaoだけ完成させる。

2. 変更ファイルを先に宣言する
   例: data/local配下の3ファイルだけ編集する。

3. 実装後に必ずビルドする

4. 学習ログに5行だけ残す

### 6-2. 推奨の編集順

1. 設定ファイル
2. data/local
3. repository
4. viewmodel
5. activity_main.xml
6. MainActivity

### 6-3. ターミナル確認コマンド

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:lintDebug
```

### 6-4. 失敗時の切り分け

1. コンパイルエラーか実行時エラーかを分ける
2. コンパイルエラーならimportと依存関係を確認
3. 実行時エラーならLogcatで最初の例外を確認

---

## 第7章 エラー対処の早見表

### 7-1. Cannot find symbol Room

原因:

- Room依存不足

対処:

- app/build.gradle.kts と libs.versions.toml を再確認

### 7-2. KSP関連のエラー

原因:

- プラグイン定義の不足
- compilerの設定ミス

対処:

- pluginsにksp
- dependenciesに ksp(room-compiler)

### 7-3. Database access on main thread

原因:

- suspendではない関数からDBアクセス

対処:

- ViewModelの viewModelScope.launch で呼ぶ

### 7-4. RecyclerViewが表示されない

原因:

- layoutManager未設定
- adapter未設定

対処:

- MainActivityで以下2行があるか確認

```kotlin
taskRecyclerView.layoutManager = LinearLayoutManager(this)
taskRecyclerView.adapter = adapter
```

---

## 第8章 学習の確認問題

1. Repositoryを挟むメリットを説明できるか
2. TaskEntityのどの項目が永続化対象か説明できるか
3. Flowで一覧更新する理由を説明できるか
4. ViewModelを使わずActivityにロジックを置くと何が困るか

5. ListAdapter + DiffUtilを使う理由を説明できるか

---

## 第9章 次の拡張

最小版が終わったら、次を追加する。

1. 優先度（HIGH/MEDIUM/LOW）選択
2. 期限入力
3. 検索
4. ソート
5. フィルタ（未完了のみ）

---

## 第10章 ミニ演習（教科書ドリル）

### 演習1

TaskEntityに priority を enumで扱う準備を入れる。

### 演習2

空文字で追加ボタンを押したとき、Toastで警告を表示する。

### 演習3

完了タスクを薄い色で表示する。

### 演習4

長押し削除の前に確認ダイアログを出す。

---

## 付録A 用語集

- Entity: DBテーブルに対応するデータクラス
- DAO: DBアクセスの窓口
- Repository: データ取得方法を隠蔽する層
- ViewModel: 画面状態とイベント処理の管理者
- StateFlow: 状態の監視に使うデータストリーム

---

## 付録B チェックシート

Day 1を実作業で進めるときは以下を使う。

- docs/day1-implementation-checksheet.md
