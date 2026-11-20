## 変更の概要

このプルリクエストで解決する課題と変更内容の要点を記載します。

## 関連 Issue

関連する Issue を `Closes #番号` または `Refs #番号` の形式で記載します。

Closes #

## 更新確認チェックリスト

変更内容に応じて必要なドキュメントと記録を更新したことを以下の項目で確認します。設計や仕様に関わる変更では `README.md` や `docs/` の該当箇所を更新します。重要な設計判断を行った場合は `docs/adr/` に新しい ADR を追加します。

- [ ] `README.md` と `docs/` の確認
- [ ] `docs/adr/` への ADR 追加
- [ ] `third_party/png-glitch` 更新時の `./build-wasm.sh` 実行

## テスト確認チェックリスト

変更した箇所や境界に応じて必要なテストを実行し、通過したことを確認します。`./gradlew :app:testDebugUnitTest` はホスト JVM のユニットテストを実行します。`./gradlew -PgenerateBaselineProfileOnBuild=false :app:assembleRelease` は R8 縮小を含むリリースビルドを検証します。`./gradlew :app:assembleDebugAndroidTest` は計装テスト用 APK のビルドを確認します。

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew -PgenerateBaselineProfileOnBuild=false :app:assembleRelease`
- [ ] `./gradlew :app:assembleDebugAndroidTest`
