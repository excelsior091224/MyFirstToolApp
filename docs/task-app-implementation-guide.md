# タスク管理アプリ 実装コードガイド

このファイルは「まず動く最小構成」を目的にした実装例です。
Viewベース（XML + RecyclerView）で、Room + MVVM の最初の到達点を作ります。

## 0. 前提: AGP 9系での注意点

AGP 9.0以降は「組み込みKotlinサポート」がデフォルトで有効になり、`org.jetbrains.kotlin.android` プラグインを明示的に適用すると衝突する（`already on the classpath with an unknown version` 等のエラーになる）。

このプロジェクトで使うKSP (`2.2.10-2.0.2`) は組み込みKotlinと併用すると `kotlin.sourceSets` 関連のエラーが出るため、以下の2点を `gradle.properties` に設定して従来方式（明示的な `kotlin-android` プラグイン + 旧DSL）に固定する。

```properties
# AGP 9の組み込みKotlinはKSPのソースセット登録と競合するため無効化する
android.builtInKotlin=false
# org.jetbrains.kotlin.android 2.2.10はAGP 9の新DSLに未対応のため無効化する
android.newDsl=false
```

この設定をせずに `kotlin-android` プラグインを適用すると、ビルドが以下のように失敗する。

1. `Error resolving plugin ... already on the classpath with an unknown version`
2. (root buildにプラグインをapply falseで追加後) `Cannot add extension with name 'kotlin'`
3. (`android.builtInKotlin=false` を忘れたまま) `Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.`
4. (`android.newDsl=false` を忘れたまま) `class ...ApplicationExtensionImpl cannot be cast to class ...BaseExtension`

## 1. 依存関係

### 1-0. build.gradle.kts（ルート）

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 1-1. gradle/libs.versions.toml

```toml
[versions]
agp = "9.3.2"
kotlin = "2.2.10"
ksp = "2.2.10-2.0.2"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
activityKtx = "1.8.0"
constraintlayout = "2.1.4"
recyclerview = "1.4.0"
lifecycle = "2.9.2"
room = "2.7.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
androidx-recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 1-2. app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.myfirsttoolapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.myfirsttoolapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

## 2. データ層（Room）

### 2-1. app/src/main/java/com/example/myfirsttoolapp/data/local/TaskEntity.kt

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

### 2-2. app/src/main/java/com/example/myfirsttoolapp/data/local/TaskDao.kt

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

### 2-3. app/src/main/java/com/example/myfirsttoolapp/data/local/AppDatabase.kt

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

## 3. Repository

### app/src/main/java/com/example/myfirsttoolapp/data/repository/TaskRepository.kt

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

## 4. ViewModel

### 4-1. app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskListUiState.kt

```kotlin
package com.example.myfirsttoolapp.ui.tasklist

import com.example.myfirsttoolapp.data.local.TaskEntity

data class TaskListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val inputTitle: String = ""
)
```

### 4-2. app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskListViewModel.kt

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

## 5. 画面（RecyclerView）

### 5-1. app/src/main/java/com/example/myfirsttoolapp/ui/tasklist/TaskAdapter.kt

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

### 5-2. app/src/main/res/layout/item_task.xml

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

### 5-3. app/src/main/res/layout/activity_main.xml

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
        android:hint="タスク名"
        android:layout_marginStart="16dp"
        android:layout_marginTop="16dp"
        android:layout_marginEnd="8dp"
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
        app:layout_constraintTop_toBottomOf="@id/inputTitleEdit"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 5-4. app/src/main/java/com/example/myfirsttoolapp/MainActivity.kt

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

## 6. まず確認する動作

- 追加ボタンでタスクが一覧に出る
- タスクタップで完了/未完了が切り替わる
- タスク長押しで削除できる
- アプリを再起動してもタスクが残る
