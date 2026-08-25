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

- [x] 作業前に現在の変更状態を確認した
- [x] 今日触るファイルを確認した

今日触る予定のファイル:

- gradle.properties
- build.gradle.kts（ルート）
- gradle/libs.versions.toml
- app/build.gradle.kts
- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskEntity.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/TaskDao.kt
- app/src/main/java/com/example/myfirsttoolapp/data/local/AppDatabase.kt

---

## 2. 依存関係の追加

### 2-0. AGP 9対応（gradle.properties / ルートbuild.gradle.kts）

- [x] gradle.propertiesに `android.builtInKotlin=false` を追加した
- [x] gradle.propertiesに `android.newDsl=false` を追加した
- [x] ルートのbuild.gradle.ktsに `kotlin-android` と `ksp` を `apply false` で追加した

確認ポイント:

- AGP 9.0以降は「組み込みKotlin」と「新DSL」がデフォルト有効なため、上記を設定しないと `kotlin-android` プラグインの適用でエラーになる

### 2-1. libs.versions.toml

- [x] `kotlin` バージョンを追加した
- [x] `ksp` バージョンを追加した
- [x] `room` バージョンを追加した
- [x] `lifecycle` バージョンを追加した
- [x] `recyclerview` バージョンを追加した
- [x] Room / Lifecycle / RecyclerView の `libraries` 定義を追加した
- [x] `plugins` に `kotlin-android` と `ksp` を追加した

### 2-2. app/build.gradle.kts

- [x] plugins に `alias(libs.plugins.kotlin.android)` を追加した
- [x] plugins に `alias(libs.plugins.ksp)` を追加した
- [x] dependencies に `room-runtime` を追加した
- [x] dependencies に `room-ktx` を追加した
- [x] dependencies に `ksp(room-compiler)` を追加した
- [x] dependencies に lifecycle と recyclerview を追加した
- [x] `kotlinOptions { jvmTarget = "11" }` を追加した

確認ポイント:

- Room compiler は `implementation` ではなく `ksp` に入っている

---

## 3. Roomファイル作成

### 3-1. TaskEntity.kt

- [x] `@Entity(tableName = "tasks")` を付けた
- [x] `@PrimaryKey(autoGenerate = true)` の `id: Long` を定義した
- [x] title / description / dueDateEpochMillis / priority / isCompleted / createdAtEpochMillis / updatedAtEpochMillis を定義した

### 3-2. TaskDao.kt

- [x] `observeAll(): Flow<List<TaskEntity>>` を定義した
- [x] `@Insert` を定義した
- [x] `@Update` を定義した
- [x] `@Delete` を定義した

### 3-3. AppDatabase.kt

- [x] `@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)` を付けた
- [x] `abstract fun taskDao(): TaskDao` を定義した
- [x] `getInstance(context)` を companion object に実装した

---

## 4. ビルド確認

実行コマンド:

```bash
./gradlew :app:assembleDebug
```

- [x] コマンドを実行した
- [x] ビルドが成功した

失敗した場合:

- [ ] エラーログ先頭の原因を確認した
- [ ] 依存関係（plugins / dependencies）を再確認した
- [ ] package名とimportを再確認した
- [ ] gradle.propertiesの `android.builtInKotlin=false` / `android.newDsl=false` を確認した（AGP 9以降）

---

## 5. ふりかえり（3分）

### 今日できたこと

- ビルドするところまでできた
-
-

### 詰まったこと

- 教科書通りにやったのにビルドに失敗して、GitHub Copilotを使って原因を調査し修正してもらった
-
-

### 明日（Day 2）でやること

## 6. トラブル記録欄

### 症状

```
tadashi@development-server:~/MyFirstToolApp$ ./gradlew assembleDebug
Calculating task graph as configuration cache cannot be reused because file 'gradle/libs.versions.toml' has changed.

FAILURE: Build failed with an exception.

* Where:
Build file '/home/tadashi/MyFirstToolApp/app/build.gradle.kts' line: 1

* What went wrong:
Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '2.2.10']
> The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version, so compatibility cannot be checked.

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights from a Build Scan (powered by Develocity).
> Get more help at https://help.gradle.org.

BUILD FAILED in 608ms
tadashi@development-server:~/MyFirstToolApp$
```

### 原因

To Do リストを更新しました

Read , lines 1 to 40

Read

Replacing 4 lines with 6 lines in

```

```

ルートの `build.gradle.kts` に `kotlin-android` と `ksp` プラグインを `apply false` で宣言していなかったのが原因です(app 側だけで宣言するとバージョン解決の競合が起きる)。ルートに追加したので、再度ビルドを実行します。

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

https://developer.android.com/build/releases/gradle-plugin をフェッチしました

Read

2 リソースをフェッチしました

Read

原因が判明しました。AGP 9.0以降は「組み込みKotlinサポート」がデフォルトで有効になっており、`org.jetbrains.kotlin.android` プラグインを明示的に適用すると競合します。`kotlin-android`/`ksp` プラグインの明示適用をやめて、組み込みKotlinに任せる方針にします。

Replacing 20 lines with 14 lines in ,

```

```

```

```

jvmTarget は組み込みKotlinの場合 `compileOptions` の `targetCompatibility`(VERSION_11)から自動的に決まるので削除して問題ありません。再ビルドします。

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

Replacing 11 lines with 15 lines in , ,

```

```

```

```

```

```

To Do 項目が 3 件作成されました

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

Replacing 1 lines with 3 lines in

```

```

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

Read , lines 1 to 36

Replacing 7 lines with 11 lines in

```

```

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

Replacing 11 lines with 11 lines in

```

```

Ran terminal command: ./gradlew assembleDebug --no-configuration-cache

ビルドが成功しました。原因は AGP 9.3.2 の変更点が重なったことです。

1. AGP 9.0 以降、「組み込み Kotlin サポート」がデフォルトで有効になり、明示的に `org.jetbrains.kotlin.android` を適用すると競合する
2. 本プロジェクトの KSP バージョン (2.2.10-2.0.2) は組み込み Kotlin と併用すると `kotlin.sourceSets` 関連でエラーになる
3. Kotlin Gradle プラグイン 2.2.10 は AGP 9 の新しい DSL (`android.newDsl=true`) にまだ対応していない

**対応内容:**

- `gradle.properties` に `android.builtInKotlin=false` と `android.newDsl=false` を追加し、旧来の Kotlin プラグイン方式・旧 DSL に固定
- `build.gradle.kts` のルートで `kotlin-android` / `ksp` プラグインを `apply false` で宣言
- `build.gradle.kts` は元の `kotlinOptions { jvmTarget = "11" }` 構成のまま維持

注意点として、ビルドは成功していますが「これらの設定は AGP 10.0 で廃止予定」という警告が出ています。将来的には組み込み Kotlin + 新DSLへの移行(または KSP の更新)が必要になる見込みです。ひとまず現状のバージョン(Kotlin 2.2.10 / AGP 9.3.2 / KSP 2.2.10-2.0.2)の組み合わせでは今回の設定が安定動作します。

To Do リストを更新しました

変更を行いました。

### 解決方法

### 次回の予防策
