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
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "IntervalTimer";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

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
			// if (T) Log.v(TAG, M()+"@in: msg="+msg);

			if (msg.what == TIMER) {
				while (SystemClock.uptimeMillis() > mTimerNext) {
					if (mTimerStop) break;
					mTimerNext += mTimerStep;
					mTimerCount++;
					onTimer(mTimerCount);
				}
				if (mTimerStop == false) {
					final Message msg2 = mTimerHandler.obtainMessage(TIMER);
					sendMessageAtTime(msg2, mTimerNext);
				}
			}

			// if (T) Log.v(TAG, M()+"@out");
		}
	};

	/**
	 * タイマ開始
	 */
	public void startTimer() {
		if (T) Log.v(TAG, M()+"@in");

		mTimerStop = false;
		mTimerNext = SystemClock.uptimeMillis();
		final Message msg = mTimerHandler.obtainMessage(TIMER);
		mTimerHandler.sendMessageAtTime(msg, mTimerNext+mTimerStep);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * タイマ停止
	 */
	public void stopTimer() {
		if (T) Log.v(TAG, M()+"@in");

		mTimerStop = true;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * タイマ値の取得
	 * @return タイマ値
	 */
	public long getTimerCount() {
		if (T) Log.v(TAG, M()+"@in");

		if (T) Log.v(TAG, M()+"@out: mTimerCount="+mTimerCount);
		return mTimerCount;
	}

	/**
	 * タイマ割り込み
	 * @param timerCount タイマ値
	 */
	public void onTimer(final long timerCount) {
		if (T) Log.v(TAG, M()+"@in: timerCount="+timerCount);

		if (T) Log.v(TAG, M()+"@out");
		//
	}

}
