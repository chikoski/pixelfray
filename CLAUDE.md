# CLAUDE.md

このファイルは、コーディングエージェントが `pixelfray` リポジトリで作業する際の作業メモです。人間向けの利用手順とアプリ構成は [README.md](README.md) にまとめています。エージェントの運用方針とレビュー体制は [AGENTS.md](AGENTS.md) を参照してください。

## プロジェクト

Pixelfray は Android 向けのカメラグリッチアプリです。カメラのライブ入力フレームに対して PNG 予測フィルタ破壊を含むグリッチエフェクトを適用し、プレビュー表示と PNG 静止画保存および MP4 動画録画を行います。

グリッチ処理は `png-glitch-component.wasm` を `wasm-component-kotlin` の Chicory バックエンド上で実行して実現します。前景と背景の分離には MediaPipe Tasks Vision の `ImageSegmenter` を使います。

## モジュール構成

リポジトリは次のモジュールと外部サブモジュールで構成します。

* `:app`
* `:baselineprofile`
* `third_party/wasm-component-kotlin`
* `third_party/png-glitch`

`:app` は Jetpack Compose の UI、3 段フレームパイプライン、Wasm セッション管理、MediaPipe セグメンテーション、および MP4 録画を含む本体モジュールです。`:baselineprofile` はベースラインプロファイル生成と合成フレームによるベンチマーク計測を担当します。

`third_party/wasm-component-kotlin` は Git サブモジュールであり、`settings.gradle.kts` の `includeBuild` により `io.github.wasm2kt` プラグインと `runtime-chicory` を提供します。`third_party/png-glitch` は Wasm コンポーネントのソースリポジトリであり、`app/src/main/wasm/png-glitch-component.wasm` を再生成するときに使用します。

## コマンド

通常のビルド、ユニットテスト、プロファイル生成、および Wasm 再ビルドには次のコマンドを使用します。

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew -PgenerateBaselineProfileOnBuild=false :app:assembleRelease
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:generateBaselineProfile
./build-wasm.sh
```

`./gradlew :app:testDebugUnitTest` はホスト JVM 上でユニットテストを実行します。リリースビルド時に端末やエミュレーターを接続しない場合は、`-PgenerateBaselineProfileOnBuild=false` を付けてベースラインプロファイル自動収集を無効にします。

ローカルの別チェックアウトにある `wasm-component-kotlin` と並行開発する場合は、`-Pwasm2ktDir=/path/to/wasm-component-kotlin` を指定してサブモジュールの参照先を切り替えられます。`third_party/png-glitch` を更新した場合は `./build-wasm.sh` を実行し、`app/src/main/wasm/png-glitch-component.wasm` を再生成してコミットしてください。

ベンチマークを計測する場合は接続済み端末またはエミュレーター上で次のコマンドを実行します。

```bash
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=info.chikoski.pixelfray.baselineprofile.FramePipelineBenchmark
```

## ビルド環境の前提

Gradle デーモンは `gradle/gradle-daemon-jvm.properties` の指定により JDK 21 で動作します。Gradle が JDK 21 を自動検出できない環境では、`~/.gradle/gradle.properties` またはコマンドライン引数で `org.gradle.java.installations.paths` に JDK 21 のパスを指定します。たとえば macOS の Homebrew 環境では `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` を指定します。ビルドには Android SDK Platform 35 が必要であり、`ANDROID_HOME` 環境変数または `local.properties` の `sdk.dir` でパスを指定します。

初回ビルド時は `app/build.gradle.kts` の `downloadSelfieSegmenterModel` タスクが公式配布元から `selfie_segmenter.tflite` を取得し、SHA-256 を検証して生成アセットへ組み込みます。そのため初回のみネットワーク接続が必要です。

CI の [.github/workflows/ci.yml](.github/workflows/ci.yml) は `android` の 1 ジョブで構成されています。`submodules: recursive` でチェックアウトし、JDK 21 上でユニットテスト、release APK ビルド、および androidTest APK ビルドを検証します。

## 守ること

`GlitchEngine` における Wasm セッションへの操作は、すべて `Mutex` で直列化してください。`close` の呼び出し時は `closed` フラグを立てた上で `tryLock` を試し、使用中であればバックグラウンドで解放を待つことでメインスレッドの停止を防ぎます。セッションの色空間は `ColorType.RGB` を維持し、スキャンライン破壊による半透明化を防いでください。

3 段フレームパイプラインではバッファと描画先の所有権を崩してはなりません。変換段は `FrameBufferPool` と `LatestFrameSlot` を経由してフレームを渡し、直ちに `ImageProxy` を閉じます。エンジン段は処理完了後の `finally` で入力バッファを `framePool` へ返却します。表示段は単一スレッド `glitch-present` 上で `previewBitmap` を専有し、`PreviewRenderer.detach` のロックによって破棄後の `Surface` へ描画しないことを保証してください。

`CpuSelfieSegmenterGraph.kt` は `com.google.mediapipe.tasks.core` パッケージのまま維持する必要があります。MediaPipe のパッケージプライベートな API へリフレクションなしでアクセスし、`TensorsToSegmentationCalculator` を純 CPU の `TensorsToFloatsCalculator` へ書き換えて動作させるためです。

`Mp4VideoRecorder` と `MediaStoreHelper` ではリソース解放と一時ファイル削除の規約を守ってください。0 フレームで終了する場合は `signalEndOfInputStream` を呼ばず、EOS 待機は `MAX_EOS_POLL_ATTEMPTS` で上限を設けます。初期化失敗、終了処理失敗、または MediaStore 書き込み失敗時には保留エントリと一時ファイルを確実に削除してください。

`app/proguard-rules.pro` にある MediaPipe、Protobuf、および Flogger 向けの R8 保持ルールを不用意に削らないでください。テストのスキップや無効化、アサーションの緩和は禁止します。

コミットメッセージは Conventional Commits 形式に従い、本文に変更理由を記載します。

## PR 作成後の CI モニタリング

PR を作成または更新した直後は、`gh pr checks` の `--watch` オプションを用いて CI の完了まで自動的にモニターします。対象となる CI ジョブは `android` です。

失敗したジョブがある場合は `gh run view --log-failed` で原因を特定します。修正コミットをプッシュした上で、再度チェックが通過するまで監視を続けます。テストのスキップや無効化、アサーションの緩和でチェックを通過させてはなりません。
