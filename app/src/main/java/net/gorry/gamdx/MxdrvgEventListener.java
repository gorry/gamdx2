/**
 *
 */
package net.gorry.gamdx;

import java.util.EventListener;

/**
 * @author gorry
 *
 */
public interface MxdrvgEventListener extends EventListener {
	/**
	 * 演奏終了リスナ
	 */
	public void endPlay();

	/**
	 * タイマーイベントリスナ
	 * @param time 時刻
	 */
	public void timerEvent(int time);


}
