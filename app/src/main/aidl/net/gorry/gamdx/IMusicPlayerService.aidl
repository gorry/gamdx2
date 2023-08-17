/**
 *
 * GAMDX - Android MDX Player
 *
 * Copyright (C)2011 GORRY.
 *
 */

package net.gorry.gamdx;

/**
 *
 * MXDRVサービス インターフェース
 *
 * @author GORRY
 *
 */
interface IMusicPlayerService {
	/**
	 * IRCサービスのシャットダウン
	 */
	void shutdown();

	/**
	 * 設定のリロード
	 */
	void reloadSetting();

	/**
	 * Pingを受けたときに返すPong
	 */
	void receivePong();

	/**
	 * "Low Memory"通知の消去
	 */
	void clearLowMemoryNotification();

	/**
	 * バージョンの取得
	 */
	String getVersionString();

	/**
	 * プレイリストの設置
	 */
	int setPlayList(in String[] playList);

	/**
	 * フォルダをプレイリストとして設置
	 */
 /*
	int setPlayListAsFolder(String folder);
*/

	/**
	 * プレイリストの読み出し
	 */
	String[] getPlayList();

	/**
	 * プレイリスト番号を設定
	 */
	int setPlayNumber(int n);

	/**
	 * プレイリスト番号を次の曲へ設定
	 */
	int setPlayNumberNext();

	/**
	 * プレイリスト番号を前の曲へ設定
	 */
	int setPlayNumberPrev();

	/**
	 * 演奏中のプレイリスト番号を取得
	 */
	int getPlayNumber();

	/**
	 * 指定曲の演奏時間(ms)を取得
	 */
	int getDuration(int n);

	/**
	 * 指定曲のタイトルを取得
	 */
	String getTitle(int n);

	/**
	 * 現在曲の演奏位置(ms)を設定
	 */
	void setPlayAt(int playat, int loop, int fadeout);

	/**
	 * 現在曲の演奏位置(ms)を取得
	 */
	int getPlayAt();

	/**
	 * 現在曲の演奏時間を取得
	 */
	int getCurrentDuration();

	/**
	 * 現在曲のタイトルを取得
	 */
	String getCurrentTitle();

	/**
	 * 演奏を開始/終了
	 */
	int setPlay(boolean f);

	/**
	 * 演奏中かどうかを取得
	 */
	boolean getPlay();

	/**
	 * ポーズ/ポーズ解除
	 */
	int setPause(boolean f);

	/**
	 * ポーズ中かどうかを取得
	 */
	boolean getPause();

	/**
	 * 音楽ファイルの演奏
	 */
	int playMusicFile(String uristr);

	/**
	 * ファイルタイプの取得
	 */
	String getCurrentFileType();

	/**
	 * ファイル名の取得
	 */
	String getCurrentFileName(int id);

	/**
	 * 最後に選択したファイル名の設定
	 */
	void setLastSelectedFileName(String filename);

	/**
	 * 最後に選択したファイル名の取得
	 */
	String getLastSelectedFileName();

	/**
	 * 状態のセーブ
	 */
	void savePlayerStatus();

}
