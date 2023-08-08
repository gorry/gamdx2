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
	private static final String TAG = "AlarmManagerReceiver";
	//private static final boolean V = false;
	private static final boolean D = false;

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
		if (D) Log.d(TAG, "setAlarmManager()");
		if (DISABLE) {
			resetAlarmManager(context);
			return;
		}
		if (mAlarmManager == null) {
			mRunnable = run;
			final Intent intent = new Intent(context, MyAlarmManager.class);
			final PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
			long firstTime = SystemClock.elapsedRealtime();
			firstTime += 60 * 1000;
			mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 60 * 1000, sender);
			if (D) Log.d(TAG, "setAlarmManager(): set alarm");
		}
	}

	/**
	 * アラーム解除
	 * @param context context
	 */
	public static void resetAlarmManager(final Context context) {
		if (D) Log.d(TAG, "resetAlarmManager()");
		// if (mAlarmManager != null) {
		final Intent intent = new Intent(context, MyAlarmManager.class);
		final PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		mAlarmManager = null;
		if (D) Log.d(TAG, "resetAlarmManager(): reset alarm");
		// }

	}

}
