/**
 *
 */
package net.gorry.gamdx;

import java.util.EventListener;

/**
 * @author gorry
 *
 */
public interface MusicPlayerEventListener extends EventListener {
	/**
	 * 演奏終了リスナ
	 */
	public void endPlay();

	/**
	 * タイマーイベントリスナ
	 * @param playAt 時刻
	 */
	public void timerEvent(int playAt);

	/**
	 * 曲ファイル読み込み完了リスナ
	 */
	public void acceptMusicFile();


}
