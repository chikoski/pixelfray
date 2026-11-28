# Pixelfray

`png-glitch` の WebAssembly コンポーネントである `png-glitch-component.wasm` と `wasm-component-kotlin` の既定 `AOT` バックエンドを組み合わせた Android カメラグリッチアプリです。カメラのライブ入力フレームに対して PNG 予測フィルタ破壊を含むグリッチエフェクトを適用し、プレビュー表示と PNG 静止画保存および MP4 動画録画を行います。

## セットアップと依存構成

リポジトリは `third_party/wasm-component-kotlin` と `third_party/png-glitch` の 2 つの Git サブモジュールを含みます。クローン時は `--recursive` を付けてサブモジュールも合わせて取得してください。すでに通常クローンした場合は `git submodule update --init --recursive` を実行します。

```bash
git clone --recursive https://github.com/chikoski/pixelfray.git
```

`settings.gradle.kts` は `third_party/wasm-component-kotlin` を Composite Build として取り込み、`io.github.wasm2kt` プラグインと `runtime-aot` を供給します。ローカルの別ディレクトリにある `wasm-component-kotlin` と並行開発する場合は、コマンドライン引数 `-Pwasm2ktDir=/path/to/wasm-component-kotlin` で参照先を上書きできます。

ビルド時は Gradle デーモン用に JDK 21 と Android SDK Platform 35 が必要です。`ANDROID_HOME` 環境変数または `local.properties` の `sdk.dir` に Android SDK のパスを設定してください。

## 構成

`app/src/main/wasm/png-glitch-component.wasm` をビルド時に分解して JVM バイトコードへ AOT コンパイルし、型安全な Kotlin バインディングを `info.chikoski.pixelfray.pngglitch` パッケージへ生成します。主なソースコードは次のファイルに分かれています。

* `GlitchEngine.kt`
* `FrameMaskCompositor.kt`
* `MediaPipeFrameSegmenter.kt`
* `CpuSelfieSegmenterGraph.kt`
* `RecipePresets.kt`
* `FrameConverter.kt`
* `RgbaFrameTransform.kt`
* `FrameExchange.kt`
* `FrameTrace.kt`
* `PreviewRenderer.kt`
* `Mp4VideoRecorder.kt`
* `MediaStoreHelper.kt`
* `GlitchViewModel.kt`
* `GlitchCameraScreen.kt`
* `MainActivity.kt`

`GlitchEngine.kt` は生成された `PngglitchComponent` のセッションと `chikoski:png-glitch/live` の `Session` リソースを管理します。WIT のエクスポート関数は非同期関数として定義されており、生成された `suspend fun` バインディングと `Mutex` を組み合わせてコルーチンから直列に呼び出します。初回フレームで `LiveSession.fromPixels` を呼び出し、2 フレーム目以降は `processFrame` を繰り返すことで PNG チャンク構築の負荷を抑えます。静止画シャッター押下時の全体パスでは `snapshot` を呼び出し、グリッチ状態の PNG バイト列を取得します。

カメラの各フレームは `FrameConverter.kt` と `RgbaFrameTransform.kt` により、回転、前面カメラのミラー、縮小を 1 回の走査でまとめて行ってから `GlitchEngine` に渡します。解像度の切り替えはグリッチ処理を行うサイズを選ぶもので、360p、540p、720p の 3 段階から選択でき、既定値は 540p です。カメラにはアスペクト比 16:9 で要求以上のサイズを要求し、長辺と短辺がそれぞれ 640×360、960×540、1280×720 に収まるように縮小します。拡大処理は行いません。

フレーム処理は変換段、エンジン段、表示段の 3 つが並行して進みます。変換段は CameraX の analyzer でフレームを変換して `ImageProxy` を直ちに閉じ、結果を `LatestFrameSlot` へ置きます。エンジン段は `LatestFrameSlot` から最新のフレームを取り出してグリッチ処理を待機し、処理後にバッファを `FrameBufferPool` へ返却します。表示段は専用スレッド `glitch-present` で結果を `Bitmap` にコピーし、Compose の `AndroidExternalSurface` の `Surface` へ `lockHardwareCanvas` で直接描画しつつ録画時はエンコーダーへ渡します。

`PreviewRenderer.kt` が Compose の再コンポーズを介さずに描画するため、プレビュー更新による UI ツリーの再評価は起きません。フレームの統計情報と FPS は `PreviewFrame` の `StateFlow` で独立して配信し、FPS 表示は整数値が変わったときのみ再コンポーズされます。

`RecipePresets.kt` はプリセットレシピと単一フィルタの YAML レシピ組み立てヘルパーを提供します。ピクセル値を直接扱うフィルタの前段には無圧縮の `Regenerate` ステップを挿入し、予測フィルタの差分値へ誤って作用しないようにしています。

`GlitchCameraScreen.kt` は Jetpack Compose で画面幅に応じたモバイル向け縦並びレイアウトとフォルダブル向け 2 ペインレイアウトを切り替えます。解像度、ズーム、前後カメラ、プリセットと単一フィルタの一覧、パラメータスライダー、設定ボトムシートでの PNG 予測フィルタ戦略および `SeedMode` 切り替え、静止画保存と MP4 録画ボタンを提供します。保存された静止画は `Pictures/Pixelfray` に `PIXELFRAY_` 接頭辞付きの PNG として、録画された動画は `Movies/Pixelfray` に `PIXELFRAY_` 接頭辞付きの MP4 として出力されます。

PNG セッションのエンコード色空間は `ColorType.RGB` です。入力のアルファチャンネルを破棄して RGB の 3 バイトでスキャンラインを構成し、出力時にアルファ値を不透明へ固定することで半透明の欠けを防ぎます。PNG 予測フィルタ戦略の既定値 `PngFilterOption.DEFAULT` は Paeth です。差分フィルタによりバイト置換やスキャンラインシフトの効果が周囲の画素へ伝播する一方、None に比べて 1 フレームあたりの処理時間が増加するトレードオフがあります。

### 4 パスの動的切り替えとマスク合成

プレビュー右上の対象セレクターにより、エフェクトの適用対象を次の 4 パスから動的に切り替えられます。

* 全体
* 前景
* 背景
* なし

アプリ起動時の初期値は全体です。全体パスではセグメンテーションを実行せず、グリッチ処理のみを行います。なしパスではセグメンテーションとグリッチ処理の両方を省略し、変換済みの元フレームをそのまま表示へ渡します。

前景パスと背景パスでは、MediaPipe Tasks Vision を基盤とした `CpuSelfieSegmenterGraph.kt` と `MediaPipeFrameSegmenter.kt` により被写体と背景を分離します。Selfie Segmenter のモデルファイルはリポジトリへ同梱せず、`app/build.gradle.kts` の `downloadSelfieSegmenterModel` タスクがビルド時に公式配布元から取得し、SHA-256 チェックサムを検証した上で生成アセットへ組み込みます。

前景パスと背景パスの処理では、Wasm コンポーネントによるグリッチ処理とセグメンテーション推論を並列に実行します。推論で得られた前景信頼度は `FrameMaskCompositor.kt` で 0 から 255 の整数重みへ変換し、元フレームとグリッチ済みフレームの RGB 各チャンネルをホスト側で線形補間します。

動画録画では表示用の最終画像をそのままエンコーダーへ渡すため、すべてのパスで表示内容と一致した映像が記録されます。静止画保存では、全体パスは既存の `snapshot` を使い、前景、背景、なしの 3 パスでは最終合成後の RGBA フレームから PNG バイト列を生成します。

`com.google.mediapipe:tasks-vision:0.10.21` は `arm64-v8a`、`armeabi-v7a`、`x86` 向けのネイティブライブラリのみを同梱しているため、`x86_64` エミュレーターではセグメンテーションの初期化に失敗します。失敗時は画面へ通知を表示した上で全体パスと同等の処理へフォールバックします。また、4 パスの動的切り替えとセグメンテーション連携について、実機およびエミュレーター上のライブカメラ動作は未確認です。

## WebAssembly コンポーネントの更新手順

サブモジュール `third_party/png-glitch` から `png-glitch-component.wasm` を再ビルドする場合は、付属のスクリプトを実行します。環境変数 `PNG_GLITCH_DIR` で `png-glitch` のクローン先パスを上書きすることもできます。

```bash
./build-wasm.sh
```

## ビルドとテスト

リポジトリのルートから以下のコマンドを実行します。

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew -PgenerateBaselineProfileOnBuild=false :app:assembleRelease
./gradlew :app:assembleDebugAndroidTest
```

`testDebugUnitTest` はホスト JVM 上でユニットテストを実行し、4 パスのマスク合成、全プリセットと全フィルタの `validateRecipe` 検証、および複数フレーム処理と録画と保存の異常系の動作を検証します。初回ビルド時は `selfie_segmenter.tflite` を取得するためネットワーク接続が必要です。

## フレーム処理の計測

各段の処理は `FrameTrace.kt` の名前でトレース区間に囲んでいます。エンジン段の区間は `suspend fun` の呼び出しをまたいでスレッドが変わることがあるため非同期区間としており、API 29 以降で記録されます。Perfetto や Android Studio のプロファイラーで `glitch.convert`、`glitch.analyze`、`glitch.processFrame`、`glitch.present`、`glitch.copyToBitmap`、`glitch.draw`、`glitch.encode` などの内訳を確認できます。

`:baselineprofile` モジュールの `FramePipelineBenchmark` は、カメラの代わりに 1280×720 の RGBA を 90 度回転する合成フレームを 120 フレーム流し、360p、540p、720p の各解像度で各区間の平均時間を計測します。カメラのないエミュレーターでも動作します。アプリを `info.chikoski.pixelfray.SYNTHETIC_FRAMES` と `info.chikoski.pixelfray.SYNTHETIC_RESOLUTION` の extra 付きで起動すると合成フレームが流れ、完了後に `Synthetic frames done` を表示します。

```bash
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=info.chikoski.pixelfray.baselineprofile.FramePipelineBenchmark
```

合成フレームは PNG 予測フィルタ戦略の既定値である Paeth で処理します。`glitch.syntheticRun` はエンジン段で 120 フレームを処理し終えるまでの時間であり、120 をこの時間で割るとエンジンのスループットとして FPS が求まります。`glitch.presentedFrames` は実際に表示したフレーム数であり、120 より少なければ表示段が律速していることを示します。

## ベースラインプロファイルの生成

`:baselineprofile` モジュールと `androidx.baselineprofile` プラグインを使って、アプリ起動と `png-glitch-component` のホットパスを含むベースラインプロファイルを生成します。`automaticGenerationDuringBuild = true` を設定しているため、端末やエミュレーターを接続した状態で `:app:assembleRelease` を実行すると自動収集が走ります。未接続でリリースビルドを作る場合は `-PgenerateBaselineProfileOnBuild=false` を指定してください。

```bash
./gradlew :app:generateBaselineProfile
```

`app/src/release/generated/baselineProfiles/` 配下の既存プロファイルは、リポジトリ移行時に旧パッケージ記述を `info.chikoski.pixelfray` へ機械的に書き換えて引き継いでいます。実機またはエミュレーターを接続できる環境では上記コマンドで再生成してください。

## ライセンス

[LICENSE](LICENSE) を参照してください。
