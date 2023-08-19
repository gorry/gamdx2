package net.gorry.gamdx;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author GORRY
 *
 */
public class MyAlarmManager extends BroadcastReceiver {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "AlarmManagerReceiver";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static final boolean DISABLE = true;

	private static AlarmManager mAlarmManager = null;
	private static Runnable mRunnable;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (D) Log.d(TAG, "onReceive()");
		if (mRunnable != null) {
			try {
				mRunnable.run();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * アラーム設定
	 * @param context context
	 * @param run run
	 */
	@SuppressWarnings("unused")
	public static void setAlarmManager(final Context context, final Runnable run) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", run="+run);

		if (DISABLE) {
			resetAlarmManager(context);
			if (T) Log.v(TAG, M()+"@out: DISABLED");
			return;
		}
		if (mAlarmManager != null) {
			if (T) Log.v(TAG, M()+"@out: already set alarm");
			return;
		}

		mRunnable = run;
		final Intent intent = new Intent(context, MyAlarmManager.class);
		final PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		long firstTime = SystemClock.elapsedRealtime();
		firstTime += 60 * 1000;
		mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 60 * 1000, sender);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * アラーム解除
	 * @param context context
	 */
	public static void resetAlarmManager(final Context context) {
		if (T) Log.v(TAG, M()+"@in: context="+context);

		// if (mAlarmManager != null) {
		final Intent intent = new Intent(context, MyAlarmManager.class);
		final PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		mAlarmManager = null;
		if (D) Log.d(TAG, "resetAlarmManager(): reset alarm");
		// }

		if (T) Log.v(TAG, M()+"@out");
	}

}

// [EOF]
