# Day 1 実装チェックシート（Room導入）

このシートは、第3章 Day 1 の実装を漏れなく終えるためのチェックリストです。

使い方:

1. 上から順番に進める
2. 完了したらチェックを付ける
3. 詰まったら「トラブル欄」に記録する

---

## 0. 今日のゴール

- Roomをプロジェクトに導入する
- TaskEntity / TaskDao / AppDatabase を作成する
- ビルドが通る状態にする

完了条件:

- `./gradlew :app:assembleDebug` が成功する

---

## 1. 事前準備

- [ ] 作業前に現在の変更状態を確認した
- [ ] 今日触るファイルを確認した

今日触る予定のファイル:

- gradle/libs.versions.toml
- app/build.gradle.kts
- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskEntity.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskDao.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/AppDatabase.kt

---

## 2. 依存関係の追加

### 2-1. libs.versions.toml

- [ ] `kotlin` バージョンを追加した
- [ ] `ksp` バージョンを追加した
- [ ] `room` バージョンを追加した
- [ ] `lifecycle` バージョンを追加した
- [ ] `recyclerview` バージョンを追加した
- [ ] Room / Lifecycle / RecyclerView の `libraries` 定義を追加した
- [ ] `plugins` に `kotlin-android` と `ksp` を追加した

### 2-2. app/build.gradle.kts

- [ ] plugins に `alias(libs.plugins.kotlin.android)` を追加した
- [ ] plugins に `alias(libs.plugins.ksp)` を追加した
- [ ] dependencies に `room-runtime` を追加した
- [ ] dependencies に `room-ktx` を追加した
- [ ] dependencies に `ksp(room-compiler)` を追加した
- [ ] dependencies に lifecycle と recyclerview を追加した
- [ ] `kotlinOptions { jvmTarget = "11" }` を追加した

確認ポイント:

- Room compiler は `implementation` ではなく `ksp` に入っている

---

## 3. Roomファイル作成

### 3-1. TaskEntity.kt

- [ ] `@Entity(tableName = "tasks")` を付けた
- [ ] `@PrimaryKey(autoGenerate = true)` の `id: Long` を定義した
- [ ] title / description / dueDateEpochMillis / priority / isCompleted / createdAtEpochMillis / updatedAtEpochMillis を定義した

### 3-2. TaskDao.kt

- [ ] `observeAll(): Flow<List<TaskEntity>>` を定義した
- [ ] `@Insert` を定義した
- [ ] `@Update` を定義した
- [ ] `@Delete` を定義した

### 3-3. AppDatabase.kt

- [ ] `@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)` を付けた
- [ ] `abstract fun taskDao(): TaskDao` を定義した
- [ ] `getInstance(context)` を companion object に実装した

---

## 4. ビルド確認

実行コマンド:

```bash
./gradlew :app:assembleDebug
```

- [ ] コマンドを実行した
- [ ] ビルドが成功した

失敗した場合:

- [ ] エラーログ先頭の原因を確認した
- [ ] 依存関係（plugins / dependencies）を再確認した
- [ ] package名とimportを再確認した

---

## 5. ふりかえり（3分）

### 今日できたこと

-
-
-

### 詰まったこと

-
-
-

### 明日（Day 2）でやること

-
-
- ***

## 6. トラブル記録欄

### 症状

### 原因

### 解決方法

### 次回の予防策
