/**
 *
 */
package net.gorry.gamdx;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 * 
 * インターバルタイマ
 * 
 * @author gorry
 *
 */
public class IntervalTimer {
	private static final String TAG = "IntervalTimer";
	private static final boolean V = false;

	private boolean mTimerStop = false;
	private static final long mTimerStep = 100;
	private long mTimerCount = 0;
	private long mTimerNext = 0;
	private static final int TIMER = 1;

	/**
	 * タイマハンドラ
	 */
	public final Handler mTimerHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == TIMER) {
				while (SystemClock.uptimeMillis() > mTimerNext) {
					mTimerNext += mTimerStep;
					mTimerCount++;
					onTimer(mTimerCount);
				}
				if (mTimerStop == false) {
					final Message msg2 = mTimerHandler.obtainMessage(TIMER);
					sendMessageAtTime(msg2, mTimerNext);
				}
			}
		}
	};

	/**
	 * タイマ開始
	 */
	public void startTimer() {
		if (V) Log.v(TAG, "startTimer()");
		mTimerStop = false;
		mTimerNext = SystemClock.uptimeMillis();
		final Message msg = mTimerHandler.obtainMessage(TIMER);
		mTimerHandler.sendMessageAtTime(msg, mTimerNext+mTimerStep);
	}

	/**
	 * タイマ停止
	 */
	public void stopTimer() {
		if (V) Log.v(TAG, "stopTimer()");
		mTimerStop = true;
	}

	/**
	 * タイマ値の取得
	 * @return タイマ値
	 */
	public long getTimerCount() {
		if (V) Log.v(TAG, "getTimerCount()");
		return mTimerCount;
	}

	/**
	 * タイマ割り込み
	 * @param timerCount タイマ値
	 */
	public void onTimer(final long timerCount) {
		if (V) Log.v(TAG, "onTimer()");
		//
	}

}
