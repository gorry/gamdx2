/**
 *
 */
package net.gorry.gamdx;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author gorry
 *
 */
public class DoMain {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "DoMain";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private final ActivityMain me;

	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	DoMain(final ActivityMain a) {
		if (T) Log.v(TAG, M()+"@in");

		me = a;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 完全終了
	 */
	public void doQuit() {
		if (T) Log.v(TAG, M()+"@in");

		ActivityMain.mShutdownServiceOnDestroy = true;
		me.finish();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 設定へ遷移
	 */
	public void doSetting() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent(
				me,
				ActivitySetting.class
		);
		final boolean isLandscape = Setting.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_SETTING);

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * バージョン表示
	 */
	public void doShowVersion() {
		if (T) Log.v(TAG, M()+"@in");

		String versionName = null;
		final PackageManager pm = me.getPackageManager();
		final String pkgname = me.getPackageName();
		try {
			PackageInfo info = null;
			info = pm.getPackageInfo(pkgname, 0);
			versionName = info.versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}
		final AlertDialog.Builder bldr = new AlertDialog.Builder(me);
		bldr.setTitle(me.getString(R.string.app_title));
		bldr.setMessage("Version " + versionName + "\n" + me.getString(R.string.copyright))
		.setIcon(R.drawable.icon);
		bldr.create().show();

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * ファイルを開く
	 */
	public void doSelectMdxFile() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent(
			me,
			ActivitySelectMdxFile.class
		);
		String uristr = null;
		try {
			uristr = ActivityMain.iMusicPlayerService.getLastSelectedFileName();
		} catch (final RemoteException e) {
			uristr = null;
		}
		if ((uristr == null) || (uristr.length() == 0)) {
			uristr = ActivitySelectMdxFile.getStringFromUri(Setting.mdxRootUri);
		}

		final Uri uri = ActivitySelectMdxFile.getUriFromString(uristr);
		intent.setData(uri);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_SELECT_MDX);

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 演奏開始＆一時停止
	 */
	public void doPlayMusicButton() {
		if (T) Log.v(TAG, M()+"@in");

		if (V) Log.v(TAG, "doPlayMusicButton()");
		try {
			final boolean p1 = ActivityMain.iMusicPlayerService.getPlay();
			final boolean p2 = ActivityMain.iMusicPlayerService.getPause();
			if (p1 && p2) {
				ActivityMain.iMusicPlayerService.setPause(false);
			} else {
				ActivityMain.iMusicPlayerService.setPause(false);
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 演奏停止
	 */
	public void doStopMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			ActivityMain.iMusicPlayerService.setPlay(false);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 次の曲
	 */
	public void doNextMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			int ret;
			ret = ActivityMain.iMusicPlayerService.setPlayNumberNext();
			if (ret != 0) {
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 次の曲
	 */
	public void doPrevMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			int ret;
			ret = ActivityMain.iMusicPlayerService.setPlayNumberPrev();
			if (ret != 0) {
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ヘルプの表示
	 */
	public void doShowHelp() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://gorry.hauN.org/android/gamdx/help/"));
		me.startActivity(intent);

		if (T) Log.v(TAG, M()+"@out");
	}


}

// [EOF]
