/**
 *
 */
package net.gorry.gamdx;

import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.util.Log;


/**
 * @author GORRY
 *
 */

public abstract class ForegroundService extends Service {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "ForegroundService";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static final Class<?>[] mStartForegroundSignature =
		new Class[] { int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private final int mNotificationId;

	private NotificationManager mNotificationManager;
	private Method mStartForeground;
	private Method mStopForeground;
	private final Object[] mStartForegroundArgs = new Object[2];
	private final Object[] mStopForegroundArgs = new Object[1];

	/**
	 * @param id ID
	 */
	public ForegroundService(final int id) {
		if (T) Log.v(TAG, M()+"@in: id="+id);

		mNotificationId = id;

		if (T) Log.v(TAG, M()+"@out");
	}

	protected NotificationManager getNotificationManager() {
		if (T) Log.v(TAG, M()+"@in");

		if (T) Log.v(TAG, M()+"@out: mNotificationManager="+mNotificationManager);
		return mNotificationManager;
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older APIs if it is not
	 * available.
	 */
	protected void startForegroundCompat(final Notification notification) {
		if (T) Log.v(TAG, M()+"@in: notification="+notification);

		if (mStartForeground != null) {
			if (V) Log.v(TAG, "startForegroundCompat(): mStartForeground != null");
			mStartForegroundArgs[0] = Integer.valueOf(mNotificationId);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return;
		}

		if (V) Log.v(TAG, M()+"notify!");
		// setForeground(true);
		mNotificationManager.notify(mNotificationId, notification);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older APIs if it is not
	 * available.
	 */
	protected void stopForegroundCompat() {
		if (T) Log.v(TAG, M()+"@in");

		if (mStopForeground != null) {
			if (V) Log.v(TAG, "stopForegroundCompat(): mStopForeground != null");
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return;
		}

		if (V) Log.v(TAG, "stopForegroundCompat(): mStopForeground == null");
		mNotificationManager.cancel(mNotificationId);
		// setForeground(false);

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onCreate() {
		if (T) Log.v(TAG, M()+"@in");

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
		} catch (final NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		if (mStartForeground != null) {
			if (V) Log.v(TAG, "mStartForeground() != null");
		} else {
			if (V) Log.v(TAG, "mStartForeground() == null");
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");

		stopForegroundCompat();

		if (T) Log.v(TAG, M()+"@out");
	}
}

// [EOF]
