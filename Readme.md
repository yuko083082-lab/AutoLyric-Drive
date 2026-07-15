# AutoLyric-Drive

車載ディスプレイ（Android AIbox等）向け、音響解析同期型歌詞表示アプリ。

## 概要
車内のスピーカーから流れる音楽をマイクでキャプチャし、リアルタイムで楽曲を特定、同期歌詞（LRC）を最前面オーバーレイに表示します。既存の音楽プレーヤーの仕様に依存せず、あらゆる音源に対して歌詞同期を提供します。

## 主な機能
- **リアルタイム楽曲識別**: ACRCloud SDK を使用し、周囲の音から楽曲タイトル・アーティスト・現在の再生位置（オフセット）を特定。
- **同期歌詞の取得**: LRCLIB API を介して、タイムタグ付きの LRC 形式歌詞を自動取得。
- **最前面オーバーレイ表示**: `WindowManager` を使用し、ナビアプリ等の上に透過型の歌詞ビューを表示。
- **スマート同期**: 楽曲の再生位置に合わせて `LyricViewX` がミリ秒単位で歌詞を自動スクロール。
- **権限管理**: マイク、オーバーレイ、バッテリー最適化除外の権限設定をスムーズに行う UI を提供。

## 技術スタック
- **UI**: Jetpack Compose
- **Language**: Kotlin
- **Architecture**: MVVM + Coroutines Flow
- **Libraries**:
    - [LyricViewX](https://github.com/Moriafly/LyricViewX) (歌詞表示・同期)
    - [ACRCloud Android SDK](https://github.com/acrcloud/acrcloud-sdk-android-java) (楽曲識別)
    - [Ktor](https://ktor.io/) (ネットワーク通信)
- **API**: [LRCLIB](https://lrclib.net/) (歌詞データ取得)

## プロジェクト構造
- `RecognitionService.kt`: マイク録音と識別ループを実行するフォアグラウンドサービス。
- `LyricsRepository.kt`: LRCLIB から歌詞を検索・取得するデータ層。
- `LyricsViewModel.kt`: 楽曲イベントを受け取り、歌詞の状態を管理する。
- `LyricsOverlayView.kt`: `WindowManager` を介して Compose UI と `LyricViewX` を最前面に表示。
- `MusicDataBus.kt`: サービスと UI を繋ぐシングルトンなイベントバス。
- `PermissionManager.kt`: Android 13+ に対応した権限管理クラス。

## セットアップ
1. `local.properties` に ACRCloud の API キーを設定します。
   ```properties
   ACR_HOST=your_host
   ACR_ACCESS_KEY=your_access_key
   ACR_ACCESS_SECRET=your_access_secret
   ```
2. アプリを起動し、「マイク」「オーバーレイ」「バッテリー最適化」の各権限を許可します。
3. 「サービス開始」をタップすると識別が始まります。

## ライセンス
このプロジェクトは、提供された技術スタックに基づき構築されています。各ライブラリのライセンスに従って使用してください。
